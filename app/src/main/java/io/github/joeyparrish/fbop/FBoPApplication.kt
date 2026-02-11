package io.github.joeyparrish.fbop

import android.app.Application
import com.google.firebase.FirebaseApp

class FBoPApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
