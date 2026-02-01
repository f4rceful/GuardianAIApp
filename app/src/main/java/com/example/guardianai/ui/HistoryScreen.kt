package com.example.guardianai.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
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
                        Text(stringResource(R.string.screen_history_title)) 
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
        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.empty_history), color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyList) { item ->
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryItemCard(
    item: HistoryItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                item.isScam -> MaterialTheme.colorScheme.errorContainer
                item.isWarning -> Color(0xFFFFCC80)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelection()
                    }
                },
                onLongClick = {
                    onLongClick()
                }
            )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Icon(
                imageVector = when {
                    item.isScam -> Icons.Default.Warning
                    item.isWarning -> Icons.Default.Warning
                    else -> Icons.Default.CheckCircle
                },
                contentDescription = null,
                tint = when {
                     item.isScam -> MaterialTheme.colorScheme.error
                     item.isWarning -> Color(0xFFEF6C00) // Deep Orange
                     else -> Color(0xFF4CAF50)
                },
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = when {
                        item.isScam -> stringResource(R.string.label_scam_detected)
                        item.isWarning -> stringResource(R.string.label_suspicious)
                        else -> stringResource(R.string.label_safe)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        item.isScam -> MaterialTheme.colorScheme.error
                        item.isWarning -> Color(0xFFEF6C00)
                        else -> Color(0xFF4CAF50)
                    },
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
