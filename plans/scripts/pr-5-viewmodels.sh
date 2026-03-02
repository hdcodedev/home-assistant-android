#!/bin/bash
#
# PR 5: App Module - ViewModels
# Creates branch, stages files, and commits
#
# Usage: ./pr-5-viewmodels.sh [base-branch]
# Example: ./pr-5-viewmodels.sh main
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

BRANCH_NAME="feature/shortcuts-v2-viewmodels"
BASE_BRANCH="${1:-main}"

echo "=========================================="
echo "PR 5: App Module - ViewModels"
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
git add app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/ManageShortcutsViewModel.kt
git add app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/EditShortcutViewModel.kt

echo ""
echo "→ Files staged:"
git status --short
echo ""

# Commit
echo "→ Committing..."
git commit -m "feat(shortcuts): add viewmodels

Add business logic and state management:
- ManageShortcutsViewModel for shortcuts list
- EditShortcutViewModel for shortcut editor

Depends on PR 2 and 3."

echo ""
echo "=========================================="
echo "✓ PR 5 branch created and committed!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "  1. Push: git push -u origin $BRANCH_NAME"
echo "  2. Create PR on GitHub"
echo "  3. After merge, you can run PR 6 and PR 8:"
echo "     - ./pr-6-ui.sh"
echo "     - ./pr-8-tests.sh (can be done in parallel with 6)"
