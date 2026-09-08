#!/usr/bin/env bash
# Builds the current, possibly-uncommitted working tree as a debug APK and
# uploads it straight to Firebase App Distribution's "wip-debug" group --
# dev-only convenience path, no CI, no static analysis, no code review.
#
# Fails fast: runs a Kotlin-compile-only pass first so a broken mid-edit
# tree doesn't waste time on a full assemble.
#
# Standalone-runnable: bare terminal, cron, or the distribute-wip-debug
# skill -- same invocation, zero args. Every exit path (success or any
# failure stage) writes a machine-readable status JSON to a fixed,
# repo-derived path so a caller never has to parse this script's stdout --
# stdout is purely the human-readable build/upload log.
#
# Never reads firebase-app-distribution-service-account.json -- only points
# GOOGLE_APPLICATION_CREDENTIALS at its path.
#
# Usage:
#   scripts/distribute-wip-debug.sh

set -euo pipefail

SRC="$(git rev-parse --show-toplevel)"
cd "$SRC"

FIREBASE_APP_ID="1:1044553396320:android:f113802dc50e178f445305" # debug app, same as deploy-internal
GROUP="wip-debug"
SERVICE_ACCOUNT="$SRC/firebase-app-distribution-service-account.json"
APK_PATH="$SRC/app/build/outputs/apk/debug/app-debug.apk"

TIMESTAMP="$(date +%Y%m%d-%H%M%S)"

# Fixed, repo-derived status path -- avoids collisions between worktrees of
# the same repo running this concurrently, no stdout parsing needed.
STATUS_DIR="/tmp/distribute-wip-debug"
REPO_HASH="$(printf '%s' "$SRC" | cksum | cut -d' ' -f1)"
STATUS_FILE="$STATUS_DIR/${REPO_HASH}.json"

escape_json() {
  local s="$1"
  s="${s//\\/\\\\}"
  s="${s//\"/\\\"}"
  s="${s//$'\n'/\\n}"
  printf '%s' "$s"
}

write_status() {
  local success="$1" stage="$2" message="$3"
  mkdir -p "$STATUS_DIR"
  local escaped
  escaped="$(escape_json "$message")"
  cat > "$STATUS_FILE" <<JSON
{
  "success": $success,
  "stage": "$stage",
  "message": "$escaped",
  "timestamp": "$TIMESTAMP"
}
JSON
}

fail() {
  local stage="$1" message="$2"
  write_status false "$stage" "$message"
  echo "FAILED: $stage -- see output above" >&2
  exit 1
}

if ! command -v firebase >/dev/null 2>&1; then
  echo "firebase CLI not installed (npm i -g firebase-tools)" >&2
  fail "preflight" "dirty build failed (preflight: firebase CLI not installed)"
fi

if [[ ! -f "$SERVICE_ACCOUNT" ]]; then
  echo "missing $SERVICE_ACCOUNT (copy-worktree-secrets.sh should have placed it)" >&2
  fail "preflight" "dirty build failed (preflight: missing service-account json)"
fi

echo "== compile check (compileDebugKotlin) =="
if ! ./gradlew compileDebugKotlin; then
  fail "compile" "dirty build failed (compile)"
fi

echo "== assembling debug APK (dirty tree) =="
if ! ./gradlew assembleDebug; then
  fail "assemble" "dirty build failed (assemble)"
fi

if [[ ! -f "$APK_PATH" ]]; then
  echo "APK not found at $APK_PATH" >&2
  fail "assemble" "dirty build failed (assemble: apk not found)"
fi

DIRTY_FILES="$(git status --short | head -20)"
if [[ -z "$DIRTY_FILES" ]]; then
  DIRTY_FILES="(clean tree at $(git rev-parse --short HEAD))"
fi
RELEASE_NOTES="wip build ${TIMESTAMP}
$(git rev-parse --short HEAD)

dirty files:
${DIRTY_FILES}"

echo "== uploading to Firebase App Distribution (${GROUP}) =="
if ! GOOGLE_APPLICATION_CREDENTIALS="$SERVICE_ACCOUNT" firebase appdistribution:distribute \
  "$APK_PATH" \
  --app "$FIREBASE_APP_ID" \
  --groups "$GROUP" \
  --release-notes "$RELEASE_NOTES"; then
  fail "upload" "dirty build failed (upload)"
fi

SUCCESS_MESSAGE="wip-debug build ${TIMESTAMP} uploaded"
write_status true "upload" "$SUCCESS_MESSAGE"
echo "SUCCESS: $SUCCESS_MESSAGE"
