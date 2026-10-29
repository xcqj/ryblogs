#!/usr/bin/env bash
# =============================================================================
# 公网冒烟（S29.1）—— 走真实域名，验证隧道 + 反代 + SEO 三件套
#
# 用法：./smoke-public.sh
# 依赖 .env 的 PUBLIC_SITE_URL / ADMIN_PUBLIC_URL
#
# 只断言"从公网能不能用"，不重复 smoke.sh 的数据库校验。
# =============================================================================
set -uo pipefail
cd "$(dirname "$(realpath "$0")")"

[[ -f .env ]] || { echo "缺少 .env" >&2; exit 1; }
set -a
# shellcheck disable=SC1091
. ./.env
set +a

SITE="${PUBLIC_SITE_URL:?PUBLIC_SITE_URL 未设置}"
ADMIN="${ADMIN_PUBLIC_URL:?ADMIN_PUBLIC_URL 未设置}"

# 本机 shell 里可能存在 https_proxy（实测会让部分请求 TLS 抖动、随机返回 000），
# 公网自检必须绕过代理，否则结果不可信
export no_proxy='*' NO_PROXY='*'

pass=0; fail=0
ok()   { printf '  \033[32m✓\033[0m %s\n' "$1"; pass=$((pass + 1)); }
bad()  { printf '  \033[31m✗\033[0m %s\n' "$1"; fail=$((fail + 1)); }
section() { printf '\n\033[1m%s\033[0m\n' "$1"; }
# 公网经 Cloudflare，偶发抖动；给 3 次机会
code() {
  local url="$1" i out=""
  for i in 1 2 3; do
    out=$(curl -s --noproxy '*' -o /dev/null -w '%{http_code}' --max-time 20 "$url" 2>/dev/null || true)
    [[ "$out" == "200" ]] && break
    sleep 2
  done
  printf '%s' "$out"
}

section "1. 前台"
[[ "$(code "$SITE/")" == "200" ]] && ok "$SITE/ 200" || bad "$SITE/ 失败"
grep -q 'Stack Depth' <<<"$(curl -s --max-time 15 "$SITE/")" && ok "首屏 SSR 内容正常" || bad "首屏内容异常"

section "2. 后台"
[[ "$(code "$ADMIN/")" == "200" ]] && ok "$ADMIN/ 200" || bad "$ADMIN/ 失败"
body=$(curl -s --max-time 15 "$ADMIN/prod-api/auth/code" || true)
grep -q '"code":200' <<<"$body" && ok "后台 /prod-api 反代通（envelope 200）" || bad "后台反代异常：${body:0:120}"

section "3. SEO 三件套（必须指向真实域名）"
sitemap=$(curl -s --max-time 20 "$SITE/sitemap.xml" || true)
grep -q "$SITE" <<<"$sitemap" && ok "sitemap 内链接为 $SITE" || bad "sitemap 未包含真实域名（检查 NUXT_PUBLIC_SITE_URL）"
robots=$(curl -s --max-time 10 "$SITE/robots.txt" || true)
grep -qi 'sitemap:' <<<"$robots" && ok "robots.txt 声明 sitemap" || bad "robots.txt 缺 Sitemap 行"

section "4. 内容页"
# 从首页抓第一个真实的详情链接（比猜 slug 更贴近读者路径；列表 VO 不含 slug 字段）
# 注意：先把 HTML 落变量再 grep —— 直接 curl | grep -m1 会让 curl 收到 SIGPIPE，
# 在 set -o pipefail 下整条管线失败、结果被吞掉（实测踩过）
home_html=$(curl -s --max-time 20 "$SITE/" || true)
link=""
if [[ "$home_html" =~ href=\"(/post/[^\"]+)\" ]]; then
  link="${BASH_REMATCH[1]}"
fi
if [[ -n "$link" ]]; then
  [[ "$(code "$SITE$link")" == "200" ]] && ok "详情页 ${link} 200" || bad "详情页 ${link} 失败"
  body=$(curl -s --max-time 20 "$SITE$link")
  grep -q 'prose' <<<"$body" && ok "详情页正文容器渲染正常" || bad "详情页正文容器缺失"
else
  bad "首页抓不到详情链接（列表为空或首页异常）"
fi

printf '\n\033[1m结果：%d 通过，%d 失败\033[0m\n' "$pass" "$fail"
[[ "$fail" -eq 0 ]] || exit 1
