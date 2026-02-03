package com.example.guardianai.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.guardianai.data.HistoryItem
import com.example.guardianai.data.HistoryManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource
import com.example.guardianai.R
import com.example.guardianai.data.GuardianRepository
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)


@Composable
fun HistoryScreen(repository: GuardianRepository? = null) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    
    // Резервный вариант, если репозиторий равен null
    val safeRepository = repository ?: GuardianRepository()
    
    val viewModel: HistoryViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel(application, safeRepository) as T
            }
        }
    )

    // Если экран показывается после скрытия, обновим список
    LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }

    val historyList by viewModel.historyList.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedItems by viewModel.selectedItems.collectAsState()
    
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isSelectionMode) {
                        Text(stringResource(R.string.fmt_history_selected, selectedItems.size))
                    } else {
                        Text(
                            stringResource(R.string.screen_history_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineMedium
                        ) 
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        // Кнопка "Выбрать все"
                        TextButton(onClick = { viewModel.selectAll() }) {
                            Text(if (selectedItems.size == historyList.size) stringResource(R.string.btn_deselect_all) else stringResource(R.string.btn_all))
                        }
                        
                        // Кнопка удаления
                        if (selectedItems.isNotEmpty()) {
                            IconButton(onClick = { showDeleteConfirmDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    } else {
                        // Стандартный режим с меню (только если список не пуст)
                        if (historyList.isNotEmpty()) {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_select_ellipsis)) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.enterSelectionMode()
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        // Filter state
        var selectedFilter by remember { mutableStateOf(0) } // 0=All, 1=Scam, 2=Warning, 3=Safe
        
        val filteredList = when (selectedFilter) {
            1 -> historyList.filter { it.isScam }
            2 -> historyList.filter { it.isWarning && !it.isScam }
            3 -> historyList.filter { !it.isScam && !it.isWarning }
            else -> historyList
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Chips
            if (historyList.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == 0,
                        onClick = { selectedFilter = 0 },
                        label = { Text("Все") }
                    )
                    FilterChip(
                        selected = selectedFilter == 1,
                        onClick = { selectedFilter = 1 },
                        label = { Text("Скам") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == 2,
                        onClick = { selectedFilter = 2 },
                        label = { Text("Подозр.") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFE0B2)
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == 3,
                        onClick = { selectedFilter = 3 },
                        label = { Text("Безоп.") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFC8E6C9)
                        )
                    )
                }
            }
            
            if (historyList.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "История пуста",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Здесь появятся проверенные сообщения",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (filteredList.isEmpty()) {
                // No results for filter
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет записей в этой категории",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                var isRefreshing by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        scope.launch {
                            isRefreshing = true
                            viewModel.refreshHistory()
                            kotlinx.coroutines.delay(500)
                            isRefreshing = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList, key = { it.id }) { item ->
                            // Состояние меню обратной связи
                            var showItemMenu by remember { mutableStateOf(false) }
                        
                            Box {
                                HistoryItemCard(
                                    item = item,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = selectedItems.contains(item.id),
                                    onToggleSelection = {
                                        viewModel.toggleSelection(item.id)
                                    },
                                    onLongClick = {
                                         if (!isSelectionMode) {
                                             showItemMenu = true
                                         }
                                    }
                                )
                        
                                // Контекстное меню элемента
                                DropdownMenu(
                                    expanded = showItemMenu,
                                    onDismissRequest = { showItemMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_copy)) },
                                        onClick = {
                                            showItemMenu = false
                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("Scam Text", item.text)
                                            clipboard.setPrimaryClip(clip)
                                            android.widget.Toast.makeText(context, context.getString(R.string.toast_copied), android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    if (item.isScam) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.menu_report_not_spam)) },
                                            onClick = {
                                                showItemMenu = false
                                                viewModel.sendFeedback(item)
                                                android.widget.Toast.makeText(context, context.getString(R.string.toast_report_sent), android.widget.Toast.LENGTH_SHORT).show()
                                            },
                                            leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50)) }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_select)) },
                                        onClick = {
                                            showItemMenu = false
                                            viewModel.enterSelectionMode(item.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_title)) },
            text = { Text(stringResource(R.string.fmt_dialog_delete_text, selectedItems.size)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelected()
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun HistoryItemCard(
    item: HistoryItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onLongClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val cardColor = when {
        item.isScam -> Color(0xFFFFEBEE) // Light Red
        item.isWarning -> Color(0xFFFFF3E0) // Light Orange
        else -> Color(0xFFE8F5E9) // Light Green
    }
    
    val accentColor = when {
        item.isScam -> Color(0xFFD32F2F) // Red
        item.isWarning -> Color(0xFFEF6C00) // Orange
        else -> Color(0xFF388E3C) // Green
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelection()
                    } else {
                        isExpanded = !isExpanded
                    }
                },
                onLongClick = { onLongClick() }
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Status Icon
                Icon(
                    imageVector = when {
                        item.isScam -> Icons.Default.Warning
                        item.isWarning -> Icons.Default.Warning
                        else -> Icons.Default.CheckCircle
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Title & Time
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            item.isScam -> stringResource(R.string.label_scam_detected)
                            item.isWarning -> stringResource(R.string.label_suspicious)
                            else -> stringResource(R.string.label_safe)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                
                // Expand/Collapse Icon
                Icon(
                    imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.MoreVert,
                    contentDescription = "Развернуть",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Message Preview
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (isExpanded) 10 else 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.DarkGray
            )
            
            // Expanded Details
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = accentColor.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))
                
                // Reasons Section
                if (item.reason.isNotEmpty()) {
                    Text(
                        text = "Причины:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    item.reason.forEach { reason ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("• ", color = accentColor)
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
                
                // Entity / Trigger Chips
                if (!item.entities.isNullOrEmpty() || !item.explanation.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Обнаружено:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Organization Chips
                        item.entities?.get("ORG")?.forEach { org ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text("🏢 $org") },
                                border = null,
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color(0xFFE3F2FD)
                                )
                            )
                        }
                        
                        // Person Chips
                        item.entities?.get("PER")?.forEach { per ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text("👤 $per") },
                                border = null,
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color(0xFFF3E5F5)
                                )
                            )
                        }
                        
                        // Urgency Chips
                        item.entities?.get("URGENCY")?.forEach { urg ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text("⚡ $urg") },
                                border = null,
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color(0xFFFFECB3)
                                )
                            )
                        }
                        
                        // Trigger Chips
                        item.explanation?.filter { it.type == "TRIGGER" }?.forEach { trigger ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text("🔴 ${trigger.word}") },
                                border = null,
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color(0xFFFFCDD2)
                                )
                            )
                        }
                        
                        // ML Factor Chips (High Impact Only)
                        item.explanation?.filter { it.type == "ML_FACTOR" && it.impact >= 0.1f }?.take(3)?.forEach { factor ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text("⚠️ ${factor.word}") },
                                border = null,
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color(0xFFFFE0B2)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
