package io.github.joeyparrish.fbop.ui.screens.kid

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
import io.github.joeyparrish.fbop.ui.theme.MoneyNegative
import io.github.joeyparrish.fbop.ui.theme.MoneyPositive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidHomeScreen(
    firebaseRepository: FirebaseRepository,
    configRepository: ConfigRepository
) {
    val config = configRepository.getConfig()
    val familyId = config.familyId ?: return
    val childId = config.childId ?: return
    val scope = rememberCoroutineScope()

    var child by remember { mutableStateOf<Child?>(null) }
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val balance = transactions.sumOf { it.amount }

    fun refresh() {
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

            isRefreshing = false
        }
    }

    // Initial load
    LaunchedEffect(Unit) {
        refresh()
    }

    // Also observe for real-time updates
    LaunchedEffect(familyId, childId) {
        if (firebaseRepository.isSignedIn) {
            firebaseRepository.observeTransactions(familyId, childId).collect {
                transactions = it
            }
        }
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
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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

                if (transactions.isEmpty() && !isRefreshing) {
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
            // Placeholder for pig graphic
            // TODO: Replace with fun pig image
            Surface(
                modifier = Modifier.size(80.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "PIG",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
