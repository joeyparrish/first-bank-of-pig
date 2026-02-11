package io.github.joeyparrish.fbop.ui.screens.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import io.github.joeyparrish.fbop.data.repository.ConfigRepository
import io.github.joeyparrish.fbop.data.repository.FirebaseRepository
import io.github.joeyparrish.fbop.ui.components.ModeCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidSetupScreen(
    firebaseRepository: FirebaseRepository,
    configRepository: ConfigRepository,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showManualEntry by remember { mutableStateOf(false) }
    var manualCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun processCode(code: String) {
        scope.launch {
            isLoading = true
            errorMessage = null

            // Sign in anonymously first
            firebaseRepository.signInAnonymously()
                .onFailure { e ->
                    errorMessage = "Failed to connect: ${e.message}"
                    isLoading = false
                    return@launch
                }

            // Look up the child
            val lookupResult = firebaseRepository.lookupChild(code)
            if (lookupResult.isFailure) {
                errorMessage = "Invalid code: ${lookupResult.exceptionOrNull()?.message}"
                isLoading = false
                return@launch
            }

            val lookup = lookupResult.getOrThrow()

            // Register this device for access
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
            firebaseRepository.registerDevice(
                familyId = lookup.familyId,
                childId = lookup.childId,
                deviceName = deviceName
            ).onFailure { e ->
                errorMessage = "Failed to register device: ${e.message}"
                isLoading = false
                return@launch
            }

            // Save config and proceed
            configRepository.setKidMode(
                familyId = lookup.familyId,
                childId = lookup.childId,
                lookupCode = lookup.lookupCode
            )
            onSuccess()
        }
    }

    // QR Scanner launcher
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { code ->
            processCode(code)
        }
    }

    // Camera permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("Scan the QR code from your parent's device")
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
                    setPrompt("Scan the QR code from your parent's device")
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kid Setup") },
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
            if (!showManualEntry) {
                Text(
                    text = "Ask your parent to show you the QR code for your account",
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
            } else {
                Text(
                    text = "Enter the code from your parent",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = manualCode,
                    onValueChange = { manualCode = it },
                    label = { Text("Code") },
                    singleLine = true,
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
                        onClick = { processCode(manualCode.trim()) },
                        enabled = !isLoading && manualCode.isNotBlank(),
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
