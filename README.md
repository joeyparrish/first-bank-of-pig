# First Bank of Pig

A simple Android piggy bank app for tracking kids' savings.  Parents manage
transactions and family membership, and kids can view their transactions and
balance.

## Features

 - Parent Mode: Create family, add children, manage transactions (deposits/withdrawals), invite other parents
 - Kid Mode: View balance and transaction history (read-only)
 - Device Security: Kid devices register by scanning parent's QR code (no login required!); parents can manage kid devices and revoke access at any time
 - Biometric Lock: Parent mode is protected by fingerprint/face/PIN
 - Real-time Sync: Instant updates across devices
 - Theme Support: System, light, and dark mode

## Install from the Play Store

You don't need to build it yourself. It's free on the Play Store!

I do pay a small amount in cloud computing costs to run the app.  Donations are
welcome through [GitHub Sponsors](https://github.com/sponsors/joeyparrish).

## Setup and Build

**See [docs/SETUP.md](docs/SETUP.md) for complete setup and build instructions.**

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
