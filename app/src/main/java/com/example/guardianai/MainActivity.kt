package com.example.guardianai

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.guardianai.data.SettingsManager
import com.example.guardianai.ui.HistoryScreen
import com.example.guardianai.ui.SettingsScreen
import com.example.guardianai.ui.WhitelistScreen
import com.example.guardianai.ui.theme.GuardianAITheme

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource

import com.example.guardianai.data.GuardianRepository

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.guardianai.ui.MainViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    private val repository = GuardianRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Инициализация настроек
        SettingsManager.init(this)
        
        setContent {
            val themeMode by SettingsManager.themeMode
            val useDarkTheme = when(themeMode) {
                1 -> false // Светлая
                2 -> true  // Темная
                else -> androidx.compose.foundation.isSystemInDarkTheme() // Системная
            }
            
            GuardianAITheme(darkTheme = useDarkTheme) {
                MainApp(repository)
            }
        }
    }
}

@Composable
fun MainApp(repository: GuardianRepository) {
    val navController = rememberNavController()
    val items = listOf(
        "home" to stringResource(R.string.nav_home), 
        "history" to stringResource(R.string.nav_history), 
        "settings" to stringResource(R.string.nav_settings)
    )
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                items.forEach { (route, title) ->
                    NavigationBarItem(
                        icon = {
                            when(route) {
                                "home" -> Icon(Icons.Default.Home, null)
                                "history" -> Icon(Icons.Default.History, null)
                                "settings" -> Icon(Icons.Default.Settings, null)
                            }
                        },
                        label = { Text(title) },
                        selected = currentRoute == route,
                        onClick = {
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { MainScreen(repository) }
            composable("history") { HistoryScreen(repository) }
            composable("settings") { SettingsScreen(navController, repository) }
            composable("whitelist") { WhitelistScreen(onBack = { navController.popBackStack() }, repository) }
        }
    }
}

@Composable
fun MainScreen(repository: GuardianRepository) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    val viewModel: MainViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(application, repository) as T
            }
        }
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Состояние из ViewModel
    val isPermissionGranted by viewModel.isPermissionGranted.collectAsState()
    val isServerConnected by viewModel.isServerConnected.collectAsState()
    val isCheckingStatus by viewModel.isCheckingStatus.collectAsState()
    // Состояние включения защиты
    val isProtectionEnabled by com.example.guardianai.data.SettingsManager.isProtectionEnabled

    // Показать диалог пояснения
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showContactRationale by remember { mutableStateOf(false) }
    var showOverlayDialog by remember { mutableStateOf(false) }
    var showDisableConfirmDialog by remember { mutableStateOf(false) }

    // Проверка разрешения на контакты
    val hasContactPermission = remember {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    val contactPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
             android.widget.Toast.makeText(context, context.getString(R.string.toast_contact_limited), android.widget.Toast.LENGTH_LONG).show()
        }
    }
    
    // Проверка при запуске и возврате в приложение
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermission()
                if (!Settings.canDrawOverlays(context)) {
                    showOverlayDialog = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Логика проверки контактов при старте
    LaunchedEffect(Unit) {
        if (!hasContactPermission) {
             showContactRationale = true
        }
        if (!Settings.canDrawOverlays(context)) {
             showOverlayDialog = true
        }
    }

    // (Сетевая логика удалена, так как она теперь в ViewModel)


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isCheckingStatus) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = stringResource(R.string.status_checking),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.status_connecting),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        } else {
            // Определение статуса
            val isProtected = isPermissionGranted && isServerConnected && isProtectionEnabled
            val statusText = when {
                !isProtectionEnabled -> stringResource(R.string.status_protection_disabled)
                !isPermissionGranted -> stringResource(R.string.status_protection_disabled)
                !isServerConnected -> stringResource(R.string.status_protection_inactive_no_server)
                else -> stringResource(R.string.status_protection_active)
            }
            val statusColor = if (isProtected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
            val statusIcon = if (isProtected) Icons.Default.Security else if (!isServerConnected && isPermissionGranted) Icons.Default.Warning else Icons.Default.NotificationsOff

            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = statusColor
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = statusText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = when {
                    !isPermissionGranted -> stringResource(R.string.status_desc_permission_needed)
                    !isServerConnected -> stringResource(R.string.status_desc_no_server)
                    else -> stringResource(R.string.status_desc_active)
                },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Переключатель защиты
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = if (isProtectionEnabled) "Защита включена" else "Защита выключена",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = isProtectionEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            com.example.guardianai.data.SettingsManager.setProtectionEnabled(context, true)
                        } else {
                            showDisableConfirmDialog = true
                        }
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (!isPermissionGranted) {
            Button(
                onClick = { showPermissionDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.btn_enable_protection))
            }
        }
    }

    // Диалог запроса прав на уведомления
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = {
                Text(text = stringResource(R.string.dialog_permission_title))
            },
            text = {
                Text(stringResource(R.string.dialog_permission_text))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                    }
                ) {
                    Text(stringResource(R.string.btn_open_settings))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionDialog = false }
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    // Диалог пояснения прав на контакты
    if (showContactRationale) {
        AlertDialog(
            onDismissRequest = { showContactRationale = false },
            title = { Text(stringResource(R.string.dialog_contact_title)) },
            text = { Text(stringResource(R.string.dialog_contact_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showContactRationale = false
                    contactPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                }) {
                    Text(stringResource(R.string.btn_allow))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                     showContactRationale = false
                     android.widget.Toast.makeText(context, context.getString(R.string.toast_contact_later), android.widget.Toast.LENGTH_SHORT).show()
                }) {
                    Text(stringResource(R.string.btn_no_thanks))
                }
            }
        )
    }
    
    // Диалог запроса прав "Поверх других окон"
    if (showOverlayDialog) {
        AlertDialog(
            onDismissRequest = { /* Нельзя закрыть просто так */ },
            title = { Text(stringResource(R.string.settings_overlay_title)) },
            text = { Text(stringResource(R.string.dialog_overlay_text)) },
            icon = { Icon(Icons.Default.Layers, contentDescription = null) },
            confirmButton = {
                Button(onClick = {
                    showOverlayDialog = false
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))
                    context.startActivity(intent)
                }) {
                    Text(stringResource(R.string.btn_allow))
                }
            }
        )
    }

    // Диалог подтверждения отключения
    if (showDisableConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDisableConfirmDialog = false },
            title = { Text(stringResource(R.string.dialog_disable_protection_title)) },
            text = { Text(stringResource(R.string.dialog_disable_protection_text)) },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            confirmButton = {
                Button(
                    onClick = {
                        com.example.guardianai.data.SettingsManager.setProtectionEnabled(context, false)
                        showDisableConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.btn_turn_off))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableConfirmDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}