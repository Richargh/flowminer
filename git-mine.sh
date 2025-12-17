#!/usr/bin/env bash
# Usage: ./git-mine.sh <path> --format table [--since "'<time>'"]
# Example: ./git-mine.sh . --format table --since "'2 weeks ago'"
# Note: Values with spaces need inner quotes due to gradle's --args parsing

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
"$SCRIPT_DIR/gradlew" :analysis:gitcli:run --args="$*"
