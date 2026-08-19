#!/usr/bin/env bash
# Copies gitignored local secrets (local.properties, google-services.json,
# service-account/cred JSONs, signing keystores) from this checkout into a
# freshly created worktree, so the app builds there without re-fetching
# credentials from Firebase console / Play Console by hand.
#
# Usage:
#   scripts/copy-worktree-secrets.sh <path-to-worktree>
#   scripts/copy-worktree-secrets.sh -f <path-to-worktree>   # overwrite existing files
#
# Never prints file contents -- only paths copied/skipped. Safe to run from
# an agent session; it does not require reading the secret files' contents.

set -euo pipefail

FORCE=0
if [[ "${1:-}" == "-f" ]]; then
  FORCE=1
  shift
fi

DST="${1:-}"
if [[ -z "$DST" ]]; then
  echo "usage: $0 [-f] <path-to-worktree>" >&2
  exit 1
fi

SRC="$(git rev-parse --show-toplevel)"
DST="$(cd "$DST" 2>/dev/null && pwd || true)"
if [[ -z "$DST" ]]; then
  echo "error: destination worktree path does not exist: ${1}" >&2
  exit 1
fi
if [[ "$DST" == "$SRC" ]]; then
  echo "error: destination is same as source checkout ($SRC)" >&2
  exit 1
fi

copy_one() {
  local rel="$1"
  local src_file="$SRC/$rel"
  local dst_file="$DST/$rel"
  [[ -f "$src_file" ]] || return 0
  if [[ -f "$dst_file" && "$FORCE" -ne 1 ]]; then
    echo "skip (exists): $rel"
    return 0
  fi
  mkdir -p "$(dirname "$dst_file")"
  cp "$src_file" "$dst_file"
  echo "copied: $rel"
}

# --- fixed gitignored files ---
copy_one "local.properties"
copy_one "app/google-services.json"

# --- pattern-matched gitignored files (service-account / cred JSONs) ---
while IFS= read -r -d '' f; do
  rel="${f#"$SRC"/}"
  copy_one "$rel"
done < <(find "$SRC" \( -path "$SRC/.git" -o -name ".gradle" -o -name "build" \) -prune -o \
  \( -name "*service-account*.json" -o -name "*.cred.json" \) -print0)

# --- keystore files referenced from local.properties ---
if [[ -f "$SRC/local.properties" ]]; then
  for key in DEBUG_STORE_FILE RELEASE_STORE_FILE; do
    value="$(grep -E "^${key}=" "$SRC/local.properties" 2>/dev/null | head -1 | cut -d'=' -f2- || true)"
    [[ -z "$value" ]] && continue
    if [[ "$value" = /* ]]; then
      # absolute path: copy alongside itself outside the repo tree is not our
      # job -- just report it so the human/agent knows it must exist at DST too.
      echo "note: $key points outside repo ($value) -- verify it's reachable from $DST too"
    else
      copy_one "$value"
    fi
  done
fi

echo "done."
