#!/bin/bash
# =============================================================================
# Firebase API Key Hardening Script for First Bank of Pig
# =============================================================================
#
# Restricts the Firebase Android API key to:
#   - Only your app's signing certificates (by SHA-1 fingerprint)
#   - Only the APIs Firebase actually needs
#
# Usage:
#   ./scripts/firebase-harden.sh SHA1 [SHA1 ...]
#
# Arguments:
#   SHA1 -- One or more SHA-1 signing fingerprints (colon-separated hex pairs).
#           Provide at minimum your debug key. Add your release key and the
#           Play Store distribution key for production builds.
#
# Getting SHA-1 fingerprints:
#
#   Debug key (every developer machine has a unique one):
#     keytool -list -v \
#       -keystore ~/.android/debug.keystore \
#       -alias androiddebugkey -storepass android | grep SHA1
#
#   Release key (your local keystore):
#     keytool -list -v \
#       -keystore release-keystore.jks \
#       -alias release | grep SHA1
#
#   Play Store distribution key (what Google re-signs with, if using Play App
#   Signing):
#     Play Console -> Your app -> Test and release -> App integrity
#     -> Play app signing -> "App signing key certificate"
#     -> SHA-1 certificate fingerprint
#
# Example:
#   ./scripts/firebase-harden.sh \
#     "A1:B2:C3:..." \       # debug
#     "D4:E5:F6:..." \       # release / upload key
#     "G7:H8:I9:..."         # Play Store distribution key
#
# To inspect current restrictions before running:
#   KEY_ID=$(gcloud services api-keys lookup \
#     "$(jq -r '.client[0].api_key[0].current_key' app/google-services.json)" \
#     --project="$PROJECT_ID" | grep name: | sed 's@.*/@@')
#   gcloud services api-keys describe "$KEY_ID" \
#     --project="$PROJECT_ID" --format="yaml(restrictions)"
#
# Note: this script fully replaces the key's restrictions each time it runs.
# To remove an API target, comment it out and rerun. Test the app after each
# removal to confirm no breakage before removing the next one.
#
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/config.sh"

# -----------------------------------------------------------------------------
# Validate arguments
# -----------------------------------------------------------------------------

if [ $# -eq 0 ]; then
    echo "Usage: $0 SHA1 [SHA1 ...]"
    echo ""
    echo "Provide one or more SHA-1 fingerprints for your signing keys."
    echo "See script header for how to obtain them."
    exit 1
fi

# -----------------------------------------------------------------------------
# Look up the API key ID from google-services.json
# -----------------------------------------------------------------------------

GOOGLE_SERVICES="$SCRIPT_DIR/../app/google-services.json"
if [ ! -f "$GOOGLE_SERVICES" ]; then
    echo "ERROR: app/google-services.json not found."
    echo "Run ./scripts/firebase-setup.sh first, or download it from the Firebase Console."
    exit 1
fi

API_KEY=$(jq -r '.client[0].api_key[0].current_key' "$GOOGLE_SERVICES")
echo "Looking up key ID for API key: ${API_KEY:0:10}..."

KEY_NAME=$(gcloud services api-keys lookup "$API_KEY" \
    --project="$PROJECT_ID" --format="value(name)")
KEY_ID="${KEY_NAME##*/}"

echo "Found key ID: $KEY_ID"
echo ""

# -----------------------------------------------------------------------------
# Build allowed-application args (one per SHA-1 fingerprint)
# -----------------------------------------------------------------------------

APP_ARGS=()
for SHA1 in "$@"; do
    APP_ARGS+=("--allowed-application=sha1_fingerprint=${SHA1},android_app_id=${ANDROID_PACKAGE}")
done

# -----------------------------------------------------------------------------
# API targets
#
# Starts with a broad set. Remove entries one at a time, rerun, and test the
# app to confirm no breakage before removing the next one. The ones marked
# "likely unused" were not explicitly enabled for this app but may have been
# added automatically by Firebase or Google Cloud.
# -----------------------------------------------------------------------------

API_ARGS=(
    "--api-target=service=identitytoolkit.googleapis.com"   # Firebase Auth
    "--api-target=service=securetoken.googleapis.com"       # Token refresh
    "--api-target=service=firebaseinstallations.googleapis.com"  # Firebase Installations
    "--api-target=service=firebase.googleapis.com"          # Firebase Management
    "--api-target=service=firestore.googleapis.com"         # Cloud Firestore
    "--api-target=service=crashlytics.googleapis.com"       # Crashlytics
    "--api-target=service=monitoring.googleapis.com"        # Cloud Monitoring
    "--api-target=service=logging.googleapis.com"           # Cloud Logging
    # Likely unused -- remove and test:
    "--api-target=service=fcm.googleapis.com"               # Firebase Cloud Messaging
    "--api-target=service=firebasestorage.googleapis.com"   # Firebase Storage
    "--api-target=service=firebaseremoteconfig.googleapis.com"  # Remote Config
    "--api-target=service=sqladmin.googleapis.com"          # Cloud SQL Admin
    "--api-target=service=datastore.googleapis.com"         # Cloud Datastore
    "--api-target=service=ml.googleapis.com"                # ML Kit
)

# -----------------------------------------------------------------------------
# Apply restrictions
# -----------------------------------------------------------------------------

echo "Applying restrictions to key $KEY_ID in project $PROJECT_ID..."
echo "  Android app: $ANDROID_PACKAGE"
echo "  SHA-1 fingerprints:"
for SHA1 in "$@"; do
    echo "    $SHA1"
done
echo ""

gcloud services api-keys update "$KEY_ID" \
    --project="$PROJECT_ID" \
    "${APP_ARGS[@]}" \
    "${API_ARGS[@]}"

echo ""
echo "Done. Restrictions applied."
echo ""
echo "To verify:"
echo "  gcloud services api-keys describe $KEY_ID \\"
echo "    --project=$PROJECT_ID --format='yaml(restrictions)'"
