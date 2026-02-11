# First Bank of Pig - Complete Setup Guide

This guide covers everything needed to build and deploy the app from scratch.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Install Development Tools](#install-development-tools)
3. [Firebase Setup](#firebase-setup)
4. [Configure the Android App](#configure-the-android-app)
5. [Build from Command Line](#build-from-command-line)
6. [Build from Android Studio](#build-from-android-studio)
7. [Side-loading onto Devices](#side-loading-onto-devices)
8. [Appendix: Concepts Explained](#appendix-concepts-explained)

---

## Prerequisites

- Ubuntu Linux (or similar)
- Google account (for Firebase Console)
- Google Cloud Platform account with billing enabled (free tier is sufficient)

---

## Install Development Tools

### Java Development Kit (JDK 17+)

```bash
sudo apt update
sudo apt install openjdk-17-jdk

# Verify installation
java -version
```

### Android SDK (Command Line Tools)

If you don't want to install the full Android Studio, you can install just the command-line tools:

```bash
# Create directory for Android SDK
mkdir -p ~/Android/Sdk
cd ~/Android/Sdk

# Download command-line tools (check for latest version at developer.android.com)
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
rm commandlinetools-linux-11076708_latest.zip

# The tools expect a specific directory structure
mkdir -p cmdline-tools/latest
mv cmdline-tools/* cmdline-tools/latest/ 2>/dev/null || true

# Add to PATH (add these to ~/.bashrc or ~/.zshrc)
export ANDROID_HOME=~/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools

# Reload shell config
source ~/.bashrc  # or ~/.zshrc

# Accept licenses and install required SDK components
sdkmanager --licenses
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

### Gradle

The project uses the Gradle wrapper (`gradlew`), which downloads the correct Gradle version automatically. However, you need Gradle installed initially to generate the wrapper:

```bash
# Install Gradle via apt
sudo apt install gradle

# Or via SDKMAN (recommended for version management)
curl -s "https://get.sdkman.io" | bash
source ~/.sdkman/bin/sdkman-init.sh
sdk install gradle

# Verify installation
gradle --version
```

### Generate Gradle Wrapper

From the project root directory:

```bash
cd /path/to/piggy-bank
gradle wrapper --gradle-version 8.9

# This creates:
#   gradlew          - Unix shell script
#   gradlew.bat      - Windows batch script
#   gradle/wrapper/  - Wrapper JAR and properties
```

After generating the wrapper, you no longer need system Gradle - the wrapper is self-contained.

### Node.js and npm (for Firebase CLI)

```bash
# Via apt
sudo apt install nodejs npm

# Or via nvm (recommended)
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
source ~/.bashrc
nvm install --lts

# Verify
node --version
npm --version
```

### Firebase CLI

```bash
npm install -g firebase-tools

# Verify
firebase --version

# Login to Firebase (opens browser)
firebase login
```

### Google Cloud SDK (gcloud)

If not already installed:

```bash
# Download and install
curl https://sdk.cloud.google.com | bash

# Restart shell, then initialize
gcloud init
```

---

## Firebase Setup

Firebase is the backend for this app. It provides:
- **Authentication**: Google Sign-In for parents, anonymous auth for kids
- **Firestore**: Real-time database for storing families, children, transactions
- **Security Rules**: Server-side validation of who can read/write what

### Understanding the Firebase Files

| File | Purpose |
|------|---------|
| `scripts/firebase-setup.sh` | Automated setup script (read and run step-by-step) |
| `firebase/firestore.rules` | Security rules defining access permissions |
| `firebase/firestore.indexes.json` | Query optimization indexes |
| `app/google-services.json` | Generated config file connecting your app to Firebase |

### Step 1: Configure the Setup Script

Edit `scripts/firebase-setup.sh` and set these values:

```bash
# Must be globally unique across all of Google Cloud
PROJECT_ID="first-bank-of-pig-yourname"

# Human-readable name
PROJECT_NAME="First Bank of Pig"

# Optional: your GCP billing account ID (format: XXXXXX-XXXXXX-XXXXXX)
# Find at: https://console.cloud.google.com/billing
# Leave empty for free Spark tier (sufficient for this app)
BILLING_ACCOUNT=""

# Must match build.gradle.kts
ANDROID_PACKAGE="io.github.joeyparrish.fbop"
```

### Step 2: Run the Setup Script

```bash
chmod +x scripts/firebase-setup.sh
./scripts/firebase-setup.sh
```

The script will:
1. Install Firebase CLI if needed
2. Create a GCP project
3. Add Firebase to the project
4. Create Firestore database
5. Prompt you to enable Google Sign-In in the Firebase Console
6. Register your Android app
7. Download `google-services.json` to `app/`
8. Deploy security rules

**Note**: The script pauses at certain points for manual steps in the Firebase Console.

### Step 3: Enable Authentication Providers

In the Firebase Console (https://console.firebase.google.com):

1. Select your project
2. Go to **Authentication** → **Sign-in method**
3. Enable **Google** provider:
   - Toggle ON
   - Set support email (your email)
   - Save
4. Enable **Anonymous** provider:
   - Toggle ON
   - Save

### Step 4: Add SHA-1 Fingerprint

Google Sign-In requires registering your app's signing key fingerprint.

**Get your debug SHA-1:**

```bash
keytool -list -v \
  -keystore ~/.android/debug.keystore \
  -alias androiddebugkey \
  -storepass android
```

Look for the line starting with `SHA1:` - it looks like:
```
SHA1: A1:B2:C3:D4:E5:F6:...  (40 hex characters with colons)
```

**Add it to Firebase:**

1. Firebase Console → Project Settings (gear icon)
2. Scroll to "Your apps" → find the Android app
3. Click "Add fingerprint"
4. Paste the SHA-1 value
5. Save

**Note**: Each developer needs to add their own debug SHA-1. For release builds, you'll add your release keystore's SHA-1 later.

---

## Configure the Android App

### Verify google-services.json

After running the Firebase setup, confirm the file exists:

```bash
ls -la app/google-services.json
```

If missing, download manually:
1. Firebase Console → Project Settings
2. Your apps → Android app
3. Download `google-services.json`
4. Place in `app/` directory

### Environment Variables

Ensure these are set (add to `~/.bashrc` or `~/.zshrc`):

```bash
export ANDROID_HOME=~/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

---

## Build from Command Line

### First-time Setup

```bash
cd /path/to/piggy-bank

# Generate Gradle wrapper if not present
gradle wrapper --gradle-version 8.9

# Verify wrapper works
./gradlew --version
```

### Debug Build

```bash
# Build debug APK
./gradlew assembleDebug

# Output location:
# app/build/outputs/apk/debug/app-debug.apk
```

### Release Build

For release builds, you need a signing keystore:

```bash
# Generate a release keystore (one-time)
keytool -genkey -v \
  -keystore release-keystore.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias release

# Keep this file safe! Back it up securely.
# If you lose it, you cannot update your app on Play Store.
```

Create `keystore.properties` in the project root (don't commit this!):

```properties
storeFile=../release-keystore.jks
storePassword=your_keystore_password
keyAlias=release
keyPassword=your_key_password
```

Add to `app/build.gradle.kts` (before `android {`):

```kotlin
import java.util.Properties
import java.io.FileInputStream

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}
```

And inside `android { buildTypes { release { ... } } }`:

```kotlin
signingConfigs {
    create("release") {
        if (keystorePropertiesFile.exists()) {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        // ... existing config
    }
}
```

Then build:

```bash
./gradlew assembleRelease

# Output location:
# app/build/outputs/apk/release/app-release.apk
```

**Don't forget** to add the release keystore's SHA-1 to Firebase for Google Sign-In to work in release builds!

### Other Useful Commands

```bash
# Clean build artifacts
./gradlew clean

# Build and show all warnings
./gradlew assembleDebug --warning-mode all

# List all available tasks
./gradlew tasks

# Run lint checks
./gradlew lint

# Build both debug and release
./gradlew assemble
```

---

## Build from Android Studio

1. Open Android Studio
2. File → Open → select the `piggy-bank` directory
3. Wait for Gradle sync to complete
4. If prompted about Gradle wrapper, click "OK" to use it
5. Build → Build Bundle(s) / APK(s) → Build APK(s)

Or press the green "Run" button to build and install on a connected device/emulator.

---

## Side-loading onto Devices

### Enable Developer Options on Android Device

1. Settings → About Phone
2. Tap "Build number" 7 times
3. Go back to Settings → Developer options
4. Enable "USB debugging"

### Install via USB

```bash
# Connect device via USB, then:
adb install app/build/outputs/apk/debug/app-debug.apk

# Or for release:
adb install app/build/outputs/apk/release/app-release.apk
```

### Install via File Transfer

1. Copy the APK to the device (USB, email, cloud storage, etc.)
2. On the device, open a file manager and tap the APK
3. If prompted, enable "Install from unknown sources" for your file manager

---

## Appendix: Concepts Explained

### What is google-services.json?

A configuration file Firebase generates for your specific project. Contains:
- Firebase project ID
- API keys (for Firebase services)
- OAuth client IDs (for Google Sign-In)
- App-specific identifiers

It's in `.gitignore` because:
- It ties to your Firebase project/billing
- Each developer or fork should use their own Firebase project

### What is SHA-1 Fingerprint?

A cryptographic hash of your app's signing key. Google uses it to verify that requests claiming to be from your app are legitimate.

- **Debug keystore**: Auto-generated at `~/.android/debug.keystore`, different per developer machine
- **Release keystore**: You create this, guards your production identity

Without the correct SHA-1 registered, Google Sign-In fails with "Developer Error".

### What Does Obfuscation Do?

ProGuard/R8 in release builds:

| Feature | Effect | Benefit |
|---------|--------|---------|
| Minification | Removes unused code | Smaller APK |
| Obfuscation | Renames `MyClass` to `a` | Slightly smaller APK, minor reverse-engineering hurdle |
| Optimization | Inlines methods, removes dead branches | Faster code |

For open-source projects, obfuscation provides no security benefit (source is public), but minification still reduces APK size.

### How Does Firebase Code Work?

Firebase provides SDKs, not magic. You write real code:

```kotlin
// Create document
db.collection("families").document(id).set(data)

// Read document
db.collection("families").document(id).get()

// Listen for changes (real-time)
db.collection("families").document(id)
    .addSnapshotListener { snapshot, error -> ... }
```

The database is schemaless - structure exists in your code and security rules, not enforced by Firebase itself.

### Firebase Security Rules

Server-side rules that Firebase enforces. Even if someone decompiles your app, they can't bypass these:

```javascript
match /families/{familyId} {
  // Only parents can write
  allow write: if request.auth.uid in resource.data.parents;
  // Anyone authenticated can read (kids use anonymous auth)
  allow read: if request.auth != null;
}
```

---

## Troubleshooting

### "SDK location not found"

Create `local.properties` in project root:
```properties
sdk.dir=/home/youruser/Android/Sdk
```

### "Google Sign-In failed: Developer Error"

- Verify SHA-1 is added to Firebase Console
- Verify `google-services.json` is in `app/` directory
- Verify package name matches in Firebase Console and `build.gradle.kts`

### Gradle wrapper fails

Ensure you have Gradle installed, then regenerate:
```bash
rm -rf gradle/wrapper gradlew gradlew.bat
gradle wrapper --gradle-version 8.9
```

### Build fails with memory error

Increase Gradle memory in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```
