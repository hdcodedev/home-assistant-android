#!/bin/zsh
set -euo pipefail

# ⚠️ Force-pushes & rewrites history — never `git merge main` here; rebase instead.

# Pushes the single implementation commit for feat/shortcuts-v2.
#
# This script amends the current commit and force-pushes with lease. It assumes
# HEAD is already the branch's single implementation commit.
#
# The branch keeps a SINGLE flat commit on top of origin/main so it is easy to
# review locally against main. Do NOT merge main into this branch: a merge commit
# breaks the single-commit history and `commit --amend` cannot realign it.
#
# Instead, when you need main's latest changes, REBASE your single commit onto
# origin/main using the stash dance below. This replays your commit on top of
# main (parent = origin/main) and keeps your commit as the only change on the
# branch. Changes main already made (e.g. deletions) live in the parent, so they
# do NOT show up as your deletions in the diff.
#
#   1. (optional but recommended) back up the current branch:
#        git branch "backup/before-flatten-$(date +%Y%m%d-%H%M%S)"
#
#   2. so we only work from main's base. The single commit's parent is the base
#      to reset to. If HEAD is currently your single commit, reset to its parent:
#        git reset --soft HEAD~1
#      (HEAD now points at origin/main's tip that your commit was built on.)
#
#   3. stash all your working changes (including untracked files):
#        git stash push -u -m "shortcuts-v2 working changes"
#
#   4. bring main in (fast-forwards HEAD to origin/main's latest tip):
#        git fetch origin
#        git merge --ff-only origin/main
#
#   5. restore your changes on top of the new main:
#        git stash pop
#
#   6. commit them as the single flat commit, preserving the original message,
#      author, and author date (replace the date below with the original commit's):
#        git add -A
#        GIT_AUTHOR_DATE="2026-07-17 20:52:37 +0200" \
#        GIT_COMMITTER_DATE="2026-07-17 20:52:37 +0200" \
#        git commit --author="HDCode <hdcodedev@gmail.com>" \
#          -m "feat(shortcuts-v2): Shortcuts V2 complete implementation + tooling"
#
#   7. verify there is exactly one commit above origin/main, then run this script:
#        git log --oneline origin/main..HEAD
#
# Then run this script to push.

git add -A
git commit --amend --no-edit
git push --force-with-lease origin feat/shortcuts-v2
