#!/usr/bin/env bash
set -euo pipefail

# Extract release notes for a specific version from changelog files
# Usage: ./scripts/extract-release-notes.sh <version> <changelog1> [changelog2...]
# Example: ./scripts/extract-release-notes.sh v0.0.1 analysis/CHANGELOG.md visualization/CHANGELOG.md

if [[ $# -lt 2 ]]; then
  echo "Usage: $0 <version> <changelog1> [changelog2...]" >&2
  echo "Example: $0 v0.0.1 analysis/CHANGELOG.md visualization/CHANGELOG.md" >&2
  exit 1
fi

VERSION="$1"
shift

# Strip 'v' prefix: v0.0.4 -> 0.0.4
VER="${VERSION#v}"

# Extract section for this version (from ## [x.y.z] until next ## [)
extract_version() {
  local file="$1"
  local name
  name="$(basename "$(dirname "$file")")"
  # Capitalize first letter (portable)
  name="$(echo "$name" | awk '{print toupper(substr($0,1,1)) substr($0,2)}')"

  local content
  content="$(sed -n "/^## \[${VER}\]/,/^## \[/p" "$file" | sed '$d')"

  echo "# $name"
  if [[ -z "$content" ]]; then
    echo "No changes."
  else
    echo "$content"
  fi
  echo ""
}

first=true
for changelog in "$@"; do
  if [[ ! -f "$changelog" ]]; then
    echo "Warning: $changelog not found, skipping" >&2
    continue
  fi

  if [[ "$first" == "true" ]]; then
    first=false
  else
    echo "---"
    echo ""
  fi

  extract_version "$changelog"
done
