#!/usr/bin/env bash
# AloCard 官网与 APK 镜像同步脚本（阿里云 ECS，workbench CLI）。
# 用法：
#   bash scripts/deploy-website.sh            # 只同步官网（website/ → /var/www/card）
#   bash scripts/deploy-website.sh 0.2.4      # 同步官网 + releases/AloCard-v0.2.4.apk 到下载目录
# 前置：workbench CLI 已配置凭证；website/ 为本地官网源码（不入库）。
set -euo pipefail
cd "$(dirname "$0")/.."
export MSYS_NO_PATHCONV=1   # 防止 Git Bash 把 /var/www 转成 Windows 路径

INSTANCE="i-0jl74tl0h58wis1uui3i"
REMOTE_DIR="/var/www/card"
TAG="${1:-}"

echo "→ 目标实例：$INSTANCE  目录：$REMOTE_DIR"

# 0. 远端目录就位
workbench exec --instance-id "$INSTANCE" --timeout 30 --command \
  "mkdir -p $REMOTE_DIR/assets/fonts $REMOTE_DIR/assets/banks $REMOTE_DIR/assets/shots $REMOTE_DIR/download"

# 1. 同步官网文件（md5 对比，变了才传）
sync_file() {
  local local_path="$1" remote_path="$2"
  [ -f "$local_path" ] || { echo "  跳过（本地不存在）：$local_path"; return 0; }
  local md5_local
  md5_local=$(md5sum "$local_path" | cut -d' ' -f1)
  local md5_remote
  md5_remote=$(workbench exec --instance-id "$INSTANCE" --timeout 30 --command \
    "md5sum '$remote_path' 2>/dev/null | cut -d' ' -f1" | tr -d '\r' | tail -1)
  if [ "$md5_local" = "$md5_remote" ]; then
    echo "  未变化：$remote_path"
  else
    workbench upload "$local_path" "$remote_path" --instance-id "$INSTANCE" --force 2>&1 | tail -1
    echo "  已同步：$remote_path"
  fi
}

echo "→ 同步官网文件…"
sync_file website/index.html "$REMOTE_DIR/index.html"
while IFS= read -r f; do
  rel="${f#website/}"
  sync_file "$f" "$REMOTE_DIR/$rel"
done < <(find website/assets -type f | sort)

# 2. 同步 APK（指定版本时）
if [ -n "$TAG" ]; then
  APK="releases/AloCard-v${TAG}.apk"
  [ -f "$APK" ] || { echo "APK 不存在：$APK" >&2; exit 1; }
  echo "→ 同步 APK：$APK"
  sync_file "$APK" "$REMOTE_DIR/download/AloCard-latest.apk"
  sync_file "$APK" "$REMOTE_DIR/download/AloCard-v${TAG}.apk"
  LOCAL_SHA=$(sha256sum "$APK" | cut -d' ' -f1)
  REMOTE_SHA=$(workbench exec --instance-id "$INSTANCE" --timeout 30 --command \
    "sha256sum $REMOTE_DIR/download/AloCard-latest.apk | cut -d' ' -f1" | tr -d '\r' | tail -1)
  [ "$LOCAL_SHA" = "$REMOTE_SHA" ] && echo "→ APK 校验一致（sha256 ${LOCAL_SHA:0:12}…）" \
    || { echo "APK 校验不一致！" >&2; exit 1; }
fi

echo "→ 完成：https://card.alosir.com"
