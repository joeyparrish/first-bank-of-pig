// Copyright (c) 2026 Joey Parrish
// SPDX-License-Identifier: MIT

package io.github.joeyparrish.fbop.ui.screens.parent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.joeyparrish.fbop.data.model.*
import io.github.joeyparrish.fbop.data.repository.ConfigRepository
import io.github.joeyparrish.fbop.data.repository.FirebaseRepository
import io.github.joeyparrish.fbop.ui.theme.MoneyPositive
import io.github.joeyparrish.fbop.ui.theme.MoneyNegative
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildDetailScreen(
    childId: String,
    firebaseRepository: FirebaseRepository,
    configRepository: ConfigRepository,
    onBack: () -> Unit,
    onEditChild: () -> Unit,
    onAddTransaction: () -> Unit,
    onEditTransaction: (String) -> Unit,
    onShowQrCode: () -> Unit,
    onManageDevices: () -> Unit
) {
    val config = configRepository.getConfig()
    val familyId = config.familyId ?: return
    val scope = rememberCoroutineScope()

    var child by remember { mutableStateOf<Child?>(null) }
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val balance = transactions.sumOf { it.amount }

    // Observe child
    LaunchedEffect(familyId, childId) {
        firebaseRepository.getChild(familyId, childId)
            .onSuccess { child = it }
    }

    // Observe transactions
    LaunchedEffect(familyId, childId) {
        firebaseRepository.observeTransactions(familyId, childId).collect {
            transactions = it
            isLoading = false
        }
    }

    fun refresh() {
        scope.launch {
            isRefreshing = true
            kotlinx.coroutines.delay(500)
            isRefreshing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(child?.name ?: "Loading...") },
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
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
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
                            text = { Text("Show QR Code") },
                            onClick = {
                                showMenu = false
                                onShowQrCode()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.QrCode, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Child") },
                            onClick = {
                                showMenu = false
                                onEditChild()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Manage Devices") },
                            onClick = {
                                showMenu = false
                                onManageDevices()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                    // Balance header
                    item {
                        BalanceHeader(balance = balance)
                    }

                    // Transactions section header
                    item {
                        Text(
                            text = "Transaction History",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
                                    text = "No transactions yet.\nTap + to add the first one!",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                    items(transactions) { transaction ->
                        TransactionRow(
                            transaction = transaction,
                            onClick = { onEditTransaction(transaction.id) }
                        )
                    }
                }

                // Bottom spacing for FAB
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
            }
        }
    }
}

@Composable
private fun BalanceHeader(balance: Long) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Current Balance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatCurrency(balance),
                style = MaterialTheme.typography.displayMedium,
                color = if (balance >= 0) MoneyPositive else MoneyNegative
            )
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (transaction.isDeposit)
                    MoneyPositive.copy(alpha = 0.1f)
                else
                    MoneyNegative.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (transaction.isDeposit)
                            Icons.Default.Add
                        else
                            Icons.Default.Remove,
                        contentDescription = null,
                        tint = if (transaction.isDeposit) MoneyPositive else MoneyNegative,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description.ifBlank { if (transaction.isDeposit) "Deposit" else "Withdrawal" },
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

    HorizontalDivider(modifier = Modifier.padding(start = 68.dp))
}
