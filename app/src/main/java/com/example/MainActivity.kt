package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.attendance.AttendanceEntryPosScreen
import com.example.ui.screens.attendance.AttendanceListScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.export.ExportScreen
import com.example.ui.screens.ledger.AdvanceLedgerScreen
import com.example.ui.screens.sites.SiteMasterScreen
import com.example.ui.screens.workers.WorkerMasterScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.LabourViewModel
import com.example.util.AppLanguage
import com.example.util.LanguageManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppRoot(viewModel: LabourViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    var showMoreMenu by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showClearSampleDialog by remember { mutableStateOf(false) }

    // Android back handler to navigate back to Dashboard
    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
        viewModel.navigateTo(AppScreen.DASHBOARD)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_root_scaffold"),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ConstructionGold
                        ) {
                            Icon(
                                imageVector = Icons.Default.Construction,
                                contentDescription = null,
                                tint = Slate900,
                                modifier = Modifier
                                    .padding(5.dp)
                                    .size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = LanguageManager.getString("app_title"),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = when (currentScreen) {
                                    AppScreen.DASHBOARD -> LanguageManager.getString("dashboard_subtitle")
                                    AppScreen.POS_ENTRY -> LanguageManager.getString("pos_subtitle")
                                    AppScreen.ATTENDANCE_LIST -> LanguageManager.getString("log_subtitle")
                                    AppScreen.WORKER_MASTER -> LanguageManager.getString("worker_subtitle")
                                    AppScreen.SITE_MASTER -> LanguageManager.getString("site_subtitle")
                                    AppScreen.ADVANCE_LEDGER -> LanguageManager.getString("ledger_subtitle")
                                    AppScreen.EXCEL_EXPORT -> LanguageManager.getString("export_subtitle")
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Quick Backup & Restore Icon Action
                    IconButton(
                        onClick = { showBackupDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("topbar_backup_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Backup and Restore",
                            tint = OtBlue
                        )
                    }

                    // Quick In-App Language Switcher Chip (English / မြန်မာ)
                    Surface(
                        modifier = Modifier
                            .clickable { LanguageManager.toggleLanguage() }
                            .testTag("toggle_language_btn"),
                        shape = RoundedCornerShape(20.dp),
                        color = if (currentLanguage == AppLanguage.MYANMAR) ConstructionGold else MaterialTheme.colorScheme.surfaceVariant,
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language",
                                tint = if (currentLanguage == AppLanguage.MYANMAR) Slate900 else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (currentLanguage == AppLanguage.MYANMAR) "မြန်မာ" else "EN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentLanguage == AppLanguage.MYANMAR) Slate900 else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("topbar_menu_button")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (currentLanguage == AppLanguage.MYANMAR) "💾 ဒေတာ အရန်သိမ်းခြင်းနှင့် ပြန်ယူခြင်း" else "💾 Backup & Restore (Drive/Local)") },
                                leadingIcon = { Icon(Icons.Default.CloudSync, contentDescription = null, tint = OtBlue) },
                                onClick = {
                                    showBackupDialog = true
                                    showMoreMenu = false
                                }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text(if (currentLanguage == AppLanguage.MYANMAR) "လုပ်သားစာရင်း" else "Worker Master") },
                                leadingIcon = { Icon(Icons.Default.People, contentDescription = null) },
                                onClick = {
                                    viewModel.navigateTo(AppScreen.WORKER_MASTER)
                                    showMoreMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (currentLanguage == AppLanguage.MYANMAR) "ဆောက်လုပ်ရေးဆိုဒ်များ" else "Site Master") },
                                leadingIcon = { Icon(Icons.Default.Apartment, contentDescription = null) },
                                onClick = {
                                    viewModel.navigateTo(AppScreen.SITE_MASTER)
                                    showMoreMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (currentLanguage == AppLanguage.MYANMAR) "ကြိုထုတ်ငွေစာရင်း" else "Advance Ledger") },
                                leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                                onClick = {
                                    viewModel.navigateTo(AppScreen.ADVANCE_LEDGER)
                                    showMoreMenu = false
                                }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text(if (currentLanguage == AppLanguage.MYANMAR) "အစီရင်ခံစာ ထုတ်ယူမည် (PDF/Excel)" else "Export Report (PDF/Excel)") },
                                leadingIcon = { Icon(Icons.Default.Summarize, contentDescription = null) },
                                onClick = {
                                    viewModel.navigateTo(AppScreen.EXCEL_EXPORT)
                                    showMoreMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.DASHBOARD,
                    onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text(LanguageManager.getString("nav_home"), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = PrimaryContainerLight),
                    modifier = Modifier.testTag("nav_item_dashboard")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.WORKER_MASTER,
                    onClick = { viewModel.navigateTo(AppScreen.WORKER_MASTER) },
                    icon = { Icon(Icons.Default.People, contentDescription = "Workers") },
                    label = { Text(LanguageManager.getString("nav_workers"), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = PrimaryContainerLight),
                    modifier = Modifier.testTag("nav_item_workers")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.POS_ENTRY,
                    onClick = { viewModel.navigateTo(AppScreen.POS_ENTRY) },
                    icon = { Icon(Icons.Default.TouchApp, contentDescription = "POS Entry") },
                    label = { Text(LanguageManager.getString("nav_pos"), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = PrimaryContainerLight),
                    modifier = Modifier.testTag("nav_item_pos")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.ATTENDANCE_LIST,
                    onClick = { viewModel.navigateTo(AppScreen.ATTENDANCE_LIST) },
                    icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = "Log") },
                    label = { Text(LanguageManager.getString("nav_log"), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = PrimaryContainerLight),
                    modifier = Modifier.testTag("nav_item_log")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.ADVANCE_LEDGER,
                    onClick = { viewModel.navigateTo(AppScreen.ADVANCE_LEDGER) },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Ledger") },
                    label = { Text(LanguageManager.getString("nav_ledger"), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = PrimaryContainerLight),
                    modifier = Modifier.testTag("nav_item_ledger")
                )
            }
        }
    ) { innerPadding ->
        if (currentScreen != AppScreen.DASHBOARD) {
            androidx.activity.compose.BackHandler {
                viewModel.navigateTo(AppScreen.DASHBOARD)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                AppScreen.POS_ENTRY -> AttendanceEntryPosScreen(viewModel = viewModel)
                AppScreen.ATTENDANCE_LIST -> AttendanceListScreen(viewModel = viewModel)
                AppScreen.WORKER_MASTER -> WorkerMasterScreen(viewModel = viewModel)
                AppScreen.SITE_MASTER -> SiteMasterScreen(viewModel = viewModel)
                AppScreen.ADVANCE_LEDGER -> AdvanceLedgerScreen(viewModel = viewModel)
                AppScreen.EXCEL_EXPORT -> ExportScreen(viewModel = viewModel)
            }
        }
    }

    if (showBackupDialog) {
        com.example.ui.components.BackupRestoreDialog(
            viewModel = viewModel,
            onDismiss = { showBackupDialog = false }
        )
    }

    if (showClearSampleDialog) {
        com.example.ui.components.ClearSampleDialog(
            viewModel = viewModel,
            onDismiss = { showClearSampleDialog = false }
        )
    }
}
