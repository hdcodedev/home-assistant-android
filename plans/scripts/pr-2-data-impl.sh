#!/bin/bash
#
# PR 2: Common Data Layer - Implementation
# Creates branch, stages files, and commits
#
# Usage: ./pr-2-data-impl.sh [base-branch]
# Example: ./pr-2-data-impl.sh main
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

BRANCH_NAME="feature/shortcuts-v2-data-impl"
BASE_BRANCH="${1:-main}"

echo "=========================================="
echo "PR 2: Common Data Layer - Implementation"
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
git add common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/ShortcutsRepositoryImpl.kt
git add common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/MockShortcutsRepositoryImpl.kt
git add common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/ShortcutIntentCodecImpl.kt

echo ""
echo "→ Files staged:"
git status --short
echo ""

# Commit
echo "→ Committing..."
git commit -m "feat(shortcuts): add data layer implementation

Add repository and intent codec implementations:
- ShortcutsRepositoryImpl with ShortcutManagerCompat integration
- MockShortcutsRepositoryImpl for testing
- ShortcutIntentCodecImpl for encoding/decoding shortcut data

Depends on PR 1 (data foundation)."

echo ""
echo "=========================================="
echo "✓ PR 2 branch created and committed!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "  1. Push: git push -u origin $BRANCH_NAME"
echo "  2. Create PR on GitHub"
echo "  3. After merge, run: ./pr-3-app-di.sh"
