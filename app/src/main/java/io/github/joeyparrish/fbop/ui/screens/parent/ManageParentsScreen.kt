// Copyright (c) 2026 Joey Parrish
// SPDX-License-Identifier: MIT

package io.github.joeyparrish.fbop.ui.screens.parent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.joeyparrish.fbop.data.model.Family
import io.github.joeyparrish.fbop.data.model.Parent
import io.github.joeyparrish.fbop.data.repository.ConfigRepository
import io.github.joeyparrish.fbop.data.repository.FirebaseRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageParentsScreen(
    firebaseRepository: FirebaseRepository,
    configRepository: ConfigRepository,
    onBack: () -> Unit
) {
    val config = configRepository.getConfig()
    val familyId = config.familyId ?: return
    val scope = rememberCoroutineScope()

    var family by remember { mutableStateOf<Family?>(null) }
    var parents by remember { mutableStateOf<List<Parent>>(emptyList()) }
    var parentToRemove by remember { mutableStateOf<Parent?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val currentUserUid = firebaseRepository.currentUser?.uid
    val isOwner = family?.ownerUid == currentUserUid

    // Observe family and parents
    LaunchedEffect(familyId) {
        firebaseRepository.observeFamily(familyId).collect { f ->
            family = f
        }
    }

    LaunchedEffect(familyId) {
        firebaseRepository.observeParents(familyId).collect { p ->
            parents = p
        }
    }

    fun removeParent(parent: Parent) {
        scope.launch {
            isLoading = true
            errorMessage = null

            firebaseRepository.removeParent(familyId, parent.uid)
                .onSuccess {
                    parentToRemove = null
                }
                .onFailure { e ->
                    errorMessage = "Failed to remove: ${e.message}"
                }

            isLoading = false
        }
    }

    parentToRemove?.let { parent ->
        AlertDialog(
            onDismissRequest = { parentToRemove = null },
            title = { Text("Remove Parent?") },
            text = {
                Text("Remove ${parent.email} from this family? They will no longer be able to manage children or transactions.")
            },
            confirmButton = {
                TextButton(
                    onClick = { removeParent(parent) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { parentToRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Parents") },
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
        ) {
            if (parents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No other parents yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(parents) { parent ->
                        val isThisOwner = parent.uid == family?.ownerUid
                        val isCurrentUser = parent.uid == currentUserUid

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isThisOwner) Icons.Default.Star else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (isThisOwner)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = parent.email,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isThisOwner) {
                                        Text(
                                            text = "Owner",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (isCurrentUser) {
                                        Text(
                                            text = "(You)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Only owner can remove parents, and can't remove themselves
                                if (isOwner && !isThisOwner && !isCurrentUser) {
                                    IconButton(
                                        onClick = { parentToRemove = parent },
                                        enabled = !isLoading
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
