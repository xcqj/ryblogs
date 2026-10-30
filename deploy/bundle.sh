#!/usr/bin/env bash
# =============================================================================
# 打部署包（S29.1）
#
#   ./bundle.sh [版本]            # 默认取 .env 的 RELEASE_VERSION
#   → dist/deploy-<版本>.tar.gz
#
# 包里**必须**有：四份 compose（deps/app/target-wsl/target-cloud）、脚本、
#   .env.example、README、sql/（有序）、mysql/my.cnf、redis/redis.conf
# 包里**必须没有**：compose.build.yml（跨仓 context，部署机用不了）、
#   .env（密钥）、.state（部署记录）、dist
#
# 部署机侧：解压到目标目录 → cp .env.example .env 填写 → ./deploy.sh up
# =============================================================================
set -Eeuo pipefail
cd "$(dirname "$(realpath "$0")")"

VERSION="${1:-${RELEASE_VERSION:-local}}"
if [[ ! "$VERSION" =~ ^(local|v[0-9]+\.[0-9]+\.[0-9]+)$ ]]; then
  echo "版本号必须形如 v1.2.3 或 local（当前: $VERSION）" >&2
  exit 1
fi

# 资产先暂存（单一来源 → deploy/sql + deploy/mysql）
./stage-sql.sh

ENTRY="deploy-$VERSION"
TMPROOT="$(mktemp -d)"
STAGE="$TMPROOT/$ENTRY"
mkdir -p "$STAGE"

install -m 0644 compose.deps.yml          "$STAGE/"
install -m 0644 compose.app.yml           "$STAGE/"
install -m 0644 compose.target-wsl.yml    "$STAGE/"
install -m 0644 compose.target-cloud.yml  "$STAGE/"
install -m 0755 deploy.sh stage-sql.sh smoke.sh smoke-public.sh db-audit.sh "$STAGE/"
install -m 0644 .env.example README.md    "$STAGE/"
install -m 0755 bundle.sh                 "$STAGE/"

mkdir -p "$STAGE/sql" "$STAGE/mysql" "$STAGE/redis" "$STAGE/seed"
install -m 0644 sql/*.sql       "$STAGE/sql/"
install -m 0644 mysql/my.cnf    "$STAGE/mysql/"
install -m 0644 redis/redis.conf "$STAGE/redis/"
install -m 0644 seed/*.sql      "$STAGE/seed/"

# 自检：不允许多余/缺失
for f in compose.build.yml .env .state dist; do
  [[ -e "$STAGE/$f" ]] && { echo "打包异常：包含 $f" >&2; exit 1; }
done
[[ "$(ls -1 "$STAGE/sql" | wc -l)" -eq 7 ]] || { echo "打包异常：sql 不是 7 个" >&2; exit 1; }
[[ "$(ls -1 "$STAGE/seed" | wc -l)" -ge 1 ]] || { echo "打包异常：seed 为空" >&2; exit 1; }

mkdir -p dist
OUT="dist/${ENTRY}.tar.gz"
tar czf "$OUT" -C "$TMPROOT" "$ENTRY"
rm -rf "$TMPROOT"

printf '\033[32m✓ 部署包：%s（%s）\033[0m\n' "$OUT" "$(du -h "$OUT" | cut -f1)"
