package com.example.ui.screens.sites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Site
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.LabourViewModel
import com.example.util.AppLanguage
import com.example.util.LanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteMasterScreen(
    viewModel: LabourViewModel,
    modifier: Modifier = Modifier
) {
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    val sites by viewModel.allSites.collectAsState()
    val allAttendance by viewModel.allAttendance.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSite by remember { mutableStateOf<Site?>(null) }
    var siteToDelete by remember { mutableStateOf<Site?>(null) }

    val filteredSites = remember(sites, searchQuery, statusFilter) {
        sites.filter { s ->
            val matchesSearch = searchQuery.isBlank() ||
                    s.siteName.contains(searchQuery, ignoreCase = true) ||
                    s.siteCode.contains(searchQuery, ignoreCase = true) ||
                    s.location.contains(searchQuery, ignoreCase = true) ||
                    s.supervisor.contains(searchQuery, ignoreCase = true)
            val matchesStatus = statusFilter == "All" || s.status == statusFilter
            matchesSearch && matchesStatus
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("site_master_screen"),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ConstructionGold,
                contentColor = Slate900,
                modifier = Modifier.testTag("add_site_fab")
            ) {
                Icon(Icons.Default.AddBusiness, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (currentLanguage == AppLanguage.MYANMAR) "ဆိုဒ်အသစ် ထည့်မည်" else "Add Construction Site", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.MYANMAR) "ဆောက်လုပ်ရေးဆိုဒ် စာရင်း" else "Construction Site Master",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (currentLanguage == AppLanguage.MYANMAR) "ဆိုဒ်ပေါင်း ${sites.size} ခု (လုပ်ဆောင်ဆဲ ${sites.count { it.status == "Ongoing" }} ခု)" else "${sites.size} project locations (${sites.count { it.status == "Ongoing" }} active ongoing)",
                        fontSize = 12.sp,
                        color = Slate500
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search site name, code, location, supervisor...") },
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
                            .testTag("site_search_bar"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All", "Ongoing", "Completed", "On Hold").forEach { st ->
                            FilterChip(
                                selected = statusFilter == st,
                                onClick = { statusFilter = st },
                                label = { Text(st) },
                                modifier = Modifier.testTag("site_status_filter_${st.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                }
            }

            if (filteredSites.isEmpty()) {
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
                        Icon(
                            imageVector = Icons.Default.Apartment,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(48.dp)
                        )
                        Text("No sites found", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Tap '+ Add Construction Site' to create a new project.", fontSize = 12.sp, color = Slate500)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredSites) { site ->
                        val siteRecords = allAttendance.filter { it.siteCode == site.siteCode }
                        val totalLabourCost = siteRecords.sumOf { it.totalEarned }

                        SiteItemCard(
                            site = site,
                            attendanceCount = siteRecords.size,
                            totalCost = totalLabourCost,
                            onEdit = { editingSite = site },
                            onDelete = { siteToDelete = site }
                        )
                    }
                }
            }
        }
    }

    // Add Site Dialog
    if (showAddDialog) {
        val nextCode = remember(sites) {
            val maxNum = sites.mapNotNull { it.siteCode.removePrefix("S-").toIntOrNull() }.maxOrNull() ?: 0
            String.format("S-%02d", maxNum + 1)
        }
        SiteFormDialog(
            initialSite = Site(siteCode = nextCode, siteName = ""),
            isNew = true,
            onDismiss = { showAddDialog = false },
            onSave = { newSite ->
                viewModel.addSite(newSite) {
                    showAddDialog = false
                }
            }
        )
    }

    // Edit Site Dialog
    if (editingSite != null) {
        SiteFormDialog(
            initialSite = editingSite!!,
            isNew = false,
            onDismiss = { editingSite = null },
            onSave = { updated ->
                viewModel.updateSite(updated) {
                    editingSite = null
                }
            }
        )
    }

    // Delete Site Confirmation
    if (siteToDelete != null) {
        val targetSite = siteToDelete!!
        val isMy = currentLanguage == AppLanguage.MYANMAR
        val recordCount = remember(targetSite, allAttendance) {
            allAttendance.count { it.siteCode == targetSite.siteCode }
        }
        AlertDialog(
            onDismissRequest = { siteToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AdvanceRed, modifier = Modifier.size(32.dp)) },
            title = {
                Text(
                    text = if (isMy) "ဆိုဒ်စာရင်း ဖျက်ရန်" else "Delete Site?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isMy)
                            "${targetSite.siteName} (${targetSite.siteCode}) ကို ဆိုဒ်စာရင်းမှ ဖျက်မည်မှာ သေချာပါသလား။"
                        else
                            "Are you sure you want to delete ${targetSite.siteName} (${targetSite.siteCode})?",
                        fontSize = 13.sp
                    )
                    if (recordCount > 0) {
                        Surface(
                            color = AdvanceRed.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isMy)
                                    "⚠️ သတိပေးချက်- ဤဆိုဒ်တွင် တက်ရောက်မှု မှတ်တမ်းပေါင်း $recordCount ခု ရှိနေပါသည်။ ဆိုဒ်ကို ဖျက်လိုက်ပါက ၎င်းနှင့် သက်ဆိုင်သော မှတ်တမ်းအားလုံးပါ ပျက်သွားမည်ဖြစ်သည်။"
                                else
                                    "⚠️ Notice: This site currently has $recordCount attendance log(s). Deleting will also remove all its attendance records.",
                                color = AdvanceRed,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSite(targetSite, deleteAttendanceRecords = true)
                        siteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdvanceRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_delete_site_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isMy) "အတည်ပြု ဖျက်မည်" else "Delete Site & Logs",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { siteToDelete = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isMy) "မဖျက်ပါ" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun SiteItemCard(
    site: Site,
    attendanceCount: Int,
    totalCost: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("site_card_${site.siteCode}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = OtBlueLight
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apartment,
                            contentDescription = null,
                            tint = OtBlue,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(24.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = site.siteName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            StatusBadge(status = site.status)
                        }
                        Text(
                            text = "Site Code: ${site.siteCode}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate500
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("edit_site_btn_${site.siteCode}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Site", tint = OtBlue, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("delete_site_btn_${site.siteCode}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Site", tint = AdvanceRed, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (site.location.isNotBlank()) {
                    Text(
                        text = "📍 ${site.location}",
                        fontSize = 12.sp,
                        color = Slate600
                    )
                }
                if (site.supervisor.isNotBlank()) {
                    Text(
                        text = "👤 Sup: ${site.supervisor}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Logs: $attendanceCount entries",
                        fontSize = 11.sp,
                        color = Slate600
                    )
                    Text(
                        text = "Total Labour: " + com.example.util.ExcelExportHelper.formatCurrency(totalCost),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WagesGreenDark
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteFormDialog(
    initialSite: Site,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (Site) -> Unit
) {
    var siteCode by remember { mutableStateOf(initialSite.siteCode) }
    var siteName by remember { mutableStateOf(initialSite.siteName) }
    var location by remember { mutableStateOf(initialSite.location) }
    var supervisor by remember { mutableStateOf(initialSite.supervisor) }
    var status by remember { mutableStateOf(initialSite.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Add Construction Site" else "Edit Site Details", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = siteCode,
                    onValueChange = { siteCode = it },
                    label = { Text("Site Code (e.g. S-01)") },
                    enabled = isNew,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("site_code_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = siteName,
                    onValueChange = { siteName = it },
                    label = { Text("Site Name *") },
                    placeholder = { Text("e.g. Metro Tower Phase 2") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("site_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    placeholder = { Text("e.g. Expressway Sector 62") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = supervisor,
                    onValueChange = { supervisor = it },
                    label = { Text("Site Supervisor Name") },
                    placeholder = { Text("e.g. John Miller") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Status:", fontSize = 12.sp, color = Slate600)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Ongoing", "Completed", "On Hold").forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (siteCode.isNotBlank() && siteName.isNotBlank()) {
                        onSave(
                            Site(
                                siteCode = siteCode.trim(),
                                siteName = siteName.trim(),
                                location = location.trim(),
                                supervisor = supervisor.trim(),
                                status = status
                            )
                        )
                    }
                },
                enabled = siteCode.isNotBlank() && siteName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ConstructionGold),
                modifier = Modifier.testTag("save_site_dialog_button")
            ) {
                Text(if (isNew) "Add Site" else "Save Changes", color = Slate900, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
