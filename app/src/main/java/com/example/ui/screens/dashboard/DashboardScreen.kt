package com.example.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecord
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.TradeIcon
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.LabourViewModel
import com.example.util.AppLanguage
import com.example.util.CategoryManager
import com.example.util.ExcelExportHelper
import com.example.util.LanguageManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: LabourViewModel,
    modifier: Modifier = Modifier
) {
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    val workers by viewModel.allWorkers.collectAsState()
    val activeWorkers by viewModel.activeWorkers.collectAsState()
    val sites by viewModel.allSites.collectAsState()
    val activeSites by viewModel.activeSites.collectAsState()
    val attendanceList by viewModel.allAttendance.collectAsState()
    val todayDate = viewModel.todayDateString

    val todayAttendance = remember(attendanceList, todayDate) {
        attendanceList.filter { it.date == todayDate }
            .sortedWith { a, b -> com.example.data.model.compareWorkerIds(a.workerId, b.workerId) }
    }

    var showBackupDialog by remember { mutableStateOf(false) }
    var showClearSampleDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<AttendanceRecord?>(null) }

    // Top Site prioritization per requirement (3)
    val topSite = sites.firstOrNull()
    val topSiteTodayRecords = remember(todayAttendance, topSite) {
        if (topSite != null) todayAttendance.filter { it.siteCode == topSite.siteCode } else emptyList()
    }

    val totalWagesEarned = attendanceList.sumOf { it.totalEarned }
    val totalAdvanceGiven = attendanceList.sumOf { it.advanceToday }
    val totalWagesPaid = attendanceList.sumOf { it.paidToday }
    val netBalanceDue = totalWagesEarned - totalAdvanceGiven - totalWagesPaid

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_banner_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(WagesGreen)
                                )
                                Text(
                                    text = "SITE MANAGEMENT READY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ConstructionGold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Construction Labour POS",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Slate800
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = ConstructionGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = todayDate,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Quick POS attendance logging with real-time wage calculation, advance ledger & Excel export.",
                        fontSize = 13.sp,
                        color = Slate300,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary POS action button
                    Button(
                        onClick = {
                            viewModel.resetForm()
                            viewModel.navigateTo(AppScreen.POS_ENTRY)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pos_fast_entry_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ConstructionGold)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = Slate900,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.MYANMAR) "အမြန်စာရင်းသွင်း စတင်မည်" else "START POS ATTENDANCE ENTRY",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    }
                }
            }
        }

        // Top Priority Site Card (Requirement 3: prioritize top site)
        if (topSite != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("top_site_banner_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ConstructionGold
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Slate900,
                                        modifier = Modifier.padding(4.dp).size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = if (currentLanguage == AppLanguage.MYANMAR) "ဦးစားပေး ဆောက်လုပ်ရေးဆိုဒ်" else "Top Priority Construction Site",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ConstructionGoldDark
                                    )
                                    Text(
                                        text = topSite.siteName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            StatusBadge(status = topSite.status)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (topSite.supervisor.isNotBlank()) {
                                Text("👤 ${topSite.supervisor}", fontSize = 12.sp, color = Slate600)
                            }
                            Text(
                                text = if (currentLanguage == AppLanguage.MYANMAR) "ယနေ့တက်ရောက်: ${topSiteTodayRecords.size} ဦး" else "Today: ${topSiteTodayRecords.size} workers",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WagesGreenDark
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.selectSite(topSite)
                                    viewModel.navigateTo(AppScreen.POS_ENTRY)
                                },
                                modifier = Modifier.weight(1f).testTag("top_site_log_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = ConstructionGold)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Slate900, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (currentLanguage == AppLanguage.MYANMAR) "ဤဆိုဒ်အတွက် စာရင်းသွင်းမည်" else "Log for this Site", color = Slate900, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Quick Backup & Clear Data Row (Requirements 4 & 5)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showBackupDialog = true },
                    modifier = Modifier.weight(1f).testTag("dash_backup_btn"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = OtBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (currentLanguage == AppLanguage.MYANMAR) "Drive / အရန်သိမ်း" else "Backup Data", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.navigateTo(AppScreen.WORKER_MASTER) },
                    modifier = Modifier.weight(1f).testTag("dash_add_worker_quick_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ConstructionGold),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Slate900, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (currentLanguage == AppLanguage.MYANMAR) "+ လုပ်သားအသစ်" else "+ Add Worker", color = Slate900, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Financial KPI Cards Row
        item {
            Text(
                text = if (currentLanguage == AppLanguage.MYANMAR) "ဘဏ္ဍာရေးနှင့် လုပ်သားအခြေအနေ" else "Financial & Labour Overview",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = if (currentLanguage == AppLanguage.MYANMAR) "စုစုပေါင်းရငွေ" else "Total Earned",
                    value = ExcelExportHelper.formatCurrency(totalWagesEarned),
                    icon = Icons.Default.MonetizationOn,
                    iconTint = WagesGreen,
                    modifier = Modifier.weight(1f),
                    subtitle = if (currentLanguage == AppLanguage.MYANMAR) "ဆိုဒ်အားလုံး" else "All Sites"
                )
                StatCard(
                    title = if (currentLanguage == AppLanguage.MYANMAR) "ပေးရန်ကျန်ငွေ" else "Net Balance Due",
                    value = ExcelExportHelper.formatCurrency(netBalanceDue),
                    icon = Icons.Default.AccountBalanceWallet,
                    iconTint = if (netBalanceDue > 0) ConstructionGoldDark else WagesGreen,
                    modifier = Modifier.weight(1f),
                    subtitle = if (netBalanceDue > 0) (if (currentLanguage == AppLanguage.MYANMAR) "ကျန်ရှိ" else "Pending") else (if (currentLanguage == AppLanguage.MYANMAR) "ရှင်းပြီး" else "Settled"),
                    badgeColor = if (netBalanceDue > 0) SafetyOrange else WagesGreen
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = if (currentLanguage == AppLanguage.MYANMAR) "ကြိုထုတ်ငွေ" else "Advance Given",
                    value = ExcelExportHelper.formatCurrency(totalAdvanceGiven),
                    icon = Icons.Default.ArrowOutward,
                    iconTint = AdvanceRed,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = if (currentLanguage == AppLanguage.MYANMAR) "ပေးချေပြီးငွေ" else "Paid Today/Past",
                    value = ExcelExportHelper.formatCurrency(totalWagesPaid),
                    icon = Icons.Default.CheckCircle,
                    iconTint = OtBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Operational Counts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = if (currentLanguage == AppLanguage.MYANMAR) "လက်ရှိလုပ်သား" else "Active Workers",
                    value = "${activeWorkers.size} / ${workers.size}",
                    icon = Icons.Default.People,
                    iconTint = ConstructionGold,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = if (currentLanguage == AppLanguage.MYANMAR) "လက်ရှိဆိုဒ်များ" else "Active Sites",
                    value = "${activeSites.size}",
                    icon = Icons.Default.Apartment,
                    iconTint = OtBlue,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = if (currentLanguage == AppLanguage.MYANMAR) "ယနေ့မှတ်တမ်း" else "Today's Logs",
                    value = "${todayAttendance.size}",
                    icon = Icons.Default.FactCheck,
                    iconTint = WagesGreen,
                    modifier = Modifier.weight(1f),
                    subtitle = "${activeWorkers.size - todayAttendance.size} left"
                )
            }
        }

        // Quick Navigation Grid
        item {
            Text(
                text = if (currentLanguage == AppLanguage.MYANMAR) "အမြန်သွားရန်" else "Quick Navigation",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickNavButton(
                    title = if (currentLanguage == AppLanguage.MYANMAR) "တက်ရောက်မှု" else "Attendance List",
                    icon = Icons.Default.FormatListBulleted,
                    color = SafetyOrange,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.ATTENDANCE_LIST) }
                )
                QuickNavButton(
                    title = if (currentLanguage == AppLanguage.MYANMAR) "ငွေစာရင်း" else "Advance Ledger",
                    icon = Icons.Default.AccountBalanceWallet,
                    color = OtBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.ADVANCE_LEDGER) }
                )
                QuickNavButton(
                    title = if (currentLanguage == AppLanguage.MYANMAR) "Excel ထုတ်ယူ" else "Excel Export",
                    icon = Icons.Default.FileDownload,
                    color = WagesGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.EXCEL_EXPORT) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickNavButton(
                    title = if (currentLanguage == AppLanguage.MYANMAR) "လုပ်သားစာရင်း" else "Worker Master",
                    icon = Icons.Default.PersonAdd,
                    color = ConstructionGoldDark,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.WORKER_MASTER) }
                )
                QuickNavButton(
                    title = if (currentLanguage == AppLanguage.MYANMAR) "ဆိုဒ်စာရင်း" else "Site Master",
                    icon = Icons.Default.Business,
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.SITE_MASTER) }
                )
            }
        }

        // Today's Attendance Overview
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Logged Workers (${todayAttendance.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = { viewModel.navigateTo(AppScreen.ATTENDANCE_LIST) },
                    modifier = Modifier.testTag("view_all_attendance_button")
                ) {
                    Text("View All")
                }
            }
        }

        if (todayAttendance.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No workers logged yet for today",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Start POS Attendance Entry' to log site workers.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(todayAttendance.take(5)) { record ->
                AttendanceSummaryCard(
                    record = record,
                    onEdit = { viewModel.editRecord(record) },
                    onDelete = { recordToDelete = record }
                )
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

    // Delete Attendance Record Dialog from Dashboard
    if (recordToDelete != null) {
        val target = recordToDelete!!
        val isMy = currentLanguage == AppLanguage.MYANMAR
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AdvanceRed, modifier = Modifier.size(32.dp)) },
            title = {
                Text(
                    text = if (isMy) "တက်ရောက်မှု မှတ်တမ်း ဖျက်ရန်" else "Delete Attendance Record?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isMy)
                            "${target.workerName} (${target.workerId}) ၏ ${target.date} ရက်စွဲပါ တက်ရောက်မှု မှတ်တမ်းကို ဖျက်မည်မှာ သေချာပါသလား။"
                        else
                            "Are you sure you want to delete the attendance record for ${target.workerName} on ${target.date}?",
                        fontSize = 13.sp
                    )
                    Surface(
                        color = AdvanceRed.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🏢 ဆိုဒ်: ${target.siteName}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("⏰ အလုပ်ချိန်: နံနက် (${target.morningAttendance}) • ညနေ (${target.eveningAttendance}) • OT: ${target.otHours}h", fontSize = 12.sp)
                            Text("💰 စုစုပေါင်းရငွေ: ${ExcelExportHelper.formatCurrency(target.totalEarned)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WagesGreenDark)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAttendance(target)
                        recordToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdvanceRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_delete_dash_attendance_btn")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isMy) "အတည်ပြု ဖျက်မည်" else "Confirm Delete", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { recordToDelete = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isMy) "မဖျက်ပါ" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun QuickNavButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("nav_btn_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AttendanceSummaryCard(
    record: AttendanceRecord,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("attendance_card_${record.workerId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f).clickable(onClick = onEdit)
            ) {
                TradeIcon(trade = record.workerType)
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = ConstructionGold,
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = record.workerId,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Text(
                            text = record.workerName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${CategoryManager.getDisplayName(record.workerType)} • ${record.siteName}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusBadge(status = "AM: ${record.morningAttendance}")
                        StatusBadge(status = "PM: ${record.eveningAttendance}")
                        if (record.otHours > 0) {
                            StatusBadge(status = "OT: ${record.otHours}h")
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.clickable(onClick = onEdit)
                ) {
                    Text(
                        text = ExcelExportHelper.formatCurrency(record.totalEarned),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = WagesGreenDark
                    )
                    if (record.advanceToday > 0 || record.paidToday > 0) {
                        Text(
                            text = "Adv: ${ExcelExportHelper.formatCurrency(record.advanceToday)}",
                            fontSize = 11.sp,
                            color = AdvanceRed
                        )
                    }
                    Text(
                        text = "Bal: ${ExcelExportHelper.formatCurrency(record.balance)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (record.balance > 0) ConstructionGoldDark else Slate600
                    )
                }

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).testTag("dash_delete_attendance_${record.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = AdvanceRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
