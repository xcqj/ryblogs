#!/usr/bin/env bash
# =============================================================================
# 生成 compose 所需的应用密钥并写入 .env（幂等：已有值不覆盖）
#
#   cd script/docker && ./gen-secrets.sh
#
# 产出：
#   .env                          JWT_SECRET_KEY / API_DECRYPT_* / NUXT_RSA_* / NUXT_APP_CLIENT_ID 保留原值
#   ../blog-web/.env（若存在）      NUXT_RSA_PUBLIC_KEY / NUXT_RSA_PRIVATE_KEY
#   ../plus-ui/.env.development.local（若存在该仓）VITE_APP_RSA_*，供 `pnpm dev` 使用
#
# 为什么是两对 RSA：
#   后端 api-decrypt.privateKey（解密请求）↔ 前端公钥（加密请求）
#   后端 api-decrypt.publicKey（加密响应）↔ 前端私钥（解密响应）
# 密钥值只进 .env 与本地 .local 文件，均不入库；脚本不打印密钥内容。
#
# ⚠️ 换密钥后必须：重建后端与管理后台镜像（后台是构建期内联），并同步
#    管理端仓库的 GitHub Actions Secrets（VITE_APP_RSA_PUBLIC_KEY / _PRIVATE_KEY）。
# =============================================================================
set -euo pipefail

cd "$(dirname "$0")"

ENV_FILE=.env
get_key() { sed -n "s/^$1=//p" "$ENV_FILE" | head -1; }

put_key() { # $1=文件 $2=键 $3=值（值中的 / + = 由 awk 原样写入）
  local file="$1" key="$2" val="$3"
  if grep -qE "^${key}=" "$file"; then
    awk -v k="$key" -v v="$val" 'BEGIN{d=0} $0 ~ "^"k"=" {print k"="v; d=1; next} {print} END{if(!d) print k"="v}' \
      "$file" > "$file.tmp" && mv "$file.tmp" "$file"
  else
    printf '\n%s=%s\n' "$key" "$val" >> "$file"
  fi
}

command -v openssl >/dev/null || { echo "缺少 openssl" >&2; exit 1; }

if [ ! -f "$ENV_FILE" ]; then
  cp .env.example "$ENV_FILE"
  echo "已从 .env.example 创建 .env（中间件口令仍为样例值，请自行确认）"
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

gen_pair() { # $1=私钥输出变量名 $2=公钥输出变量名
  openssl genrsa -out "$TMP/k.pem" 1024 >/dev/null 2>&1
  local priv pub
  priv="$(openssl pkcs8 -topk8 -nocrypt -in "$TMP/k.pem" -outform DER | base64 -w0)"
  pub="$(openssl rsa -in "$TMP/k.pem" -pubout -outform DER 2>/dev/null | base64 -w0)"
  printf -v "$1" '%s' "$priv"
  printf -v "$2" '%s' "$pub"
}

JWT_NEW="$(openssl rand -base64 48 | tr -d '\n')"
gen_pair REQ_PRIV REQ_PUB   # 请求方向：后端私钥解密 / 前端公钥加密
gen_pair RESP_PRIV RESP_PUB # 响应方向：后端公钥加密 / 前端私钥解密

filled=()
for pair in "JWT_SECRET_KEY=$JWT_NEW" "API_DECRYPT_PRIVATE_KEY=$REQ_PRIV" \
            "API_DECRYPT_PUBLIC_KEY=$RESP_PUB" "NUXT_RSA_PUBLIC_KEY=$REQ_PUB" \
            "NUXT_RSA_PRIVATE_KEY=$RESP_PRIV"; do
  k="${pair%%=*}"
  v="${pair#*=}"
  if [ -n "$(get_key "$k")" ]; then
    echo "跳过 $k（已有值）"
    # 已有值需要回填给前端文件，从 .env 读回
    case "$k" in
      NUXT_RSA_PUBLIC_KEY)  REQ_PUB="$(get_key "$k")" ;;
      NUXT_RSA_PRIVATE_KEY) RESP_PRIV="$(get_key "$k")" ;;
      API_DECRYPT_PRIVATE_KEY) REQ_PRIV="$(get_key "$k")" ;;
      API_DECRYPT_PUBLIC_KEY)  RESP_PUB="$(get_key "$k")" ;;
    esac
  else
    put_key "$ENV_FILE" "$k" "$v"
    filled+=("$k")
  fi
done

[ ${#filled[@]} -eq 0 ] && echo "全部密钥已存在，未改动 .env" || echo "已写入 .env：${filled[*]}"

# 读者站本地开发（.env 已存在才更新，避免覆盖其它配置）
WEB_ENV="../../../blog-web/.env"
if [ -f "$WEB_ENV" ]; then
  put_key "$WEB_ENV" NUXT_RSA_PUBLIC_KEY "$REQ_PUB"
  put_key "$WEB_ENV" NUXT_RSA_PRIVATE_KEY "$RESP_PRIV"
  echo "已同步 blog-web/.env 的 RSA 两项"
fi

# 管理端本地开发：Vite 的 *.local 不被提交
ADMIN_DIR="../../../plus-ui"
if [ -d "$ADMIN_DIR" ]; then
  printf "VITE_APP_RSA_PUBLIC_KEY='%s'\nVITE_APP_RSA_PRIVATE_KEY='%s'\n" "$REQ_PUB" "$RESP_PRIV" \
    > "$ADMIN_DIR/.env.development.local"
  echo "已写入 plus-ui/.env.development.local（供 pnpm dev）"
fi

cat <<'EOF'

后续动作：
  1. 重建后端与管理后台镜像：docker compose up -d --build   （后台密钥构建期内联）
  2. 管理端仓库 GitHub Actions Secrets 填：
       VITE_APP_RSA_PUBLIC_KEY  = .env 的 NUXT_RSA_PUBLIC_KEY
       VITE_APP_RSA_PRIVATE_KEY = .env 的 NUXT_RSA_PRIVATE_KEY
  3. 密钥只留在 .env 与 *.local，不要贴进文档、Issue 或聊天记录。
EOF
