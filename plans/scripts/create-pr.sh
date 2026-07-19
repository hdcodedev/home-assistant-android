#!/bin/bash
#
# Create a Shortcuts V2 review slice: branch from base, stage the slice's files
# from the integration branch, and commit.
#
# Usage:
#   ./create-pr.sh <SLICE_NUMBER> [base-branch]
#
# Examples:
#   ./create-pr.sh 1                    # base = main
#   ./create-pr.sh 5 some-other-branch  # base = some-other-branch
#
# Environment overrides:
#   SOURCE_BRANCH=feat/shortcuts-v2         # branch the files are pulled from
#   BASE_BRANCH_DEFAULT=main                # base branch for slice PRs
#   NO_COMMIT=1                             # stage only, skip the commit
#
# Note: each slice compiles and passes tests independently when merged in
# order (1 through 12). Verified by verify.sh.
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# shellcheck source=pr-manifest.sh
source "${SCRIPT_DIR}/pr-manifest.sh"

if [ $# -eq 0 ]; then
    pr_usage "$0"
    exit 1
fi

PR_NUMBER=$1
BASE_BRANCH="${2:-$BASE_BRANCH_DEFAULT}"

if ! pr_valid "$PR_NUMBER"; then
    echo "Error: invalid slice number '$PR_NUMBER' (must be 1-$PR_COUNT)."
    echo ""
    pr_usage "$0"
    exit 1
fi

IFS='|' read -r _BRANCH_NAME TITLE REFS <<< "$(pr_fields "$PR_NUMBER")"
COMMIT_TITLE="$(pr_commit_title "$PR_NUMBER")"
SLICE_BRANCH="$(pr_branch "$PR_NUMBER")"

cd "$PROJECT_ROOT"

if ! git rev-parse --git-dir > /dev/null 2>&1; then
    echo "Error: not a git repository. Run from the project root."
    exit 1
fi

# Make sure the source branch exists locally.
if ! git show-ref --verify --quiet "refs/heads/$SOURCE_BRANCH" &&
   ! git show-ref --verify --quiet "refs/remotes/origin/$SOURCE_BRANCH"; then
    echo "Error: source branch '$SOURCE_BRANCH' not found locally or on origin."
    exit 1
fi

echo "=========================================="
echo "Slice $PR_NUMBER/$PR_COUNT: $TITLE"
echo "Branch:     $SLICE_BRANCH"
echo "Base:       $BASE_BRANCH"
echo "Source:     $SOURCE_BRANCH"
echo "References: $REFS"
echo "=========================================="
echo ""

# Require a clean tree.
if ! git diff-index --quiet HEAD --; then
    echo "Error: you have uncommitted changes. Commit or stash them first."
    exit 1
fi

# Checkout + update base.
echo "→ Checking out $BASE_BRANCH..."
git checkout "$BASE_BRANCH"
git pull --ff-only origin "$BASE_BRANCH" || echo "  (skipping pull)"

# Recreate the slice branch.
if git show-ref --verify --quiet "refs/heads/$SLICE_BRANCH"; then
    echo "→ Deleting existing branch $SLICE_BRANCH..."
    git branch -D "$SLICE_BRANCH"
fi
echo "→ Creating branch $SLICE_BRANCH from $BASE_BRANCH..."
git checkout -b "$SLICE_BRANCH"

# Stage the slice's files from the source branch.
echo "→ Staging slice files from $SOURCE_BRANCH..."
STAGED=0
MISSING=0
while IFS= read -r file; do
    [ -z "$file" ] && continue
    if git checkout "$SOURCE_BRANCH" -- "$file" 2>/dev/null; then
        echo "  ✓ $file"
        ((STAGED++)) || true
    else
        echo "  ✗ $file (not found on $SOURCE_BRANCH)"
        ((MISSING++)) || true
    fi
done < <(pr_files "$PR_NUMBER")

# Delete files declared for this slice (e.g. replaced/obsolete files).
DELETED=0
while IFS= read -r file; do
    [ -z "$file" ] && continue
    if [ -e "$file" ]; then
        git rm -q "$file"
        echo "  ✗ (deleted) $file"
        ((DELETED++)) || true
    else
        echo "  ⚠ $file (already absent, skipping delete)"
    fi
done < <(pr_files_to_delete "$PR_NUMBER")

# Guard: lockfiles must never be part of a slice.
echo ""
echo "→ Checking for stray gradle.lockfile changes..."
LOCK_DIRTY=0
for lf in "${LOCKFILES[@]}"; do
    if ! git diff --quiet "$BASE_BRANCH" -- "$lf" 2>/dev/null; then
        echo "  ⚠ $lf differs from $BASE_BRANCH — it should NOT be in this slice."
        LOCK_DIRTY=1
    fi
done
[ "$LOCK_DIRTY" -eq 0 ] && echo "  ✓ no lockfile drift"

echo ""
echo "=========================================="
echo "Staged: $STAGED   Deleted: $DELETED   Missing: $MISSING"
echo "=========================================="
git status --short
echo ""

if [ "$MISSING" -gt 0 ]; then
    echo "Warning: some files were missing on $SOURCE_BRANCH. Check the manifest."
fi

# Commit (unless NO_COMMIT).
if [ "${NO_COMMIT:-0}" = "1" ]; then
    echo "NO_COMMIT=1 set — leaving changes staged without committing."
else
    git commit -m "$COMMIT_TITLE"
    echo ""
    echo "✓ Committed slice $PR_NUMBER."
fi

echo ""
echo "Next steps:"
echo "  git push -u origin $SLICE_BRANCH"
echo "  gh pr create --base $BASE_BRANCH --title \"$COMMIT_TITLE\" \\"
echo "    --template .github/pull_request_template.md"
