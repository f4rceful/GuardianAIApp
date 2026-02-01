package com.example.guardianai.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.guardianai.data.SettingsManager
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource
import com.example.guardianai.R
import com.example.guardianai.data.GuardianRepository
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.text.font.FontWeight
import com.example.guardianai.ui.WhitelistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(onBack: () -> Unit, repository: GuardianRepository? = null) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application

    val safeRepository = repository ?: GuardianRepository()
    
    val viewModel: WhitelistViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WhitelistViewModel(application, safeRepository) as T
            }
        }
    )

    val selectedTab by viewModel.selectedTab.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedItems by viewModel.selectedItems.collectAsState()
    
    val trustedApps by viewModel.trustedApps.collectAsState()
    val trustedContacts by viewModel.trustedContacts.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    
    val tabs = listOf(stringResource(R.string.tab_apps), stringResource(R.string.tab_contacts))
    
    var searchQuery by remember { mutableStateOf("") }




    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isSelectionMode) {
                        Text(stringResource(R.string.fmt_whitelist_selected, selectedItems.size))
                    } else {
                        Text(stringResource(R.string.screen_whitelist_title)) 
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        Text(stringResource(R.string.fmt_whitelist_selected, selectedItems.size))
                        IconButton(onClick = { viewModel.deleteSelected() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    } else {
                        IconButton(onClick = { 
                            if (selectedTab == 0) viewModel.loadInstalledApps() else viewModel.loadContacts()
                            showAddDialog = true 
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                        IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Select for delete")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(onClick = { 
                    if (selectedTab == 0) {
                         viewModel.loadInstalledApps()
                    } else {
                         viewModel.loadContacts()
                    }
                    showAddDialog = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }

    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Вкладки
            if (!isSelectionMode) {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { viewModel.setTab(index) },
                            text = { Text(title) }
                        )
                    }
                }
            }
            

            
            if (!isSelectionMode) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true
                )
            }
            
            // Список элементов
            val currentList = if (selectedTab == 0) trustedApps.toList() else trustedContacts.toList()
            val filteredList = if (searchQuery.isBlank()) {
                currentList
            } else {
                currentList.filter { it.contains(searchQuery, ignoreCase = true) }
            }
            val emptyMessage = if (selectedTab == 0) stringResource(R.string.empty_trusted_apps) else stringResource(R.string.empty_trusted_contacts)

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList) { item ->
                        val isSelected = selectedItems.contains(item)
                        ListItem(
                            headlineContent = { Text(item) },
                            leadingContent = {
                                if (isSelectionMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { 
                                            viewModel.toggleSelection(item)
                                        }
                                    )
                                } else {
                                    Icon(
                                        if(selectedTab == 0) Icons.Default.Android else Icons.Default.Person,
                                        contentDescription = null
                                    )
                                }
                            },
                            trailingContent = {
                                if (!isSelectionMode) {
                                    IconButton(onClick = {
                                        if (selectedTab == 0) {
                                            viewModel.removeTrustedApp(item)
                                        } else {
                                            viewModel.removeTrustedContact(item)
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove")
                                    }
                                }
                            },
                            modifier = Modifier.clickable {
                                if (isSelectionMode) {
                                    viewModel.toggleSelection(item)
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    // ... Диалоги выбора ...

    if (showAddDialog) {
        if (selectedTab == 0) {
            val installedApps by viewModel.installedApps.collectAsState()
            val isLoading by viewModel.isLoadingPicker.collectAsState()
            
            AddAppDialog(
                installedApps = installedApps,
                isLoading = isLoading,
                onDismiss = { showAddDialog = false },
                onAdd = { apps ->
                    viewModel.addTrustedApps(apps.map { it.packageName })
                    showAddDialog = false
                }
            )
        } else {
            val contacts by viewModel.contactsList.collectAsState()
            val isLoading by viewModel.isLoadingPicker.collectAsState()

            AddContactDialog(
                contacts = contacts,
                isLoading = isLoading,
                onDismiss = { showAddDialog = false },
                onAdd = { selectedContacts ->
                    viewModel.addTrustedContacts(selectedContacts.map { it.name })
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun AddAppDialog(
    installedApps: List<com.example.guardianai.util.AppInfo>,
    isLoading: Boolean,
    onDismiss: () -> Unit, 
    onAdd: (List<com.example.guardianai.util.AppInfo>) -> Unit
) {
    var selectedItems by remember { mutableStateOf(setOf<com.example.guardianai.util.AppInfo>()) }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredApps = if (searchQuery.isBlank()) {
        installedApps
    } else {
        installedApps.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.packageName.contains(searchQuery, ignoreCase = true) 
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_select_apps_title)) },
        text = {
            if (isLoading) {
                 Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                     CircularProgressIndicator()
                 }
            } else {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true
                    )
                    
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(filteredApps) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedItems = if (selectedItems.contains(app)) selectedItems - app else selectedItems + app
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = selectedItems.contains(app), onCheckedChange = null)
                                Spacer(Modifier.width(8.dp))
                                
                                androidx.compose.ui.viewinterop.AndroidView(
                                    factory = { ctx -> 
                                        android.widget.ImageView(ctx).apply { 
                                            setImageDrawable(app.icon) 
                                        } 
                                    },
                                    modifier = Modifier.size(40.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column {
                                    Text(app.name, fontWeight = FontWeight.Bold)
                                    Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    if (filteredApps.isEmpty()) {
                         Text(stringResource(R.string.dialog_no_apps_found))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(selectedItems.toList()) }) {
                Text(if (selectedItems.isEmpty()) stringResource(R.string.btn_close) else stringResource(R.string.fmt_btn_add_count, selectedItems.size))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
fun AddContactDialog(
    contacts: List<com.example.guardianai.util.ContactInfo>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onAdd: (List<com.example.guardianai.util.ContactInfo>) -> Unit
) {
    var selectedItems by remember { mutableStateOf(setOf<com.example.guardianai.util.ContactInfo>()) }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredContacts = if (searchQuery.isBlank()) {
        contacts
    } else {
        contacts.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_select_contacts_title)) },
        text = {
            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column {
                    OutlinedTextField(
                         value = searchQuery,
                         onValueChange = { searchQuery = it },
                         modifier = Modifier
                             .fillMaxWidth()
                             .padding(bottom = 8.dp),
                         placeholder = { Text(stringResource(R.string.search_hint)) },
                         leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                         trailingIcon = {
                             if (searchQuery.isNotEmpty()) {
                                 IconButton(onClick = { searchQuery = "" }) {
                                     Icon(Icons.Default.Clear, contentDescription = "Clear")
                                 }
                             }
                         },
                         singleLine = true
                    )

                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(filteredContacts) { contact ->
                            Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedItems = if (selectedItems.contains(contact)) selectedItems - contact else selectedItems + contact
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = selectedItems.contains(contact), onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Text(contact.name, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                    if (filteredContacts.isEmpty()) {
                        Text(stringResource(R.string.dialog_no_contacts_found))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(selectedItems.toList()) }) {
                Text(if (selectedItems.isEmpty()) stringResource(R.string.btn_close) else stringResource(R.string.fmt_btn_add_count, selectedItems.size))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) } }
    )
}
