#!/usr/bin/env bash
# =============================================================================
# 把「初始化资产」从单一来源暂存到 deploy/ 下（S29.1）
#
# 为什么需要它：
#   initdb 必须拿到**按序号命名**的 SQL，且 my.cnf 里的 lower_case_table_names=1
#   只在空卷首次初始化时生效，两者都得随部署包走；但仓库里的权威副本在
#   script/sql/ 与 docs/，不能在 deploy/ 下再维护第二份（必然漂移）。
#   所以：源码目录运行时从真源复制，部署包内已存在时原样保留。
#
# 产物（均不入库）：
#   deploy/sql/01-ry-vue.sql … 13-menu-s10.sql
#   deploy/mysql/my.cnf
# =============================================================================
set -Eeuo pipefail

cd "$(dirname "$(realpath "$0")")"
DEPLOY_DIR="$PWD"
REPO_ROOT="$(cd .. && pwd)"
SQL_DIR="$DEPLOY_DIR/sql"
MYSQL_DIR="$DEPLOY_DIR/mysql"
SEED_DIR="$DEPLOY_DIR/seed"

copy() {
  local src="$1" dst="$2"
  [[ -f "$src" ]] || { printf '\033[31m✗ 缺源文件: %s\033[0m\n' "$src" >&2; exit 1; }
  install -m 0644 "$src" "$dst"
}

if [[ ! -f "$REPO_ROOT/script/sql/ry_vue.sql" ]]; then
  # 部署包内没有源码，资产应由打包时带齐
  if [[ -f "$SQL_DIR/01-ry-vue.sql" && -f "$MYSQL_DIR/my.cnf" && -n "$(ls -A "$SEED_DIR" 2>/dev/null)" ]]; then
    echo "→ 非源码目录且资产已就绪，跳过暂存"
    exit 0
  fi
  printf '\033[31m✗ 既不是源码目录、资产也不完整；请重新解压部署包\033[0m\n' >&2
  exit 1
fi

mkdir -p "$SQL_DIR" "$MYSQL_DIR" "$SEED_DIR"

copy "$REPO_ROOT/script/sql/ry_vue.sql"       "$SQL_DIR/01-ry-vue.sql"
copy "$REPO_ROOT/docs/blog_schema_v0.1.sql"   "$SQL_DIR/02-blog-schema.sql"
copy "$REPO_ROOT/docs/blog_s9_web_client.sql" "$SQL_DIR/03-blog-web-client.sql"
copy "$REPO_ROOT/docs/blog_s5_menu.sql"       "$SQL_DIR/10-menu-s5.sql"
copy "$REPO_ROOT/docs/blog_s6_menu.sql"       "$SQL_DIR/11-menu-s6.sql"
copy "$REPO_ROOT/docs/blog_s7_menu.sql"       "$SQL_DIR/12-menu-s7.sql"
copy "$REPO_ROOT/docs/blog_s10_menu.sql"      "$SQL_DIR/13-menu-s10.sql"

# 字符集与 lower_case_table_names：必须 initdb 生效，故随资产分发
copy "$REPO_ROOT/script/docker/mysql/my.cnf"  "$MYSQL_DIR/my.cnf"

# 演示种子（S26）：两级，靠 .env 的 SEED_FILE 选文件；不进 initdb（需要可重放）
if [[ -d "$REPO_ROOT/script/sql/seed" ]]; then
  install -m 0644 "$REPO_ROOT"/script/sql/seed/*.sql "$SEED_DIR/"
fi

echo "→ 已暂存 7 个 SQL + my.cnf + 种子（$(ls -1 "$SEED_DIR" 2>/dev/null | wc -l) 个）"
