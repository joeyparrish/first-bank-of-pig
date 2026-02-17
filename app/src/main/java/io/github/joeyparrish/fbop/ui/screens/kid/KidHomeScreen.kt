// Copyright (c) 2026 Joey Parrish
// SPDX-License-Identifier: MIT

package io.github.joeyparrish.fbop.ui.screens.kid

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.joeyparrish.fbop.R
import io.github.joeyparrish.fbop.data.model.*
import io.github.joeyparrish.fbop.data.repository.ConfigRepository
import io.github.joeyparrish.fbop.data.repository.FirebaseRepository
import io.github.joeyparrish.fbop.ui.theme.AppTheme
import io.github.joeyparrish.fbop.ui.theme.MoneyNegative
import io.github.joeyparrish.fbop.ui.theme.MoneyPositive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidHomeScreen(
    firebaseRepository: FirebaseRepository,
    configRepository: ConfigRepository,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onAbout: () -> Unit,
    onAccessRevoked: () -> Unit
) {
    val config = configRepository.getConfig()
    val familyId = config.familyId ?: return
    val childId = config.childId ?: return
    val scope = rememberCoroutineScope()

    var child by remember { mutableStateOf<Child?>(null) }
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showPig by remember { mutableStateOf(false) }
    var currentThemeMode by remember { mutableStateOf(configRepository.getThemeMode()) }
    var accessRevoked by remember { mutableStateOf(false) }

    val balance = transactions.sumOf { it.amount }

    fun checkAccessAndLoad() {
        scope.launch {
            isRefreshing = true
            errorMessage = null

            // Ensure we're signed in anonymously
            if (!firebaseRepository.isSignedIn) {
                firebaseRepository.signInAnonymously()
                    .onFailure { e ->
                        errorMessage = "Failed to connect: ${e.message}"
                        isRefreshing = false
                        return@launch
                    }
            }

            // Check if this device still has access
            val hasAccess = firebaseRepository.checkDeviceAccess(familyId, childId)
                .getOrDefault(false)

            if (!hasAccess) {
                accessRevoked = true
                isLoading = false
                isRefreshing = false
                return@launch
            }

            // Update last accessed timestamp
            firebaseRepository.updateLastAccessed(familyId, childId)

            // Load child info
            firebaseRepository.getChild(familyId, childId)
                .onSuccess { child = it }
                .onFailure { e ->
                    errorMessage = "Failed to load: ${e.message}"
                }

            // Load transactions
            firebaseRepository.getTransactions(familyId, childId)
                .onSuccess { transactions = it }
                .onFailure { e ->
                    errorMessage = "Failed to load transactions: ${e.message}"
                }

            isLoading = false
            isRefreshing = false
        }
    }

    // Initial load and access check
    LaunchedEffect(Unit) {
        checkAccessAndLoad()
    }

    // Also observe for real-time updates
    LaunchedEffect(familyId, childId) {
        try {
            if (firebaseRepository.isSignedIn) {
                firebaseRepository.observeTransactions(familyId, childId).collect {
                    transactions = it
                }
            }
        } catch (e: Exception) {
            // Data no longer exists or access denied
            accessRevoked = true
        }
    }

    // Show access revoked dialog
    if (accessRevoked) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Access Revoked") },
            text = { Text("Your access to this account has been removed. Please contact your parent to set up access again.") },
            confirmButton = {
                Button(onClick = {
                    configRepository.clear()
                    onAccessRevoked()
                }) {
                    Text("OK")
                }
            }
        )
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

    if (showPig) {
        BackHandler { showPig = false }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Image(
                painter = painterResource(id = R.drawable.piggy_bank),
                contentDescription = "Piggy bank",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(child?.name ?: "My Piggy Bank")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Theme") },
                            onClick = {
                                showMenu = false
                                showThemeDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Palette, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("About") },
                            onClick = {
                                showMenu = false
                                onAbout()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Info, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Have a pig") },
                            onClick = {
                                showMenu = false
                                showPig = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Favorite, contentDescription = null)
                            }
                        )
                    }
                }
            )
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
                onRefresh = { checkAccessAndLoad() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Balance header - large and prominent for kids
                    item {
                        KidBalanceHeader(
                            childName = child?.name ?: "",
                            balance = balance
                        )
                    }

                // Error message if any
                if (errorMessage != null) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = errorMessage!!,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Transactions header
                item {
                    Text(
                        text = "Transaction History",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                if (transactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No transactions yet.\nAsk your parent to add some!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(transactions) { transaction ->
                        KidTransactionRow(transaction = transaction)
                    }
                }

                // Bottom padding
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
            }
        }
        }
    }
}

@Composable
private fun KidBalanceHeader(
    childName: String,
    balance: Long
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Your Balance",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatCurrency(balance),
                style = MaterialTheme.typography.displayLarge,
                color = if (balance >= 0) MoneyPositive else MoneyNegative
            )
        }
    }
}

@Composable
private fun KidTransactionRow(transaction: Transaction) {
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    Surface(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (transaction.isDeposit)
                    MoneyPositive.copy(alpha = 0.15f)
                else
                    MoneyNegative.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (transaction.isDeposit)
                            Icons.Default.Add
                        else
                            Icons.Default.Remove,
                        contentDescription = null,
                        tint = if (transaction.isDeposit) MoneyPositive else MoneyNegative,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description.ifBlank {
                        if (transaction.isDeposit) "Deposit" else "Spent"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dateFormat.format(transaction.date.toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = (if (transaction.isDeposit) "+" else "") + formatCurrency(transaction.amount),
                style = MaterialTheme.typography.titleMedium,
                color = if (transaction.isDeposit) MoneyPositive else MoneyNegative
            )
        }
    }

    HorizontalDivider(modifier = Modifier.padding(start = 80.dp))
}

@Composable
private fun ThemeModeDialog(
    currentMode: ThemeMode,
    onDismiss: () -> Unit,
    onModeSelected: (ThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Theme") },
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
                                ThemeMode.SYSTEM -> "System default"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
