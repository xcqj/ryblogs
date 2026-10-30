#!/usr/bin/env bash
# =============================================================================
# 数据库脚本一致性审计（S29.1 收尾工具）
#
# 背景：本项目没有迁移框架，schema 与基础数据全靠 docs/*.sql 一次性脚本。
#       历史上有两次"在开发库手工改过、没回流脚本"的事故（S29.1 验收时暴露）：
#         ① web 客户端缺 social 授权类型 → 社交登录报 Auth grant type error
#         ② 「博客管理」父目录菜单缺失 → 新库治理页入口整组不显示
#       本脚本用"重放法"发现这一类漂移：拿部署包里那套 sql/ 在**临时库**重放
#       initdb，再与运行库逐项比对；有任何差异就非零退出。
#
# 用法：
#   ./db-audit.sh            # 审计并清理临时库
#   ./db-audit.sh --keep     # 保留临时库（排查用，会打印库名）
#
# 前置：sql/ 已暂存（源码目录跑 ./stage-sql.sh；部署包内自带）
# 退出码：0 = 一致；1 = 有差异或执行失败
# =============================================================================
set -Eeuo pipefail

cd "$(dirname "$(realpath "$0")")"

KEEP=0
[[ "${1:-}" == "--keep" ]] && KEEP=1

die()  { printf '\033[31m✗ %s\033[0m\n' "$*" >&2; exit 1; }
ok()   { printf '\033[32m✓ %s\033[0m\n' "$*"; }
warn() { printf '\033[33m! %s\033[0m\n' "$*"; }
info() { printf '\033[36m→ %s\033[0m\n' "$*"; }

[[ -f .env ]] || die ".env 不存在（在部署目录执行；源码目录请先 cp .env.example .env）"
set -a
# shellcheck disable=SC1091
. ./.env
set +a
: "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD 未设置}"

LIVE_DB="${MYSQL_DATABASE:-ry-vue}"
VERIFY_DB="ry_verify_$(date +%H%M%S)_$$"

compose() {
  docker compose --project-directory "$PWD" \
    -f compose.deps.yml -f compose.app.yml -f "compose.target-${TARGET:-wsl}.yml" "$@"
}
# 统一走 mysql 客户端；密码用 MYSQL_PWD 传，不出现在 ps 里
sqlx() {
  local db="$1"; shift
  compose exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql \
    mysql -uroot -N --default-character-set=utf8mb4 "$db" -e "$1"
}

cleanup() {
  if (( KEEP )); then
    warn "临时库保留：${VERIFY_DB}（手动确认后 drop database \`${VERIFY_DB}\`;）"
  else
    sqlx "$LIVE_DB" "drop database if exists \`${VERIFY_DB}\`;" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

# ---------- 0. 前置检查 ----------
SQL_FILES=(sql/01-*.sql sql/02-*.sql sql/03-*.sql sql/10-*.sql sql/11-*.sql sql/12-*.sql sql/13-*.sql)
local_missing=0
for f in "${SQL_FILES[@]}"; do [[ -f $f ]] || local_missing=1; done
(( local_missing == 0 )) || die "sql/ 不全：先在**源码目录**跑 ./stage-sql.sh（部署包内自带）"

compose ps -q mysql >/dev/null 2>&1 || die "mysql 容器未运行"
[[ -n "$(sqlx "$LIVE_DB" "select 1;")" ]] || die "连不上运行库 ${LIVE_DB}"

# ---------- 1. 建临时库并重放 ----------
info "创建临时库 ${VERIFY_DB} 并按 initdb 顺序重放"
sqlx "$LIVE_DB" "drop database if exists \`${VERIFY_DB}\`; create database \`${VERIFY_DB}\` default character set utf8mb4 collate utf8mb4_general_ci;"

for f in "${SQL_FILES[@]}"; do
  printf '   %-28s ' "$f"
  if compose exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql \
       mysql -uroot --default-character-set=utf8mb4 "$VERIFY_DB" < "$f" >/dev/null 2>&1; then
    echo OK
  else
    echo FAIL
    die "脚本重放失败：$f（上面的 SQL 报错被吞了，用 mysql 手工跑一次看原因）"
  fi
done

# ---------- 2. 比对 ----------
fail=0
cmp_rows() {  # $1=描述 $2=sql（两库各跑一次）
  local desc="$1" q="$2" a b
  a=$(sqlx "$LIVE_DB" "$q" | sort)
  b=$(sqlx "$VERIFY_DB" "$q" | sort)
  if [[ "$a" == "$b" ]]; then
    ok "$desc"
  else
    printf '\033[31m✗ %s\033[0m\n' "$desc"
    diff <(printf '%s\n' "$a") <(printf '%s\n' "$b") | sed 's/^/    /' | head -20
    fail=1
  fi
}
cmp_count() {
  local desc="$1" table="$2" a b
  a=$(sqlx "$LIVE_DB" "select count(*) from \`${table}\`;")
  b=$(sqlx "$VERIFY_DB" "select count(*) from \`${table}\`;")
  if [[ "$a" == "$b" ]]; then ok "${desc}（${a} 行）"; else
    printf '\033[31m✗ %s：live=%s verify=%s\033[0m\n' "$desc" "$a" "$b"; fail=1
  fi
}

printf '\n\033[1m1. 表结构\033[0m\n'
cmp_rows "表/列集合一致" "
  select table_name, column_name, column_type, is_nullable, column_default
  from information_schema.columns where table_schema = database() order by 1,2;"

printf '\n\033[1m2. 基础数据（配置类表）\033[0m\n'
cmp_rows "sys_client（client_key/grant_type/status）" "select client_key, grant_type, status from sys_client order by client_key;"
cmp_rows "sys_config（key/value）"                   "select config_key, config_value from sys_config order by config_key;"
cmp_rows "sys_dict_type"                              "select dict_type from sys_dict_type order by dict_type;"
cmp_rows "sys_dict_data（type/label/value/sort）"     "select dict_type, dict_label, dict_value, dict_sort from sys_dict_data order by dict_type, dict_sort;"
cmp_rows "sys_role（key/status）"                     "select role_key, status from sys_role order by role_key;"
cmp_rows "sys_dept（id/name/parent）"                 "select dept_id, dept_name, parent_id from sys_dept order by dept_id;"
cmp_rows "sys_oss_config（key/bucket）"                "select config_key, bucket_name from sys_oss_config order by config_key;"
# endpoint / 凭据 / domain_url / access_policy 属**环境配置**（各栈 MinIO 不同、域名不同），
# 由 deploy.sh 的 apply_env_config 从 .env 幂等注入，不写进基础 SQL；
# 这里断言运行库与 .env 一致（凭据只比相等性，不打印内容）
if [[ -n "${IMG_PUBLIC_URL:-}" && -n "${MINIO_ROOT_USER:-}" && -n "${MINIO_ROOT_PASSWORD:-}" ]]; then
  oss_ok=$(sqlx "$LIVE_DB" "
    select count(*) from sys_oss_config
    where config_key in ('minio','image')
      and endpoint = 'minio:9000'
      and bucket_name = '${MINIO_BUCKET:-ruoyi}'
      and access_key = '${MINIO_ROOT_USER}'
      and secret_key = '${MINIO_ROOT_PASSWORD}'
      and ifnull(domain_url,'') = '${IMG_PUBLIC_URL%/}'
      and ifnull(access_policy,0) = 2
      and is_https = 'N';")
  if [[ "$oss_ok" == "2" ]]; then
    ok "OSS 环境配置与 .env 一致（endpoint/bucket/凭据/域名/policy）"
  else
    printf '\033[31m✗ OSS 环境配置与 .env 不一致（命中 %s/2 条）——跑一次 deploy.sh up 让它注入\033[0m\n' "$oss_ok"
    fail=1
  fi
else
  warn "IMG_PUBLIC_URL / MINIO_* 未设置，跳过 OSS 环境配置断言"
fi

printf '\n\033[1m3. 菜单（含博客治理）\033[0m\n'
cmp_rows "sys_menu 全量（id/父/类型/路径/组件/权限/可见）" "
  select menu_id, ifnull(parent_id,0), menu_name, menu_type, ifnull(path,''), ifnull(component,''),
         ifnull(perms,''), visible, status
  from sys_menu order by menu_id;"
cmp_count "菜单总数" sys_menu
cmp_rows "孤儿菜单（应为空）" "
  select m.menu_id from sys_menu m left join sys_menu p on m.parent_id = p.menu_id
  where m.parent_id <> 0 and p.menu_id is null order by 1;"

printf '\n\033[1m4. 业务表（结构已比，数据不比）\033[0m\n'
cmp_rows "博客表清单" "
  select table_name from information_schema.tables
  where table_schema = database() and table_name like 'sd\_blog%' order by 1;"

printf '\n'
if (( fail )); then
  die "审计未通过：运行库与脚本存在漂移。请把差异回流到 docs/*.sql 后重新 stage 再跑"
fi
ok "审计通过：脚本重放结果与运行库一致（临时库将自动清理）"
