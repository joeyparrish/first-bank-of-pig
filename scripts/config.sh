#!/bin/bash
# =============================================================================
# Shared configuration for First Bank of Pig scripts
# =============================================================================
#
# Edit these defaults, or override them in scripts/config.local.sh (which is
# not revision controlled, so your overrides won't cause merge conflicts).
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# Firebase/GCP project ID (must be globally unique)
PROJECT_ID="first-bank-of-pig"

# Human-readable project name
PROJECT_NAME="First Bank of Pig"

# Android package name (extracted from build.gradle.kts as single source of truth)
ANDROID_PACKAGE=$(grep 'applicationId' "$PROJECT_DIR/app/build.gradle.kts" | grep -o '"[^"]*"' | tr -d '"')

# GCP billing account ID (format: XXXXXX-XXXXXX-XXXXXX)
# Leave empty to skip billing setup (Spark free tier only)
# Find yours at: https://console.cloud.google.com/billing
BILLING_ACCOUNT=""

# SHA-1 fingerprint of your debug signing key (for Google Sign-In)
# Get this by running:
#   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
# Look for the SHA1: line
DEBUG_SHA1=""

# =============================================================================
# Load local overrides (not revision controlled)
# =============================================================================

if [ -f "$SCRIPT_DIR/config.local.sh" ]; then
    source "$SCRIPT_DIR/config.local.sh"
fi
