#!/bin/bash
# Log all commits across all branches for the last three months
# Output: single line per commit with branch, message, author, date, and line changes
#
# Usage: git-log-all-branches.sh [repo-path]

REPO_PATH="${1:-.}"

if [ ! -d "$REPO_PATH/.git" ]; then
    echo "Error: '$REPO_PATH' is not a git repository"
    exit 1
fi

cd "$REPO_PATH" || exit 1

(echo "Branch|Author|Date|Files|Additions|Deletions|Commit Message" && \
 git log --all --source --since="3 months ago" --format="%S|%s|%an|%ad|%h" --date=short | \
 while IFS='|' read -r branch msg author date hash; do
   stats=$(git show --shortstat --format="" "$hash" | tail -1)
   files=$(echo "$stats" | grep -oE '[0-9]+ file' | grep -oE '[0-9]+')
   additions=$(echo "$stats" | grep -oE '[0-9]+ insertion' | grep -oE '[0-9]+')
   deletions=$(echo "$stats" | grep -oE '[0-9]+ deletion' | grep -oE '[0-9]+')
   echo "${branch:-(no branch)}|$author|$date|${files:-0}|${additions:-0}|${deletions:-0}|$msg"
 done) | column -t -s'|'
