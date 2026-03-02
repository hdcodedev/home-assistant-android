#!/bin/bash
#
# PR 7: App Module - Fragment and Settings Integration
# Creates branch, stages files, and commits
#
# Usage: ./pr-7-integration.sh [base-branch]
# Example: ./pr-7-integration.sh main
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

BRANCH_NAME="feature/shortcuts-v2-integration"
BASE_BRANCH="${1:-main}"

echo "=========================================="
echo "PR 7: App Module - Integration"
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
git add app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/ManageShortcutsSettingsFragment.kt
git add app/src/main/kotlin/io/homeassistant/companion/android/settings/SettingsFragment.kt

echo ""
echo "→ Files staged:"
git status --short
echo ""

# Commit
echo "→ Committing..."
git commit -m "feat(shortcuts): add fragment and settings integration

Add entry point for Shortcuts V2:
- ManageShortcutsSettingsFragment as UI container
- SettingsFragment modification to add v2 preference

This PR makes the feature accessible from settings.
Depends on PR 6."

echo ""
echo "=========================================="
echo "✓ PR 7 branch created and committed!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "  1. Push: git push -u origin $BRANCH_NAME"
echo "  2. Create PR on GitHub"
echo "  3. After merge, run: ./pr-8-tests.sh"
