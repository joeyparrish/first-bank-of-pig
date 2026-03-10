# First Bank of Pig -- Manual Test Plan

## Setup

**Devices needed:** Three emulator instances (or two emulators + one physical device). Call them
**A** (owner), **B** (secondary parent), **C** (kid).

**Before starting:**
- Install a debug build on each device
- Add a Google account to emulators A and B (Settings > Accounts > Add account > Google) --
  required for Sign In with Google
- Biometric prompt on emulators: in Android Studio's Extended Controls, use
  Fingerprint > Touch Sensor, or `adb -s <emulator> emu finger touch 1`
- Use **Enter manually** for all QR/code flows on emulators (camera is unreliable)
- The child QR lookup code expires after **1 hour** -- complete kid setup within that window

**How to wipe app state (simulates new install or factory reset):**
Settings > Apps > First Bank of Pig > Storage & cache > Clear storage

**How to wipe only auth session (simulates token expiry without data loss):**
Not easily reproducible manually; force-stopping and restarting the app leaves auth intact.

---

## Suite 1: Owner -- First-Time Setup

| # | Steps | Expected |
|---|-------|----------|
| 1.1 | Launch app on A | Mode selection screen |
| 1.2 | Tap "I'm a parent" | Parent setup screen |
| 1.3 | Tap "Create new family" | Create family screen with Sign In button |
| 1.4 | Tap Sign in with Google, complete auth | Returns to create family, name field visible |
| 1.5 | Leave name blank, tap Create | Validation error shown, no navigation |
| 1.6 | Enter a family name, tap Create | Navigates to parent home, family name in top bar |
| 1.7 | Force-stop and relaunch app | Biometric prompt, then parent home (no mode selection) |

---

## Suite 2: Owner -- Children

| # | Steps | Expected |
|---|-------|----------|
| 2.1 | Tap + FAB | Add child screen |
| 2.2 | Leave name blank, tap Save | Validation error, no navigation |
| 2.3 | Enter child name, tap Save | Returns to parent home, child card visible |
| 2.4 | Add a second child | Both cards on home screen |
| 2.5 | Tap first child card | Child detail screen |
| 2.6 | Tap edit (pencil icon), change name, save | Name updated in top bar and on home card |

---

## Suite 3: Owner -- Transactions

Run from the child detail screen.

| # | Steps | Expected |
|---|-------|----------|
| 3.1 | Tap + to add transaction | Add transaction screen |
| 3.2 | Leave amount blank, tap Save | Validation error |
| 3.3 | Enter amount $5.00, select Deposit, add description, tap Save | Returns to child detail, transaction listed, balance shows $5.00 |
| 3.4 | Add a $2.50 withdrawal | Balance shows $2.50, withdrawal row shows negative/red |
| 3.5 | Tap a transaction row | Edit transaction screen, fields pre-filled |
| 3.6 | Change amount and description, save | Transaction updated in list, balance recalculated |
| 3.7 | Long-press or find delete on a transaction | Confirmation dialog shown |
| 3.8 | Confirm delete | Transaction removed, balance updated |
| 3.9 | Go back to parent home | Child card shows updated balance and correct transaction count |

---

## Suite 4: Owner -- Invite Secondary Parent

| # | Steps | Expected |
|---|-------|----------|
| 4.1 | Menu (⋮) on parent home | Owner sees: Invite parent, Manage parents, Delete family, Theme, About, Support |
| 4.2 | Tap "Invite parent" | Invite screen with 8-character code displayed |
| 4.3 | Tap Copy button | Snackbar confirms "Code copied to clipboard" |
| 4.4 | Note the code for use in Suite 5 | -- |
| 4.5 | Navigate back; tap Invite parent again | New code generated (different from previous) |

---

## Suite 5: Secondary Parent -- Join Family

Run on device B with the code from Suite 4 step 4.3. The invite code expires after 24 hours.

| # | Steps | Expected |
|---|-------|----------|
| 5.1 | Launch on B, tap "I'm a parent" | Parent setup screen |
| 5.2 | Tap "Join with an invite code" | Join family screen, scan or enter manually |
| 5.3 | Tap "Enter code manually" | Manual entry field |
| 5.4 | Type 7 characters | Continue button remains disabled |
| 5.5 | Type the 8th character | Continue button enables |
| 5.6 | Backspace one character | Continue button disables again |
| 5.7 | Enter a completely wrong 8-character code, tap Continue | Error "Invalid code" shown |
| 5.8 | Enter the real code, tap Continue | "Code verified" screen, Sign in button |
| 5.9 | Tap Sign in with Google (different account than A) | Navigates to parent home |
| 5.10 | Verify family name matches A, same children visible | -- |
| 5.11 | On A: Menu > Manage parents | Both parent accounts listed |
| 5.12 | On B: Menu (⋮) | B only sees: Theme, About, Support (no owner-only items) |

**Join via QR (physical devices only):**
- On A: Menu > Invite parent, display QR
- On B: Tap "Scan QR code", scan A's screen

---

## Suite 6: Owner -- Manage Parents

| # | Steps | Expected |
|---|-------|----------|
| 6.1 | On A: Menu > Manage parents | Both parents listed |
| 6.2 | Tap Remove on secondary parent | Confirmation dialog |
| 6.3 | Confirm | Secondary parent removed from list |
| 6.4 | Observe B | Note what happens (expected: access lost, may show error or return to mode selection) |
| 6.5 | On A: re-invite B and have B rejoin | B back in family |

---

## Suite 7: Kid -- Setup

Run on device C. First, on device A: go to child detail for the child you want to set up,
tap the QR code icon, note the code.

| # | Steps | Expected |
|---|-------|----------|
| 7.1 | Launch on C, tap "I'm a kid" | Kid setup screen |
| 7.2 | Tap "Enter code manually" | Manual entry field |
| 7.3 | Enter a wrong code, tap Continue | Error "Invalid code" |
| 7.4 | Enter the correct code from A | Navigates to kid home screen |
| 7.5 | Verify child name in top bar, balance matches parent view | -- |
| 7.6 | Transactions list matches parent view, in descending date order | -- |
| 7.7 | On A: Manage devices for that child | C's device name appears (manufacturer + model) |

---

## Suite 8: Kid -- Daily Use

| # | Steps | Expected |
|---|-------|----------|
| 8.1 | On A: add a new transaction for that child | On C: balance and transaction update in real time without refreshing |
| 8.2 | On C: pull down to refresh | Spinner appears briefly, data reloads |
| 8.3 | On C: Menu > Theme | Dialog with System/Light/Dark options |
| 8.4 | Select Dark | UI switches to dark theme |
| 8.5 | Select System | Follows device theme |
| 8.6 | On C: Menu > "Have a pig" | Full-screen pig image |
| 8.7 | Tap back | Returns to kid home |
| 8.8 | On C (debug build): "Have a pig" | Calendar + pig image; changing date changes the pig variant on applicable dates |
| 8.9 | On C: tap About | About screen with version and OSS licenses link |

---

## Suite 9: Access Revocation

| # | Steps | Expected |
|---|-------|----------|
| 9.1 | Ensure C is on kid home screen | -- |
| 9.2 | On A: child detail > Manage devices > revoke C's device | -- |
| 9.3 | Observe C | "Access revoked" dialog appears (may take a moment) |
| 9.4 | Tap OK on C | Config cleared, returns to mode selection |
| 9.5 | On A: confirm device no longer listed | -- |

---

## Suite 10: Reconnection Flows

### 10a: Owner reconnect after data wipe

| # | Steps | Expected |
|---|-------|----------|
| 10a.1 | Wipe app data on A | -- |
| 10a.2 | Launch A, tap "I'm a parent" | Parent setup |
| 10a.3 | Sign in with same Google account used in Suite 1 | Finds existing family via collection group query, navigates to parent home |
| 10a.4 | Family, children, and transactions all present | -- |

### 10b: Secondary parent reconnect after data wipe

Same as 10a using B's Google account. Should reconnect to the same family.

### 10c: Kid reconnect after force-stop (no data loss)

| # | Steps | Expected |
|---|-------|----------|
| 10c.1 | Force-stop app on C | -- |
| 10c.2 | Relaunch C | Biometric prompt, then directly to kid home (no setup) |
| 10c.3 | Data loads normally | Anonymous auth session and device registration still valid |

### 10d: Kid reconnect after data wipe

| # | Steps | Expected |
|---|-------|----------|
| 10d.1 | Wipe app data on C | -- |
| 10d.2 | Launch C | Mode selection (config wiped) |
| 10d.3 | Tap "I'm a kid", enter code manually (parent generates new QR on A) | New anonymous UID registered as new device |
| 10d.4 | On A: Manage devices | Two device entries for that child (old + new) |
| 10d.5 | On A: remove the stale old device entry | -- |

---

## Suite 11: Delete Family

Run last -- this is destructive.

| # | Steps | Expected |
|---|-------|----------|
| 11.1 | On A (owner): Menu > Delete family | Confirmation dialog with family name |
| 11.2 | Tap Cancel | Dialog closes, family intact |
| 11.3 | Tap Delete family, confirm | Spinner while deleting, then mode selection screen |
| 11.4 | On B: observe | Note what happens (expected: access lost) |
| 11.5 | In Firebase Console: verify family document gone, no orphaned subcollections | -- |

---

## Suite 12: Theme Persistence

| # | Steps | Expected |
|---|-------|----------|
| 12.1 | On A: set theme to Dark | UI goes dark |
| 12.2 | Force-stop and relaunch | Dark theme applied immediately on launch, no flash |
| 12.3 | Set theme to System, switch device to dark mode | App follows |

---

## Notes

- **Firestore rule changes** require `./scripts/firebase-deploy.sh` before testing; client-side
  code changes do not
- **Biometric on emulator:** Extended Controls > Fingerprint > Touch Sensor (enroll first in
  emulator Settings > Security > Fingerprint)
- **Google Sign-In on emulator:** requires a real Google account added in emulator Settings;
  `setFilterByAuthorizedAccounts(false)` is already set so the account picker appears
- **Invite code expiry:** codes expire 24h; child lookup codes expire 1h -- plan kid setup
  (Suite 7) within 1h of generating the code on A
- **Real-time updates** (Suite 8.1) verify the `observeTransactions` flow is working; if
  updates don't appear, check Firestore rules are deployed and the kid device still has access
- **Suite 9 (access revocation):** if the "access revoked" dialog appears on network loss
  instead of only on deliberate revocation, the PERMISSION_DENIED fix is not working correctly
