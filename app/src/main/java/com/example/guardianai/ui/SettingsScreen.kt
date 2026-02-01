package com.example.guardianai.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Star

import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import com.example.guardianai.R
import com.example.guardianai.data.GuardianRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController? = null, repository: GuardianRepository? = null) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        com.example.guardianai.data.SettingsManager.init(context)
    }

    val isStrict by com.example.guardianai.data.SettingsManager.isStrictMode
    val currentTheme by com.example.guardianai.data.SettingsManager.themeMode
    
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.screen_settings_title)) })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SettingsItem(
                title = stringResource(R.string.settings_whitelist_title),
                subtitle = stringResource(R.string.settings_whitelist_subtitle),
                icon = Icons.Default.CheckCircle,
                onClick = { navController?.navigate("whitelist") }
            )
            HorizontalDivider()


            
            // Проверка соединения с сервером
            val scope = rememberCoroutineScope()
            var isCheckingServer by remember { mutableStateOf(false) }
            
            SettingsItem(
                title = stringResource(R.string.settings_check_server_title),
                subtitle = stringResource(R.string.settings_check_server_subtitle),
                icon = Icons.Default.Info,
                onClick = {
                    if (isCheckingServer) return@SettingsItem
                    isCheckingServer = true
                    Toast.makeText(context, context.getString(R.string.toast_checking_connection), Toast.LENGTH_SHORT).show()
                    
                    scope.launch(Dispatchers.IO) {
                        try {
                            // Простой пинг через репозиторий
                            val isConnected = repository?.checkServerConnection() ?: false
                            
                            withContext(Dispatchers.Main) {
                                if (isConnected) {
                                    Toast.makeText(context, context.getString(R.string.toast_server_available), Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "❌ Сервер недоступен", Toast.LENGTH_LONG).show()
                                }
                                isCheckingServer = false
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "❌ Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                                isCheckingServer = false
                            }
                        }
                    }
                }
            )
            HorizontalDivider()


            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_strict_mode_title)) },
                supportingContent = { Text(stringResource(R.string.settings_strict_mode_subtitle)) },
                leadingContent = { Icon(Icons.Default.Security, null) },
                trailingContent = {
                    Switch(
                        checked = isStrict,
                        onCheckedChange = { 
                            com.example.guardianai.data.SettingsManager.setStrictMode(context, it) 
                        }
                    )
                }
            )
            HorizontalDivider()
            
            SettingsItem(
                title = stringResource(R.string.settings_theme_title),
                subtitle = when(currentTheme) {
                    1 -> stringResource(R.string.theme_light)
                    2 -> stringResource(R.string.theme_dark)
                    else -> stringResource(R.string.theme_system)
                },
                icon = Icons.Default.Star,
                onClick = { showThemeDialog = true }
            )
            HorizontalDivider()

            SettingsItem(
                title = stringResource(R.string.settings_about_title),
                subtitle = stringResource(R.string.settings_about_subtitle),
                icon = Icons.Default.Info,
                onClick = { }
            )
        }
    }
    
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.dialog_theme_title)) },
            text = {
                Column {
                    ThemeOption(text = stringResource(R.string.theme_system), selected = currentTheme == 0) {
                        com.example.guardianai.data.SettingsManager.setThemeMode(context, 0)
                        showThemeDialog = false
                    }
                    ThemeOption(text = stringResource(R.string.theme_light), selected = currentTheme == 1) {
                        com.example.guardianai.data.SettingsManager.setThemeMode(context, 1)
                        showThemeDialog = false
                    }
                    ThemeOption(text = stringResource(R.string.theme_dark), selected = currentTheme == 2) {
                        com.example.guardianai.data.SettingsManager.setThemeMode(context, 2)
                        showThemeDialog = false
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text(stringResource(R.string.btn_cancel)) } }
        )
    }
}

@Composable
fun ThemeOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable { onClick() }
    )
}
