#!/usr/bin/env bash
# Flow-Miner Shell - unified CLI for analysis
#
# Usage:
#   ./fmsh.sh <subcommand> [args...]
#
# Subcommands:
#   git-commits      Parse git repository and output commits (alias: git-mine)
#   github-workitems Fetch GitHub issues and output them (alias: github-mine)
#
# Examples:
#   ./fmsh.sh --help
#   ./fmsh.sh git-commits -h
#   ./fmsh.sh github-workitems -h

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

./gradlew :analysis:fmsh:run --args="$*" --quiet
