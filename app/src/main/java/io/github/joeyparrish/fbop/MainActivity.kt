// Copyright (c) 2026 Joey Parrish
// SPDX-License-Identifier: MIT

package io.github.joeyparrish.fbop

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import io.github.joeyparrish.fbop.data.model.AppMode
import io.github.joeyparrish.fbop.data.model.ThemeMode
import io.github.joeyparrish.fbop.data.repository.ConfigRepository
import io.github.joeyparrish.fbop.data.repository.FirebaseRepository
import io.github.joeyparrish.fbop.ui.AppNavigation
import io.github.joeyparrish.fbop.ui.theme.FirstBankOfPigTheme

class MainActivity : FragmentActivity() {
    private lateinit var configRepository: ConfigRepository
    private lateinit var firebaseRepository: FirebaseRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configRepository = ConfigRepository(this)
        firebaseRepository = FirebaseRepository()

        setContent {
            var themeMode by remember { mutableStateOf(configRepository.getThemeMode()) }

            FirstBankOfPigTheme(themeMode = themeMode) {
                MainScreen(
                    activity = this,
                    configRepository = configRepository,
                    firebaseRepository = firebaseRepository,
                    onThemeModeChanged = { themeMode = it }
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    activity: FragmentActivity,
    configRepository: ConfigRepository,
    firebaseRepository: FirebaseRepository,
    onThemeModeChanged: (ThemeMode) -> Unit
) {
    val config = configRepository.getConfig()
    val navController = rememberNavController()

    // Biometric state for parent mode
    var isAuthenticated by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var biometricAvailable by remember { mutableStateOf(true) }

    // Check if we need biometric auth (parent mode only)
    val needsAuth = config.mode == AppMode.PARENT && !isAuthenticated

    // Check biometric availability
    LaunchedEffect(Unit) {
        if (config.mode == AppMode.PARENT) {
            val biometricManager = BiometricManager.from(activity)
            when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    biometricAvailable = true
                }
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    // No biometric available, skip authentication
                    biometricAvailable = false
                    isAuthenticated = true
                }
                else -> {
                    biometricAvailable = false
                    isAuthenticated = true
                }
            }
        }
    }

    // Show biometric prompt
    fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                isAuthenticated = true
                authError = null
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    authError = errString.toString()
                }
            }

            override fun onAuthenticationFailed() {
                // This is called for each failed attempt, but we don't need to show error
                // The biometric prompt handles retry internally
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("First Bank of Pig")
            .setSubtitle("Authenticate to access parent mode")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    // Trigger biometric prompt when needed
    LaunchedEffect(needsAuth, biometricAvailable) {
        if (needsAuth && biometricAvailable) {
            showBiometricPrompt()
        }
    }

    when {
        // Not configured or kid mode - show app directly
        config.mode != AppMode.PARENT || isAuthenticated -> {
            AppNavigation(
                navController = navController,
                configRepository = configRepository,
                firebaseRepository = firebaseRepository,
                onThemeModeChanged = onThemeModeChanged
            )
        }
        // Parent mode but not authenticated - show lock screen
        else -> {
            BiometricLockScreen(
                onAuthenticate = { showBiometricPrompt() },
                error = authError
            )
        }
    }
}

@Composable
fun BiometricLockScreen(
    onAuthenticate: () -> Unit,
    error: String?
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "First Bank of Pig",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.size(100.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Parent mode is locked",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Authenticate to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            error?.let { err ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onAuthenticate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Unlock")
            }
        }
    }
}
