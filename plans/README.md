# Shortcuts V2 - Review Slice Plan

> [!IMPORTANT]
> Each slice is designed to compile independently when merged in order (1 through 12). The integration branch `feat/shortcuts-v2` is the source of truth for the code; `main` is the base that each slice PR targets.


## Slice overview

| # | Title | Files | Tests included |
|---|-------|-------|----------------|
| 1 | Data contracts & models | 14 (13 src + 1 test) | `ShortcutDestinationTest` |
| 2 | Data sources & tests | 7 (4 src + 1 Java + 2 test) | `ServersDataSourceTest`, `AppShortcutsDataSourceTest`, `HomeShortcutsDataSourceTest` |
| 3 | App DI, repository binding, factory & shortcut intent codec | 7 (5 src + 2 test) + 5 delete | `HaShortcutManagerTest`, `FrontendTargetTest` |
| 4 | Manage (list) use case, ViewModel & tests | 4 (2 src + 2 test) | `LoadShortcutsUseCaseTest`, `ManageShortcutsViewModelTest` |
| 5 | Editor UI state | 1 src | — |
| 6 | Shortcut editor use cases, ViewModel & tests | 8 (4 src + 3 test + `strings.xml`) | `LoadShortcutEditorUseCaseTest`, `ModifyShortcutUseCaseTest`, `ShortcutEditorViewModelTest` |
| 7 | Shared UI components | 3 src | — |
| 8 | Editor field components | 3 src | — |
| 9 | List screen & screenshot test | 5 (3 src + 2 test) + 31 PNG | `ShortcutsListScreenTest`, `ShortcutsListScreenScreenshotTest` |
| 10 | Editor screen & screenshot test | 6 (1 src + 5 test) + 48 PNG | `ShortcutEditorScreenTest`, `CreateAppShortcutScreenScreenshotTest`, `EditAppShortcutScreenScreenshotTest`, `CreateHomeShortcutScreenScreenshotTest`, `EditHomeShortcutScreenScreenshotTest` |
| 11 | Navigation graph | 2 (1 src + 1 test) | `ShortcutsNavigationTest` |
| 12 | Settings entry point fragment (update stub + delete `ShortcutsScreen.kt`) | 2 src + 1 delete | — |


## Quick start

```bash
# Create a slice branch and commit
./plans/scripts/create-pr.sh 1

# Apply each slice sequentially on a throwaway branch. Base defaults to main.
./plans/scripts/verify.sh
./plans/scripts/verify.sh --slices some-base-branch

# Run checks on the current branch, or on a named branch.
./plans/scripts/verify.sh --check
./plans/scripts/verify.sh --check feat/shortcuts-v2

# Skip screenshot test compile + validation when you only need unit/compile checks.
SKIP_SCREENSHOTS=1 ./plans/scripts/verify.sh --slices
SKIP_SCREENSHOTS=1 ./plans/scripts/verify.sh --slices some-base-branch
SKIP_SCREENSHOTS=1 ./plans/scripts/verify.sh --check
SKIP_SCREENSHOTS=1 ./plans/scripts/verify.sh --check feat/shortcuts-v2

# Skip all unit + screenshot test runs (compile / ktlint / build still run).
SKIP_TESTS=1 ./plans/scripts/verify.sh --slices
SKIP_TESTS=1 ./plans/scripts/verify.sh --check feat/shortcuts-v2
SKIP_TESTS=1 SKIP_SCREENSHOTS=1 ./plans/scripts/verify.sh --check
```

## Scripts

| Script | Purpose |
|--------|---------|
| `pr-manifest.sh` | Slice → files mapping (single source of truth) |
| `create-pr.sh` | Create branch, stage files, commit |
| `stage-pr-files.sh` | Stage a slice's files on current branch |
| `verify.sh` | `--slices`: apply slices one-by-one, compile + test. `--check`: ktlint + tests + build |
