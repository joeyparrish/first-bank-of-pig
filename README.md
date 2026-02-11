# First Bank of Pig

A simple Android piggy bank app for tracking kids' savings. Features parent mode for managing transactions and kid mode for viewing balances.

**See [docs/SETUP.md](docs/SETUP.md) for complete setup and build instructions.**

## Features

- **Parent Mode**: Create family, add children, manage transactions (deposits/withdrawals), invite other parents
- **Kid Mode**: View balance and transaction history (read-only)
- **Biometric Lock**: Parent mode is protected by fingerprint/face/PIN
- **Real-time Sync**: Uses Firebase Firestore for instant updates across devices
- **QR Code Setup**: Easy device pairing with QR codes

## Setup

### Prerequisites

- Android Studio 2024.2 or later
- Node.js and npm (for Firebase CLI)
- Google Cloud account with billing enabled

### Firebase Setup

1. Edit `scripts/firebase-setup.sh` to set your project ID
2. Run the setup script:
   ```bash
   chmod +x scripts/firebase-setup.sh
   ./scripts/firebase-setup.sh
   ```
3. The script will create your Firebase project and download `google-services.json`

### Build

1. Open the project in Android Studio
2. Place `google-services.json` in the `app/` directory
3. Get your debug SHA-1 fingerprint:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
   ```
4. Add the SHA-1 fingerprint in Firebase Console (Project Settings > Your Apps > Add Fingerprint)
5. Build and run!

### Generate APK

```bash
./gradlew assembleRelease
```

The APK will be in `app/build/outputs/apk/release/`

## Architecture

- **Kotlin** with **Jetpack Compose** for UI
- **Firebase Authentication** (Google Sign-In for parents, Anonymous for kids)
- **Firebase Firestore** for data storage
- **Material 3** design system

## Data Model

```
/families/{familyId}/
  name: "Family Name"
  ownerUid: "parent-uid"
  parents/{uid}: { email, joinedAt }
  invites/{id}: { code, expiresAt }
  children/{childId}: { name }
    transactions/{txId}: { amount, description, date }
```

Balance is computed client-side by summing transactions.

## License

[MIT](LICENSE.md)
