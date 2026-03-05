// Copyright (c) 2026 Joey Parrish
// SPDX-License-Identifier: MIT

package io.github.joeyparrish.fbop.ui.screens.parent

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.joeyparrish.fbop.R
import io.github.joeyparrish.fbop.data.model.*
import io.github.joeyparrish.fbop.data.repository.ConfigRepository
import io.github.joeyparrish.fbop.data.repository.FirebaseRepository
import io.github.joeyparrish.fbop.ui.theme.AppTheme
import io.github.joeyparrish.fbop.ui.theme.MoneyPositive
import io.github.joeyparrish.fbop.ui.theme.MoneyNegative
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentHomeScreen(
    firebaseRepository: FirebaseRepository,
    configRepository: ConfigRepository,
    onChildClick: (String) -> Unit,
    onAddChild: () -> Unit,
    onInviteParent: () -> Unit,
    onManageParents: () -> Unit,
    onAbout: () -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onFamilyNotFound: () -> Unit
) {
    val config = configRepository.getConfig()
    val familyId = config.familyId ?: return
    val scope = rememberCoroutineScope()

    var family by remember { mutableStateOf<Family?>(null) }
    var familyLoaded by remember { mutableStateOf(false) }
    var childrenWithBalances by remember { mutableStateOf<List<ChildWithBalance>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    var showMenu by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var isOwner by remember { mutableStateOf(false) }
    var currentThemeMode by remember { mutableStateOf(configRepository.getThemeMode()) }

    // Observe family data
    LaunchedEffect(familyId) {
        firebaseRepository.observeFamily(familyId).collect { f ->
            if (f == null && familyLoaded) {
                // Family was loaded before but now doesn't exist - clear config and restart
                configRepository.clear()
                onFamilyNotFound()
            } else if (f != null) {
                family = f
                familyLoaded = true
                isOwner = f.ownerUid == firebaseRepository.currentUser?.uid
            }
        }
    }

    // Observe children and their transactions
    LaunchedEffect(familyId) {
        firebaseRepository.observeChildren(familyId).collect { children ->
            // For each child, get their transactions and compute balance
            val withBalances = children.map { child ->
                val transactions = firebaseRepository.getTransactions(familyId, child.id)
                    .getOrDefault(emptyList())
                val balance = transactions.sumOf { it.amount }
                ChildWithBalance(child, balance, transactions)
            }
            childrenWithBalances = withBalances
            isLoading = false
        }
    }

    fun refresh() {
        scope.launch {
            isRefreshing = true
            // The observers will automatically update, but we can force a refresh
            kotlinx.coroutines.delay(500)
            isRefreshing = false
        }
    }

    if (showThemeDialog) {
        ThemeModeDialog(
            currentMode = currentThemeMode,
            onDismiss = { showThemeDialog = false },
            onModeSelected = { mode ->
                currentThemeMode = mode
                configRepository.setThemeMode(mode)
                onThemeModeChanged(mode)
                showThemeDialog = false
            }
        )
    }

    if (showDeleteConfirmDialog) {
        DeleteFamilyDialog(
            familyName = family?.name ?: "",
            isDeleting = isDeleting,
            onDismiss = { if (!isDeleting) showDeleteConfirmDialog = false },
            onConfirm = {
                scope.launch {
                    isDeleting = true
                    firebaseRepository.deleteFamily(familyId)
                    configRepository.clear()
                    onFamilyNotFound()
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(family?.name ?: stringResource(R.string.loading)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (isOwner) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.invite_parent)) },
                                onClick = {
                                    showMenu = false
                                    onInviteParent()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.manage_parents)) },
                                onClick = {
                                    showMenu = false
                                    onManageParents()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Group, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_family)) },
                                onClick = {
                                    showMenu = false
                                    showDeleteConfirmDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.theme)) },
                            onClick = {
                                showMenu = false
                                showThemeDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Palette, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.about)) },
                            onClick = {
                                showMenu = false
                                onAbout()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Info, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.support_this_project)) },
                            onClick = {
                                showMenu = false
                                uriHandler.openUri("https://github.com/sponsors/joeyparrish")
                            },
                            leadingIcon = {
                                Icon(Icons.Default.VolunteerActivism, contentDescription = null)
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddChild,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_child))
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Watermark background
            Image(
                painter = painterResource(id = R.drawable.piggy_bank),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(AppTheme.watermarkAlpha),
                contentScale = ContentScale.Fit
            )

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { refresh() },
                modifier = Modifier.fillMaxSize()
        ) {
            if (isLoading) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (childrenWithBalances.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Face,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_children),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.no_children_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(childrenWithBalances) { childWithBalance ->
                        ChildCard(
                            childWithBalance = childWithBalance,
                            onClick = { onChildClick(childWithBalance.child.id) }
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun ChildCard(
    childWithBalance: ChildWithBalance,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Instead of an avatar, show the first letter of the child's name
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = childWithBalance.child.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = childWithBalance.child.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.transaction_count,
                        childWithBalance.transactions.size,
                        childWithBalance.transactions.size
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = childWithBalance.formattedBalance,
                style = MaterialTheme.typography.titleLarge,
                color = if (childWithBalance.balanceCents >= 0) MoneyPositive else MoneyNegative
            )
        }
    }
}

@Composable
private fun ThemeModeDialog(
    currentMode: ThemeMode,
    onDismiss: () -> Unit,
    onModeSelected: (ThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme)) },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModeSelected(mode) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = mode == currentMode,
                            onClick = { onModeSelected(mode) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (mode) {
                                ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                                ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                ThemeMode.DARK -> stringResource(R.string.theme_dark)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun DeleteFamilyDialog(
    familyName: String,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_family)) },
        text = {
            if (isDeleting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(stringResource(R.string.deleting))
                }
            } else {
                Text(stringResource(R.string.delete_family_confirmation, familyName))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
