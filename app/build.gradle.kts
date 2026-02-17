plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.oss.licenses.plugin)
}

fun gitVersionCode(): Int {
    val process = ProcessBuilder("git", "rev-list", "--count", "HEAD").start()
    return process.inputStream.bufferedReader().readText().trim().toIntOrNull() ?: 1
}

fun gitVersionName(): String {
    val process = ProcessBuilder("git", "describe", "--tags", "--abbrev=0").start()
    val tag = process.inputStream.bufferedReader().readText().trim().removePrefix("v")
    return tag.ifEmpty { "0.0.0" }
}

android {
    namespace = "io.github.joeyparrish.fbop"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.joeyparrish.fbop"
        minSdk = 26
        targetSdk = 35
        versionCode = gitVersionCode()
        versionName = gitVersionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            // Note: No applicationIdSuffix so Firebase google-services.json works
            // without registering a separate debug app
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// In debug builds, the OSS licenses plugin only generates placeholder text.
// Copy the real license data from the release task into the debug output.
afterEvaluate {
    tasks.register("copyReleaseLicensesToDebug") {
        dependsOn("releaseOssLicensesTask")
        mustRunAfter("debugOssLicensesTask")
        doLast {
            copy {
                from("build/generated/third_party_licenses/release/res/raw/")
                into("build/generated/third_party_licenses/debug/res/raw/")
            }
        }
    }
    tasks.named("mergeDebugResources") {
        dependsOn("copyReleaseLicensesToDebug")
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Biometric
    implementation(libs.androidx.biometric)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Credential Manager (Google Sign-In)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.id)

    // QR Code
    implementation(libs.zxing.core)
    implementation(libs.zxing.android)

    // OSS Licenses
    implementation(libs.oss.licenses)

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
}
