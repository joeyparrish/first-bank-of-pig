package io.github.joeyparrish.fbop.ui.screens

import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import io.github.joeyparrish.fbop.BuildConfig
import io.github.joeyparrish.fbop.data.model.ThemeMode
import io.github.joeyparrish.fbop.data.repository.ConfigRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    configRepository: ConfigRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "First Bank of Pig",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "MIT License",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Copyright (c) 2025 Joey Parrish\n\n" +
                            "Permission is hereby granted, free of charge, to any person " +
                            "obtaining a copy of this software and associated documentation " +
                            "files (the \"Software\"), to deal in the Software without " +
                            "restriction, including without limitation the rights to use, " +
                            "copy, modify, merge, publish, distribute, sublicense, and/or " +
                            "sell copies of the Software, and to permit persons to whom the " +
                            "Software is furnished to do so, subject to the following " +
                            "conditions:\n\n" +
                            "The above copyright notice and this permission notice shall be " +
                            "included in all copies or substantial portions of the Software.\n\n" +
                            "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY " +
                            "KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE " +
                            "WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE " +
                            "AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT " +
                            "HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, " +
                            "WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING " +
                            "FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR " +
                            "OTHER DEALINGS IN THE SOFTWARE.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = {
                    // Sync AppCompat night mode with the app's theme preference
                    // so the OSS activity matches the current theme
                    AppCompatDelegate.setDefaultNightMode(
                        when (configRepository.getThemeMode()) {
                            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        }
                    )
                    OssLicensesMenuActivity.setActivityTitle("Open Source Licenses")
                    context.startActivity(
                        Intent(context, OssLicensesMenuActivity::class.java)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Source Licenses")
            }
        }
    }
}
