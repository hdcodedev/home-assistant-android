#!/bin/bash
#
# Script to create a branch and stage files for a specific Shortcuts V2 PR
#
# Usage: ./create-shortcuts-pr.sh <PR_NUMBER>
# Example: ./create-shortcuts-pr.sh 1
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# PR definitions (CONSOLIDATED - 8 PRs)
# Format: PR_NUMBER|BRANCH_NAME|DESCRIPTION|FILE1:FILE2:FILE3...
declare -A PR_DEFINITIONS=(
    [1]="feature/shortcuts-v2-data-foundation|Common Data Layer - Foundation|common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/ShortcutsRepository.kt:common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/ShortcutFactory.kt:common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/ShortcutIntentCodec.kt:common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/di/ShortcutsRepositoryModule.kt:common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/entities/ShortcutDraft.kt:common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/entities/ShortcutResult.kt:common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/entities/ShortcutTargetValue.kt:common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/entities/EditorData.kt:common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/entities/ServerData.kt:common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/entities/ShortcutsListData.kt"

    [2]="feature/shortcuts-v2-data-impl|Common Data Layer - Implementation|common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/ShortcutsRepositoryImpl.kt:common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/MockShortcutsRepositoryImpl.kt:common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts/impl/ShortcutIntentCodecImpl.kt"

    [3]="feature/shortcuts-v2-app-di|App Module - DI and Factory|app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/di/ShortcutsModule.kt:app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/di/WebViewShortcutFactory.kt"

    [4]="feature/shortcuts-v2-utils-nav|App Module - Utilities and Navigation|app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/util/ShortcutIconRenderer.kt:app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/navigation/ShortcutsNavigation.kt"

    [5]="feature/shortcuts-v2-viewmodels|App Module - ViewModels|app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/ManageShortcutsViewModel.kt:app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/EditShortcutViewModel.kt"

    [6]="feature/shortcuts-v2-ui|App Module - UI Components|app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/screens/ShortcutsListScreen.kt:app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/screens/ShortcutEditorScreen.kt:app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/screens/ShortcutEditorScreenState.kt:app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/screens/ShortcutEditAction.kt:app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/screens/ShortcutDraftSaver.kt:app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/components/AppShortcutEditor.kt:app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/components/HomeShortcutEditor.kt:app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/components/ShortcutEditorForm.kt:app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/components/EmptyStateContent.kt:app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/components/ErrorStateContent.kt:app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/selector/ShortcutIconPicker.kt:app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/preview/ShortcutPreviewData.kt"

    [7]="feature/shortcuts-v2-integration|App Module - Fragment and Settings Integration|app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/ManageShortcutsSettingsFragment.kt:app/src/main/kotlin/io/homeassistant/companion/android/settings/SettingsFragment.kt"

    [8]="feature/shortcuts-v2-tests|Tests - Unit and Screenshot|app/src/test/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/ManageShortcutsViewModelTest.kt:app/src/test/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/EditShortcutViewModelTest.kt:app/src/screenshotTest/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/screens/ShortcutEditorScreenScreenshotTest.kt:app/src/screenshotTest/kotlin/io/homeassistant/companion/android/settings/shortcuts/v2/views/screens/ShortcutsListScreenScreenshotTest.kt"
)

# Function to print usage
print_usage() {
    echo "Usage: $0 <PR_NUMBER>"
    echo ""
    echo "Available PRs (Consolidated - 8 Total):"
    for i in {1..8}; do
        IFS='|' read -r branch desc files <<< "${PR_DEFINITIONS[$i]}"
        file_count=$(echo "$files" | tr ':' '\n' | wc -l)
        echo "  $i. $desc ($file_count files)"
        echo "     Branch: $branch"
    done
    echo ""
    echo "Example: $0 1"
}

# Check if PR number is provided
if [ $# -eq 0 ]; then
    print_usage
    exit 1
fi

PR_NUMBER=$1

# Validate PR number
if ! [[ "$PR_NUMBER" =~ ^[0-9]+$ ]] || [ "$PR_NUMBER" -lt 1 ] || [ "$PR_NUMBER" -gt 8 ]; then
    echo "Error: Invalid PR number. Must be between 1 and 8."
    print_usage
    exit 1
fi

# Get PR definition
IFS='|' read -r BRANCH_NAME DESCRIPTION FILES <<< "${PR_DEFINITIONS[$PR_NUMBER]}"

echo "=========================================="
echo "Creating PR $PR_NUMBER: $DESCRIPTION"
echo "Branch: $BRANCH_NAME"
echo "=========================================="
echo ""

# Check if we're in a git repository
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    echo "Error: Not a git repository. Please run from the project root."
    exit 1
fi

cd "$PROJECT_ROOT"

# Check for uncommitted changes
if ! git diff-index --quiet HEAD --; then
    echo "Warning: You have uncommitted changes."
    read -p "Do you want to stash them? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git stash push -m "Stashed before creating $BRANCH_NAME"
        echo "Changes stashed."
    else
        echo "Please commit or stash your changes before running this script."
        exit 1
    fi
fi

# Get base branch
BASE_BRANCH=$(git branch --show-current)
echo "Base branch: $BASE_BRANCH"

# Check if branch already exists
if git show-ref --verify --quiet "refs/heads/$BRANCH_NAME"; then
    echo "Branch '$BRANCH_NAME' already exists."
    read -p "Do you want to delete and recreate it? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git branch -D "$BRANCH_NAME"
        echo "Deleted existing branch."
    else
        echo "Checking out existing branch..."
        git checkout "$BRANCH_NAME"
        echo ""
        echo "Branch already exists. To reset to base:"
        echo "  git reset --hard $BASE_BRANCH"
        exit 0
    fi
fi

# Create new branch
echo "Creating new branch from $BASE_BRANCH..."
git checkout -b "$BRANCH_NAME"
echo ""

# Stage files
echo "Staging files..."
IFS=':' read -ra FILE_ARRAY <<< "$FILES"
STAGED_COUNT=0
MISSING_COUNT=0

for file in "${FILE_ARRAY[@]}"; do
    if [ -f "$file" ]; then
        git add "$file"
        echo "  ✓ $file"
        ((STAGED_COUNT++))
    else
        echo "  ✗ $file (not found)"
        ((MISSING_COUNT++))
    fi
done

echo ""
echo "=========================================="
echo "Summary:"
echo "  Staged: $STAGED_COUNT files"
echo "  Missing: $MISSING_COUNT files"
echo "=========================================="

if [ $MISSING_COUNT -gt 0 ]; then
    echo ""
    echo "Warning: Some files were not found. They may need to be created."
fi

echo ""
echo "Next steps:"
echo "  1. Review the staged files: git status"
echo "  2. Make any additional changes"
echo "  3. Commit: git commit -m \"feat(shortcuts): $DESCRIPTION\""
echo "  4. Push: git push -u origin $BRANCH_NAME"
echo "  5. Create PR on GitHub"
echo ""
echo "To run checks before committing:"
echo "  ./gradlew ktlintCheck test"
