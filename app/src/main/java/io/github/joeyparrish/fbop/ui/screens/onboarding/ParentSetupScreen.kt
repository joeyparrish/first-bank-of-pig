// Copyright (c) 2026 Joey Parrish
// SPDX-License-Identifier: MIT

package io.github.joeyparrish.fbop.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import io.github.joeyparrish.fbop.R
import io.github.joeyparrish.fbop.data.repository.ConfigRepository
import io.github.joeyparrish.fbop.data.repository.FirebaseRepository
import io.github.joeyparrish.fbop.ui.components.ModeCard
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentSetupScreen(
    firebaseRepository: FirebaseRepository,
    configRepository: ConfigRepository,
    onCreateFamily: () -> Unit,
    onJoinFamily: () -> Unit,
    onReconnected: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var isSignedIn by remember { mutableStateOf(firebaseRepository.isSignedIn) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // If already signed in, check for existing family immediately
    LaunchedEffect(Unit) {
        if (firebaseRepository.isSignedIn) {
            isLoading = true
            if (tryReconnect(firebaseRepository, configRepository, onReconnected)) return@LaunchedEffect
            // Already signed in but no existing family — show create/join
            isSignedIn = true
            isLoading = false
        }
    }

    fun signInWithGoogle() {
        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(R.string.default_web_client_id))
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(
                    googleIdTokenCredential.idToken,
                    null
                )

                FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()

                // Check for existing family after sign-in
                if (tryReconnect(firebaseRepository, configRepository, onReconnected)) return@launch

                // No existing family — show create/join options
                isSignedIn = true
            } catch (e: GetCredentialException) {
                errorMessage = "Sign in failed: ${e.message}"
            } catch (e: Exception) {
                errorMessage = "Sign in failed: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parent Setup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (!isSignedIn) {
                // Step 1: Sign in with Google
                Text(
                    text = "Sign in to get started",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { signInWithGoogle() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign in with Google")
                }
            } else {
                // Step 2: Create or join
                Text(
                    text = "Are you starting a new family account or joining an existing one?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(32.dp))

                ModeCard(
                    icon = Icons.Default.Add,
                    title = "Create New Family",
                    description = "Start fresh with a new family account",
                    onClick = onCreateFamily,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                ModeCard(
                    icon = Icons.Default.GroupAdd,
                    title = "Join Existing Family",
                    description = "Enter an invite code from another parent",
                    onClick = onJoinFamily,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Error message
            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private suspend fun tryReconnect(
    firebaseRepository: FirebaseRepository,
    configRepository: ConfigRepository,
    onReconnected: () -> Unit
): Boolean {
    val result = firebaseRepository.findExistingFamily()

    result
        .onSuccess { familyId ->
            if (familyId != null) {
                configRepository.setParentMode(familyId)
                onReconnected()
                return true
            }
        }

    return false
}
