# Shortcuts V2 - PR Creation Scripts Documentation

This document describes the scripts available for managing the Shortcuts V2 partial PRs.

## Scripts Overview

### 1. `create-shortcuts-pr.sh`
Creates a new branch and stages the appropriate files for a specific PR.

**Usage:**
```bash
./plans/scripts/create-shortcuts-pr.sh <PR_NUMBER>
```

**Example:**
```bash
./plans/scripts/create-shortcuts-pr.sh 1
```

This will:
1. Create branch `feature/shortcuts-v2-data-foundation` from current branch
2. Stage all files for PR 1 (10 files)
3. Show a summary of staged files

**Available PR Numbers (8 Total):**

| PR # | Description | Branch Name | Files |
|------|-------------|-------------|-------|
| 1 | Data Foundation | `feature/shortcuts-v2-data-foundation` | 10 |
| 2 | Data Implementation | `feature/shortcuts-v2-data-impl` | 3 |
| 3 | App DI & Factory | `feature/shortcuts-v2-app-di` | 2 |
| 4 | Utils & Navigation | `feature/shortcuts-v2-utils-nav` | 2 |
| 5 | ViewModels | `feature/shortcuts-v2-viewmodels` | 2 |
| 6 | UI Components | `feature/shortcuts-v2-ui` | 12 |
| 7 | Integration | `feature/shortcuts-v2-integration` | 2 |
| 8 | Tests | `feature/shortcuts-v2-tests` | 4 |

---

### 2. `verify-pr.sh`
Verifies that a PR branch is ready by running code quality checks.

**Usage:**
```bash
./plans/scripts/verify-pr.sh [BRANCH_NAME]
```

If no branch name is provided, uses the current branch.

**Checks performed:**
- KTLint formatting
- Unit tests
- Debug build compilation

---

### 3. `list-pr-files.sh`
Lists all files that belong to a specific PR without creating a branch.

**Usage:**
```bash
./plans/scripts/list-pr-files.sh <PR_NUMBER>
```

---

### 4. `stage-pr-files.sh`
Stages files for a PR on the current branch (useful if you already created the branch).

**Usage:**
```bash
./plans/scripts/stage-pr-files.sh <PR_NUMBER>
```

---

## Workflow Example

### Creating a new PR from scratch:

```bash
# 1. Make sure you're on the base branch (e.g., main)
git checkout main
git pull

# 2. Create PR 1 branch and stage files
./plans/scripts/create-shortcuts-pr.sh 1

# 3. Review staged files
git status

# 4. Verify code quality
./plans/scripts/verify-pr.sh

# 5. Commit
git commit -m "feat(shortcuts): add common data layer foundation"

# 6. Push
git push -u origin feature/shortcuts-v2-data-foundation

# 7. Create PR on GitHub
gh pr create --title "feat(shortcuts): add common data layer foundation" \
             --body "Part 1 of Shortcuts V2 implementation. Adds core interfaces, entities, and DI module."
```

### Working on subsequent PRs:

```bash
# After PR 1 is merged to main...
git checkout main
git pull

# Create PR 2
./plans/scripts/create-shortcuts-pr.sh 2
./plans/scripts/verify-pr.sh
git commit -m "feat(shortcuts): add data layer implementation"
git push -u origin feature/shortcuts-v2-data-impl
```

---

## Dependency Management

The PRs have dependencies:
- PR 1 → None (foundation)
- PR 2 → PR 1
- PR 3 → PR 1, 2
- PR 4 → PR 3
- PR 5 → PR 2, 3
- PR 6 → PR 4, 5
- PR 7 → PR 6
- PR 8 → PR 5, 6

The scripts don't enforce these dependencies - it's the developer's responsibility to ensure dependent PRs are merged before creating dependent branches.

**Recommended approach:**
1. Create PR 1 branch, push, create PR
2. Wait for PR 1 to be merged
3. Update main: `git checkout main && git pull`
4. Create PR 2 branch from updated main
5. Repeat...

---

## Handling Missing Files

If a file listed in a PR definition doesn't exist yet (e.g., you're creating it as part of the PR), the script will:
- Show it as "not found" in the summary
- Not stage it (since it doesn't exist)
- Remind you to create it manually

After creating the file, run:
```bash
git add <file-path>
```

---

## Troubleshooting

### "Not a git repository"
Make sure you're running the script from the project root directory.

### "Branch already exists"
The script will ask if you want to delete and recreate the branch. If you choose 'n', it will checkout the existing branch.

### Uncommitted changes
The script will detect uncommitted changes and offer to stash them. Alternatively, commit them before running the script.

### Large PRs (especially PR 6)
PR 6 has 12 files and may be the largest PR. Consider:
- Reviewing it in multiple sessions
- Focusing on one component group at a time
- Using the preview functions to understand the UI

---

## Script Output Examples

### Successful PR Creation
```
==========================================
Creating PR 1: Common Data Layer - Foundation
Branch: feature/shortcuts-v2-data-foundation
==========================================

Base branch: main
Creating new branch from main...

Staging files...
  ✓ common/src/main/kotlin/.../shortcuts/ShortcutsRepository.kt
  ✓ common/src/main/kotlin/.../shortcuts/ShortcutFactory.kt
  ...

==========================================
Summary:
  Staged: 10 files
  Missing: 0 files
==========================================

Next steps:
  1. Review the staged files: git status
  2. Make any additional changes
  3. Commit: git commit -m "feat(shortcuts): Common Data Layer - Foundation"
  4. Push: git push -u origin feature/shortcuts-v2-data-foundation
  5. Create PR on GitHub
```

### Verification Success
```
==========================================
Running PR Verification Checks
==========================================

→ Running KTLint check...
  ✓ KTLint passed

→ Running unit tests...
  ✓ Unit tests passed

→ Building debug APK...
  ✓ Debug build succeeded

==========================================
✓ All checks passed!
==========================================

This PR is ready for submission.
```
