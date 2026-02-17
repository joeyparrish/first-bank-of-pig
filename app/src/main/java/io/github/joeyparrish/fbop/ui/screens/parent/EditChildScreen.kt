// Copyright (c) 2026 Joey Parrish
// SPDX-License-Identifier: MIT

package io.github.joeyparrish.fbop.ui.screens.parent

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.joeyparrish.fbop.data.model.Child
import io.github.joeyparrish.fbop.data.repository.ConfigRepository
import io.github.joeyparrish.fbop.data.repository.FirebaseRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditChildScreen(
    childId: String,
    firebaseRepository: FirebaseRepository,
    configRepository: ConfigRepository,
    onSuccess: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    val config = configRepository.getConfig()
    val familyId = config.familyId ?: return
    val scope = rememberCoroutineScope()

    var child by remember { mutableStateOf<Child?>(null) }
    var name by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Load child
    LaunchedEffect(familyId, childId) {
        firebaseRepository.getChild(familyId, childId)
            .onSuccess {
                child = it
                name = it.name
            }
    }

    fun save() {
        if (name.isBlank()) {
            errorMessage = "Please enter a name"
            return
        }

        scope.launch {
            isLoading = true
            errorMessage = null

            firebaseRepository.updateChild(familyId, childId, name.trim())
                .onSuccess { onSuccess() }
                .onFailure { e ->
                    errorMessage = "Failed to update: ${e.message}"
                    isLoading = false
                }
        }
    }

    fun delete() {
        scope.launch {
            isLoading = true
            firebaseRepository.deleteChild(familyId, childId)
                .onSuccess { onDelete() }
                .onFailure { e ->
                    errorMessage = "Failed to delete: ${e.message}"
                    isLoading = false
                }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Child?") },
            text = {
                Text("This will permanently delete ${child?.name ?: "this child"} and all their transactions. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        delete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Child") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onPrimary
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
        if (child == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Child's Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { save() },
                    enabled = !isLoading && name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save Changes")
                    }
                }

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
}
