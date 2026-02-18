# Privacy Policy

**First Bank of Pig**
*Last updated: February 2026*

## Overview

First Bank of Pig is an open-source piggy bank app that helps parents track
their children's savings. This policy describes what data is collected, how it
is used, and how it is stored.

## Data You Provide

When you use the app, you provide the following data, which is stored in
Google Cloud Firestore:

- **Family name** — chosen by the parent when creating a family
- **Child names** — entered by the parent
- **Transaction records** — amounts and descriptions entered by the parent
- **Google account identity** — used for parent authentication (email and
  display name from your Google account)

This data is stored on Google's servers and is only accessible to
authenticated parents within your family. Child devices access a read-only
view of their own transaction data.

No financial account numbers, bank details, or real currency is involved.
All amounts are simple numerical records managed by parents.

## Data Collected Automatically

The app uses the following Firebase services, which collect some data
automatically:

### Firebase Analytics

Firebase Analytics collects anonymous usage data including:
- App opens and screen views
- Session duration
- Device type, operating system version, and language
- Country and region (coarse location derived from IP address)
- App version

This data is aggregated and anonymous. It helps us understand how the app is
used and identify areas for improvement. No personally identifiable
information is collected by Analytics.

For more information, see
[Google's Analytics data collection documentation](https://support.google.com/analytics/answer/11593727).

### Firebase Crashlytics

When the app crashes, Crashlytics automatically collects:
- Stack trace (technical details of the error)
- Device type and operating system version
- App version
- Crash timestamp

This data is used solely to identify and fix bugs. Crash reports do not
contain your family data, transaction details, or personal information.

For more information, see
[Google's Crashlytics data collection documentation](https://firebase.google.com/docs/crashlytics/troubleshoot-faq#data_collection).

### Firebase Authentication

Parent accounts use Google Sign-In. The app receives your Google display
name and email address to identify you within your family. This information
is managed by Firebase Authentication.

Child devices use anonymous authentication. No personal information is
collected from children for authentication purposes.

## Children's Privacy

The app is designed for families. Children access a read-only view of their
savings using a device registered by their parent. The child-facing portion
of the app:

- Does not require children to provide any personal information
- Uses anonymous authentication (no accounts or email addresses)
- Does not display advertising
- Does not include in-app purchases

Parents control all data entry and can remove child devices at any time.

## Data Storage and Security

All app data is stored in Google Cloud Firestore, which provides encryption
at rest and in transit. Access to family data is restricted by Firebase
Security Rules:

- Only authenticated parents within a family can read or modify that
  family's data
- Child devices can only read their own transaction data
- Device access can be revoked by parents at any time

## Data Retention

Your data remains in Firestore as long as you use the app. To delete your
data, you can:

- Remove individual children or transactions from within the app
- Contact the developer to request full deletion of your family's data

Temporary data such as invite codes and device lookup codes expire
automatically (24 hours and 1 hour respectively) and are cleaned up
periodically.

## Third-Party Services

The app uses the following Google services, which are subject to
[Google's Privacy Policy](https://policies.google.com/privacy):

- Firebase Authentication
- Cloud Firestore
- Firebase Analytics
- Firebase Crashlytics

No data is shared with any other third parties.

## Open Source

This app is open source under the MIT License. You can review the complete
source code at
[github.com/joeyparrish/first-bank-of-pig](https://github.com/joeyparrish/first-bank-of-pig)
to verify exactly what data is collected and how it is handled.

## Changes to This Policy

If this policy changes, the updated version will be posted to the repository
linked above. The "last updated" date at the top will be revised accordingly.

## Contact

If you have questions about this privacy policy or want to request deletion
of your data, please open an issue on the
[GitHub repository](https://github.com/joeyparrish/first-bank-of-pig/issues)
or contact the developer at the email address listed on the Play Store
listing.
