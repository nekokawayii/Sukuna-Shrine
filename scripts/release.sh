#!/usr/bin/env bash
# 版本留档：给当前版本打 tag，并在 GitHub Release 上传构建产物（方便随时回档）。
#
# 用法：
#   bash scripts/release.sh                 # 用默认说明发当前版本
#   bash scripts/release.sh notes.md        # 用 notes.md 的内容当发行说明（markdown）
#
# 约定：
#   · 版本号取自 build.gradle 的 version（如 1.6.0-r189），tag 为 v<版本号>
#   · 产物取自 C:\Users\Administrator\Desktop\打包（gradlew build 会自动同步过去）
#   · GitHub 凭证复用仓库已存的 git 凭证（git credential fill），不需要装 gh
set -euo pipefail
cd "$(dirname "$0")/.."

VER=$(sed -n "s/^version = '\(.*\)'/\1/p" build.gradle | head -1)
[ -n "$VER" ] || { echo "读不到 build.gradle 里的 version"; exit 1; }
TAG="v$VER"
DIST="${DIST_DIR:-C:/Users/Administrator/Desktop/打包}"
JARS=("$DIST/sukuna-forge-1.20.1-$VER-auto-share.jar" "$DIST/sukuna-forge-1.20.1-$VER-ask.jar")
for j in "${JARS[@]}"; do [ -f "$j" ] || { echo "缺少产物：$j（先跑 ./gradlew build）"; exit 1; }; done

REPO=$(git remote get-url origin | sed -E 's#.*github\.com[:/]([^/]+/[^/.]+?)(\.git)?$#\1#')
[ -n "$REPO" ] || { echo "读不到 origin 仓库"; exit 1; }
TOKEN=$(printf 'protocol=https\nhost=github.com\n\n' | git credential fill 2>/dev/null | sed -n 's/^password=//p')
[ -n "$TOKEN" ] || { echo "取不到 GitHub 凭证（git credential fill 为空）"; exit 1; }
API="https://api.github.com/repos/$REPO"

if git rev-parse -q --verify "refs/tags/$TAG" >/dev/null; then
  echo "tag $TAG 已存在"
else
  git tag "$TAG"; git push origin "$TAG"; echo "已创建并推送 tag $TAG"
fi

ID=$(curl -s -H "Authorization: Bearer $TOKEN" "$API/releases/tags/$TAG" | grep -o '"id": *[0-9]*' | head -1 | grep -o '[0-9]*' || true)
if [ -z "${ID:-}" ]; then
  NOTES="版本 $VER。产物：$(basename "${JARS[0]}")（同意共享）与 $(basename "${JARS[1]}")（同意询问），二选一放进 mods/。"
  [ $# -ge 1 ] && [ -f "$1" ] && NOTES="$(cat "$1")"
  printf '{"tag_name":"%s","name":"%s","body":%s,"draft":false,"prerelease":false}' \
    "$TAG" "$VER" "$(printf '%s' "$NOTES" | node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>process.stdout.write(JSON.stringify(s)))')" > /tmp/rel.json
  ID=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Accept: application/vnd.github+json" \
    -H "Content-Type: application/json" --data-binary @/tmp/rel.json "$API/releases" \
    | grep -o '"id": *[0-9]*' | head -1 | grep -o '[0-9]*')
  [ -n "$ID" ] && echo "已创建 Release $TAG（id=$ID）"
else
  echo "Release $TAG 已存在（id=$ID），只补传缺的产物"
fi
[ -n "${ID:-}" ] || { echo "建 Release 失败"; exit 1; }

HAVE=$(curl -s -H "Authorization: Bearer $TOKEN" "$API/releases/$ID/assets" | grep -o '"name": *"[^"]*"' || true)
for j in "${JARS[@]}"; do
  n=$(basename "$j")
  if printf '%s' "$HAVE" | grep -qF "$n"; then echo "跳过已上传的 $n"; continue; fi
  echo -n "上传 $n ... "
  RESP=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Accept: application/vnd.github+json" \
    -H "Content-Type: application/java-archive" --data-binary @"$j" \
    "https://uploads.github.com/repos/$REPO/releases/$ID/assets?name=$n" || true)
  printf '%s' "$RESP" | grep -o '"state": *"[a-z]*"' | head -1 || echo "上传结果未知（响应：${RESP:0:120}）"
done
echo "完成：https://github.com/$REPO/releases/tag/$TAG"
