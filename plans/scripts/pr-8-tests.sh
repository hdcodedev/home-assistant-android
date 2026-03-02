#!/bin/bash
#
# PR 8: Tests - Unit and Screenshot
# Creates branch, stages files, and commits
#
# Usage: ./pr-8-tests.sh [base-branch]
# Example: ./pr-8-tests.sh main
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

BRANCH_NAME="feature/shortcuts-v2-tests"
BASE_BRANCH="${1:-main}"

echo "=========================================="
echo "PR 8: Tests - Unit and Screenshot"
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
git add app/src/test/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/ManageShortcutsViewModelTest.kt
git add app/src/test/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/EditShortcutViewModelTest.kt
git add app/src/screenshotTest/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/screens/ShortcutEditorScreenScreenshotTest.kt
git add app/src/screenshotTest/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/screens/ShortcutsListScreenScreenshotTest.kt

echo ""
echo "→ Files staged:"
git status --short
echo ""

# Commit
echo "→ Committing..."
git commit -m "feat(shortcuts): add tests

Add unit and screenshot tests:
- ManageShortcutsViewModelTest
- EditShortcutViewModelTest
- ShortcutEditorScreenScreenshotTest
- ShortcutsListScreenScreenshotTest

Depends on PR 5 and 6."

echo ""
echo "=========================================="
echo "✓ PR 8 branch created and committed!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "  1. Push: git push -u origin $BRANCH_NAME"
echo "  2. Create PR on GitHub"
echo "  3. All PRs complete! 🎉"
