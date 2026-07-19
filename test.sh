#!/bin/bash
#
# Run all tests, including unit tests and screenshot (preview) validation,
# across the app modules.
#
# Tasks are grouped so each group runs in a single Gradle invocation (the
# build is configured once per group instead of once per task) — this is much
# faster. Gradle's --continue keeps one failure from aborting the rest.
#
# Usage:
#   ./test.sh
#   ./test.sh --info
#
# Any extra arguments are forwarded to Gradle.
# Extra Gradle flags (e.g. to disable parallelism) can be set via GRADLE_FLAGS.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR" && pwd)"

EXTRA_ARGS=("$@")

GRADLE="${PROJECT_ROOT}/gradlew"
GRADLE_FLAGS="${GRADLE_FLAGS:---parallel --build-cache}"

echo "=== Running all tests ==="
echo ""

FAILURES=0

run_group() {
    local label="$1"
    shift
    echo "--- ${label} ---"
    echo ""
    # shellcheck disable=SC2086
    if "${GRADLE}" -p "${PROJECT_ROOT}" ${GRADLE_FLAGS} --continue "$@" "${EXTRA_ARGS[@]+"${EXTRA_ARGS[@]}"}"; then
        echo "  ${label} — PASSED"
    else
        echo "  ${label} — FAILED"
        FAILURES=$((FAILURES + 1))
    fi
    echo ""
}

# --- Unit tests (mirrors the CI unit_tests job) ---
run_group "unit tests" \
    :app:testFullDebugUnitTest \
    testDebugUnitTest \
    :lint:test

# --- Screenshot (preview) validation (mirrors the CI screenshot_test job) ---
run_group "screenshot validation" \
    :app:validateFullDebugScreenshotTest \
    :wear:validateDebugScreenshotTest \
    :common:validateDebugScreenshotTest

if [ "$FAILURES" -gt 0 ]; then
    echo "=== $FAILURES group(s) FAILED ==="
    exit 1
else
    echo "=== All tests PASSED ==="
fi
