#!/bin/bash

# =============================================================================
# Firebase Deploy Script for First Bank of Pig
# =============================================================================
# This script deploys Firestore security rules and indexes to an existing
# Firebase project. Run this after making changes to:
#   - firebase/firestore.rules
#   - firebase/firestore.indexes.json
#
# Prerequisites:
#   - Firebase CLI installed (npm install -g firebase-tools)
#   - Logged into Firebase (firebase login)
#   - Project already set up (firebase-setup.sh completed successfully)
#
# Usage:
#   ./scripts/firebase-deploy.sh
# =============================================================================

set -e

# -----------------------------------------------------------------------------
# Configuration - EDIT THESE VALUES
# -----------------------------------------------------------------------------

# Choose a globally unique project ID (lowercase, numbers, hyphens only)
# This will be your project's identifier across all of Google Cloud/Firebase
PROJECT_ID="first-bank-of-pig"

# -----------------------------------------------------------------------------
# End of Configuration
# -----------------------------------------------------------------------------

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

echo "=============================================="
echo "First Bank of Pig - Firebase Deploy"
echo "=============================================="
echo ""

# Check for Firebase CLI
if ! command -v firebase &> /dev/null; then
    echo "Error: Firebase CLI is not installed."
    echo "Install it with: npm install -g firebase-tools"
    exit 1
fi

# Check if logged in
if ! firebase projects:list &> /dev/null; then
    echo "Error: Not logged into Firebase."
    echo "Run: firebase login"
    exit 1
fi

echo "Deploying to project: $PROJECT_ID"
echo ""

# Deploy Firestore rules
echo "Deploying Firestore security rules..."
firebase deploy --only firestore:rules --project="$PROJECT_ID"

echo ""

# Deploy Firestore indexes (if any)
if [ -f "firebase/firestore.indexes.json" ]; then
    echo "Deploying Firestore indexes..."
    firebase deploy --only firestore:indexes --project="$PROJECT_ID"
    echo ""
fi

echo "=============================================="
echo "Deployment complete!"
echo "=============================================="
echo ""
echo "Deployed:"
echo "  - Firestore security rules"
if [ -f "firebase/firestore.indexes.json" ]; then
    echo "  - Firestore indexes"
fi
echo ""
echo "You can verify the rules in the Firebase Console:"
echo "  https://console.firebase.google.com/project/$PROJECT_ID/firestore/rules"
echo ""
