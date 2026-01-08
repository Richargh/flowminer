#!/usr/bin/env bash
set -euo pipefail

# Release script for teamcharta
# Usage: ./scripts/release.sh [OPTIONS]
# Test Usage: ./scripts/release.sh --no-commit-release --no-commit-snapshot --no-tag

# Defaults
EXPLICIT_VERSION=""
DO_COMMIT_RELEASE=true
DO_COMMIT_SNAPSHOT=true
DO_TAG=true

VERSION_FILE="VERSION"
CHANGELOGS=(
  "analysis/CHANGELOG.md"
  "visualization/CHANGELOG.md"
)

usage() {
  cat << EOF
Usage: $0 [OPTIONS]

Options:
  --version <x.x.x>     Explicit version to release (overrides VERSION file)
  --no-commit-release   Skip creating the release commit
  --no-commit-snapshot  Skip creating the snapshot version commit
  --no-tag              Skip creating the git tag
  --help                Show this help message

Examples:
  $0                           # Release based on VERSION file
  $0 --version 1.0.0           # Release explicit version
  $0 --no-commit-release       # Update files but don't commit release
  $0 --no-tag --no-commit-snapshot  # Only make release commit
EOF
  exit 0
}

# Parse arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --version)
      EXPLICIT_VERSION="$2"
      shift 2
      ;;
    --no-commit-release)
      DO_COMMIT_RELEASE=false
      shift
      ;;
    --no-commit-snapshot)
      DO_COMMIT_SNAPSHOT=false
      shift
      ;;
    --no-tag)
      DO_TAG=false
      shift
      ;;
    --help)
      usage
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      ;;
  esac
done

# --- Step 1: Determine release version ---

if [[ -n "$EXPLICIT_VERSION" ]]; then
  RELEASE_VERSION="$EXPLICIT_VERSION"
  echo "Using explicit version: $RELEASE_VERSION"
else
  if [[ ! -f "$VERSION_FILE" ]]; then
    echo "Error: VERSION file not found" >&2
    exit 1
  fi

  CURRENT_VERSION=$(cat "$VERSION_FILE" | tr -d '[:space:]')

  if [[ ! "$CURRENT_VERSION" =~ -SNAPSHOT$ ]]; then
    echo "Error: VERSION file must contain -SNAPSHOT suffix (got: $CURRENT_VERSION)" >&2
    exit 1
  fi

  # Strip -SNAPSHOT suffix
  RELEASE_VERSION="${CURRENT_VERSION%-SNAPSHOT}"
  echo "Current version: $CURRENT_VERSION"
  echo "Release version: $RELEASE_VERSION"
fi

# Validate version format (x.y.z)
if [[ ! "$RELEASE_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Error: Invalid version format: $RELEASE_VERSION (expected x.y.z)" >&2
  exit 1
fi

# --- Step 2: Update changelogs ---

TODAY=$(date +%Y-%m-%d)
UPDATED_CHANGELOGS=()

# Check if unreleased section has content
has_unreleased_content() {
  local file="$1"
  # Extract content between [unreleased] and next ## [ section
  # Check if there's any non-blank line
  local content
  content=$(sed -n '/^## \[unreleased\]/,/^## \[/p' "$file" | sed '1d;$d' | grep -v '^[[:space:]]*$' || true)
  [[ -n "$content" ]]
}

# Insert version header after unreleased section header
update_changelog() {
  local file="$1"
  local version="$2"
  local date="$3"

  # Insert new version line after the unreleased header line
  # The unreleased line stays, we add a blank line + new version header after it
  sed -i '' "/^## \[unreleased\]/a\\
\\
## [$version] - $date
" "$file"

  echo "Updated: $file"
}

for changelog in "${CHANGELOGS[@]}"; do
  if [[ ! -f "$changelog" ]]; then
    echo "Warning: $changelog not found, skipping" >&2
    continue
  fi

  if has_unreleased_content "$changelog"; then
    update_changelog "$changelog" "$RELEASE_VERSION" "$TODAY"
    UPDATED_CHANGELOGS+=("$changelog")
  else
    echo "Skipping $changelog (no unreleased content)"
  fi
done

# --- Step 3: Update VERSION file and commit release ---

echo "$RELEASE_VERSION" > "$VERSION_FILE"
echo "Updated VERSION to: $RELEASE_VERSION"

if [[ "$DO_COMMIT_RELEASE" == "true" ]]; then
  # Stage VERSION and any updated changelogs
  git add "$VERSION_FILE"
  for changelog in "${UPDATED_CHANGELOGS[@]}"; do
    git add "$changelog"
  done

  git commit -m ". V (VERSION) release v$RELEASE_VERSION"
  echo "Created release commit"
else
  echo "Skipping release commit (--no-commit-release)"
fi

# --- Step 4: Tag release ---

if [[ "$DO_TAG" == "true" ]]; then
  if [[ "$DO_COMMIT_RELEASE" == "false" ]]; then
    echo "Warning: Tagging without release commit - tag will be on current HEAD" >&2
  fi
  git tag "v$RELEASE_VERSION"
  echo "Created tag: v$RELEASE_VERSION"
else
  echo "Skipping tag (--no-tag)"
fi

# --- Step 5: Push release ---

if [[ "$DO_TAG" == "true" && "$DO_COMMIT_RELEASE" == "true" ]]; then
  echo ""
  read -p "Push release commit and tag v$RELEASE_VERSION? [y/N] " -n 1 -r
  echo
  if [[ $REPLY =~ ^[Yy]$ ]]; then
    git push && git push origin "v$RELEASE_VERSION"
    echo "Pushed release commit and tag"
  else
    echo "Skipping push. To push later: git push && git push origin v$RELEASE_VERSION"
  fi
fi

# --- Step 6: Prepare next version ---

if [[ "$DO_COMMIT_SNAPSHOT" == "true" ]]; then
  # Increment patch version: x.y.z -> x.y.(z+1)
  IFS='.' read -r major minor patch <<< "$RELEASE_VERSION"
  NEXT_PATCH=$((patch + 1))
  NEXT_VERSION="$major.$minor.$NEXT_PATCH-SNAPSHOT"

  echo ""
  read -p "Prepare next version ($NEXT_VERSION)? [y/N] " -n 1 -r
  echo
  if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "$NEXT_VERSION" > "$VERSION_FILE"
    echo "Updated VERSION to: $NEXT_VERSION"

    git add "$VERSION_FILE"
    git commit -m ". v (VERSION) prepare next version"
    echo "Created snapshot commit"
  else
    echo "Skipping snapshot commit"
  fi
else
  echo "Skipping snapshot commit (--no-commit-snapshot)"
fi

echo ""
echo "Release complete!"
