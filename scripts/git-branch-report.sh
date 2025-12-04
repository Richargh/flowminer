#!/bin/bash

# Git Branch Report
# Shows all local branches with: first commit, last commit, relative time, ahead/behind, merged status
#
# Usage: git-branch-report.sh [repo-path] [base-branch]

REPO_PATH="${1:-.}"

if [ ! -d "$REPO_PATH/.git" ]; then
    echo "Error: '$REPO_PATH' is not a git repository"
    exit 1
fi

cd "$REPO_PATH" || exit 1

# Detect default branch from origin/HEAD (e.g., "origin/main" -> "main")
DEFAULT_BRANCH=$(git symbolic-ref refs/remotes/origin/HEAD 2>/dev/null | sed 's|refs/remotes/origin/||')
[ -z "$DEFAULT_BRANCH" ] && DEFAULT_BRANCH="main"

# Base branch for comparison (defaults to default branch)
BASE_BRANCH="${2:-$DEFAULT_BRANCH}"

# Cache merged remote branches (1 git call instead of N)
merged_remote=$(git branch -r --merged "origin/$BASE_BRANCH" 2>/dev/null | tr -d ' ')

# Collect all output, then use column for alignment
{
    echo " |BRANCH|FIRST|FIRST_AUTHOR|LAST|LAST_AUTHOR|AGE|RELATIVE|BEHIND/AHEAD|MERGED"

    # Use for-each-ref to get branch name, last commit date, author, and relative time in one call
    git for-each-ref --sort=-committerdate --format='%(refname:short)|%(authordate:short)|%(authorname)|%(authordate:relative)' refs/remotes | while IFS='|' read -r branch last last_author relative; do
        # Skip HEAD pointer
        [[ "$branch" == */HEAD ]] && continue

        # Determine marker: * for default branch, > for base branch (if different)
        marker=" "
        branch_name="${branch#origin/}"
        [ "$branch_name" = "$DEFAULT_BRANCH" ] && marker="*"
        [ "$branch_name" = "$BASE_BRANCH" ] && [ "$BASE_BRANCH" != "$DEFAULT_BRANCH" ] && marker=">"

        # First commit on branch
        if [ "$branch" = "origin/$BASE_BRANCH" ]; then
            # For base branch, get the very first commit in history
            first_info=$(git log --format='%as|%an' --reverse "$branch" 2>/dev/null | head -1)
        else
            # For other branches, find first commit after diverging from base
            merge_base=$(git merge-base "$BASE_BRANCH" "$branch" 2>/dev/null)
            if [ -n "$merge_base" ]; then
                first_info=$(git log "$merge_base".."$branch" --format='%as|%an' --reverse 2>/dev/null | head -1)
                # If empty (no unique commits), use the branch tip
                [ -z "$first_info" ] && first_info=$(git log -1 --format='%as|%an' "$branch" 2>/dev/null)
            else
                first_info=$(git log --format='%as|%an' --reverse "$branch" 2>/dev/null | head -1)
            fi
        fi
        first=$(echo "$first_info" | cut -d'|' -f1)
        first_author=$(echo "$first_info" | cut -d'|' -f2)

        # Calculate age (difference between first and last commit dates)
        age=""
        if [ -n "$first" ] && [ -n "$last" ]; then
            first_ts=$(date -j -f "%Y-%m-%d" "$first" "+%s" 2>/dev/null || date -d "$first" "+%s" 2>/dev/null)
            last_ts=$(date -j -f "%Y-%m-%d" "$last" "+%s" 2>/dev/null || date -d "$last" "+%s" 2>/dev/null)
            if [ -n "$first_ts" ] && [ -n "$last_ts" ]; then
                diff_days=$(( (last_ts - first_ts) / 86400 ))
                if [ "$diff_days" -eq 0 ]; then
                    age="<1d"
                elif [ "$diff_days" -lt 7 ]; then
                    age="${diff_days}d"
                elif [ "$diff_days" -lt 30 ]; then
                    age="$(( diff_days / 7 ))w"
                elif [ "$diff_days" -lt 365 ]; then
                    age="$(( diff_days / 30 ))mo"
                else
                    age="$(( diff_days / 365 ))y"
                fi
            fi
        fi

        # Ahead/behind (unavoidable per-branch call)
        ahead_behind=$(git rev-list --left-right --count "$BASE_BRANCH"..."$branch" 2>/dev/null | tr '\t' '/')

        # Check against cached merged branches
        if echo "$merged_remote" | grep -qx "$branch"; then
            merged="Y"
        else
            merged=""
        fi

        echo "$marker|$branch|$first|$first_author|$last|$last_author|$age|$relative|$ahead_behind|$merged"
    done
} | column -t -s'|'