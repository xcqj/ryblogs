#!/usr/bin/env bash
# =============================================================================
# 演示栈冒烟（S29.1）—— 断言"真的能用"，不是"容器在跑"
#
# 用法：./smoke.sh [--wait]
#   --wait  先等全部容器健康（首次起栈 ES 较慢，最多 300s）
#
# 与 script/docker/smoke.sh 的区别：本脚本**不依赖固定容器名**（多栈共存后
# blog-* 已被开发栈占用），一律通过 compose 定位；且覆盖初始化数据与搜索链路。
#
# 退出码 0 = 全过。
# =============================================================================
set -uo pipefail
cd "$(dirname "$(realpath "$0")")"

[[ -f .env ]] || { echo "缺少 .env" >&2; exit 1; }
set -a
# shellcheck disable=SC1091
. ./.env
set +a

compose() {
  docker compose --project-directory "$PWD" \
    -f compose.deps.yml -f compose.app.yml -f "compose.target-${TARGET:-wsl}.yml" "$@"
}
sql() {
  compose exec -T -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql \
    mysql -uroot -N --default-character-set=utf8mb4 "${MYSQL_DATABASE:-ry-vue}" -e "$1" 2>/dev/null
}
# 容器刚重建时会有两类瞬时失败，必须重试：
#   ① 后端首个请求 JIT/连接池冷启动；② nginx 的 resolver 缓存 10s，容器换 IP 后短期 502
code() {
  local url="$1" i out=""
  for i in 1 2 3 4 5; do
    out=$(curl -s --noproxy '*' -o /dev/null -w '%{http_code}' --max-time 30 "$url" 2>/dev/null || true)
    [[ "$out" == "200" ]] && break
    sleep 3
  done
  printf '%s' "$out"
}
# 同上，返回响应体（用于断言 envelope 结构）
fetch() {
  local url="$1" i out=""
  for i in 1 2 3 4 5; do
    out=$(curl -s --noproxy '*' --max-time 30 "$url" 2>/dev/null || true)
    grep -q '"code":200' <<<"$out" && break
    sleep 3
  done
  printf '%s' "$out"
}

pass=0; fail=0
ok()   { printf '  \033[32m✓\033[0m %s\n' "$1"; pass=$((pass + 1)); }
bad()  { printf '  \033[31m✗\033[0m %s\n' "$1"; fail=$((fail + 1)); }
warn() { printf '  \033[33m!\033[0m %s\n' "$1"; }
section() { printf '\n\033[1m%s\033[0m\n' "$1"; }

if [[ "${1:-}" == "--wait" ]]; then
  section "等待容器健康（最多 300s）"
  for _ in $(seq 1 100); do
    bad_n=0
    for cid in $(compose ps -q); do
      st=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$cid" 2>/dev/null || echo unknown)
      case "$st" in healthy|running) ;; *) bad_n=$((bad_n + 1)) ;; esac
    done
    [[ "$bad_n" == 0 ]] && break
    sleep 3
  done
fi

section "1. 容器"
for svc in mysql redis minio es backend web admin; do
  cid=$(compose ps -q "$svc" 2>/dev/null || true)
  if [[ -z "$cid" ]]; then bad "$svc 未运行"; continue; fi
  st=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$cid")
  case "$st" in healthy|running) ok "$svc ($st)" ;; *) bad "$svc ($st)" ;; esac
done

section "2. 数据库初始化"
total=$(sql "select count(*) from information_schema.tables where table_schema='${MYSQL_DATABASE:-ry-vue}'")
blog=$(sql "select count(*) from information_schema.tables where table_schema='${MYSQL_DATABASE:-ry-vue}' and table_name like 'sd_blog%'")
menus=$(sql "select count(*) from sys_menu where perms like 'blog:%' or path like 'blog%'")
client=$(sql "select count(*) from sys_client where client_key='web'")
[[ "${total:-0}" -ge 31 ]] && ok "总表 ${total} 张（应 ≥31）" || bad "总表只有 ${total} 张（initdb 可能未完成）"
[[ "${blog:-0}" -eq 7 ]] && ok "博客表 ${blog} 张" || bad "博客表 ${blog} 张（应 7）"
[[ "${menus:-0}" -ge 10 ]] && ok "博客菜单 ${menus} 条" || bad "博客菜单只有 ${menus} 条"
[[ "${client:-0}" -eq 1 ]] && ok "web 客户端 1 条" || bad "web 客户端缺失（前台登录会报「客户端不存在」）"

section "3. 后端直连"
[[ "$(code "http://127.0.0.1:${BACKEND_PORT:-19081}/auth/code")" == "200" ]] \
  && ok "GET /auth/code 200（DB + Redis 连通）" || bad "GET /auth/code 失败"
[[ "$(code "http://127.0.0.1:${BACKEND_PORT:-19081}/blog/public/articles?pageNum=1&pageSize=1")" == "200" ]] \
  && ok "GET /blog/public/articles 200" || bad "公共文章列表失败"

section "4. 后台反代"
body=$(fetch "http://127.0.0.1:${ADMIN_PORT:-19080}/prod-api/auth/code")
grep -q '"code":200' <<<"$body" && ok "/prod-api/auth/code 反代正常" || bad "/prod-api 反代异常：${body:0:120}"
[[ "$(code "http://127.0.0.1:${ADMIN_PORT:-19080}/healthz")" == "200" ]] \
  && ok "后台 /healthz 200" || bad "后台 /healthz 失败"

section "5. 前台 SSR"
home=$(curl -s --max-time 30 "http://127.0.0.1:${WEB_PORT:-19300}/" || true)
[[ "$(code "http://127.0.0.1:${WEB_PORT:-19300}/")" == "200" ]] && ok "GET / 200" || bad "GET / 失败"
grep -q 'Stack Depth' <<<"$home" && ok "首屏含站点名（SSR 渲染成功）" || bad "首屏内容异常"

section "6. 搜索"
[[ "$(code "http://127.0.0.1:${BACKEND_PORT:-19081}/blog/public/search?keyword=Java&pageNum=1&pageSize=1")" == "200" ]] \
  && ok "GET /blog/public/search 200" || bad "搜索接口失败"

section "7. ES 索引（不阻塞交付）"
alias_n=$(compose exec -T es curl -s 'http://127.0.0.1:9200/_cat/aliases/blog_article' 2>/dev/null | wc -l)
if [[ "${alias_n:-0}" -ge 1 ]]; then ok "alias blog_article 已就绪"; else warn "alias 尚未就绪（搜索会降级 MySQL LIKE，索引重建中）"; fi

printf '\n\033[1m结果：%d 通过，%d 失败\033[0m\n' "$pass" "$fail"
[[ "$fail" -eq 0 ]] || exit 1
