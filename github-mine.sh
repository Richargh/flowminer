#!/usr/bin/env bash
# Runs the GitHub CLI to fetch issues from a repository
#
# Usage:
#   ./github-mine.sh --token TOKEN --owner OWNER --repo REPO [--format table|jsonl] [--output FILE]
#
# Example:
#   ./github-mine.sh --token ghp_xxx --owner anthropics --repo claude-code --format jsonl -o project.workitems.fm.jsonl

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd "$SCRIPT_DIR"

./gradlew :analysis:github-cli:run --args="$*" --quiet
