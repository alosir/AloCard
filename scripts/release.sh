#!/usr/bin/env bash
# AloCard 发版脚本。
# 用法：bash scripts/release.sh <versionName> <versionCode>
#   如：bash scripts/release.sh 1.1.0 2
# 前置：android/keystore/alocard-release.keystore 与 keystore.properties 已就位（见 android/README.md）
# 可选：环境变量 GITHUB_TOKEN 有效时自动创建 GitHub Release 并上传 APK；否则输出手动指引。
set -euo pipefail
cd "$(dirname "$0")/.."

if [ $# -lt 2 ]; then
  echo "用法: bash scripts/release.sh <versionName> <versionCode>  （如 1.1.0 2）" >&2
  exit 1
fi
VERSION_NAME="$1"
VERSION_CODE="$2"
TAG="v${VERSION_NAME}"

# 1. 更新版本号（单一事实来源：android/gradle.properties）
sed -i "s/^alocard.versionName=.*/alocard.versionName=${VERSION_NAME}/" android/gradle.properties
sed -i "s/^alocard.versionCode=.*/alocard.versionCode=${VERSION_CODE}/" android/gradle.properties
echo "→ 版本：versionName=${VERSION_NAME} versionCode=${VERSION_CODE}"

# 2. 构建 Release APK
echo "→ 构建 Release APK…"
(cd android && JAVA_HOME="${JAVA_HOME:-F:/Program Files/Android/Android Studio/jbr}" ./gradlew assembleRelease --console=plain)

APK_SRC="android/app/build/outputs/apk/release/app-release.apk"
[ -f "$APK_SRC" ] || { echo "Release APK 未生成：$APK_SRC" >&2; exit 1; }

# 3. 归档到 releases/
mkdir -p releases
APK_OUT="releases/AloCard-${TAG}.apk"
cp "$APK_SRC" "$APK_OUT"
echo "→ APK：$APK_OUT（$(du -h "$APK_OUT" | cut -f1)）"

# 4. 提交版本变更 + 标签 + 推送
git add android/gradle.properties CHANGELOG.md
git commit -m "release: ${TAG}" || true
git push origin main
git tag -a "$TAG" -m "AloCard ${TAG}"
git push origin "$TAG"
echo "→ 已推送标签 ${TAG}"

# 5. GitHub Release（需要 token；公开仓库的读取不需要，创建需要）
if [ -n "${GITHUB_TOKEN:-}" ]; then
  echo "→ 创建 GitHub Release…"
  RELEASE_ID=$(curl -s -X POST -H "Authorization: token ${GITHUB_TOKEN}" \
    -d "{\"tag_name\":\"${TAG}\",\"name\":\"AloCard ${TAG}\",\"body\":\"详见 CHANGELOG.md\"}" \
    https://api.github.com/repos/alosir/AloCard/releases | grep -o '"id": *[0-9]*' | head -1 | grep -o '[0-9]*')
  curl -s -X POST -H "Authorization: token ${GITHUB_TOKEN}" \
    -H "Content-Type: application/vnd.android.package-archive" \
    --data-binary @"$APK_OUT" \
    "https://uploads.github.com/repos/alosir/AloCard/releases/${RELEASE_ID}/assets?name=AloCard-${TAG}.apk" \
    -o /dev/null -w "APK 上传：HTTP %{http_code}\n"
  echo "→ 完成：https://github.com/alosir/AloCard/releases/tag/${TAG}"
else
  echo ""
  echo "未设置 GITHUB_TOKEN（或已失效）。请二选一："
  echo "  1) 在浏览器打开 https://github.com/alosir/AloCard/releases/new?tag=${TAG} 上传 $APK_OUT"
  echo "  2) 提供有效 PAT 后重跑本脚本自动发布"
fi
