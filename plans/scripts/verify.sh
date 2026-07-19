#!/bin/bash
#
# Shortcuts V2 verification. Two modes:
#
#   --slices [base]   Apply each slice in order on a throwaway branch,
#                     compile + test after each (default mode).
#   --check [branch]  Run ktlint, the full test suite (unit + screenshot
#                     validation), and a debug build on the current (or
#                     specified) branch.
#
# Environment overrides (--slices mode only):
#   SOURCE_BRANCH=feat/shortcuts-v2   # branch the slice files are pulled from
#
# Environment overrides (all modes):
#   SKIP_SCREENSHOTS=1                # skip screenshot test compile + validation
#   SKIP_TESTS=1                      # skip unit + screenshot test runs
#                                     # (compile / ktlint / build still run)
#
# Usage:
#   ./plans/scripts/verify.sh                          # slice verification, base = main
#   ./plans/scripts/verify.sh --slices some-branch     # slice verification, custom base
#   ./plans/scripts/verify.sh --check                  # branch checks on current branch
#   ./plans/scripts/verify.sh --check feat/shortcuts-v2
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# shellcheck source=pr-manifest.sh
source "${SCRIPT_DIR}/pr-manifest.sh"

cd "$PROJECT_ROOT"

# Gradle speed flags. Override via env, e.g. GRADLE_FLAGS="--build-cache".
GRADLE_FLAGS="${GRADLE_FLAGS:---parallel --build-cache}"
SKIP_SCREENSHOTS="${SKIP_SCREENSHOTS:-0}"
SKIP_TESTS="${SKIP_TESTS:-0}"

# ── Slice verification mode ────────────────────────────────────────────────────

MODE="${1:---slices}"

run_slices() {
    BASE_BRANCH="${1:-main}"

    RANDOM_SUFFIX=$(od -An -N4 -tx1 /dev/urandom 2>/dev/null | tr -d ' \n' || echo "$RANDOM$RANDOM")
    VERIFY_BRANCH="feature/shortcuts-v2-verify-${RANDOM_SUFFIX}"

    STARTING_BRANCH="$(git branch --show-current)"

    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        echo "Error: not a git repository. Run from the project root."
        exit 1
    fi

    if ! git show-ref --verify --quiet "refs/heads/$SOURCE_BRANCH" &&
       ! git show-ref --verify --quiet "refs/remotes/origin/$SOURCE_BRANCH"; then
        echo "Error: source branch '$SOURCE_BRANCH' not found locally or on origin."
        exit 1
    fi

    echo "=========================================="
    echo "Shortcuts V2 - slice order verification"
    echo "=========================================="
    echo "Base branch:   $BASE_BRANCH"
    echo "Source branch: $SOURCE_BRANCH"
    echo "Verify branch: $VERIFY_BRANCH"
    echo "Slices:        1-$PR_COUNT"
    echo "=========================================="
    echo ""

    if ! git diff-index --quiet HEAD -- || [ -n "$(git status --porcelain)" ]; then
        echo "Error: you have uncommitted changes. Commit or stash them first."
        exit 1
    fi

    echo "→ Checking out $BASE_BRANCH..."
    git checkout "$BASE_BRANCH"
    git pull --ff-only origin "$BASE_BRANCH" || echo "  (skipping pull)"

    echo "→ Creating $VERIFY_BRANCH from $BASE_BRANCH..."
    git checkout -b "$VERIFY_BRANCH"
    echo ""

    RESULTS=()
    TOTAL_PASS=0
    TOTAL_FAIL=0
    FIRST_FAIL=0

    for i in $(seq 1 "$PR_COUNT"); do
        IFS='|' read -r _BRANCH_NAME TITLE REFS <<< "$(pr_fields "$i")"
        COMMIT_TITLE="$(pr_commit_title "$i")"

        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "  Slice $i/$PR_COUNT: $TITLE"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

        STAGED=0
        MISSING=0
        while IFS= read -r file; do
            [ -z "$file" ] && continue
            # A path ending in '/' (or resolving as a tree in the source ref)
            # is a directory — e.g. screenshot baseline image directories.
            # Materialize directories with `git checkout -- <dir>` (copies the
            # whole subtree); single files use `git show` to avoid the flaky
            # index/ref behavior `git checkout -- <path>` showed under
            # concurrent terminal git activity (reporting success yet leaving
            # the file absent, cascading into KSP "unresolved" failures).
            if [ "${file%/}" != "$file" ] || git cat-file -e "${SOURCE_BRANCH}:${file}^{tree}" 2>/dev/null; then
                dir="${file%/}"
                if git checkout "${SOURCE_BRANCH}" -- "$dir" 2>"/tmp/verify-checkout-${i}.log"; then
                    git add -- "$dir"
                    ((STAGED++)) || true
                else
                    echo "    ✗ missing on $SOURCE_BRANCH: $dir (see /tmp/verify-checkout-${i}.log)"
                    ((MISSING++)) || true
                fi
            else
                mkdir -p "$(dirname "$file")"
                if git show "${SOURCE_BRANCH}:${file}" > "$file" 2>"/tmp/verify-checkout-${i}.log" && [ -s "$file" ]; then
                    git add -- "$file"
                    ((STAGED++)) || true
                else
                    echo "    ✗ missing on $SOURCE_BRANCH: $file (see /tmp/verify-checkout-${i}.log)"
                    rm -f -- "$file"
                    ((MISSING++)) || true
                fi
            fi
        done < <(pr_files "$i")

        DELETED=0
        while IFS= read -r file; do
            [ -z "$file" ] && continue
            if [ -e "$file" ]; then
                git rm -q "$file"
                ((DELETED++)) || true
            fi
        done < <(pr_files_to_delete "$i")

        if [ "$STAGED" -eq 0 ] && [ "$DELETED" -eq 0 ]; then
            echo "    Error: no files staged or deleted for slice $i, aborting."
            exit 1
        fi

        git commit --quiet -m "$COMMIT_TITLE" -m "Slice order verification: slice ${i}/${PR_COUNT}."
        echo "    ✓ committed ($STAGED staged, $DELETED deleted, $MISSING missing)"

        # Guard against a transient checkout that reported success but left the
        # file absent (stale index/git state). KSP/Kotlin would otherwise fail
        # crypticly for every later slice. Fail fast with a clear message.
        while IFS= read -r file; do
            [ -z "$file" ] && continue
            if ! [ -e "$file" ]; then
                echo "    ✗ declared file absent after checkout: $file"
                echo "      (run with a clean working tree / no concurrent git ops, then re-run)"
                exit 1
            fi
        done < <(pr_files "$i")

        COMPILE_OK=0
        # Compilation is always checked, even when test *execution* is skipped
        # (SKIP_TESTS) or screenshot *validation* is skipped (SKIP_SCREENSHOTS).
        # Only test/validation *runs* are skipped, never compilation.
        echo "    → Compiling (main, unit tests, screenshot tests)..."
        # Mirrors the test *execution* tasks run below (:app + :common unit tests,
        # :app screenshot tests). :common main sources compile transitively via
        # :app's dependency; its unit-test sources need their own compile task.
        COMPILE_TASKS=":app:compileFullDebugKotlin :app:compileFullDebugUnitTestKotlin :app:compileFullDebugScreenshotTestKotlin :common:compileDebugUnitTestKotlin"

        # shellcheck disable=SC2086
        if ./gradlew ${GRADLE_FLAGS} ${COMPILE_TASKS} > /tmp/verify-compile-${i}.log 2>&1; then
            echo "    ✓ Main, unit-test, and screenshot-test compilation passed"
            COMPILE_OK=1
        else
            echo "    ✗ Compile FAILED (see /tmp/verify-compile-${i}.log)"
        fi

        TESTS_OK=0
        if [ "$COMPILE_OK" -eq 1 ]; then
            if [ "$SKIP_TESTS" = "1" ]; then
                TESTS_OK=1
                echo "    ⏭ Tests skipped (SKIP_TESTS=1)"
            else
                TEST_TASKS=":app:testFullDebugUnitTest :common:testDebugUnitTest"
                if [ "$SKIP_SCREENSHOTS" != "1" ]; then
                    TEST_TASKS="$TEST_TASKS :app:validateFullDebugScreenshotTest"
                    echo "    → Running tests (unit + screenshot validation)..."
                else
                    echo "    → Running tests (unit only; screenshots skipped)..."
                fi
                # shellcheck disable=SC2086
                if ./gradlew ${GRADLE_FLAGS} --continue ${TEST_TASKS} > /tmp/verify-test-${i}.log 2>&1; then
                    TESTS_OK=1
                    echo "    ✓ Tests passed"
                else
                    echo "    ✗ Tests FAILED (see /tmp/verify-test-${i}.log)"
                fi
            fi
        fi

        if [ "$COMPILE_OK" -eq 1 ] && [ "$TESTS_OK" -eq 1 ]; then
            RESULTS[$i]="PASS"
            ((TOTAL_PASS++)) || true
            echo "    ✓ Slice $i PASSED"
        else
            RESULTS[$i]="FAIL"
            ((TOTAL_FAIL++)) || true
            [ "$FIRST_FAIL" -eq 0 ] && FIRST_FAIL=$i
            echo "    ✗ Slice $i FAILED"
        fi
        echo ""
    done

    echo "=========================================="
    echo "Verification summary"
    echo "=========================================="
    printf "  %-2s  %-40s  %s\n" "#" "Title" "Result"
    printf "  %-2s  %-40s  %s\n" "--" "----" "------"
    for i in $(seq 1 "$PR_COUNT"); do
        IFS='|' read -r _ TITLE _ <<< "$(pr_fields "$i")"
        printf "  %-2s  %-40s  %s\n" "$i" "$TITLE" "${RESULTS[$i]}"
    done
    echo ""
    echo "  Passed: $TOTAL_PASS / $PR_COUNT"
    echo "  Failed: $TOTAL_FAIL / $PR_COUNT"
    [ "$FIRST_FAIL" -gt 0 ] && echo "  First failure: slice $FIRST_FAIL"
    echo "=========================================="
    echo ""

    echo "Branch '$VERIFY_BRANCH' left in place for inspection."
    echo "To clean up: git checkout $STARTING_BRANCH && git branch -D $VERIFY_BRANCH"
    echo ""

    if [ "$TOTAL_FAIL" -eq 0 ]; then
        echo ""
        echo "✓ All $PR_COUNT slices compile and test independently in order."
        exit 0
    else
        echo ""
        echo "✗ $TOTAL_FAIL slice(s) failed. Fix and re-run."
        exit 1
    fi
}

# ── Branch check mode ──────────────────────────────────────────────────────────

run_check() {
    if [ $# -eq 0 ]; then
        BRANCH_NAME=$(git branch --show-current)
        echo "Using current branch: $BRANCH_NAME"
    else
        BRANCH_NAME=$1
        CURRENT_BRANCH=$(git branch --show-current)
        if [ "$CURRENT_BRANCH" != "$BRANCH_NAME" ]; then
            echo "Checking out $BRANCH_NAME..."
            git checkout "$BRANCH_NAME"
        fi
    fi

    echo ""
    echo "=========================================="
    echo "Running verification checks"
    echo "=========================================="
    echo ""

    FAILED=0

    echo "→ Checking gradle.lockfile drift vs main..."
    LOCK_DIRTY=0
    for lf in "${LOCKFILES[@]}"; do
        if ! git diff --quiet main -- "$lf" 2>/dev/null; then
            echo "  ⚠ $lf differs from main (toolchain churn — revert it)"
            LOCK_DIRTY=1
        fi
    done
    if [ "$LOCK_DIRTY" -eq 0 ]; then
        echo "  ✓ no lockfile drift"
    else
        echo "    Fix: git checkout main -- ${LOCKFILES[*]}"
    fi
    echo ""

    echo "→ Running KTLint check..."
    # shellcheck disable=SC2086
    if ./gradlew ${GRADLE_FLAGS} ktlintCheck :build-logic:convention:ktlintCheck --continue > /tmp/ktlint.log 2>&1; then
        echo "  ✓ KTLint passed"
    else
        echo "  ✗ KTLint failed (see /tmp/ktlint.log)"
        echo "    Fix: ./gradlew :build-logic:convention:ktlintFormat ktlintFormat"
        FAILED=1
    fi
    echo ""

    TEST_TASKS=":app:testFullDebugUnitTest testDebugUnitTest :lint:test"
    if [ "$SKIP_TESTS" = "1" ]; then
        echo "→ Tests skipped (SKIP_TESTS=1)"
    else
        if [ "$SKIP_SCREENSHOTS" != "1" ]; then
            TEST_TASKS="$TEST_TASKS :app:validateFullDebugScreenshotTest :wear:validateDebugScreenshotTest :common:validateDebugScreenshotTest"
            echo "→ Running tests (unit + screenshot validation)..."
        else
            echo "→ Running tests (unit only; screenshots skipped)..."
        fi
        # shellcheck disable=SC2086
        if ./gradlew ${GRADLE_FLAGS} --continue ${TEST_TASKS} > /tmp/test.log 2>&1; then
            echo "  ✓ Tests passed"
        else
            echo "  ✗ Tests failed (see /tmp/test.log)"
            FAILED=1
        fi
    fi
    echo ""

    echo "→ Building debug APK..."
    # shellcheck disable=SC2086
    if ./gradlew ${GRADLE_FLAGS} assembleDebug > /tmp/build.log 2>&1; then
        echo "  ✓ Debug build succeeded"
    else
        echo "  ✗ Debug build failed (see /tmp/build.log)"
        FAILED=1
    fi
    echo ""

    echo "=========================================="
    if [ $FAILED -eq 0 ] && [ "$LOCK_DIRTY" -eq 0 ]; then
        echo "✓ All checks passed!"
        echo "=========================================="
        exit 0
    else
        echo "✗ Some checks need attention"
        echo "=========================================="
        echo ""
        echo "Logs: /tmp/ktlint.log  /tmp/test.log  /tmp/build.log"
        exit 1
    fi
}

# ── Mode dispatch ──────────────────────────────────────────────────────────────

MODE="${1:---slices}"
shift || true

case "$MODE" in
    --slices) run_slices "$@" ;;
    --check)  run_check "$@" ;;
    *)        echo "Usage: verify.sh [--slices [base]] [--check [branch]]"; exit 1 ;;
esac
