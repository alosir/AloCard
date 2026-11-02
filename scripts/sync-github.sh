#!/usr/bin/env bash
# AloCard → GitHub 自动同步。
# 用法：bash scripts/sync-github.sh ["提交说明"]   （无参数则用带时间戳的默认说明）
set -euo pipefail
cd "$(dirname "$0")/.."

MSG="${1:-chore: 自动同步 $(date '+%Y-%m-%d %H:%M')}"

git add -A
if git diff --cached --quiet; then
  echo "无变更，跳过提交"
else
  git commit -m "$MSG"
fi

git push origin main
echo "已同步到 GitHub main"
