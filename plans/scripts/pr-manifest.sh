#!/bin/bash
#
# Shortcuts V2 - review slice manifest (single source of truth).
#
# SOURCE this file; do not execute it.
#   source "$(dirname "$0")/pr-manifest.sh"
#
# To change which files belong to a slice, edit PR_DEF below and nothing else.
#

# Branch the slice files are pulled from (override via env if needed).
SOURCE_BRANCH="${SOURCE_BRANCH:-feat/shortcuts-v2}"

# Base branch that partial/slice PRs are opened against (override via env if
# needed). Each slice PR targets main directly and must compile and pass tests
# independently. The integration branch feat/shortcuts-v2 carries
# WIPFeature.USE_SHORTCUTS_V2 and the flag-based fragment switch in
# SettingsFragment.kt (merged via refactor/move-shortcuts-to-legacy), so
# slices never need to touch those — only the real fragment class reference
# in slice 12.
BASE_BRANCH_DEFAULT="${BASE_BRANCH_DEFAULT:-main}"

# Number of review slices.
PR_COUNT=12

# Path prefixes (keep lines below readable).
C="common/src/main/kotlin/io/homeassistant/companion/android/common/data/shortcuts"
CT="common/src/test/kotlin/io/homeassistant/companion/android/common/data/shortcuts"
STRINGS="common/src/main/res/values/strings.xml"
A="app/src/main/kotlin/io/homeassistant/companion/android/settings/shortcuts"
AT="app/src/test/kotlin/io/homeassistant/companion/android/settings/shortcuts"
AS="app/src/screenshotTest/kotlin/io/homeassistant/companion/android/settings/shortcuts"
REF="app/src/screenshotTestFullDebug/reference/io/homeassistant/companion/android/settings/shortcuts"
COMMON_COMPOSE="common/src/main/kotlin/io/homeassistant/companion/android/common/compose/composable"
COMMON_COMPOSE_ST="common/src/screenshotTest/kotlin/io/homeassistant/companion/android/compose/composable"
COMMON_COMPOSE_REF="common/src/screenshotTestDebug/reference/io/homeassistant/companion/android/compose/composable"
# Per-test-class reference image directories (baselines travel with their screen).
REF_LIST="$REF/views/screens/ShortcutsListScreenScreenshotTest/"
REF_DIALOG="$REF/views/screens/CreateShortcutDialogScreenshotTest/"
REF_EDITOR_CREATE_APP="$REF/views/screens/CreateAppShortcutScreenScreenshotTest/"
REF_EDITOR_EDIT_APP="$REF/views/screens/EditAppShortcutScreenScreenshotTest/"
REF_EDITOR_CREATE_HOME="$REF/views/screens/CreateHomeShortcutScreenScreenshotTest/"
REF_EDITOR_EDIT_HOME="$REF/views/screens/EditHomeShortcutScreenScreenshotTest/"
REF_COMMON_HA_FAB="$COMMON_COMPOSE_REF/HAFloatingActionButtonScreenshotTest/"

# Slice definitions.
# Format: PR_DEF[<n>]="branch|title|references|file1:file2:..."
# NOTE: gradle.lockfile changes are intentionally NOT listed (toolchain churn).
# Plain indexed array (keys 1..PR_COUNT) for macOS bash 3.2 compatibility.
PR_DEF=(
    [1]="feat/shortcuts-v2-data-contracts|Data contracts & models|none|\
 $C/AppShortcutsRepository.kt:\
$C/HomeShortcutsRepository.kt:\
$C/ShortcutServersRepository.kt:\
$C/ShortcutInfoFactory.kt:\
$C/entities/Shortcut.kt:\
$C/entities/ShortcutDestination.kt:\
$CT/entities/ShortcutDestinationTest.kt:\
$C/entities/ShortcutDraft.kt:\
$C/entities/ShortcutIcon.kt:\
$C/entities/ShortcutResult.kt:\
$C/entities/ShortcutServer.kt:\
$C/entities/ShortcutServersSnapshot.kt:\
$C/entities/ShortcutsListData.kt:\
$C/ShortcutIntentSerializer.kt"

    [2]="feat/shortcuts-v2-data-sources|Data sources & tests|slice 1|\
$C/impl/AppShortcutsDataSource.kt:\
$C/impl/HomeShortcutsDataSource.kt:\
$C/impl/ServersDataSource.kt:\
$C/impl/ShortcutResultExt.kt:\
common/src/test/java/android/os/Build.java:\
$CT/impl/ServersDataSourceTest.kt:\
$CT/impl/AppShortcutsDataSourceTest.kt:\
$CT/impl/HomeShortcutsDataSourceTest.kt"

    [3]="feat/shortcuts-v2-app-di|App DI, repository binding, factory & shortcut intent codec|slices 1, 2|\
$C/di/ShortcutsRepositoryModule.kt:\
common/src/main/kotlin/io/homeassistant/companion/android/database/IconDialogCompat.kt:\
$A/ShortcutFrontendMapping.kt:\
$A/HaShortcutManager.kt:\
$A/ShortcutSupport.kt:\
$A/di/ShortcutsModule.kt:\
app/src/test/kotlin/io/homeassistant/companion/android/frontend/navigation/FrontendTargetTest.kt:\
$AT/HaShortcutManagerTest.kt"

    [4]="feat/shortcuts-v2-viewmodel-manage|Manage (list) use case, ViewModel & tests|slices 1, 2, 3|\
$A/LoadShortcutsUseCase.kt:\
$A/ManageShortcutsViewModel.kt:\
$AT/LoadShortcutsUseCaseTest.kt:\
$AT/ManageShortcutsViewModelTest.kt"

    [5]="feat/shortcuts-v2-editor-state|Editor UI state|slices 1, 3|\
$A/ShortcutsUiState.kt:\
$AT/EditorStateTest.kt"

    [6]="feat/shortcuts-v2-viewmodel-editor|Shortcut editor use cases, ViewModel & tests|slices 1, 2, 3, 5|\
$A/LoadShortcutEditorUseCase.kt:\
$A/ModifyShortcutUseCase.kt:\
$A/ShortcutKind.kt:\
$A/ShortcutEditorViewModel.kt:\
$AT/LoadShortcutEditorUseCaseTest.kt:\
$AT/ModifyShortcutUseCaseTest.kt:\
$AT/ShortcutEditorViewModelTest.kt:\
$STRINGS"

    [7]="feat/shortcuts-v2-ui-shared|Shared UI components|slices 4, 5|\
$A/views/components/ErrorStateContent.kt:\
$A/views/preview/ShortcutPreviewData.kt:\
app/src/main/kotlin/io/homeassistant/companion/android/settings/views/EmptyState.kt"

    [8]="feat/shortcuts-v2-ui-editor-components|Editor field components|slices 5, 6, 7|\
$A/views/components/ShortcutEditorForm.kt:\
$A/views/components/ServerPicker.kt:\
$A/views/components/ShortcutEditorFields.kt"

    [9]="feat/shortcuts-v2-ui-list|List screen & screenshot test|slices 4, 7|\
$COMMON_COMPOSE/HAFloatingActionButton.kt:\
$COMMON_COMPOSE_ST/HAFloatingActionButtonScreenshotTest.kt:\
$A/views/screens/ShortcutsListScreen.kt:\
$A/views/screens/CreateShortcutDialog.kt:\
$AT/views/screens/ShortcutsListScreenTest.kt:\
$AS/views/screens/ShortcutSingleDevicePreview.kt:\
$AS/views/screens/ShortcutsListScreenScreenshotTest.kt:\
$AS/views/screens/CreateShortcutDialogScreenshotTest.kt:\
$REF_COMMON_HA_FAB:\
$REF_LIST:\
$REF_DIALOG"

    [10]="feat/shortcuts-v2-ui-editor-screen|Editor screen & screenshot test|slices 5, 7, 8, 9|\
$A/views/screens/ShortcutEditorScreen.kt:\
$AT/views/screens/ShortcutEditorScreenTest.kt:\
$AS/views/screens/CreateAppShortcutScreenScreenshotTest.kt:\
$AS/views/screens/EditAppShortcutScreenScreenshotTest.kt:\
$AS/views/screens/CreateHomeShortcutScreenScreenshotTest.kt:\
$AS/views/screens/EditHomeShortcutScreenScreenshotTest.kt:\
$REF_EDITOR_CREATE_APP:\
$REF_EDITOR_EDIT_APP:\
$REF_EDITOR_CREATE_HOME:\
$REF_EDITOR_EDIT_HOME"

    [11]="feat/shortcuts-v2-navigation|Navigation graph|slices 4, 5, 6, 9, 10|\
$A/navigation/ShortcutsNavigation.kt:\
$AT/navigation/ShortcutsNavigationTest.kt"

    [12]="feat/shortcuts-v2-integration|Settings entry point fragment|slice 11|\
$A/ManageShortcutsSettingsFragment.kt:\
app/src/main/kotlin/io/homeassistant/companion/android/webview/WebViewActivity.kt"
)

PR_COMMIT_TITLE=(
    [1]="Add Shortcuts V2 data contracts and models"
    [2]="Add Shortcuts V2 data sources and tests"
    [3]="Add Shortcuts V2 app DI, repository binding, factory and shortcut intent codec"
    [4]="Add Shortcuts V2 manage use case, ViewModel and tests"
    [5]="Add Shortcuts V2 editor UI state"
    [6]="Add Shortcuts V2 editor use cases, ViewModel and tests"
    [7]="Add Shortcuts V2 shared UI components"
    [8]="Add Shortcuts V2 editor field components"
    [9]="Add Shortcuts V2 list screen and screenshot test"
    [10]="Add Shortcuts V2 editor screen and screenshot test"
    [11]="Add Shortcuts V2 navigation graph"
    [12]="Add Shortcuts V2 settings entry point"
)

# Files to delete per slice (optional). Format: PR_DELETE[<n>]="file1:file2:..."
# These files exist on the base branch but should be removed as part of the slice.
PR_DELETE=(
    [3]="$C/impl/ShortcutIconIntentSerializer.kt:$CT/impl/ShortcutIntentSerializerTest.kt"
    [12]="$A/ShortcutsScreen.kt"
)

# Lockfiles that should NOT appear in any slice (used by guards/warnings).
LOCKFILES=(
    "app/gradle.lockfile"
    "common/gradle.lockfile"
    "automotive/gradle.lockfile"
)

# Screenshot test class shipped by a slice, keyed by slice number (empty for
# slices with no screenshot test). Lets tooling validate a slice's own
# screenshot baselines right after that slice lands, instead of only at the
# very end. Plain indexed array (keys 1..PR_COUNT) for macOS bash 3.2
# compatibility.
PR_SCREENSHOT_TEST=(
    [9]="io.homeassistant.companion.android.compose.composable.HAFloatingActionButtonScreenshotTest:io.homeassistant.companion.android.settings.shortcuts.views.screens.ShortcutsListScreenScreenshotTest:io.homeassistant.companion.android.settings.shortcuts.views.screens.CreateShortcutDialogScreenshotTest"
    [10]="io.homeassistant.companion.android.settings.shortcuts.views.screens.CreateAppShortcutScreenScreenshotTest:io.homeassistant.companion.android.settings.shortcuts.views.screens.EditAppShortcutScreenScreenshotTest:io.homeassistant.companion.android.settings.shortcuts.views.screens.CreateHomeShortcutScreenScreenshotTest:io.homeassistant.companion.android.settings.shortcuts.views.screens.EditHomeShortcutScreenScreenshotTest"
)

# pr_valid <n> -> returns 0 if n is a valid slice number
pr_valid() {
    [[ "$1" =~ ^[0-9]+$ ]] && [ "$1" -ge 1 ] && [ "$1" -le "$PR_COUNT" ]
}

# pr_screenshot_test <n> -> echoes the slice's screenshot test class, or
# nothing if the slice doesn't ship one.
pr_screenshot_test() {
    echo "${PR_SCREENSHOT_TEST[$1]:-}"
}

# pr_fields <n> -> echoes "branch|title|references" (no file list)
pr_fields() {
    IFS='|' read -r branch title refs _files <<< "${PR_DEF[$1]}"
    echo "$branch|$title|$refs"
}

# pr_commit_title <n> -> echoes the commit/PR title for the slice
pr_commit_title() {
    echo "${PR_COMMIT_TITLE[$1]}"
}

# pr_branch <n> -> echoes the review branch for the slice
pr_branch() {
    IFS='|' read -r branch _title _refs <<< "$(pr_fields "$1")"
    local suffix="${branch#feat/shortcuts-v2-}"
    echo "feature/shortcuts-v2-${suffix}"
}

# pr_files <n> -> echoes one file path per line
pr_files() {
    IFS='|' read -r _b _t _r files <<< "${PR_DEF[$1]}"
    # Strip leading/trailing whitespace from each entry: the PR_DEF literals
    # use line-continuation indentation that leaves a leading space on every
    # path, and git rejects paths with a leading space ("exists on disk, but
    # not in '<ref>'").
    echo "$files" | tr ':' '\n' | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' | grep -v '^$'
}

# pr_files_to_delete <n> -> echoes one file path per line (empty if none)
pr_files_to_delete() {
    local val="${PR_DELETE[$1]:-}"
    [ -n "$val" ] && echo "$val" | tr ':' '\n' | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' | grep -v '^$'
}

# pr_usage <script-name> -> prints the slice list
pr_usage() {
    echo "Usage: $1 <SLICE_NUMBER> [base-branch]"
    echo ""
    echo "Shortcuts V2 review slices (1-$PR_COUNT):"
    local i
    for i in $(seq 1 "$PR_COUNT"); do
        IFS='|' read -r branch title refs <<< "$(pr_fields "$i")"
        printf "  %2s. %-40s (%s)\n" "$i" "$title" "$(pr_branch "$i")"
    done
    echo ""
    echo "Files come from: ${SOURCE_BRANCH}"
}
