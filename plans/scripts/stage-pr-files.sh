#!/bin/bash
#
# Stage a Shortcuts V2 review slice's files on the CURRENT branch.
# Assumes the files already exist in the working tree (e.g. you are on
# feat/shortcuts-v2). To create a fresh slice branch instead, use
# create-pr.sh.
#
# Usage: ./stage-pr-files.sh <SLICE_NUMBER>
# Example: ./stage-pr-files.sh 1
#

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# shellcheck source=pr-manifest.sh
source "${SCRIPT_DIR}/pr-manifest.sh"

if [ $# -eq 0 ]; then
    pr_usage "$0"
    exit 1
fi

PR_NUMBER=$1

if ! pr_valid "$PR_NUMBER"; then
    echo "Error: invalid slice number '$PR_NUMBER' (must be 1-$PR_COUNT)."
    echo ""
    pr_usage "$0"
    exit 1
fi

IFS='|' read -r _BRANCH_NAME TITLE REFS <<< "$(pr_fields "$PR_NUMBER")"
COMMIT_TITLE="$(pr_commit_title "$PR_NUMBER")"

echo "=========================================="
echo "Staging slice $PR_NUMBER/$PR_COUNT: $TITLE"
echo "=========================================="
echo ""

cd "$PROJECT_ROOT"

STAGED=0
MISSING=0
while IFS= read -r file; do
    [ -z "$file" ] && continue
    if [ -e "${file%/}" ]; then
        git add "$file"
        echo "  ✓ $file"
        ((STAGED++)) || true
    else
        echo "  ✗ $file (not found)"
        ((MISSING++)) || true
    fi
done < <(pr_files "$PR_NUMBER")

# Delete files declared for this slice.
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

echo ""
echo "=========================================="
echo "Staged: $STAGED   Deleted: $DELETED   Missing: $MISSING"
echo "=========================================="

if [ "$MISSING" -gt 0 ]; then
    echo ""
    echo "Warning: some files were not found in the working tree."
fi

echo ""
echo "Next steps:"
echo "  git status"
echo "  git commit -m \"$COMMIT_TITLE\""
