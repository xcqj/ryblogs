#!/usr/bin/env bash
# =============================================================================
# Stack Depth 博客 · 部署入口（S29.1）
#
#   ./deploy.sh up        发布/更新到 .env 指定的版本（pull → 记录摘要 → 起栈 → 冒烟）
#   ./deploy.sh rollback  回上一组镜像（按 .state/previous 里的 digest）
#   ./deploy.sh adopt     接管当前已在跑的容器，只记录摘要、不重启
#   ./deploy.sh down      停止并移除容器（保留数据与 .state）
#   ./deploy.sh seed [文件]  装载演示种子（默认 seed-demo.sql）并重建 ES 索引
#   ./deploy.sh backup    导出数据库 + 打包对象存储
#   ./deploy.sh restore <备份目录> [--yes]
#   ./deploy.sh status    容器 / 摘要 / 磁盘 / 最近备份
#   ./deploy.sh gc [--apply]  按前缀清理旧镜像（默认 dry-run）
#
# 设计红线：
#   1) 部署机只 pull，永不 build（build 覆盖层 compose.build.yml 不进部署包）
#   2) 回滚按 digest，不按 tag
#   3) 数据库只前滚：DB_CHANGED=true 时 rollback 需 --force
#   4) 永不 docker compose down -v；永不 docker system prune（同机还有 appt 栈）
# =============================================================================
set -Eeuo pipefail

DEPLOY_DIR="$(cd "$(dirname "$(realpath "$0")")" && pwd)"
cd "$DEPLOY_DIR"

STATE_DIR="$DEPLOY_DIR/.state"
CURRENT="$STATE_DIR/current.env"
PREVIOUS="$STATE_DIR/previous.env"
LOCK_FILE="${TMPDIR:-/tmp}/sdblog-deploy.lock"

# 四枚镜像：变量名 → 镜像仓库（顺序即状态文件顺序）
IMAGE_MAP=(
  "BACKEND_IMAGE:ghcr.io/xcqj/sdblog-backend:backend"
  "ADMIN_IMAGE:ghcr.io/xcqj/sdblog-admin:admin"
  "WEB_IMAGE:ghcr.io/xcqj/sdblog-web:web"
  "ES_IMAGE:ghcr.io/xcqj/sdblog-es:es"
)

die()  { printf '\033[31m✗ %s\033[0m\n' "$*" >&2; exit 1; }
warn() { printf '\033[33m! %s\033[0m\n' "$*"; }
info() { printf '\033[36m→ %s\033[0m\n' "$*"; }
ok()   { printf '\033[32m✓ %s\033[0m\n' "$*"; }

usage() {
  sed -n '4,15p' "$0" | sed 's/^# \{0,1\}//'
}

load_env() {
  [[ -f .env ]] || die ".env 不存在：cp .env.example .env 后填写（至少三个口令）"
  local mode; mode="$(stat -c %a .env)"
  [[ "$mode" == "600" || "$mode" == "400" ]] || warn ".env 权限为 $mode，建议 chmod 600 .env"
  set -a
  # shellcheck disable=SC1091
  . ./.env
  set +a
  : "${DATA_DIR:?DATA_DIR 未设置}"
  [[ "$DATA_DIR" == /* ]] || die "DATA_DIR 必须是绝对路径（当前: $DATA_DIR）"
  [[ "$DATA_DIR" != /mnt/* ]] || die "DATA_DIR 不能放 /mnt/*（9p 文件系统会让 MySQL 抖动）"
}

compose() {
  docker compose --project-directory "$DEPLOY_DIR" \
    -f compose.deps.yml -f compose.app.yml -f "compose.target-${TARGET:-wsl}.yml" "$@"
}

take_lock() {
  mkdir -p "$STATE_DIR"
  exec 9>"$LOCK_FILE"
  flock -n 9 || die "另一个部署进程正在运行（$LOCK_FILE）；确认无进程后可删除该文件重试"
}

# 有 digest 用 digest（回滚精确），但**必须是同一个仓库**的 digest：
# 本地构建/加载的镜像，RepoDigests 里可能残留旧仓库名（如 sd-backend@sha256:...），
# 直接拿来当 ghcr 引用会在 rollback 时去错 registry 拉取 → 退回 tag。
ref_for_state() {
  local ref="$1" repo="$2" dg
  dg=$(docker image inspect --format '{{if .RepoDigests}}{{index .RepoDigests 0}}{{end}}' "$ref" 2>/dev/null || true)
  if [[ -n "$dg" && "$dg" == "${repo}@"* ]]; then
    printf '%s' "$dg"
  else
    printf '%s' "$ref"
  fi
}

check_ports() {
  local p
  for p in "${BACKEND_PORT:-19081}" "${ADMIN_PORT:-19080}" "${WEB_PORT:-19300}" \
           "${MYSQL_PORT:-13306}" "${REDIS_PORT:-16379}" "${MINIO_PORT:-19000}" \
           "${MINIO_CONSOLE_PORT:-19001}" "${ES_PORT:-19200}"; do
    if ss -ltn 2>/dev/null | grep -qE "[:.]${p}([[:space:]]|$)"; then
      die "宿主机端口 ${p} 已被占用（改 .env 里的端口变量，不要改 compose）"
    fi
  done
}

preflight() {
  command -v docker >/dev/null || die "docker 未安装"
  docker compose version >/dev/null 2>&1 || die "docker compose 不可用"
  local t="compose.target-${TARGET:-wsl}.yml"
  [[ -f compose.deps.yml && -f compose.app.yml && -f "$t" ]] || die "compose 文件不全（缺 $t）"

  if [[ ! -f sql/01-ry-vue.sql || ! -f mysql/my.cnf ]]; then
    info "暂存初始化资产（SQL + my.cnf）"
    ./stage-sql.sh
  fi
  local f
  for f in sql/01-ry-vue.sql sql/02-blog-schema.sql sql/03-blog-web-client.sql \
           sql/10-menu-s5.sql sql/11-menu-s6.sql sql/12-menu-s7.sql sql/13-menu-s10.sql mysql/my.cnf; do
    [[ -f "$f" ]] || die "缺少初始化资产: $f"
  done

  mkdir -p "$DATA_DIR"

  # 守卫：有部署记录但数据目录是空的 —— 历史上踩过"相对路径解析到空目录 → 空库 → api crash loop"
  if [[ -f "$CURRENT" && ! -d "$DATA_DIR/mysql/mysql" && ! -f "$DATA_DIR/mysql/ibdata1" ]]; then
    die "存在部署记录但 ${DATA_DIR}/mysql 为空，疑似数据目录解析错误，拒绝启动。
   若确为全新部署（旧数据已废弃），删除 ${CURRENT} 后重试。"
  fi

  local avail
  avail=$(df -Pk "$DATA_DIR" | awk 'NR==2{print int($4/1024/1024)}')
  (( avail >= 10 )) || die "磁盘剩余 ${avail}G < 10G，先清理再部署"

  local mmm
  mmm=$(sysctl -n vm.max_map_count 2>/dev/null || echo 0)
  (( mmm >= 262144 )) || die "vm.max_map_count=${mmm} < 262144，ES 无法启动。
   执行：sudo sysctl -w vm.max_map_count=262144
   并写入 /etc/sysctl.d/99-sdblog.conf（WSL 重启会重置，必须持久化）"

  # 本栈未运行时才检查端口（重启自己的容器不算冲突）
  if [[ -z "$(compose ps -q 2>/dev/null)" ]]; then
    check_ports
  fi

  compose config >/dev/null || die "compose 配置校验失败"
}

# bind mount 的属主必须与容器用户一致；Docker 创建缺失的宿主目录时属主是 root，
# 而 ES 镜像**不会**像 mysql/redis 那样在 entrypoint 里自动 chown → 会无限重启。
# 这里用容器内 root 改属主，避免部署需要 sudo。
init_data_dir() {
  mkdir -p "$DATA_DIR"/{mysql,redis,minio,es}
  local es_uid es_ref
  es_uid="$(stat -c %u "$DATA_DIR/es")"
  if [[ "$es_uid" != "1000" ]]; then
    es_ref="${ES_IMAGE:-ghcr.io/xcqj/sdblog-es:${RELEASE_VERSION:-local}}"
    info "修正 ${DATA_DIR}/es 属主（当前 uid=$es_uid，ES 需要 1000:0）"
    docker run --rm --user 0 -v "$DATA_DIR/es:/d" --entrypoint chown "$es_ref" -R 1000:0 /d \
      || die "修正 es 目录属主失败（镜像 $es_ref 是否已拉取？）"
  fi
  # MinIO 目录交给调用者（容器内 root 写入不受影响），否则 backup/restore
  # 无法以当前用户清理与覆盖（实测：restore 的 rm -rf 会 PermissionDenied）
  if [[ "$(stat -c %u "$DATA_DIR/minio")" != "$(id -u)" ]]; then
    info "修正 ${DATA_DIR}/minio 属主为 $(id -u):$(id -g)"
    docker run --rm --user 0 -v "$DATA_DIR/minio:/d" --entrypoint chown mysql:8.4.9 \
      -R "$(id -u):$(id -g)" /d || warn "修正 minio 目录属主失败，备份/恢复可能受影响"
  fi
}

# OSS 公网域名属环境配置（WSL 与云目标不同），不写进基础 SQL：
# 每次发布按 .env 的 IMG_PUBLIC_URL 幂等注入，避免"新库图片指向 127.0.0.1:9000"
apply_env_config() {
  local img="${IMG_PUBLIC_URL:-}" muser="${MINIO_ROOT_USER:-}" mpass="${MINIO_ROOT_PASSWORD:-}"
  local bucket="${MINIO_BUCKET:-ruoyi}"
  if [[ -z "$img" || -z "$muser" || -z "$mpass" ]]; then
    warn "IMG_PUBLIC_URL / MINIO_ROOT_USER / MINIO_ROOT_PASSWORD 有缺项，跳过 OSS 配置注入"
    return 0
  fi
  # 六件事必须一起改，缺一条就出错：
  #   ① endpoint 走容器内服务名（不能是 127.0.0.1）
  #   ② 凭据用本栈 MinIO 的（模板里是 ruoyi/ruoyi123 → 上传 403
  #      "The Access Key Id you provided does not exist in our records"）
  #   ③ bucket_name
  #   ④ domain_url **只写域名，不带桶名**：本版本上传返回的 url 是
  #      domain + "/" + bucket + "/" + key，带桶名会变成 /ruoyi/ruoyi/...（404）
  #   ⑤ access_policy=2 → 公有读，URL 不带签名
  #   ⑥ is_https 必须为 **N**：这个字段同时决定 S3 客户端协议（OssClientConfig.getEndpointUrl
  #      会 rebuildUrlHeader(useHttps, endpoint)），而 MinIO 内网是明文 http。
  #      填 Y 会让客户端对 MinIO 说 TLS → MinIO 回明文 400 → NotSslRecordException → 上传 500。
  #      代价：返回的图片 URL 是 http://（现代浏览器会把 https 页面里的 http 图片自动升级，仍可显示）；
  #      根治需改 OssClientConfig 尊重 endpoint 自带协议，见 docs/cloud-deployment.md 待办。
  local dom="${img%/}"
  compose exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql \
    mysql -uroot --default-character-set=utf8mb4 "${MYSQL_DATABASE:-ry-vue}" -e "
      update sys_oss_config
         set endpoint = 'minio:9000',
             bucket_name = '${bucket}',
             access_key = '${muser}',
             secret_key = '${mpass}',
             domain_url = '${dom}',
             access_policy = 2,
             is_https = 'N'
       where config_key in ('minio','image')
         and (endpoint <> 'minio:9000'
              or bucket_name <> '${bucket}'
              or access_key <> '${muser}'
              or secret_key <> '${mpass}'
              or ifnull(domain_url,'') <> '${dom}'
              or ifnull(access_policy,0) <> 2
              or is_https <> 'N');" >/dev/null 2>&1 \
    && info "OSS 配置已对齐（endpoint=minio:9000 / bucket=${bucket} / domain=${dom} / https）" \
    || warn "OSS 配置注入失败（不影响起栈，可手动 update sys_oss_config 修正）"
}

wait_healthy() {
  local timeout="${1:-300}" start now st bad cid
  start=$(date +%s)
  while :; do
    bad=0
    for cid in $(compose ps -q); do
      st=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$cid" 2>/dev/null || echo unknown)
      case "$st" in healthy|running) ;; *) bad=$((bad + 1)) ;; esac
    done
    if (( bad == 0 )); then
      ok "全部容器健康"
      return 0
    fi
    now=$(date +%s)
    if (( now - start >= timeout )); then
      compose ps
      die "等待健康超时（${timeout}s）"
    fi
    sleep 3
  done
}

write_state() {
  local out="$1" entry var repo svc ref
  {
    echo "RELEASE_VERSION=${RELEASE_VERSION:-local}"
    for entry in "${IMAGE_MAP[@]}"; do
      var="${entry%%:*}"; repo="${entry#*:}"; svc="${repo##*:}"; repo="${repo%%:*}"
      ref="${!var:-${repo}:${RELEASE_VERSION:-local}}"
      echo "${var}=$(ref_for_state "$ref" "$repo")"
    done
    echo "DB_CHANGED=${DB_CHANGED:-false}"
    echo "DEPLOYED_AT=$(date -Iseconds)"
  } > "$out"
}

cmd_deploy() {
  local from_state="" no_pull=0 no_smoke=0 cli_version=""
  while (( $# )); do
    case "$1" in
      --from-state) from_state="$2"; shift 2 ;;
      --version)    cli_version="$2"; shift 2 ;;
      --db-changed) DB_CHANGED=true; shift ;;
      --no-pull)    no_pull=1; shift ;;
      --no-smoke)   no_smoke=1; shift ;;
      *) die "未知参数: $1" ;;
    esac
  done

  load_env
  # ⚠️ 必须晚于 load_env：.env 里的 RELEASE_VERSION（本地演示为 local）会覆盖命令行参数，
  # 否则 `up --version v0.1.0` 会去拉 `:local`（实测踩过，导致发布失败）
  [[ -n "$cli_version" ]] && RELEASE_VERSION="$cli_version"

  if [[ -n "$from_state" ]]; then
    [[ -f "$from_state" ]] || die "找不到状态文件: $from_state"
    set -a
    # shellcheck disable=SC1090
    . "$from_state"
    set +a
    info "目标版本取自 $(basename "$from_state")：${RELEASE_VERSION}"
  fi

  take_lock
  preflight

  if (( ! no_pull )); then
    info "拉取镜像（私有包需先 docker login ghcr.io，见 README）"
    compose pull --quiet || die "镜像拉取失败：先确认已 docker login ghcr.io 且版本 tag 存在"
  fi

  init_data_dir

  local tmp; tmp=$(mktemp)
  trap 'rm -f "$tmp"' RETURN
  write_state "$tmp"

  [[ -f "$CURRENT" ]] && cp "$CURRENT" "$PREVIOUS"
  install -m 600 "$tmp" "$CURRENT"

  info "起栈（无 build）"
  compose up -d --no-build --remove-orphans
  wait_healthy
  apply_env_config

  ok "已部署 ${RELEASE_VERSION:-local}"
  if (( ! no_smoke )); then
    ./smoke.sh || die "冒烟未通过。状态已记录为当前版本；可执行 ./deploy.sh rollback 回到上一组"
  fi
}

cmd_adopt() {
  load_env
  [[ -n "$(compose ps -q 2>/dev/null)" ]] || die "当前没有运行中的容器，无法 adopt"
  take_lock
  local tmp; tmp=$(mktemp)
  {
    echo "RELEASE_VERSION=${RELEASE_VERSION:-adopted}"
    local entry var repo svc ref
    for entry in "${IMAGE_MAP[@]}"; do
      var="${entry%%:*}"; repo="${entry#*:}"; svc="${repo##*:}"; repo="${repo%%:*}"
      ref=$(docker inspect -f '{{.Config.Image}}' "$(compose ps -q "$svc")" 2>/dev/null || true)
      [[ -n "$ref" ]] || die "取不到 $svc 的运行镜像"
      echo "${var}=$(ref_for_state "$ref" "$repo")"
    done
    echo "DB_CHANGED=false"
    echo "DEPLOYED_AT=$(date -Iseconds)"
  } > "$tmp"
  [[ -f "$CURRENT" ]] && cp "$CURRENT" "$PREVIOUS"
  install -m 600 "$tmp" "$CURRENT"; rm -f "$tmp"
  ok "已接管当前运行镜像（未重启）"
}

cmd_rollback() {
  local force=0
  [[ "${1:-}" == "--force" ]] && force=1
  load_env
  [[ -f "$PREVIOUS" ]] || die "没有 previous 记录，无法回滚（首次部署后才会产生）"
  if grep -q '^DB_CHANGED=true' "$CURRENT" 2>/dev/null && (( ! force )); then
    die "当前版本标记了 DB_CHANGED=true：镜像是回退了，数据库不会。确认要回滚请加 --force"
  fi
  info "回滚到上一组镜像"
  cmd_deploy --from-state "$PREVIOUS" --no-pull
}

cmd_down() {
  load_env
  take_lock
  info "停止并移除容器（保留 ${DATA_DIR} 与 .state；永不 -v）"
  compose down --remove-orphans
  ok "已停止"
}

# 装载演示种子并重建 ES 索引。
# 为什么要重建索引：种子是直插 SQL，绕过应用事件，ES 不会自动感知；
# 而 initialFullSync 只在"alias 不存在"时触发（见 ArticleIndexBootstrap），
# 所以删掉索引版本再重启后端 = 确定性重建（ES 是派生数据，删了不丢东西）。
cmd_seed() {
  local file="${1:-${SEED_FILE:-seed-demo.sql}}"
  load_env
  take_lock
  info "装载种子 ${file}（幂等，可重放）"
  SEED_FILE="$file" compose --profile seed run --rm seed

  info "重建 ES 索引（种子直插 SQL 不触发应用事件）"
  local idx
  for idx in $(compose exec -T es curl -s 'http://127.0.0.1:9200/_cat/indices/blog_article_v*?h=index' 2>/dev/null); do
    compose exec -T es curl -s -X DELETE "http://127.0.0.1:9200/${idx}" >/dev/null && echo "  已删除 ${idx}"
  done
  compose restart backend >/dev/null
  sleep 10
  wait_healthy
  ok "种子已装载、索引已重建"
}

cmd_backup() {
  load_env
  take_lock
  local ts dir
  ts=$(date +%Y%m%d-%H%M%S)
  dir="$BACKUP_DIR/$ts"
  mkdir -p "$dir"

  info "导出数据库 ${MYSQL_DATABASE:-ry-vue}"
  compose exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql \
    mysqldump -uroot --single-transaction --routines --triggers \
    --default-character-set=utf8mb4 "${MYSQL_DATABASE:-ry-vue}" \
    | gzip -1 > "$dir/db.sql.gz"
  [[ -s "$dir/db.sql.gz" ]] || die "导出失败：db.sql.gz 为空"
  ok "数据库 $(du -h "$dir/db.sql.gz" | cut -f1)"

  info "打包对象存储（MinIO 目录）"
  tar czf "$dir/minio.tar.gz" -C "$DATA_DIR" minio
  ok "对象存储 $(du -h "$dir/minio.tar.gz" | cut -f1)"

  {
    echo "CREATED_AT=$(date -Iseconds)"
    echo "RELEASE_VERSION=${RELEASE_VERSION:-unknown}"
    echo "MYSQL_DATABASE=${MYSQL_DATABASE:-ry-vue}"
  } > "$dir/manifest.txt"

  # 轮转：保留最近 N 份
  local keep="${KEEP_BACKUPS:-7}"
  ls -1dt "$BACKUP_DIR"/*/ 2>/dev/null | tail -n +$((keep + 1)) | while read -r old; do
    rm -rf "$old" && echo "  清理旧备份 $(basename "$old")"
  done

  ok "备份完成：$dir"
}

cmd_restore() {
  local src="${1:-}"
  [[ -n "$src" && "$src" != -* ]] || die "用法: ./deploy.sh restore <备份目录> [--yes]"
  shift || true
  [[ -d "$src" ]] || die "备份目录不存在: $src"
  [[ -f "$src/db.sql.gz" ]] || die "缺少 $src/db.sql.gz"
  load_env
  take_lock

  if [[ "${1:-}" != "--yes" && "${src}" != "" ]]; then
    warn "将清空数据库 ${MYSQL_DATABASE:-ry-vue} 并用 $(basename "$src") 覆盖，MinIO 目录也会被替换"
    local a; read -r -p "确认继续？输入 yes: " a
    [[ "$a" == "yes" ]] || die "已取消"
  fi

  info "停应用（中间件保留）"
  compose stop backend web admin || true

  info "重建数据库"
  compose exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql mysql -uroot -e \
    "DROP DATABASE IF EXISTS \`${MYSQL_DATABASE:-ry-vue}\`; CREATE DATABASE \`${MYSQL_DATABASE:-ry-vue}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

  info "导入数据"
  gunzip -c "$src/db.sql.gz" | compose exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql \
    mysql -uroot --default-character-set=utf8mb4 "${MYSQL_DATABASE:-ry-vue}"

  if [[ -f "$src/minio.tar.gz" ]]; then
    info "恢复对象存储"
    rm -rf "${DATA_DIR:?}/minio" || warn "清理旧对象目录未完全成功，继续覆盖"
    mkdir -p "$DATA_DIR/minio"
    tar xzf "$src/minio.tar.gz" -C "$DATA_DIR"
  fi

  info "启应用（ES 索引由 ArticleIndexBootstrap 全量重建）"
  compose up -d --no-build
  wait_healthy

  info "行数校验（seed-demo 基线：文章 52 / 版本 52 / 评论 200 / 标签 20 / 用户 8）"
  compose exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql mysql -uroot -N --default-character-set=utf8mb4 "${MYSQL_DATABASE:-ry-vue}" -e "
    SELECT CONCAT('  文章 ', COUNT(*)) FROM sd_blog_article;
    SELECT CONCAT('  版本 ', COUNT(*)) FROM sd_blog_article_version;
    SELECT CONCAT('  评论 ', COUNT(*)) FROM sd_blog_comment;
    SELECT CONCAT('  标签 ', COUNT(*)) FROM sd_blog_tag;
    SELECT CONCAT('  用户 ', COUNT(*)) FROM sys_user;"

  ./smoke.sh
  ok "恢复完成"
}

cmd_status() {
  load_env
  local proj; proj=$(grep -m1 '^name:' "compose.target-${TARGET:-wsl}.yml" | awk '{print $2}')
  printf '== 项目 %s  目标 %s  版本 %s\n' "$proj" "${TARGET:-wsl}" "${RELEASE_VERSION:-local}"
  compose ps
  printf '\n== 状态记录\n'
  local f
  for f in "$CURRENT" "$PREVIOUS"; do
    if [[ -f "$f" ]]; then
      printf -- '--- %s\n' "$(basename "$f")"
      sed 's/^/  /' "$f"
    else
      printf -- '--- %s（无）\n' "$(basename "$f")"
    fi
  done
  printf '\n== 数据目录 %s（%s）\n' "$DATA_DIR" "$(du -sh "$DATA_DIR" 2>/dev/null | cut -f1)"
  df -h "$DATA_DIR" | tail -1
  printf '\n== 最近备份（%s）\n' "$BACKUP_DIR"
  ls -1t "$BACKUP_DIR" 2>/dev/null | head -3 || echo "  （无）"
  printf '\n== 自检\n'
  printf '  容器内自检: http://127.0.0.1:%s/\n' "${WEB_PORT:-19300}"
  printf '  公网入口  : %s / %s\n' "${PUBLIC_SITE_URL:-?}" "${ADMIN_PUBLIC_URL:-?}"
  printf '  提示: 演示端口只绑 127.0.0.1，Windows 浏览器打不开属预期（WSL2 不转发回环地址）\n'
}

cmd_gc() {
  local apply=0
  [[ "${1:-}" == "--apply" ]] && apply=1
  load_env
  local keep; keep=$(mktemp)
  local f
  for f in "$CURRENT" "$PREVIOUS"; do
    [[ -f "$f" ]] && sed -n 's/^[A-Z_]*IMAGE=//p' "$f" >> "$keep"
  done
  grep -q . "$keep" || warn "状态文件为空，将只保留 latest 与当前版本 tag"

  info "清理 ghcr.io/xcqj/sdblog-* 旧镜像（$( ((apply)) && echo 实删 || echo dry-run )）"
  local ref dg
  while read -r ref; do
    [[ -n "$ref" ]] || continue
    dg=$(docker image inspect --format '{{if .RepoDigests}}{{index .RepoDigests 0}}{{end}}' "$ref" 2>/dev/null || true)
    if grep -qxF "$ref" "$keep" 2>/dev/null || { [[ -n "$dg" ]] && grep -qxF "$dg" "$keep"; }; then
      continue
    fi
    case "$ref" in
      *:latest|*:"${RELEASE_VERSION:-local}") continue ;;
    esac
    if (( apply )); then
      docker rmi "$ref" >/dev/null 2>&1 && echo "  已删除 $ref" || echo "  跳过 $ref（被使用中）"
    else
      echo "  可删除 $ref"
    fi
  done < <(docker images --format '{{.Repository}}:{{.Tag}}' | grep -E '^ghcr\.io/xcqj/sdblog-' || true)
  rm -f "$keep"
  printf '\n  注意：绝不执行 docker system prune -a（同机还有 appt 栈）\n'
}

case "${1:-}" in
  up)       shift; cmd_deploy "$@" ;;
  rollback) shift; cmd_rollback "$@" ;;
  adopt)    shift; cmd_adopt "$@" ;;
  down)     shift; cmd_down "$@" ;;
  seed)     shift; cmd_seed "$@" ;;
  backup)   shift; cmd_backup "$@" ;;
  restore)  shift; cmd_restore "$@" ;;
  status)   shift; cmd_status "$@" ;;
  gc)       shift; cmd_gc "$@" ;;
  *)        usage; exit 1 ;;
esac
