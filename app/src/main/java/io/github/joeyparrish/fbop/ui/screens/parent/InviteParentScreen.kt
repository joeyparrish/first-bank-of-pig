// Copyright (c) 2026 Joey Parrish
// SPDX-License-Identifier: MIT

package io.github.joeyparrish.fbop.ui.screens.parent

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import io.github.joeyparrish.fbop.data.model.Invite
import io.github.joeyparrish.fbop.data.repository.ConfigRepository
import io.github.joeyparrish.fbop.data.repository.FirebaseRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteParentScreen(
    firebaseRepository: FirebaseRepository,
    configRepository: ConfigRepository,
    onBack: () -> Unit
) {
    val config = configRepository.getConfig()
    val familyId = config.familyId ?: return
    val scope = rememberCoroutineScope()

    var invite by remember { mutableStateOf<Invite?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    fun generateInvite() {
        scope.launch {
            isLoading = true
            errorMessage = null

            firebaseRepository.createInvite(familyId)
                .onSuccess { inv ->
                    invite = inv
                    // Generate QR code
                    try {
                        val writer = QRCodeWriter()
                        val bitMatrix = writer.encode(inv.code, BarcodeFormat.QR_CODE, 512, 512)
                        val width = bitMatrix.width
                        val height = bitMatrix.height
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                        for (x in 0 until width) {
                            for (y in 0 until height) {
                                bitmap.setPixel(
                                    x, y,
                                    if (bitMatrix[x, y]) android.graphics.Color.BLACK
                                    else android.graphics.Color.WHITE
                                )
                            }
                        }
                        qrBitmap = bitmap
                    } catch (e: Exception) {
                        // QR generation failed, but we still have the code
                    }
                    isLoading = false
                }
                .onFailure { e ->
                    errorMessage = "Failed to create invite: ${e.message}"
                    isLoading = false
                }
        }
    }

    // Generate invite on first load
    LaunchedEffect(Unit) {
        generateInvite()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invite Parent") },
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
            when {
                isLoading -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Generating invite code...")
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { generateInvite() }) {
                        Text("Try Again")
                    }
                }
                invite != null -> {
                    Text(
                        text = "Share this code with the other parent",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // QR Code
                    qrBitmap?.let { bitmap ->
                        Card(
                            modifier = Modifier.size(200.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "or enter this code manually:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Code display
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = invite!!.code,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                            letterSpacing = androidx.compose.ui.unit.TextUnit(4f, androidx.compose.ui.unit.TextUnitType.Sp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "This code expires in 24 hours and can only be used once.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(onClick = { generateInvite() }) {
                        Text("Generate New Code")
                    }
                }
            }
        }
    }
}
