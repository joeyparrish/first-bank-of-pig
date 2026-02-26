# First Bank of Pig — Claude Context

## Maintaining This File

This file is the primary context source for Claude across sessions. **Keep it
current.** After any session that changes something captured here, update this
file before finishing.

**Always update when:**
- `firebase/firestore.rules` changes — the Security Model section must stay
  accurate; outdated security docs are worse than none
- A version is pinned with a reason (e.g. "stay on 12.x until compileSdk 36") —
  update the reason and version when the constraint is lifted
- A non-obvious pattern, gotcha, or deliberate trade-off is added to the codebase
- The data model changes (fields added/removed, new collections)
- An architectural decision is revisited or reversed

**Don't add:**
- Things that are obvious from reading the code
- Routine implementation details with no gotchas
- Session-specific context or in-progress work

Keep entries concise. This file should stay under ~200 lines.

## What This Is

Android piggy bank app (Kotlin, Jetpack Compose, Material 3, Firebase).
Package: `io.github.joeyparrish.fbop` | compileSdk=35 | minSdk=26

## Source Layout

```
app/src/main/java/io/github/joeyparrish/fbop/
  MainActivity.kt                  — entry point, biometric lock
  FBoPApplication.kt               — Application class
  ui/Navigation.kt                 — sealed Screen class + AppNavigation
  ui/theme/                        — Theme.kt, Color.kt; `AppTheme` object
                                     provides `watermarkAlpha` CompositionLocal
                                     (0.24f light / 0.12f dark)
  ui/screens/onboarding/           — ModeSelection, ParentSetup, KidSetup,
                                     CreateFamily, JoinFamily
  ui/screens/parent/               — ParentHome, ChildDetail, AddTransaction,
                                     EditTransaction, AddChild, EditChild,
                                     InviteParent, ManageParents,
                                     ManageDevices, ChildQrCode
  ui/screens/kid/                  — KidHome
  ui/screens/                      — About, OssLicenses
  ui/components/                   — ModeCard (shared component)
  data/model/Models.kt             — data classes
  data/repository/
    FirebaseRepository.kt          — all Firestore + Auth operations
    ConfigRepository.kt            — SharedPrefs (theme mode, device state)

firebase/
  firestore.rules                  — security rules (edit then deploy)
  firestore.indexes.json           — composite indexes

functions/
  index.js                         — Cloud Function: weekly cleanup of expired codes
  cleanup.js                       — standalone cleanup (run locally on Spark plan)

app/src/main/res/
  drawable/piggy_bank.png          — pig artwork; used as watermark + easter egg
  values-night/themes.xml          — dark window background (prevents white flash
                                     during screen transitions in dark mode)

play_assets/screenshots/           — Play Store screenshot assets
```

## Data Model

```
/families/{familyId}/
  name, ownerUid, createdAt
  parents/{uid}: { email, joinedAt, inviteCode }   — inviteCode for server-side validation
  invites/{id}: { code, expiresAt, createdBy }
  children/{childId}: { name, createdAt }
    devices/{deviceUid}: { deviceName, registeredAt, lastAccessedAt, lookupCode }
                          — deviceUid = anonymous Firebase Auth UID; lookupCode for validation
    transactions/{txId}: { amount, description, date, createdAt, modifiedAt }

/inviteCodes/{code}: { familyId, expiresAt }              — expires 24h, list:false
/childLookup/{code}: { familyId, childId, expiresAt }     — expires 1h,  list:false
```

Balance is computed client-side. Transaction `amount` is cents as `Long`
(positive = deposit, negative = withdrawal). Short codes: 8 chars from alphabet
`ABCDEFGHJKMNPQRSTWXYZ23456789` (29 chars; omits I, L, O, U, V, 0, 1).

`AppConfig` (SharedPreferences via `ConfigRepository`): `mode`
(NOT_CONFIGURED/PARENT/KID), `familyId`, `childId` (kid only), `lookupCode`
(kid only — stored locally so kid devices can reconnect without rescanning QR).

**Note**: `deleteChild()` does NOT cascade-delete the child's `transactions` or
`devices` subcollections. Only `deleteFamily()` does full cleanup. Orphaned
subcollections are inaccessible (rules require a parent record) but waste storage.

## Security Model

All enforcement is server-side in `firebase/firestore.rules`; a modified client
cannot bypass it.

**Enumeration prevention** — `inviteCodes` and `childLookup` split `read` into
`get` (allowed) / `list` (denied). Subcollection listing for kid devices is
naturally blocked: Firestore rejects any query where rules depend on per-document
values (`childId`, `deviceUid`) that can't be guaranteed across all results.

**Server-side invite validation** — The joining parent's document includes an
`inviteCode` field. The create rule cross-checks via `exists(/inviteCodes/$(code))`
+ `familyId` match + `expiresAt > request.time`. Firestore writes have no
side-channel for parameters; embedding the code in the document is the only way
to pass it to security rules for validation.

**Invite cleanup** — `families/{familyId}/invites/{code}` uses the code itself as
the document ID (matching `inviteCodes/{code}`). The invite delete rule allows any
parent (not just owner) so that `joinFamily()` can batch-delete both records on
join. Non-owner parents can't list invites, so they can only target a doc if they
know its ID (the code), which is unguessable (29^8 ≈ 500B).

**Server-side device registration** — The device document includes a `lookupCode`
field. The create rule cross-checks via `exists(/childLookup/$(code))` + `familyId`
+ `childId` path match.

**Kid transaction access** — `hasDeviceAccess()` checks
`exists(.../devices/$(request.auth.uid))`. The anonymous UID must be present as a
device document. Parent deleting that document immediately revokes server-side access
for that UID, regardless of what the client does.

**Trusted `lastAccessedAt`** — Client sends `FieldValue.serverTimestamp()`. Rules
enforce `request.resource.data.lastAccessedAt == request.time` and
`request.resource.data.diff(resource.data).affectedKeys().hasOnly(['lastAccessedAt'])`
so the client cannot fake the timestamp or modify any other field.

## Key Architectural Decisions

- **Parent mode**: Google Sign-In via Credential Manager
- **Kid mode**: Anonymous Firebase Auth + device registered by QR code scan
- **Theme**: Compose-managed via `ConfigRepository.getThemeMode()`; no AppCompat
- **OSS Licenses**: AboutLibraries 12.2.4 (`LibrariesContainer` composable); stay
  on 12.x until compileSdk bumps to 36 (v13.x requires compileSdk 36 + AGP 8.9.1)
- **Versioning**: git-based — `versionCode` from `git rev-list --count HEAD`,
  `versionName` from `git describe --tags --abbrev=0`
- **Debug builds**: no `applicationIdSuffix` so one `google-services.json` works
  for both debug and release
- **Analytics/Crash**: Firebase Analytics + Crashlytics enabled (anonymous; no PII)
- **AboutLibraries config** in `app/build.gradle.kts`: `offlineMode = true`,
  `duplicationMode = MERGE`, `duplicationRule = GROUP`
- **Java 17**: `compileOptions` and `jvmTarget` both target Java 17
- **Parent reconnect**: `findExistingFamily()` uses a Firestore collection group query
  on `parents` where `uid == request.auth.uid`. The `uid` field is written explicitly
  after `@DocumentId`-annotated set() calls (since `@DocumentId` is excluded from
  writes). A special collection group rule in `firestore.rules` allows this query.
- **`inviteCode` on Parent is nullable** — only set for joining parents, not the owner

## Building

**Java version**: Kotlin 2.0.x cannot parse Java 25 version strings and will
crash at build time. Build with Java 21:
`export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`

```bash
./gradlew assembleDebug    # → app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease  # requires keystore.properties (see docs/SETUP.md)
./gradlew clean
./gradlew lint
```

Release signing: `keystore.properties` in project root (gitignored). Paths
inside are relative to `app/`, not the project root:

```properties
storeFile=../release-keystore.jks
storePassword=…
keyAlias=release
keyPassword=…
```

## Firebase / Deploy

```bash
# After editing firestore.rules or firestore.indexes.json:
./scripts/firebase-deploy.sh

# Check Firestore usage (reads/writes/storage/family count):
./scripts/firebase-usage.sh

# First-time project setup (forks/new contributors):
./scripts/firebase-setup.sh
```

Config: `scripts/config.sh` (defaults) + `scripts/config.local.sh` (gitignored
local overrides — PROJECT_ID, BILLING_ACCOUNT, DEBUG_SHA1).

Cloud Functions require the Blaze plan. On Spark, run cleanup manually:
`cd functions && node cleanup.js`

## Docs

- Full setup / build guide: `docs/SETUP.md`
- Firebase cost estimates: `docs/COST.md`
- Privacy policy: `docs/PRIVACY.md`
- Data deletion instructions: `docs/DELETE.md`
