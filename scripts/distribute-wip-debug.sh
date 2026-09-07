#!/usr/bin/env bash
# Builds the current, possibly-uncommitted working tree as a debug APK and
# uploads it straight to Firebase App Distribution's "wipDebug" group --
# dev-only convenience path, no CI, no static analysis, no code review.
#
# Fails fast: runs a Kotlin-compile-only pass first so a broken mid-edit
# tree doesn't waste time on a full assemble.
#
# Never reads firebase-app-distribution-service-account.json -- only points
# GOOGLE_APPLICATION_CREDENTIALS at its path. Meant to be run in the
# background (e.g. `run_in_background: true`) so it never blocks the
# calling session; prints a single RESULT: line at the end for the caller
# to parse.
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

if ! command -v firebase >/dev/null 2>&1; then
  echo "RESULT: FAILURE preflight -- firebase CLI not installed (npm i -g firebase-tools)" >&2
  exit 1
fi

if [[ ! -f "$SERVICE_ACCOUNT" ]]; then
  echo "RESULT: FAILURE preflight -- missing $SERVICE_ACCOUNT (copy-worktree-secrets.sh should have placed it)" >&2
  exit 1
fi

echo "== compile check (compileDebugKotlin) =="
if ! ./gradlew compileDebugKotlin; then
  echo "RESULT: FAILURE compile" >&2
  exit 1
fi

echo "== assembling debug APK (dirty tree) =="
if ! ./gradlew assembleDebug; then
  echo "RESULT: FAILURE assemble" >&2
  exit 1
fi

if [[ ! -f "$APK_PATH" ]]; then
  echo "RESULT: FAILURE assemble -- APK not found at $APK_PATH" >&2
  exit 1
fi

TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
DIRTY_FILES="$(git status --short | head -20)"
if [[ -z "$DIRTY_FILES" ]]; then
  DIRTY_FILES="(clean tree at $(git rev-parse --short HEAD))"
fi
RELEASE_NOTES="wip build ${TIMESTAMP}
$(git rev-parse --short HEAD)

dirty files:
${DIRTY_FILES}"

echo "== uploading to Firebase App Distribution (${GROUP}) =="
GOOGLE_APPLICATION_CREDENTIALS="$SERVICE_ACCOUNT" firebase appdistribution:distribute \
  "$APK_PATH" \
  --app "$FIREBASE_APP_ID" \
  --groups "$GROUP" \
  --release-notes "$RELEASE_NOTES"

echo "RESULT: SUCCESS ${TIMESTAMP}"
