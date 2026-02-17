#!/bin/bash
# =============================================================================
# Firebase Setup Script for First Bank of Pig
# =============================================================================
#
# This script sets up a Firebase project for the First Bank of Pig Android app.
# READ THROUGH THIS SCRIPT before running - it's designed to be executed
# section by section, or you can run the whole thing if you're feeling brave.
#
# Prerequisites:
#   - Google Cloud SDK (gcloud) installed and authenticated
#   - Node.js and npm installed (for Firebase CLI)
#   - A GCP billing account (Firebase Blaze plan for full features, though
#     Spark free tier works for this app's usage levels)
#
# Usage:
#   chmod +x scripts/firebase-setup.sh
#   ./scripts/firebase-setup.sh
#
# Or run sections manually by copying commands to your terminal.
# =============================================================================

set -e  # Exit on any error

# -----------------------------------------------------------------------------
# Configuration - EDIT THESE VALUES
# -----------------------------------------------------------------------------

# Choose a globally unique project ID (lowercase, numbers, hyphens only)
# This will be your project's identifier across all of Google Cloud/Firebase
PROJECT_ID="first-bank-of-pig"

# Human-readable project name
PROJECT_NAME="First Bank of Pig"

# Your GCP billing account ID (find at https://console.cloud.google.com/billing)
# Format: XXXXXX-XXXXXX-XXXXXX
# Leave empty to skip billing setup (Spark free tier only)
BILLING_ACCOUNT=""

# Android package name (must match build.gradle.kts)
ANDROID_PACKAGE="io.github.joeyparrish.fbop"

# SHA-1 fingerprint of your debug signing key (for Google Sign-In)
# Get this by running:
#   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
# Look for the SHA1: line
DEBUG_SHA1=""

# -----------------------------------------------------------------------------
# Helper functions
# -----------------------------------------------------------------------------

echo_step() {
    echo ""
    echo "==========================================================================="
    echo "STEP: $1"
    echo "==========================================================================="
    echo ""
}

check_command() {
    if ! command -v "$1" &> /dev/null; then
        echo "ERROR: $1 is not installed. Please install it first."
        exit 1
    fi
}

# -----------------------------------------------------------------------------
# Step 1: Verify prerequisites
# -----------------------------------------------------------------------------

echo_step "Verifying prerequisites"

check_command gcloud
check_command npm

echo "gcloud version:"
gcloud --version | head -1

echo ""
echo "Checking gcloud authentication..."
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" | head -1; then
    echo "Not authenticated. Running 'gcloud auth login'..."
    gcloud auth login
fi

echo ""
echo "Active account: $(gcloud auth list --filter=status:ACTIVE --format='value(account)' | head -1)"

# -----------------------------------------------------------------------------
# Step 2: Install Firebase CLI (if not already installed)
# -----------------------------------------------------------------------------

echo_step "Installing Firebase CLI"

if ! command -v firebase &> /dev/null; then
    echo "Firebase CLI not found. Installing via npm..."
    npm install -g firebase-tools
else
    echo "Firebase CLI already installed: $(firebase --version)"
fi

echo ""
echo "Logging into Firebase..."
firebase login

# -----------------------------------------------------------------------------
# Step 3: Create GCP Project
# -----------------------------------------------------------------------------

echo_step "Creating GCP Project"

# Check if project already exists
if gcloud projects describe "$PROJECT_ID" &> /dev/null; then
    echo "Project $PROJECT_ID already exists. Skipping creation."
else
    echo "Creating project: $PROJECT_ID"
    gcloud projects create "$PROJECT_ID" --name="$PROJECT_NAME"
fi

# Set as active project
gcloud config set project "$PROJECT_ID"

# -----------------------------------------------------------------------------
# Step 4: Link billing account (optional, for Blaze plan)
# -----------------------------------------------------------------------------

echo_step "Linking billing account"

if [ -n "$BILLING_ACCOUNT" ]; then
    # Check if billing is already linked
    CURRENT_BILLING=$(gcloud billing projects describe "$PROJECT_ID" --format="value(billingAccountName)" 2>/dev/null || true)
    if [ -n "$CURRENT_BILLING" ]; then
        echo "Billing already linked: $CURRENT_BILLING. Skipping."
    else
        echo "Linking billing account: $BILLING_ACCOUNT"
        gcloud billing projects link "$PROJECT_ID" --billing-account="$BILLING_ACCOUNT"
    fi
else
    echo "No billing account specified. Skipping."
    echo "Note: The Firebase Spark (free) tier is sufficient for this app."
    echo "You can link billing later if needed for Cloud Functions or higher usage."
fi

# -----------------------------------------------------------------------------
# Step 5: Add Firebase to the project
# -----------------------------------------------------------------------------

echo_step "Adding Firebase to the project"

# Check if Firebase is already enabled on this project
if firebase projects:list --json 2>/dev/null | grep -q "\"projectId\": \"$PROJECT_ID\""; then
    echo "Firebase already enabled for $PROJECT_ID. Skipping."
else
    echo "Adding Firebase to project..."
    firebase projects:addfirebase "$PROJECT_ID"
fi

# -----------------------------------------------------------------------------
# Step 6: Enable required APIs
# -----------------------------------------------------------------------------

echo_step "Enabling required Google Cloud APIs"

# Firebase Auth
gcloud services enable identitytoolkit.googleapis.com --project="$PROJECT_ID"

# Firestore
gcloud services enable firestore.googleapis.com --project="$PROJECT_ID"

# Firebase Management API
gcloud services enable firebase.googleapis.com --project="$PROJECT_ID"

echo "APIs enabled successfully."

# -----------------------------------------------------------------------------
# Step 7: Create Firestore database
# -----------------------------------------------------------------------------

echo_step "Creating Firestore database"

# Create Firestore in Native mode
# nam5 = North America multi-region (higher availability, slightly higher cost)
# Or use a single region like us-central1 for lower latency/cost
# Once created, location cannot be changed.
# See: https://firebase.google.com/docs/firestore/locations

# Check if Firestore database already exists
if gcloud firestore databases describe --project="$PROJECT_ID" &>/dev/null; then
    echo "Firestore database already exists. Skipping creation."
else
    echo "Creating Firestore database in nam5 (North America multi-region)..."
    gcloud firestore databases create \
        --project="$PROJECT_ID" \
        --location="nam5" \
        --type="firestore-native"
fi

# -----------------------------------------------------------------------------
# Step 8: Enable Google Sign-In authentication
# -----------------------------------------------------------------------------

echo_step "Configuring Firebase Authentication"

echo "Google Sign-In must be enabled in the Firebase Console:"
echo ""
echo "1. Go to: https://console.firebase.google.com/project/$PROJECT_ID/authentication/providers"
echo "2. Click on 'Google' in the provider list"
echo "3. Toggle 'Enable' to ON"
echo "4. Set a support email (your email)"
echo "5. Click 'Save'"
echo ""
echo "Also enable Anonymous authentication:"
echo "1. Click on 'Anonymous' in the provider list"
echo "2. Toggle 'Enable' to ON"
echo "3. Click 'Save'"
echo ""
read -p "Press Enter after completing these steps in the Firebase Console..."

# -----------------------------------------------------------------------------
# Step 9: Register Android app with Firebase
# -----------------------------------------------------------------------------

echo_step "Registering Android app"

echo "Registering Android app: $ANDROID_PACKAGE"

# Check if Android app already exists
if firebase apps:list --project="$PROJECT_ID" 2>/dev/null | grep -q "ANDROID"; then
    echo "Android app already registered. Skipping creation."
else
    firebase apps:create \
        --project="$PROJECT_ID" \
        -a "$ANDROID_PACKAGE" \
        android "First Bank of Pig"
fi

# Download google-services.json (always do this to ensure it's up to date)
echo ""
echo "Downloading google-services.json..."
firebase apps:sdkconfig android \
    --project="$PROJECT_ID" \
    --out="app/google-services.json"

echo ""
echo "google-services.json saved to app/google-services.json"

# -----------------------------------------------------------------------------
# Step 10: Add SHA-1 fingerprint for Google Sign-In
# -----------------------------------------------------------------------------

echo_step "Adding SHA-1 fingerprint"

if [ -n "$DEBUG_SHA1" ]; then
    echo "To add your debug SHA-1 fingerprint:"
    echo ""
    echo "1. Go to: https://console.firebase.google.com/project/$PROJECT_ID/settings/general"
    echo "2. Scroll down to 'Your apps' and find the Android app"
    echo "3. Click 'Add fingerprint'"
    echo "4. Enter: $DEBUG_SHA1"
    echo "5. Click 'Save'"
else
    echo "No SHA-1 fingerprint configured in this script."
    echo ""
    echo "To get your debug SHA-1, run:"
    echo "  keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android"
    echo ""
    echo "Then add it in Firebase Console:"
    echo "1. Go to: https://console.firebase.google.com/project/$PROJECT_ID/settings/general"
    echo "2. Scroll down to 'Your apps' and find the Android app"
    echo "3. Click 'Add fingerprint'"
    echo "4. Paste your SHA-1 fingerprint"
    echo "5. Click 'Save'"
fi

echo ""
read -p "Press Enter after adding your SHA-1 fingerprint..."

# -----------------------------------------------------------------------------
# Step 11: Deploy Firestore security rules
# -----------------------------------------------------------------------------

echo_step "Deploying Firestore security rules"

firebase deploy --only firestore:rules --project="$PROJECT_ID"

echo ""
echo "Firestore rules deployed successfully."

# -----------------------------------------------------------------------------
# Step 12: Enable monitoring API
# -----------------------------------------------------------------------------

echo_step "Enabling monitoring API"

gcloud services enable monitoring.googleapis.com --project="$PROJECT_ID"

echo ""
echo "Monitoring API enabled."

# -----------------------------------------------------------------------------
# Done!
# -----------------------------------------------------------------------------

echo_step "Setup Complete!"

echo "Your Firebase project is ready. Here's a summary:"
echo ""
echo "  Project ID:     $PROJECT_ID"
echo "  Console URL:    https://console.firebase.google.com/project/$PROJECT_ID"
echo "  Firestore:      https://console.firebase.google.com/project/$PROJECT_ID/firestore"
echo "  Auth:           https://console.firebase.google.com/project/$PROJECT_ID/authentication"
echo ""
echo "Files created:"
echo "  - app/google-services.json (Firebase config for Android)"
echo ""
echo "Next steps:"
echo "  1. Open the project in Android Studio"
echo "  2. Build and run on a device/emulator"
echo "  3. Test Google Sign-In"
echo ""
echo "Happy banking! 🐷"
