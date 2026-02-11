# ProGuard rules for First Bank of Pig

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep ZXing classes for QR code scanning
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }

# Keep data model classes (for Firestore serialization)
-keep class io.github.joeyparrish.fbop.data.model.** { *; }

# Standard Android rules
-keepattributes Signature
-keepattributes *Annotation*
