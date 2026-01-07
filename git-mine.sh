#!/usr/bin/env bash
# Usage: ./git-mine.sh <path> --format table|jsonl [--since "'<time>'"] [--output FILE]
# Example: ./git-mine.sh . --format jsonl --since "'1 year ago' -o project.commits.fm.jsonl"
# Note: Values with spaces need inner quotes due to gradle's --args parsing

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
"$SCRIPT_DIR/gradlew" :analysis:gitcli:run --args="$*"
