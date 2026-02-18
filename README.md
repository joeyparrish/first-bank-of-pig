# First Bank of Pig

A simple Android piggy bank app for tracking kids' savings. Features parent mode for managing transactions and kid mode for viewing balances.

## Install from the Play Store

You don't need to build it yourself. It's free on the Play Store.

I do pay a small amount in cloud computing costs to run the app.  Donations are welcome through [GitHub Sponsors](https://github.com/sponsors/joeyparrish).

## Setup and Build

**See [docs/SETUP.md](docs/SETUP.md) for complete setup and build instructions.**

## Features

- **Parent Mode**: Create family, add children, manage transactions (deposits/withdrawals), invite other parents
- **Kid Mode**: View balance and transaction history (read-only)
- **Device Security**: Kid devices register via QR code; parents can view and revoke access
- **Biometric Lock**: Parent mode is protected by fingerprint/face/PIN
- **Real-time Sync**: Uses Firebase Firestore for instant updates across devices
- **Theme Support**: System, light, and dark mode

## Architecture

- **Kotlin** with **Jetpack Compose** for UI
- **Firebase Authentication** (Google Sign-In for parents, Anonymous for kids)
- **Firebase Firestore** for data storage
- **Material 3** design system

## Data Model

```
/families/{familyId}/
  name, ownerUid, createdAt
  parents/{uid}: { email, joinedAt }
  invites/{id}: { code, expiresAt, createdBy }
  children/{childId}: { name, createdAt }
    devices/{deviceUid}: { deviceName, registeredAt, lastAccessedAt }
    transactions/{txId}: { amount, description, date, createdAt, modifiedAt }

/inviteCodes/{code}: { familyId, expiresAt }
/childLookup/{code}: { familyId, childId }
```

Balance is computed client-side by summing transactions.

## License

[MIT](LICENSE.md)
