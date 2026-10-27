#!/usr/bin/env bash
# =============================================================================
# 一键启动冒烟（S25）
#
# 验证「容器起来了 且 关键链路真的通」，不只检查进程存活。
# 用法：cd RuoYi-Vue-Plus/script/docker && ./smoke.sh [--wait]
#   --wait  先等所有服务健康（首次启动约 60~90s）
#
# 退出码 0 = 全通过；非 0 = 有失败项。
#
# 为什么不做「登录 → 发文 → 查列表」：
#   RuoYi 登录要验证码（位图），且密码走 RSA 加密。脚本内取得验证码答案需要读 Redis 并解密，
#   脆弱且与业务耦合。完整业务流由 tooling/shot 的 SHOT_TOKEN 方案在浏览器里跑。
#   本脚本只覆盖「基础设施 → 后端 → 反代 → 前台 SSR」四层是否打通。
# =============================================================================
set -uo pipefail

BACKEND_PORT="${BACKEND_PORT:-8081}"
WEB_PORT="${WEB_PORT:-3000}"
ADMIN_PORT="${ADMIN_PORT:-8080}"
REDIS_PASSWORD="${REDIS_PASSWORD:-ruoyi123}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root}"

pass=0
fail=0

ok()   { printf '  \033[32m✓\033[0m %s\n' "$1"; pass=$((pass + 1)); }
bad()  { printf '  \033[31m✗\033[0m %s\n' "$1"; fail=$((fail + 1)); }
head() { printf '\n\033[1m%s\033[0m\n' "$1"; }

code() { curl -s -o /dev/null -w '%{http_code}' --max-time 8 "$1"; }

if [[ "${1:-}" == "--wait" ]]; then
  head "等待服务健康（最多 180s）"
  for _ in $(seq 1 36); do
    bad_ones=$(docker compose -f docker-compose.yml ps --format '{{.Name}} {{.Status}}' | grep -cE 'unhealthy|starting|Restarting' || true)
    [[ "$bad_ones" == "0" ]] && break
    sleep 5
  done
fi

head "1. 容器状态"
for name in blog-mysql blog-redis blog-minio blog-es blog-backend blog-web blog-admin; do
  status=$(docker inspect -f '{{.State.Status}}' "$name" 2>/dev/null || echo missing)
  [[ "$status" == "running" ]] && ok "$name 运行中" || bad "$name 状态 $status"
done

head "2. 数据库"
tables=$(docker exec blog-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e \
  "select count(*) from information_schema.tables where table_schema='ry-vue' and table_name like 'sd_blog%';" 2>/dev/null || echo 0)
[[ "$tables" -ge 7 ]] && ok "博客表 $tables 张" || bad "博客表只有 $tables 张（应为 7，初始化可能未执行）"

head "3. 后端直连"
[[ "$(code "http://127.0.0.1:${BACKEND_PORT}/auth/code")" == "200" ]] \
  && ok "GET /auth/code 200（DB + Redis 连通）" || bad "GET /auth/code 失败"

head "4. 后台反代"
body=$(curl -s --max-time 8 "http://127.0.0.1:${ADMIN_PORT}/prod-api/auth/code" || true)
echo "$body" | grep -q '"code":200' \
  && ok "/prod-api/auth/code 反代正常（前缀已剥离）" || bad "/prod-api 反代异常：$(echo "$body" | head -c 80)"

head "5. 前台 SSR"
[[ "$(code "http://127.0.0.1:${WEB_PORT}/")" == "200" ]] \
  && ok "GET / 200（SSR 渲染成功）" || bad "GET / 失败"

head "6. 公共接口"
[[ "$(code "http://127.0.0.1:${BACKEND_PORT}/blog/public/articles?pageNum=1&pageSize=1")" == "200" ]] \
  && ok "GET /blog/public/articles 200" || bad "公共文章列表失败"

printf '\n\033[1m结果：%d 通过，%d 失败\033[0m\n' "$pass" "$fail"
[[ "$fail" -eq 0 ]] || exit 1
