#!/bin/bash
#
# PR 6: App Module - UI (Screens, Components, Previews, Strings)
# Creates branch, stages files, and commits
#
# Usage: ./pr-6-ui.sh [base-branch]
# Example: ./pr-6-ui.sh main
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

BRANCH_NAME="feature/shortcuts-v2-ui"
BASE_BRANCH="${1:-main}"

echo "=========================================="
echo "PR 6: App Module - UI Components"
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
git add app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/screens/ShortcutsListScreen.kt
git add app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/screens/ShortcutEditorScreen.kt
git add app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/screens/ShortcutEditorScreenState.kt
git add app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/screens/ShortcutEditAction.kt
git add app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/screens/ShortcutDraftSaver.kt
git add app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/components/AppShortcutEditor.kt
git add app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/components/HomeShortcutEditor.kt
git add app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/components/ShortcutEditorForm.kt
git add app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/components/EmptyStateContent.kt
git add app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/components/ErrorStateContent.kt
git add app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/selector/ShortcutIconPicker.kt
git add app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/preview/ShortcutPreviewData.kt

# Note: For strings, we need to be careful to only include v2 strings
# The script assumes strings are already in the file
echo "→ Staging strings.xml (v2 strings)..."
git add common/src/main/res/values/strings.xml

echo ""
echo "→ Files staged:"
git status --short
echo ""

# Commit
echo "→ Committing..."
git commit -m "feat(shortcuts): add ui components and strings

Add all UI components for Shortcuts V2:
- Screens: ShortcutsListScreen, ShortcutEditorScreen
- Components: AppShortcutEditor, HomeShortcutEditor, ShortcutEditorForm
- States: EmptyStateContent, ErrorStateContent
- Selector: ShortcutIconPicker
- Previews: ShortcutPreviewData
- String resources for v2 feature

Depends on PR 4 and 5."

echo ""
echo "=========================================="
echo "✓ PR 6 branch created and committed!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "  1. Push: git push -u origin $BRANCH_NAME"
echo "  2. Create PR on GitHub"
echo "  3. After merge, run: ./pr-7-integration.sh"
