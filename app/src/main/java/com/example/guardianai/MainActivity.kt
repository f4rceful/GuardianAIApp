package com.example.guardianai

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.animation.core.*
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

@OptIn(ExperimentalMaterial3Api::class)
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
    val isProtectionEnabled by com.example.guardianai.data.SettingsManager.isProtectionEnabled
    
    // Статистика
    var stats by remember { mutableStateOf(com.example.guardianai.data.HistoryManager.Stats()) }
    
    // Диалоги
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showContactRationale by remember { mutableStateOf(false) }
    var showOverlayDialog by remember { mutableStateOf(false) }
    var showDisableConfirmDialog by remember { mutableStateOf(false) }

    val hasContactPermission = remember {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.READ_CONTACTS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    val contactPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            android.widget.Toast.makeText(context, context.getString(R.string.toast_contact_limited), android.widget.Toast.LENGTH_LONG).show()
        }
    }
    
    // Проверка при запуске и возврате
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermission()
                stats = com.example.guardianai.data.HistoryManager.getStats(context)
                if (!Settings.canDrawOverlays(context)) {
                    showOverlayDialog = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        stats = com.example.guardianai.data.HistoryManager.getStats(context)
        if (!hasContactPermission) showContactRationale = true
        if (!Settings.canDrawOverlays(context)) showOverlayDialog = true
    }

    // Определение статуса
    val isProtected = isPermissionGranted && isServerConnected && isProtectionEnabled

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Title
        Text(
            text = "Guardian AI",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (isCheckingStatus) {
            Spacer(modifier = Modifier.weight(1f))
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.status_checking),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.weight(1f))
        } else {
            // === Status Card ===
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pulsing animation when protected
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.7f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_alpha"
                    )
                    
                    Icon(
                        imageVector = if (isProtected) Icons.Default.Security else Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer {
                                if (isProtected && isProtectionEnabled) {
                                    scaleX = scale
                                    scaleY = scale
                                }
                            },
                        tint = if (isProtected) 
                            MaterialTheme.colorScheme.primary.copy(alpha = if (isProtectionEnabled) alpha else 1f)
                        else 
                            MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = when {
                            !isProtectionEnabled -> stringResource(R.string.status_protection_disabled)
                            !isPermissionGranted -> stringResource(R.string.status_protection_disabled)
                            !isServerConnected -> stringResource(R.string.status_protection_inactive_no_server)
                            else -> stringResource(R.string.status_protection_active)
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (isProtected) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = when {
                            !isPermissionGranted -> stringResource(R.string.status_desc_permission_needed)
                            !isServerConnected -> stringResource(R.string.status_desc_no_server)
                            else -> stringResource(R.string.status_desc_active)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Protection Toggle Button
                    // Protection Toggle Button
                    if (isProtectionEnabled) {
                        OutlinedButton(
                            onClick = { showDisableConfirmDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(0.8f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = "Выключить защиту",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Button(
                            onClick = { com.example.guardianai.data.SettingsManager.setProtectionEnabled(context, true) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            Text(
                                text = "Включить защиту",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // === Statistics Section ===
            Text(
                text = "Статистика",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Scams Blocked Card
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Security,
                    value = stats.scamsBlocked.toString(),
                    label = "Угроз\nзаблокировано",
                    accentColor = MaterialTheme.colorScheme.error
                )
                
                // Total Scans Card
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CheckCircle,
                    value = stats.totalScans.toString(),
                    label = "Сообщений\nпроверено",
                    accentColor = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Warnings Card
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Warning,
                    value = stats.warningsShown.toString(),
                    label = "Предупреждений",
                    accentColor = MaterialTheme.colorScheme.tertiary
                )
                
                // Safe Messages Card
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CheckCircle,
                    value = stats.safeMessages.toString(),
                    label = "Безопасных",
                    accentColor = MaterialTheme.colorScheme.secondary
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // ... buttons ...
        }
    }
    
    // ... dialogs ...
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp), // Reduced padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon with tonal background
            Box(
                modifier = Modifier
                    .size(40.dp) // Reduced box size
                    .background(accentColor.copy(alpha = 0.1f), CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp), // Reduced icon size
                    tint = accentColor
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp)) // Reduced spacer
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall, // Smaller headline
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall, // Smaller label
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 0.9, // Tighter lines
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}