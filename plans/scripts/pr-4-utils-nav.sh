#!/bin/bash
#
# PR 4: App Module - Utilities and Navigation
# Creates branch, stages files, and commits
#
# Usage: ./pr-4-utils-nav.sh [base-branch]
# Example: ./pr-4-utils-nav.sh main
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

BRANCH_NAME="feature/shortcuts-v2-utils-nav"
BASE_BRANCH="${1:-main}"

echo "=========================================="
echo "PR 4: App Module - Utilities and Navigation"
echo "Branch: $BRANCH_NAME"
echo "Base: $BASE_BRANCH"
echo "=========================================="
echo ""

cd "$PROJECT_ROOT"

# Check for uncommitted changes
if ! git diff-index --quiet HEAD --; then
    echo "Error: You have uncommitted changes."
    echo "Please commit or stash them before running this script."
    exit 1
fi

# Checkout base branch and update
echo "→ Checking out $BASE_BRANCH..."
git checkout "$BASE_BRANCH"
git pull origin "$BASE_BRANCH"

# Delete branch if exists
if git show-ref --verify --quiet "refs/heads/$BRANCH_NAME"; then
    echo "→ Deleting existing branch..."
    git branch -D "$BRANCH_NAME"
fi

# Create new branch
echo "→ Creating branch $BRANCH_NAME..."
git checkout -b "$BRANCH_NAME"

# Stage files
echo "→ Staging files..."
git add app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/util/ShortcutIconRenderer.kt
git add app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/navigation/ShortcutsNavigation.kt

echo ""
echo "→ Files staged:"
git status --short
echo ""

# Commit
echo "→ Committing..."
git commit -m "feat(shortcuts): add utilities and navigation

Add icon rendering and navigation:
- ShortcutIconRenderer for bitmap generation
- ShortcutsNavigation for navigation graph

Depends on PR 3."

echo ""
echo "=========================================="
echo "✓ PR 4 branch created and committed!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "  1. Push: git push -u origin $BRANCH_NAME"
echo "  2. Create PR on GitHub"
echo "  3. After merge, you can run PR 5 and PR 6 in parallel:"
echo "     - ./pr-5-viewmodels.sh"
echo "     - ./pr-6-ui.sh"
