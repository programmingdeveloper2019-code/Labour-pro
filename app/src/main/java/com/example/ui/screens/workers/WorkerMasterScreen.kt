package com.example.ui.screens.workers

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Worker
import com.example.ui.components.BackupRestoreDialog
import com.example.ui.components.CategorySelectionDialog
import com.example.ui.components.StatusBadge
import com.example.ui.components.TradeIcon
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.LabourViewModel
import com.example.util.AppLanguage
import com.example.util.CategoryManager
import com.example.util.ExcelExportHelper
import com.example.util.LanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerMasterScreen(
    viewModel: LabourViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    val isMy = currentLanguage == AppLanguage.MYANMAR
    val workers by viewModel.allWorkers.collectAsState()
    val detectedVault by viewModel.detectedPersistentVault.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") } // "All", "Active", "Inactive"
    var categoryFilter by remember { mutableStateOf("All") } // "All" or trade code
    var showAddDialog by remember { mutableStateOf(false) }
    var editingWorker by remember { mutableStateOf<Worker?>(null) }
    var viewingWorker by remember { mutableStateOf<Worker?>(null) }
    var workerToDelete by remember { mutableStateOf<Worker?>(null) }
    var showManageCategoriesDialog by remember { mutableStateOf(false) }
    var showBackupRestoreDialog by remember { mutableStateOf(false) }

    val existingWorkerTypes = remember(workers) {
        workers.map { it.workerType }.distinct()
    }

    val filteredWorkers = remember(workers, searchQuery, statusFilter, categoryFilter) {
        workers.filter { w ->
            val myanmarTrade = CategoryManager.getMyanmarName(w.workerType)
            val matchesSearch = searchQuery.isBlank() ||
                    w.name.contains(searchQuery, ignoreCase = true) ||
                    w.workerId.contains(searchQuery, ignoreCase = true) ||
                    w.workerType.contains(searchQuery, ignoreCase = true) ||
                    myanmarTrade.contains(searchQuery, ignoreCase = true) ||
                    w.address.contains(searchQuery, ignoreCase = true) ||
                    w.nrcNo.contains(searchQuery, ignoreCase = true) ||
                    w.phone.contains(searchQuery)
            val matchesStatus = statusFilter == "All" || w.status == statusFilter
            val matchesCategory = categoryFilter == "All" ||
                    w.workerType.equals(categoryFilter, ignoreCase = true) ||
                    myanmarTrade.equals(categoryFilter, ignoreCase = true)
            matchesSearch && matchesStatus && matchesCategory
        }.sortedWith { a, b -> com.example.data.model.compareWorkerIds(a.workerId, b.workerId) }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("worker_master_screen"),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ConstructionGold,
                contentColor = Slate900,
                modifier = Modifier.testTag("add_worker_fab")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isMy) "လုပ်သားအသစ် ထည့်မည်" else "Add Worker", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Permanent Data Protection Status Banner (Assures user that workers never disappear)
            Surface(
                color = WagesGreenLight,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = WagesGreenDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isMy)
                            "လုပ်သားစာရင်း အမြဲတမ်း သိမ်းဆည်းပြီး (မဖျက်မချင်း မပျောက်ပျက်ဘဲ အမြဲရှိနေပါမည်)"
                        else
                            "Workforce Permanently Secured (Safe & never lost unless explicitly deleted)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = WagesGreenDark
                    )
                }
            }

            // Header & Filters
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isMy) "Worker Master (လုပ်သားစာရင်း)" else "Worker Master Directory",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${workers.size} ${if (isMy) "ဦး စာရင်းရှိ" else "registered"} (${workers.count { it.status == "Active" }} active)",
                                    fontSize = 12.sp,
                                    color = Slate500
                                )
                                Surface(
                                    color = WagesGreenLight,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "☁️ Auto-Vault Active",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WagesGreenDark,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                                Surface(
                                    color = Slate200,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (isMy) "🔢 ID အလိုက်" else "🔢 By ID",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate700,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Backup & Restore button
                            OutlinedButton(
                                onClick = { showBackupRestoreDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp), tint = OtBlue)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isMy) "အရန်သိမ်း/ယူ" else "Backup", fontSize = 11.sp, color = OtBlue, fontWeight = FontWeight.Bold)
                            }

                            // Manage Categories Button
                            OutlinedButton(
                                onClick = { showManageCategoriesDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("manage_categories_btn")
                            ) {
                                Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp), tint = SafetyOrangeDark)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isMy) "အမျိုးအစား" else "Trade", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SafetyOrangeDark)
                            }
                        }
                    }

                    // Search input (supports Name, ID, Trade, and ADDRESS / HOMETOWN)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                if (isMy) "အမည်၊ ကုဒ် (W-101)၊ နေရပ်လိပ်စာ (ပဲခူး၊ အင်းစိန်)၊ အမျိုးအစား ရှာရန်..."
                                else "Search by name, ID, address (Bago, Yangon), trade..."
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("worker_search_bar"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Status Filter Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All" to "အားလုံး", "Active" to "လက်ရှိ (Active)", "Inactive" to "နားထား (Inactive)").forEach { (status, mm) ->
                            FilterChip(
                                selected = statusFilter == status,
                                onClick = { statusFilter = status },
                                label = { Text(if (isMy) mm else status, fontSize = 12.sp) },
                                modifier = Modifier.testTag("status_filter_${status.lowercase()}")
                            )
                        }
                    }

                    // Construction Category Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isMy) "အမျိုးအစား:" else "Category:", fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.SemiBold)
                        FilterChip(
                            selected = categoryFilter == "All",
                            onClick = { categoryFilter = "All" },
                            label = { Text("All", fontSize = 12.sp) }
                        )
                        val commonTrades = listOf(
                            "Carpenter" to "လက်သမား",
                            "Mason" to "ပန်းရံ",
                            "Helper" to "အကူ",
                            "Bar Bender" to "သံချည်",
                            "Welder" to "ဝရိန်",
                            "Electrician" to "လျှပ်စစ်",
                            "Painter" to "ဆေးသုတ်",
                            "Foreman" to "ဖိုမင်"
                        )
                        commonTrades.forEach { (code, mmName) ->
                            val isSelected = categoryFilter.equals(code, ignoreCase = true) || categoryFilter.equals(mmName, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    categoryFilter = if (isSelected) "All" else code
                                },
                                label = { Text(mmName, fontSize = 12.sp) },
                                leadingIcon = {
                                    TradeIcon(trade = code, modifier = Modifier.size(18.dp))
                                }
                            )
                        }
                    }
                }
            }

            // Workers List
            if (filteredWorkers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (workers.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = ConstructionGoldDark,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (isMy) "ရှာဖွေမှု/စစ်ထုတ်မှုနှင့် ကိုက်ညီသော လုပ်သား မတွေ့ပါ" else "No matching workers found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isMy) "စာရင်းသွင်းထားသော လုပ်သား စုစုပေါင်း (${workers.size}) ဦး ရှိပါသည်။ စစ်ထုတ်မှု ပြန်ဖြုတ်၍ အားလုံးကို ကြည့်ရှုနိုင်ပါသည်။"
                                    else "You have ${workers.size} registered workers. Reset filters to see all workers.",
                                fontSize = 12.sp,
                                color = Slate500,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    searchQuery = ""
                                    statusFilter = "All"
                                    categoryFilter = "All"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ConstructionGold),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Slate900, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isMy) "လုပ်သားအားလုံး ပြန်ကြည့်မည်" else "Show All Workers", color = Slate900, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.PersonOff,
                                contentDescription = null,
                                tint = Slate500,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (isMy) "လုပ်သားစာရင်း မရှိသေးပါ" else "No workers registered yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isMy) "မိမိ၏ လုပ်သားအစစ်များကို အောက်ပါ '+ လုပ်သားအသစ် ထည့်မည်' ခလုတ်ဖြင့် စတင်ထည့်သွင်းပါ"
                                    else "Tap '+ Add Worker' below to register your real workers.",
                                fontSize = 12.sp,
                                color = Slate500,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredWorkers, key = { it.workerId }) { worker ->
                        WorkerItemCard(
                            worker = worker,
                            isMy = isMy,
                            onView = { viewingWorker = worker },
                            onEdit = { editingWorker = worker },
                            onDelete = { workerToDelete = worker },
                            onReactivate = { viewModel.reactivateWorker(worker) }
                        )
                    }
                }
            }
        }
    }

    androidx.activity.compose.BackHandler {
        viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.DASHBOARD)
    }

    // Add Worker Dialog
    if (showAddDialog) {
        val nextId = remember(workers) {
            val maxNum = workers.mapNotNull { it.workerId.removePrefix("W-").toIntOrNull() }.maxOrNull() ?: (workers.size + 100)
            "W-${maxNum + 1}"
        }
        WorkerFormDialog(
            initialWorker = Worker(workerId = nextId, name = ""),
            isNew = true,
            existingWorkerTypes = existingWorkerTypes,
            existingWorkerIds = workers.map { it.workerId },
            onDismiss = { showAddDialog = false },
            onSave = { newWorker ->
                viewModel.addWorker(newWorker) {
                    showAddDialog = false
                    searchQuery = ""
                    statusFilter = "All"
                    categoryFilter = "All"
                    android.widget.Toast.makeText(
                        context,
                        if (isMy) "လုပ်သား ${newWorker.name} ကို စာရင်းထဲ ထည့်သွင်းပြီးပါပြီ"
                        else "Added ${newWorker.name} to workforce",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    // Edit Worker Dialog
    if (editingWorker != null) {
        val currentEditId = editingWorker!!.workerId
        WorkerFormDialog(
            initialWorker = editingWorker!!,
            isNew = false,
            existingWorkerTypes = existingWorkerTypes,
            existingWorkerIds = workers.filter { it.workerId != currentEditId }.map { it.workerId },
            onDismiss = { editingWorker = null },
            onSave = { updated ->
                viewModel.updateWorker(updated) {
                    editingWorker = null
                }
            }
        )
    }

    // View Worker Profile Detail Dialog
    if (viewingWorker != null) {
        WorkerDetailDialog(
            worker = viewingWorker!!,
            onDismiss = { viewingWorker = null },
            onEdit = {
                val target = viewingWorker!!
                viewingWorker = null
                editingWorker = target
            },
            onMarkAttendance = {
                val target = viewingWorker!!
                viewingWorker = null
                viewModel.selectWorker(target)
                viewModel.navigateTo(AppScreen.POS_ENTRY)
            }
        )
    }

    // Manage Categories Dialog
    if (showManageCategoriesDialog) {
        CategorySelectionDialog(
            currentTradeCode = categoryFilter,
            existingWorkerTypes = existingWorkerTypes,
            onDismiss = { showManageCategoriesDialog = false },
            onSelectTrade = { selectedTrade ->
                categoryFilter = selectedTrade.code
                showManageCategoriesDialog = false
            }
        )
    }

    // Cloud Backup & Restore Dialog
    if (showBackupRestoreDialog) {
        BackupRestoreDialog(
            viewModel = viewModel,
            onDismiss = { showBackupRestoreDialog = false }
        )
    }

    // Delete / Deactivate Worker Confirmation Dialog
    // Satisfies: "အပြီးမဖျက်မချင်း ပြန်ပေါ်ပါစေ worker တွေကို ပြန်ပြန်ထည့်နေရမှာစိုးလို့ပါ"
    if (workerToDelete != null) {
        val target = workerToDelete!!
        AlertDialog(
            onDismissRequest = { workerToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = AdvanceRed, modifier = Modifier.size(32.dp)) },
            title = {
                Text(
                    text = if (isMy) "${target.name} ကို စာရင်းမှ ဖယ်ရှားမည်လား?" else "Remove ${target.name}?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isMy)
                            "အပြီးမဖျက်မချင်း လုပ်သားဒေတာများကို မပျောက်ပျက်စေရန် အောက်ပါ ရွေးချယ်မှုများထဲမှ ရွေးပေးပါ-"
                        else
                            "To preserve worker records until explicitly deleted permanently, choose an action:",
                        fontSize = 13.sp
                    )
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "1. 'ခေတ္တရပ်နား (Deactivate)' ရွေးလျှင်: လုပ်သား၏ အချက်အလက်နှင့် နေရပ်လိပ်စာများ ဆက်လက်ကျန်ရှိနေမည်ဖြစ်ပြီး လိုအပ်ပါက အချိန်မရွေး ပြန်လည်အသုံးပြုနိုင်ပါသည်။",
                                fontSize = 11.sp,
                                color = Slate800
                            )
                            Text(
                                text = "2. 'အပြီးဖျက်မည် (Permanently Delete)' ရွေးမှသာ ဒေတာဘေ့စ်ထဲမှ အပြီးတိုင် ဖျက်ပစ်ပါမည်။",
                                fontSize = 11.sp,
                                color = AdvanceRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Option 1: Safe Deactivate / Archive
                    Button(
                        onClick = {
                            viewModel.deactivateWorker(target)
                            workerToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isMy) "ခေတ္တရပ်နားမည်" else "Deactivate", fontWeight = FontWeight.Bold)
                    }

                    // Option 2: Permanent Delete
                    Button(
                        onClick = {
                            viewModel.deleteWorker(target)
                            workerToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AdvanceRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("confirm_delete_worker_btn")
                    ) {
                        Text(if (isMy) "အပြီးဖျက်မည်" else "Permanently Delete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { workerToDelete = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isMy) "မလုပ်ပါ" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun WorkerItemCard(
    worker: Worker,
    isMy: Boolean,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReactivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onView() }
            .testTag("worker_card_${worker.workerId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    TradeIcon(trade = worker.workerType, modifier = Modifier.size(44.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Worker Name: Black weight, 18sp, ample line-height for Myanmar fonts, multi-line (up to 2)
                        Text(
                            text = worker.name,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            lineHeight = 25.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // ID badge, Trade category, Gender, and Skill level row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = ConstructionGold,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = worker.workerId,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "• ${CategoryManager.getDisplayName(worker.workerType)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (worker.gender.isNotBlank()) {
                                Text(
                                    text = "• ${worker.gender}",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                            if (worker.skillLevel != "Skilled" && worker.skillLevel.isNotBlank()) {
                                Surface(
                                    color = Slate200,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = worker.skillLevel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate700,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Top right: Status Badge + Quick Call Button
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    StatusBadge(status = worker.status)

                    if (worker.phone.isNotBlank()) {
                        FilledTonalIconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${worker.phone.replace(" ", "")}")
                                }
                                context.startActivity(intent)
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = WagesGreenLight,
                                contentColor = WagesGreenDark
                            ),
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("call_worker_btn_${worker.workerId}")
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // WORKER ADDRESS / HOMETOWN (Prominently displayed)
            if (worker.address.isNotBlank()) {
                Surface(
                    color = Color(0xFFF1F5F9), // soft slate
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = SafetyOrangeDark, modifier = Modifier.size(14.dp))
                        Text(
                            text = worker.address,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate800
                        )
                    }
                }
            }

            // Wage rates and secondary chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Daily: ${ExcelExportHelper.formatCurrency(worker.dailyRate)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WagesGreenDark
                    )
                    Text(
                        text = "OT: ${ExcelExportHelper.formatCurrency(worker.otRate)}/hr",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OtBlue
                    )
                }

                if (worker.bloodType.isNotBlank()) {
                    Text(
                        text = "🩸 ${worker.bloodType}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdvanceRed
                    )
                }
            }

            // Action footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (worker.status == "Inactive") {
                    TextButton(
                        onClick = onReactivate,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = WagesGreenDark)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isMy) "ပြန်လည်စတင်မည်" else "Reactivate", fontSize = 11.sp, color = WagesGreenDark, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = if (worker.nrcNo.isNotBlank()) "🪪 ${worker.nrcNo}" else "",
                        fontSize = 10.sp,
                        color = Slate500
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = onView,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isMy) "အသေးစိတ်" else "Details", fontSize = 11.sp)
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp).testTag("edit_worker_btn_${worker.workerId}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = OtBlue, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp).testTag("delete_worker_btn_${worker.workerId}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AdvanceRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
