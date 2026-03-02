#!/bin/bash
#
# PR 1: Common Data Layer - Foundation
# Creates branch, stages files, and commits
#
# Usage: ./pr-1-data-foundation.sh [base-branch]
# Example: ./pr-1-data-foundation.sh main
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

BRANCH_NAME="feature/shortcuts-v2-data-foundation"
BASE_BRANCH="${1:-main}"

echo "=========================================="
echo "PR 1: Common Data Layer - Foundation"
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
git add common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/ShortcutsRepository.kt
git add common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/ShortcutFactory.kt
git add common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/ShortcutIntentCodec.kt
git add common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/di/ShortcutsRepositoryModule.kt
git add common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/entities/ShortcutDraft.kt
git add common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/entities/ShortcutResult.kt
git add common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/entities/ShortcutTargetValue.kt
git add common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/entities/EditorData.kt
git add common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/entities/ServerData.kt
git add common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/entities/ShortcutsListData.kt

echo ""
echo "→ Files staged:"
git status --short
echo ""

# Commit
echo "→ Committing..."
git commit -m "feat(shortcuts): add common data layer foundation

Add core interfaces, entity models, and DI module for Shortcuts V2:
- ShortcutsRepository interface
- ShortcutFactory interface  
- ShortcutIntentCodec interface
- Entity models (ShortcutDraft, ShortcutResult, etc.)
- DI module for data layer

This PR establishes the foundation for the Shortcuts V2 feature."

echo ""
echo "=========================================="
echo "✓ PR 1 branch created and committed!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "  1. Push: git push -u origin $BRANCH_NAME"
echo "  2. Create PR on GitHub"
echo "  3. After merge, run: ./pr-2-data-impl.sh"
