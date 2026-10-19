package com.example.ui.screens.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.TradeIcon
import com.example.ui.theme.*
import com.example.ui.viewmodel.LabourViewModel
import com.example.util.AppLanguage
import com.example.util.CategoryManager
import com.example.util.ExcelExportHelper
import com.example.util.LanguageManager
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvanceLedgerScreen(
    viewModel: LabourViewModel,
    modifier: Modifier = Modifier
) {
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    val isMy = currentLanguage == AppLanguage.MYANMAR
    val workers by viewModel.allWorkers.collectAsState()
    val sites by viewModel.allSites.collectAsState()
    val allAttendance by viewModel.allAttendance.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Worker Summary, 1 = Site Summary, 2 = Advance Log
    var searchQuery by remember { mutableStateOf("") }

    val totalEarned = allAttendance.sumOf { it.totalEarned }
    val totalAdvance = allAttendance.sumOf { it.advanceToday }
    val totalPaid = allAttendance.sumOf { it.paidToday }
    val totalBalance = totalEarned - totalAdvance - totalPaid

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("advance_ledger_screen")
    ) {
        // Top KPI Banner
        Surface(
            color = Slate900,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isMy) "ကြိုထုတ်ငွေနှင့် လက်ကျန်စာရင်း" else "Advance & Balance Ledger",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Financial Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Slate800, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(if (isMy) "စုစုပေါင်းရငွေ" else "Total Earned", fontSize = 10.sp, color = Slate400)
                        Text(ExcelExportHelper.formatCurrency(totalEarned), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WagesGreen)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Slate800, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(if (isMy) "ကြိုထုတ်ငွေ" else "Total Advance", fontSize = 10.sp, color = Slate400)
                        Text(ExcelExportHelper.formatCurrency(totalAdvance), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AdvanceRedLight)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Slate800, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(if (isMy) "ပေးငွေ" else "Total Paid", fontSize = 10.sp, color = Slate400)
                        Text(ExcelExportHelper.formatCurrency(totalPaid), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OtBlueLight)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Slate800, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(if (isMy) "ပေးရန်ကျန်" else "Outstanding", fontSize = 10.sp, color = Slate400)
                        Text(ExcelExportHelper.formatCurrency(totalBalance), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ConstructionGold)
                    }
                }

                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Slate900,
                    contentColor = ConstructionGold
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(if (isMy) "လုပ်သားတစ်ဦးချင်း" else "Worker Ledger", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(if (isMy) "ဆိုဒ်အလိုက်ရှင်းတမ်း" else "Site Summary", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text(if (isMy) "ကြိုထုတ်ငွေမှတ်တမ်း" else "Advance Log", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        // Search Filter
        PaddingValues(16.dp).let {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by worker or site...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("ledger_search_field"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
        }

        // Content
        when (selectedTab) {
            0 -> {
                // WORKER-WISE BALANCE SHEET
                val workerSummaries = remember(workers, allAttendance, searchQuery) {
                    val grouped = allAttendance.groupBy { it.workerId }
                    workers.map { w ->
                        val list = grouped[w.workerId] ?: emptyList()
                        val days = list.sumOf { it.totalDayUnits }
                        val ot = list.sumOf { it.otHours }
                        val earned = list.sumOf { it.totalEarned }
                        val adv = list.sumOf { it.advanceToday }
                        val paid = list.sumOf { it.paidToday }
                        val bal = earned - adv - paid
                        WorkerLedgerItem(w.workerId, w.name, w.workerType, w.dailyRate, days, ot, earned, adv, paid, bal, list)
                    }.filter { item ->
                        val myanmarTrade = CategoryManager.getMyanmarName(item.trade)
                        searchQuery.isBlank() ||
                                item.name.contains(searchQuery, ignoreCase = true) ||
                                item.workerId.contains(searchQuery, ignoreCase = true) ||
                                item.trade.contains(searchQuery, ignoreCase = true) ||
                                myanmarTrade.contains(searchQuery, ignoreCase = true)
                    }.sortedWith { a, b -> com.example.data.model.compareWorkerIds(a.workerId, b.workerId) }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(workerSummaries) { item ->
                        WorkerLedgerCard(item = item)
                    }
                }
            }
            1 -> {
                // SITE-WISE SUMMARY
                val siteSummaries = remember(sites, allAttendance, searchQuery) {
                    val grouped = allAttendance.groupBy { it.siteCode }
                    sites.map { s ->
                        val list = grouped[s.siteCode] ?: emptyList()
                        val days = list.sumOf { it.totalDayUnits }
                        val ot = list.sumOf { it.otHours }
                        val earned = list.sumOf { it.totalEarned }
                        val adv = list.sumOf { it.advanceToday }
                        val paid = list.sumOf { it.paidToday }
                        val bal = earned - adv - paid
                        SiteLedgerItem(s.siteCode, s.siteName, s.supervisor, s.status, days, ot, earned, adv, paid, bal, list.size)
                    }.filter { item ->
                        searchQuery.isBlank() ||
                                item.siteName.contains(searchQuery, ignoreCase = true) ||
                                item.siteCode.contains(searchQuery, ignoreCase = true) ||
                                item.supervisor.contains(searchQuery, ignoreCase = true)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(siteSummaries) { siteItem ->
                        SiteLedgerCard(item = siteItem)
                    }
                }
            }
            2 -> {
                // DETAILED CHRONOLOGICAL ADVANCES LOG
                val advanceTransactions = remember(allAttendance, searchQuery) {
                    allAttendance.filter { it.advanceToday > 0 || it.paidToday > 0 }
                        .filter {
                            searchQuery.isBlank() ||
                                    it.workerName.contains(searchQuery, ignoreCase = true) ||
                                    it.workerId.contains(searchQuery, ignoreCase = true) ||
                                    it.siteName.contains(searchQuery, ignoreCase = true)
                        }
                }

                if (advanceTransactions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No advances or direct payments logged yet.", color = Slate500)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(advanceTransactions) { r ->
                            TransactionCard(
                                record = r,
                                onEdit = { viewModel.editRecord(r) }
                            )
                        }
                    }
                }
            }
        }
    }
}

data class WorkerLedgerItem(
    val workerId: String,
    val name: String,
    val trade: String,
    val dailyRate: Double,
    val totalDays: Double,
    val totalOt: Double,
    val totalEarned: Double,
    val totalAdvance: Double,
    val totalPaid: Double,
    val balanceDue: Double,
    val history: List<com.example.data.model.AttendanceRecord>
)

data class SiteLedgerItem(
    val siteCode: String,
    val siteName: String,
    val supervisor: String,
    val status: String,
    val totalDays: Double,
    val totalOt: Double,
    val totalEarned: Double,
    val totalAdvance: Double,
    val totalPaid: Double,
    val balanceDue: Double,
    val entriesCount: Int
)

@Composable
fun WorkerLedgerCard(item: WorkerLedgerItem) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("worker_ledger_${item.workerId}"),
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
                    TradeIcon(trade = item.trade)
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = ConstructionGold,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = item.workerId,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = item.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Text(
                            text = "${CategoryManager.getDisplayName(item.trade)} • ${String.format(Locale.US, "%.1f", item.totalDays)} days",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Net Balance",
                        fontSize = 10.sp,
                        color = Slate500
                    )
                    Text(
                        text = ExcelExportHelper.formatCurrency(item.balanceDue),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = if (item.balanceDue > 0) ConstructionGoldDark else WagesGreenDark
                    )
                }
            }

            // Summary metrics grid
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
                    Column {
                        Text("Earned", fontSize = 10.sp, color = Slate500)
                        Text(ExcelExportHelper.formatCurrency(item.totalEarned), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WagesGreenDark)
                    }
                    Column {
                        Text("Advance Taken", fontSize = 10.sp, color = Slate500)
                        Text(ExcelExportHelper.formatCurrency(item.totalAdvance), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdvanceRed)
                    }
                    Column {
                        Text("Paid Out", fontSize = 10.sp, color = Slate500)
                        Text(ExcelExportHelper.formatCurrency(item.totalPaid), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OtBlue)
                    }
                    Column {
                        Text("OT Hours", fontSize = 10.sp, color = Slate500)
                        Text("${item.totalOt}h", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Expandable details
            if (expanded) {
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("Recent Daily Logs (${item.history.size}):", fontSize = 11.sp, fontWeight = FontWeight.Bold)

                if (item.history.isEmpty()) {
                    Text("No attendance logs recorded for this worker yet.", fontSize = 11.sp, color = Slate500)
                } else {
                    item.history.take(5).forEach { rec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${rec.date} (${rec.siteCode})", fontSize = 11.sp, color = Slate600)
                            Text(
                                "Earned: ${ExcelExportHelper.formatCurrency(rec.totalEarned)} | Adv: ${ExcelExportHelper.formatCurrency(rec.advanceToday)} | Bal: ${ExcelExportHelper.formatCurrency(rec.balance)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SiteLedgerCard(item: SiteLedgerItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("site_ledger_${item.siteCode}"),
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
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.siteName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        StatusBadge(status = item.status)
                    }
                    Text(
                        text = "Code: ${item.siteCode} • Sup: ${item.supervisor} • ${item.entriesCount} entries",
                        fontSize = 11.sp,
                        color = Slate500
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Net Site Balance", fontSize = 10.sp, color = Slate500)
                    Text(
                        text = ExcelExportHelper.formatCurrency(item.balanceDue),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = ConstructionGoldDark
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
                    Column {
                        Text("Total Labour", fontSize = 10.sp, color = Slate500)
                        Text(ExcelExportHelper.formatCurrency(item.totalEarned), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WagesGreenDark)
                    }
                    Column {
                        Text("Advances", fontSize = 10.sp, color = Slate500)
                        Text(ExcelExportHelper.formatCurrency(item.totalAdvance), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdvanceRed)
                    }
                    Column {
                        Text("Paid Out", fontSize = 10.sp, color = Slate500)
                        Text(ExcelExportHelper.formatCurrency(item.totalPaid), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OtBlue)
                    }
                    Column {
                        Text("Man-Days", fontSize = 10.sp, color = Slate500)
                        Text(String.format(Locale.US, "%.1f", item.totalDays), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionCard(
    record: com.example.data.model.AttendanceRecord,
    onEdit: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onEdit != null) Modifier.clickable(onClick = onEdit) else Modifier),
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
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(record.workerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    StatusBadge(status = record.siteCode)
                }
                Text("Date: ${record.date} • ${record.workerId}", fontSize = 11.sp, color = Slate500)
                if (record.remark.isNotBlank()) {
                    Text("Remark: ${record.remark}", fontSize = 10.sp, color = Slate600)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    if (record.advanceToday > 0) {
                        Text(
                            text = "Adv: ${ExcelExportHelper.formatCurrency(record.advanceToday)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = AdvanceRed
                        )
                    }
                    if (record.paidToday > 0) {
                        Text(
                            text = "Paid: ${ExcelExportHelper.formatCurrency(record.paidToday)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = OtBlue
                        )
                    }
                }

                if (onEdit != null) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Attendance",
                            tint = OtBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
