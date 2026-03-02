#!/bin/bash
#
# Script to verify a PR branch is ready by running code quality checks
#
# Usage: ./verify-pr.sh [BRANCH_NAME]
# Example: ./verify-pr.sh
#          ./verify-pr.sh feature/shortcuts-v2-data-interfaces
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

cd "$PROJECT_ROOT"

# Get branch name
if [ $# -eq 0 ]; then
    BRANCH_NAME=$(git branch --show-current)
    echo "Using current branch: $BRANCH_NAME"
else
    BRANCH_NAME=$1
    echo "Verifying branch: $BRANCH_NAME"
    
    # Check if we need to checkout
    CURRENT_BRANCH=$(git branch --show-current)
    if [ "$CURRENT_BRANCH" != "$BRANCH_NAME" ]; then
        echo "Checking out $BRANCH_NAME..."
        git checkout "$BRANCH_NAME"
    fi
fi

echo ""
echo "=========================================="
echo "Running PR Verification Checks"
echo "=========================================="
echo ""

# Track failures
FAILED=0

# Check 1: KTLint
echo "→ Running KTLint check..."
if ./gradlew ktlintCheck :build-logic:convention:ktlintCheck --continue > /tmp/ktlint.log 2>&1; then
    echo "  ✓ KTLint passed"
else
    echo "  ✗ KTLint failed"
    echo "    Run './gradlew :build-logic:convention:ktlintFormat ktlintFormat' to fix formatting issues"
    echo "    See /tmp/ktlint.log for details"
    FAILED=1
fi
echo ""

# Check 2: Unit Tests
echo "→ Running unit tests..."
if ./gradlew test > /tmp/test.log 2>&1; then
    echo "  ✓ Unit tests passed"
else
    echo "  ✗ Unit tests failed"
    echo "    See /tmp/test.log for details"
    FAILED=1
fi
echo ""

# Check 3: Debug Build
echo "→ Building debug APK..."
if ./gradlew assembleDebug > /tmp/build.log 2>&1; then
    echo "  ✓ Debug build succeeded"
else
    echo "  ✗ Debug build failed"
    echo "    See /tmp/build.log for details"
    FAILED=1
fi
echo ""

# Summary
echo "=========================================="
if [ $FAILED -eq 0 ]; then
    echo "✓ All checks passed!"
    echo "=========================================="
    echo ""
    echo "This PR is ready for submission."
    exit 0
else
    echo "✗ Some checks failed"
    echo "=========================================="
    echo ""
    echo "Please fix the issues above before submitting."
    echo ""
    echo "To see detailed logs:"
    echo "  KTLint:  cat /tmp/ktlint.log"
    echo "  Tests:   cat /tmp/test.log"
    echo "  Build:   cat /tmp/build.log"
    exit 1
fi
