#!/bin/bash
#
# Script to stage files for a PR on the current branch
# Useful if you already created the branch manually
#
# Usage: ./stage-pr-files.sh <PR_NUMBER>
# Example: ./stage-pr-files.sh 1
#

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# PR definitions (CONSOLIDATED - 8 PRs)
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
        echo "  $i. $desc"
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
echo "Staging files for PR $PR_NUMBER: $DESCRIPTION"
echo "=========================================="
echo ""

cd "$PROJECT_ROOT"

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
echo "  1. Review staged files: git status"
echo "  2. Commit: git commit -m \"feat(shortcuts): $DESCRIPTION\""
