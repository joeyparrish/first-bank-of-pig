// Copyright (c) 2026 Joey Parrish
// SPDX-License-Identifier: MIT

package io.github.joeyparrish.fbop.ui.screens.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import io.github.joeyparrish.fbop.R
import io.github.joeyparrish.fbop.data.repository.ConfigRepository
import io.github.joeyparrish.fbop.data.repository.FirebaseRepository
import io.github.joeyparrish.fbop.ui.components.ModeCard
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinFamilyScreen(
    firebaseRepository: FirebaseRepository,
    configRepository: ConfigRepository,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showManualEntry by remember { mutableStateOf(false) }
    var inviteCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingFamilyId by remember { mutableStateOf<String?>(null) }
    var isSignedIn by remember { mutableStateOf(firebaseRepository.isSignedIn) }

    val credentialManager = remember { CredentialManager.create(context) }

    fun lookupCode() {
        if (inviteCode.isBlank()) {
            errorMessage = "Please enter an invite code"
            return
        }

        scope.launch {
            isLoading = true
            errorMessage = null

            // First, sign in anonymously to be able to look up the code
            if (!firebaseRepository.isSignedIn) {
                firebaseRepository.signInAnonymously()
                    .onFailure { e ->
                        errorMessage = "Failed to connect: ${e.message}"
                        isLoading = false
                        return@launch
                    }
            }

            firebaseRepository.lookupInviteCode(inviteCode.uppercase().trim())
                .onSuccess { familyId ->
                    pendingFamilyId = familyId
                    // Sign out anonymous user so they can sign in with Google
                    firebaseRepository.signOut()
                }
                .onFailure { e ->
                    errorMessage = e.message ?: "Invalid code"
                }

            isLoading = false
        }
    }

    fun processScannedCode(code: String) {
        inviteCode = code
        lookupCode()
    }

    // QR Scanner launcher
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { code ->
            processScannedCode(code)
        }
    }

    // Camera permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("Scan the invite QR code from the other parent")
                setBeepEnabled(false)
                setOrientationLocked(true)
            }
            scanLauncher.launch(options)
        } else {
            errorMessage = "Camera permission is required to scan QR codes"
        }
    }

    fun startScan() {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                val options = ScanOptions().apply {
                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    setPrompt("Scan the invite QR code from the other parent")
                    setBeepEnabled(false)
                    setOrientationLocked(true)
                }
                scanLauncher.launch(options)
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    fun signInAndJoin() {
        val familyId = pendingFamilyId ?: return

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
                isSignedIn = true

                // Now join the family
                firebaseRepository.joinFamily(familyId, inviteCode.uppercase().trim())
                    .onSuccess {
                        configRepository.setParentMode(familyId)
                        onSuccess()
                    }
                    .onFailure { e ->
                        errorMessage = "Failed to join family: ${e.message}"
                        isLoading = false
                    }
            } catch (e: GetCredentialException) {
                errorMessage = "Sign in failed: ${e.message}"
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Sign in failed: ${e.message}"
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Join Family") },
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
            if (pendingFamilyId == null && !showManualEntry) {
                // Step 1: Choose scan or manual
                Text(
                    text = "Get the invite code from the other parent",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                ModeCard(
                    icon = Icons.Default.CameraAlt,
                    title = "Scan QR Code",
                    description = "Use the camera to scan",
                    onClick = { startScan() },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                ModeCard(
                    icon = Icons.Default.Edit,
                    title = "Enter Code Manually",
                    description = "Type the code instead",
                    onClick = { showManualEntry = true },
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (pendingFamilyId == null) {
                // Step 1b: Manual code entry
                Text(
                    text = "Enter the invite code from the other parent",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = { inviteCode = it.uppercase().take(8) },
                    label = { Text("Invite Code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { showManualEntry = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }

                    Button(
                        onClick = { lookupCode() },
                        enabled = !isLoading && inviteCode.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Continue")
                        }
                    }
                }
            } else {
                // Step 2: Sign in with Google
                Text(
                    text = "Code verified! Sign in to join the family.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { signInAndJoin() },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Sign in with Google")
                    }
                }
            }

            // Error message
            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
