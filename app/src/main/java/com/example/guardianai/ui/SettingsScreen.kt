package com.example.guardianai.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
    var showAboutDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    var isCheckingServer by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.screen_settings_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // === PROTECTION SECTION ===
            SettingsSectionHeader(title = "Защита")
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column {
                    // Whitelist
                    SettingsCardItem(
                        title = stringResource(R.string.settings_whitelist_title),
                        subtitle = stringResource(R.string.settings_whitelist_subtitle),
                        icon = Icons.Default.CheckCircle,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = { navController?.navigate("whitelist") }
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    
                    // Strict Mode
                    ListItem(
                        headlineContent = { 
                            Text(
                                stringResource(R.string.settings_strict_mode_title),
                                fontWeight = FontWeight.Medium
                            ) 
                        },
                        supportingContent = { 
                            Text(
                                stringResource(R.string.settings_strict_mode_subtitle),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        },
                        leadingContent = { 
                            SettingsIconBox(
                                icon = Icons.Default.Security,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = isStrict,
                                onCheckedChange = { 
                                    com.example.guardianai.data.SettingsManager.setStrictMode(context, it) 
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // === APPEARANCE SECTION ===
            SettingsSectionHeader(title = "Внешний вид")
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                SettingsCardItem(
                    title = stringResource(R.string.settings_theme_title),
                    subtitle = when(currentTheme) {
                        1 -> stringResource(R.string.theme_light)
                        2 -> stringResource(R.string.theme_dark)
                        else -> stringResource(R.string.theme_system)
                    },
                    icon = Icons.Default.Palette,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    onClick = { showThemeDialog = true }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // === SERVER SECTION ===
            SettingsSectionHeader(title = "Сервер")
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                SettingsCardItem(
                    title = stringResource(R.string.settings_check_server_title),
                    subtitle = if (isCheckingServer) "Проверка..." else stringResource(R.string.settings_check_server_subtitle),
                    icon = Icons.Default.Cloud,
                    iconTint = MaterialTheme.colorScheme.primary,
                    onClick = {
                        if (isCheckingServer) return@SettingsCardItem
                        isCheckingServer = true
                        Toast.makeText(context, context.getString(R.string.toast_checking_connection), Toast.LENGTH_SHORT).show()
                        
                        scope.launch(Dispatchers.IO) {
                            try {
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
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // === ABOUT SECTION ===
            SettingsSectionHeader(title = "О приложении")
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                SettingsCardItem(
                    title = stringResource(R.string.settings_about_title),
                    subtitle = stringResource(R.string.settings_about_subtitle),
                    icon = Icons.Default.Info,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { showAboutDialog = true }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // === VERSION FOOTER ===
            Text(
                text = "GuardianAI v2.1",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // Theme Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.dialog_theme_title), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ThemeOptionItem(
                        text = stringResource(R.string.theme_system), 
                        icon = Icons.Default.BrightnessAuto,
                        selected = currentTheme == 0
                    ) {
                        com.example.guardianai.data.SettingsManager.setThemeMode(context, 0)
                        showThemeDialog = false
                    }
                    ThemeOptionItem(
                        text = stringResource(R.string.theme_light), 
                        icon = Icons.Default.LightMode,
                        selected = currentTheme == 1
                    ) {
                        com.example.guardianai.data.SettingsManager.setThemeMode(context, 1)
                        showThemeDialog = false
                    }
                    ThemeOptionItem(
                        text = stringResource(R.string.theme_dark), 
                        icon = Icons.Default.DarkMode,
                        selected = currentTheme == 2
                    ) {
                        com.example.guardianai.data.SettingsManager.setThemeMode(context, 2)
                        showThemeDialog = false
                    }
                }
            },
            confirmButton = { 
                TextButton(onClick = { showThemeDialog = false }) { 
                    Text(stringResource(R.string.btn_cancel)) 
                } 
            }
        )
    }
    
    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = { Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("GuardianAI", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Версия 2.1",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Защита от мошенников с помощью искусственного интеллекта",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "© 2024 GuardianAI Team",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = { 
                TextButton(onClick = { showAboutDialog = false }) { 
                    Text("OK") 
                } 
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsIconBox(icon: ImageVector, tint: Color) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = tint.copy(alpha = 0.15f),
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .padding(8.dp)
                .size(24.dp)
        )
    }
}

@Composable
fun SettingsCardItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { 
            Text(title, fontWeight = FontWeight.Medium) 
        },
        supportingContent = { 
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) 
        },
        leadingContent = { 
            SettingsIconBox(icon = icon, tint = iconTint)
        },
        trailingContent = {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        },
        modifier = Modifier.clickable { onClick() },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun ThemeOptionItem(text: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(text) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = { 
            RadioButton(selected = selected, onClick = null) 
        },
        modifier = Modifier.clickable { onClick() },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

// Legacy SettingsItem for compatibility
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

// Legacy ThemeOption for compatibility
@Composable
fun ThemeOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}
