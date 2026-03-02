# Shortcuts V2 - Partial PR Plan

This directory contains the plan and scripts for breaking down the Shortcuts V2 implementation into **8 reviewable partial PRs**.

## 📁 Directory Structure

```
plans/
├── README.md                          # This file
├── shortcuts-v2-pr-plan.md            # Detailed PR breakdown with dependencies
├── shortcuts-v2-scripts.md            # Script usage documentation
└── scripts/
    ├── create-shortcuts-pr.sh         # Main script - creates branch & stages files
    ├── verify-pr.sh                   # Code quality verification
    ├── list-pr-files.sh               # List PR files
    └── stage-pr-files.sh              # Stage files on current branch
```

## 🚀 Quick Start

```bash
# Create a new PR branch and stage files
./plans/scripts/create-shortcuts-pr.sh <PR_NUMBER>

# Example: Create PR 1 (Data Foundation)
./plans/scripts/create-shortcuts-pr.sh 1
```

## 📋 PR Overview (8 PRs)

| PR | Description | Files | Dependencies |
|----|-------------|-------|--------------|
| 1 | **Data Foundation** | 10 | None |
| 2 | **Data Implementation** | 3 | PR 1 |
| 3 | **App DI & Factory** | 2 | PR 1, 2 |
| 4 | **Utils & Navigation** | 2 | PR 3 |
| 5 | **ViewModels** | 2 | PR 2, 3 |
| 6 | **UI Components** | 12 | PR 4, 5 |
| 7 | **Integration** | 2 | PR 6 |
| 8 | **Tests** | 4 | PR 5, 6 |

## 🗂️ Detailed PR Descriptions

### PR 1: Common Data Layer - Foundation
Interfaces, entity models, and DI module for the data layer.
- **Branch:** `feature/shortcuts-v2-data-foundation`
- **Files:** Repository interface, Factory interface, IntentCodec interface, DI module, all entity classes

### PR 2: Common Data Layer - Implementation
Repository and intent codec implementations.
- **Branch:** `feature/shortcuts-v2-data-impl`
- **Files:** ShortcutsRepositoryImpl, MockShortcutsRepositoryImpl, ShortcutIntentCodecImpl

### PR 3: App Module - DI and Factory
App-level dependency injection and WebView shortcut factory.
- **Branch:** `feature/shortcuts-v2-app-di`
- **Files:** ShortcutsModule, WebViewShortcutFactory

### PR 4: App Module - Utilities and Navigation
Icon rendering and navigation setup.
- **Branch:** `feature/shortcuts-v2-utils-nav`
- **Files:** ShortcutIconRenderer, ShortcutsNavigation

### PR 5: App Module - ViewModels
Business logic and state management.
- **Branch:** `feature/shortcuts-v2-viewmodels`
- **Files:** ManageShortcutsViewModel, EditShortcutViewModel

### PR 6: App Module - UI Components
All UI screens, components, and preview data.
- **Branch:** `feature/shortcuts-v2-ui`
- **Files:** All screen files, component files, selector, and preview data

### PR 7: App Module - Fragment and Settings Integration
Fragment container and settings entry point.
- **Branch:** `feature/shortcuts-v2-integration`
- **Files:** ManageShortcutsSettingsFragment, SettingsFragment (modification)

### PR 8: Tests
Unit tests and screenshot tests.
- **Branch:** `feature/shortcuts-v2-tests`
- **Files:** ViewModel tests, Screenshot tests

## 🔄 Workflow

### 1. Start with PR 1 (Foundation)

```bash
# Ensure you're on main and up-to-date
git checkout main
git pull

# Create PR 1
./plans/scripts/create-shortcuts-pr.sh 1

# Verify code quality
./plans/scripts/verify-pr.sh

# Commit and push
git commit -m "feat(shortcuts): add common data layer foundation"
git push -u origin feature/shortcuts-v2-data-foundation
```

### 2. Create Subsequent PRs

After a PR is merged:

```bash
# Update main
git checkout main
git pull

# Create next PR
./plans/scripts/create-shortcuts-pr.sh 2
# ... and so on
```

## 📊 Dependency Graph

```
PR 1 (Data Foundation)
    │
    ▼
PR 2 (Data Implementation)
    │
    ├──► PR 3 (App DI & Factory)
    │       │
    │       ├──► PR 4 (Utils & Navigation)
    │       │       │
    │       │       └──► PR 6 (UI Components) ◄──┐
    │       │               │                    │
    │       └──► PR 5 (ViewModels) ──────────────┤
    │                       │                    │
    │                       └──► PR 8 (Tests) ◄──┘
    │
    └──► PR 7 (Integration) ◄── PR 6
```

## 🛠️ Available Scripts

### Main Script: `create-shortcuts-pr.sh`
Creates a new branch and stages files for a specific PR.

```bash
./plans/scripts/create-shortcuts-pr.sh <PR_NUMBER>
```

### Helper Scripts

| Script | Purpose |
|--------|---------|
| `verify-pr.sh` | Run code quality checks (ktlint, tests, build) |
| `list-pr-files.sh` | List all files in a PR without creating a branch |
| `stage-pr-files.sh` | Stage PR files on the current branch |

## ✅ Review Checklist

Each PR should be reviewed for:

- [ ] **Code Quality**: KTLint formatting passes
- [ ] **Tests**: Unit tests pass
- [ ] **Build**: Debug build succeeds
- [ ] **Architecture**: Follows project patterns
- [ ] **Documentation**: Proper KDoc comments
- [ ] **Dependencies**: Correct PR dependencies

## 🚩 Feature Flag Strategy

The v2 feature is currently hidden:
- DI module uses `MockShortcutsRepositoryImpl`
- Settings has both v1 and v2 options
- v2 is accessible but uses mock data

After all PRs are merged:
1. Switch DI to real implementation
2. Remove v1 if desired
3. Update documentation

## 🐛 Troubleshooting

### Script says "Not a git repository"
Run scripts from the project root: `~/Projects/android/`

### "Branch already exists"
The script will ask if you want to delete and recreate it.

### Missing files
Some files may not exist yet (new files). Create them manually and then run `git add`.

### Uncommitted changes
The script will detect and offer to stash them before creating a new branch.

## 📖 Documentation

- [shortcuts-v2-pr-plan.md](shortcuts-v2-pr-plan.md) - Full PR breakdown with architecture diagrams
- [shortcuts-v2-scripts.md](shortcuts-v2-scripts.md) - Detailed script usage

## 📦 File Count Summary

| PR | Files | Est. Review Time |
|----|-------|------------------|
| 1 | 10 | Medium |
| 2 | 3 | Large |
| 3 | 2 | Small |
| 4 | 2 | Small |
| 5 | 2 | Medium |
| 6 | 12 | Large |
| 7 | 2 | Small |
| 8 | 4 | Medium |

**Total:** 37 files across 8 PRs
