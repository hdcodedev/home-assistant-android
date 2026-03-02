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
SOURCE_BRANCH="feat/shortcuts-v2"

echo "=========================================="
echo "PR 1: Common Data Layer - Foundation"
echo "Branch: $BRANCH_NAME"
echo "Base: $BASE_BRANCH"
echo "Source: $SOURCE_BRANCH"
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

# Retrieve files from source branch
echo "→ Retrieving files from $SOURCE_BRANCH..."
git checkout "$SOURCE_BRANCH" -- \
    common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/ShortcutsRepository.kt \
    common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/ShortcutFactory.kt \
    common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/ShortcutIntentCodec.kt \
    common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/di/ShortcutsRepositoryModule.kt \
    common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/ShortcutsRepositoryImpl.kt \
    common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/ShortcutIntentCodecImpl.kt \
    common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/entities/ShortcutDraft.kt \
    common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/entities/ShortcutResult.kt \
    common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/entities/ShortcutTargetValue.kt \
    common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/entities/EditorData.kt \
    common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/entities/ServerData.kt \
    common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/entities/ShortcutsListData.kt

echo ""
echo "→ Files staged:"
git status --short
echo ""

# Commit
echo "→ Committing..."
git commit -m "feat(shortcuts): add common data layer foundation

Add core interfaces, implementations, entity models, and DI module for Shortcuts V2:
- ShortcutsRepository interface and implementation
- ShortcutFactory interface
- ShortcutIntentCodec interface and implementation
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
