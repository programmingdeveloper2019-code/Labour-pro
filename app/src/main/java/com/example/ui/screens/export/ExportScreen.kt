package com.example.ui.screens.export

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecord
import com.example.data.model.Site
import com.example.data.model.Worker
import com.example.ui.components.StatusBadge
import com.example.ui.components.TradeIcon
import com.example.ui.theme.*
import com.example.ui.viewmodel.LabourViewModel
import com.example.util.AppLanguage
import com.example.util.ExcelExportHelper
import com.example.util.LanguageManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DateFilterMode {
    ALL,
    TODAY,
    SINGLE_DATE,
    DATE_RANGE
}

enum class ExportFileFormat {
    PDF,
    XLSX,
    CSV
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: LabourViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val allAttendance by viewModel.allAttendance.collectAsState()
    val allWorkers by viewModel.allWorkers.collectAsState()
    val allSites by viewModel.allSites.collectAsState()
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    val isMy = currentLanguage == AppLanguage.MYANMAR

    // Report Type: 0 = Daily Attendance, 1 = Worker Summary, 2 = Site Summary
    var exportType by remember { mutableStateOf(0) }

    // File Format: PDF, XLSX, CSV
    var selectedFormat by remember { mutableStateOf(ExportFileFormat.PDF) }

    // Date Filters - Default to TODAY per requirement (2)
    var dateFilterMode by remember { mutableStateOf(DateFilterMode.TODAY) }
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var singleFilterDate by remember { mutableStateOf(todayStr) }
    var rangeStartDate by remember { mutableStateOf(todayStr) }
    var rangeEndDate by remember { mutableStateOf(todayStr) }

    // Site Filter - Default to Top Site per requirement (3)
    var selectedSiteFilterCode by remember { mutableStateOf<String?>(null) }
    var showSiteFilterDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(allSites) {
        if (selectedSiteFilterCode == null && allSites.isNotEmpty()) {
            selectedSiteFilterCode = allSites.first().siteCode
        }
    }

    // UI View Mode: "table" (interactive table) or "raw" (raw code preview)
    var viewMode by remember { mutableStateOf("table") }

    // Calendar for DatePickers
    val cal = remember { Calendar.getInstance() }

    // Date Picker Dialogs
    val singleDatePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                singleFilterDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                dateFilterMode = DateFilterMode.SINGLE_DATE
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    val rangeStartDatePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                rangeStartDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                dateFilterMode = DateFilterMode.DATE_RANGE
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    val rangeEndDatePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                rangeEndDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                dateFilterMode = DateFilterMode.DATE_RANGE
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    // Filter attendance records based on Date and Site
    val filteredAttendance = remember(
        allAttendance,
        dateFilterMode,
        singleFilterDate,
        rangeStartDate,
        rangeEndDate,
        selectedSiteFilterCode
    ) {
        allAttendance.filter { record ->
            val matchesDate = when (dateFilterMode) {
                DateFilterMode.ALL -> true
                DateFilterMode.TODAY -> record.date == todayStr
                DateFilterMode.SINGLE_DATE -> record.date == singleFilterDate
                DateFilterMode.DATE_RANGE -> record.date >= rangeStartDate && record.date <= rangeEndDate
            }
            val matchesSite = selectedSiteFilterCode == null || record.siteCode == selectedSiteFilterCode
            matchesDate && matchesSite
        }.sortedWith { a, b ->
            val d = a.date.compareTo(b.date)
            if (d != 0) d else com.example.data.model.compareWorkerIds(a.workerId, b.workerId)
        }
    }

    // Date Period Display Label
    val datePeriodLabel = remember(dateFilterMode, singleFilterDate, rangeStartDate, rangeEndDate, isMy) {
        when (dateFilterMode) {
            DateFilterMode.ALL -> if (isMy) "ရက်စွဲအားလုံး" else "All Dates"
            DateFilterMode.TODAY -> if (isMy) "ယနေ့ ($todayStr)" else "Today ($todayStr)"
            DateFilterMode.SINGLE_DATE -> if (isMy) "သတ်မှတ်ရက်စွဲ: $singleFilterDate" else "Date: $singleFilterDate"
            DateFilterMode.DATE_RANGE -> if (isMy) "$rangeStartDate မှ $rangeEndDate အထိ" else "$rangeStartDate to $rangeEndDate"
        }
    }

    // Site Display Label
    val siteFilterLabel = remember(selectedSiteFilterCode, allSites, isMy) {
        if (selectedSiteFilterCode == null) {
            if (isMy) "ဆိုဒ်အားလုံး" else "All Sites"
        } else {
            val site = allSites.firstOrNull { it.siteCode == selectedSiteFilterCode }
            site?.siteName ?: selectedSiteFilterCode!!
        }
    }

    // Current Timestamp for filenames
    val stamp = remember { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) }

    // Summary calculations
    val totalManDays = filteredAttendance.sumOf { it.totalDayUnits }
    val totalEarned = filteredAttendance.sumOf { it.totalEarned }
    val totalAdvance = filteredAttendance.sumOf { it.advanceToday }
    val totalPaid = filteredAttendance.sumOf { it.paidToday }
    val netBalanceDue = totalEarned - totalAdvance - totalPaid

    // Pre-calculated CSV string
    val csvContent = remember(exportType, filteredAttendance, allWorkers, datePeriodLabel, siteFilterLabel) {
        when (exportType) {
            0 -> ExcelExportHelper.generateAttendanceCsv(filteredAttendance, datePeriodLabel, siteFilterLabel)
            1 -> ExcelExportHelper.generateWorkerSummaryCsv(filteredAttendance, allWorkers, datePeriodLabel)
            else -> ExcelExportHelper.generateSiteSummaryCsv(filteredAttendance, datePeriodLabel)
        }
    }

    // Function to generate the selected file on demand and return its File and MIME type
    fun prepareSelectedFile(): Pair<File, String>? {
        if (filteredAttendance.isEmpty() && exportType != 1) {
            Toast.makeText(
                context,
                if (isMy) "စစ်ထုတ်ထားသော ရက်စွဲအတွက် မှတ်တမ်းမရှိပါ!" else "No records found for selected date!",
                Toast.LENGTH_SHORT
            ).show()
            return null
        }

        return when (selectedFormat) {
            ExportFileFormat.PDF -> {
                val file = ExcelExportHelper.generateAttendancePdf(
                    context,
                    filteredAttendance,
                    datePeriodLabel,
                    siteFilterLabel
                )
                Pair(file, "application/pdf")
            }
            ExportFileFormat.XLSX -> {
                val xmlContent = ExcelExportHelper.generateAttendanceExcelXml(
                    filteredAttendance,
                    datePeriodLabel,
                    siteFilterLabel
                )
                val file = ExcelExportHelper.saveFile(
                    context,
                    "Labour_Attendance_${stamp}.xlsx",
                    xmlContent
                )
                Pair(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            }
            ExportFileFormat.CSV -> {
                val file = ExcelExportHelper.saveFile(
                    context,
                    "Labour_Attendance_${stamp}.csv",
                    csvContent
                )
                Pair(file, "text/csv")
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("export_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header Banner Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = LanguageManager.getString("export_title"),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${LanguageManager.getString("report_date")}: $todayStr • MMK / ကျပ်",
                                fontSize = 12.sp,
                                color = ConstructionGold,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Slate800,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Summarize,
                                    contentDescription = null,
                                    tint = ConstructionGold
                                )
                            }
                        }
                    }

                    Text(
                        text = LanguageManager.getString("export_description"),
                        fontSize = 12.sp,
                        color = Slate300,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // Section 1: Select Export Report Type
        item {
            Text(
                text = if (isMy) "၁။ အစီရင်ခံစာ အမျိုးအစား ရွေးချယ်ရန်" else "1. SELECT REPORT TYPE",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = exportType == 0,
                    onClick = { exportType = 0 },
                    label = { Text(if (isMy) "နေ့စဉ်တက်ရောက်မှု" else "Daily Attendance") },
                    leadingIcon = { Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("export_type_attendance")
                )
                FilterChip(
                    selected = exportType == 1,
                    onClick = { exportType = 1 },
                    label = { Text(if (isMy) "လုပ်သား အကျဉ်းချုပ်" else "Worker Summary") },
                    leadingIcon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("export_type_worker")
                )
                FilterChip(
                    selected = exportType == 2,
                    onClick = { exportType = 2 },
                    label = { Text(if (isMy) "ဆိုဒ် အကျဉ်းချုပ်" else "Site Summary") },
                    leadingIcon = { Icon(Icons.Default.Apartment, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("export_type_site")
                )
            }
        }

        // Section 2: Date Filter (Date ပါစေချင်ပါတယ် requirement)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(
                                text = LanguageManager.getString("filter_date_heading"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Current Active Date Scope Tag
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PrimaryContainerLight
                        ) {
                            Text(
                                text = datePeriodLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Date Mode Quick Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = dateFilterMode == DateFilterMode.ALL,
                            onClick = { dateFilterMode = DateFilterMode.ALL },
                            label = { Text(LanguageManager.getString("date_all")) },
                            modifier = Modifier.testTag("date_filter_all")
                        )
                        FilterChip(
                            selected = dateFilterMode == DateFilterMode.TODAY,
                            onClick = { dateFilterMode = DateFilterMode.TODAY },
                            label = { Text(LanguageManager.getString("date_today")) },
                            modifier = Modifier.testTag("date_filter_today")
                        )
                        FilterChip(
                            selected = dateFilterMode == DateFilterMode.SINGLE_DATE,
                            onClick = {
                                dateFilterMode = DateFilterMode.SINGLE_DATE
                                singleDatePickerDialog.show()
                            },
                            label = { Text(if (dateFilterMode == DateFilterMode.SINGLE_DATE) singleFilterDate else LanguageManager.getString("date_custom_day")) },
                            leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.testTag("date_filter_single")
                        )
                        FilterChip(
                            selected = dateFilterMode == DateFilterMode.DATE_RANGE,
                            onClick = { dateFilterMode = DateFilterMode.DATE_RANGE },
                            label = { Text(LanguageManager.getString("date_range")) },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.testTag("date_filter_range")
                        )
                    }

                    // If DATE_RANGE selected, show Start & End date pickers
                    if (dateFilterMode == DateFilterMode.DATE_RANGE) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { rangeStartDatePickerDialog.show() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(LanguageManager.getString("from_date"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(rangeStartDate, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = { rangeEndDatePickerDialog.show() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(LanguageManager.getString("to_date"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(rangeEndDate, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Site Filter Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isMy) "ဆိုဒ် စစ်ထုတ်ရန်: $siteFilterLabel" else "Site Filter: $siteFilterLabel",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TextButton(
                            onClick = { showSiteFilterDropdown = true },
                            modifier = Modifier.testTag("filter_site_button")
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isMy) "ဆိုဒ် ရွေးချယ်ရန်" else "Change Site", fontSize = 12.sp)
                        }

                        DropdownMenu(
                            expanded = showSiteFilterDropdown,
                            onDismissRequest = { showSiteFilterDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isMy) "ဆိုဒ်အားလုံး" else "All Sites") },
                                onClick = {
                                    selectedSiteFilterCode = null
                                    showSiteFilterDropdown = false
                                }
                            )
                            allSites.forEach { site ->
                                DropdownMenuItem(
                                    text = { Text("${site.siteCode} - ${site.siteName}") },
                                    onClick = {
                                        selectedSiteFilterCode = site.siteCode
                                        showSiteFilterDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: File Format Selection (PDF / XLSX / CSV)
        item {
            Text(
                text = if (isMy) "၃။ ဖိုင်ပုံစံ ရွေးချယ်ရန် (File Format)" else "3. SELECT FILE FORMAT",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // PDF Card Chip
                Card(
                    onClick = { selectedFormat = ExportFileFormat.PDF },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("format_pdf_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedFormat == ExportFileFormat.PDF) PrimaryLight else MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = if (selectedFormat == ExportFileFormat.PDF) Color.White else PrimaryLight,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "PDF ဖိုင်",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedFormat == ExportFileFormat.PDF) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = ".pdf",
                            fontSize = 10.sp,
                            color = if (selectedFormat == ExportFileFormat.PDF) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Excel Card Chip
                Card(
                    onClick = { selectedFormat = ExportFileFormat.XLSX },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("format_xlsx_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedFormat == ExportFileFormat.XLSX) WagesGreenDark else MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            tint = if (selectedFormat == ExportFileFormat.XLSX) Color.White else WagesGreenDark,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Excel ဖိုင်",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedFormat == ExportFileFormat.XLSX) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = ".xlsx",
                            fontSize = 10.sp,
                            color = if (selectedFormat == ExportFileFormat.XLSX) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // CSV Card Chip
                Card(
                    onClick = { selectedFormat = ExportFileFormat.CSV },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("format_csv_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedFormat == ExportFileFormat.CSV) Slate800 else MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = if (selectedFormat == ExportFileFormat.CSV) Color.White else Slate800,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "CSV ဖိုင်",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedFormat == ExportFileFormat.CSV) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = ".csv",
                            fontSize = 10.sp,
                            color = if (selectedFormat == ExportFileFormat.CSV) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section 4: Primary Direct Open & Share Action Buttons (တိုက်ရိုက်ကြည့်ရအောင် အသုံးချရလွယ်ကူစေရန်)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Primary Big Button: Direct Open & View
                Button(
                    onClick = {
                        val result = prepareSelectedFile()
                        if (result != null) {
                            val (file, mime) = result
                            val launched = ExcelExportHelper.openExportFile(
                                context,
                                file,
                                mime,
                                "Open ${file.name}"
                            )
                            if (launched) {
                                Toast.makeText(
                                    context,
                                    if (isMy) "${file.name} ကို တိုက်ရိုက်ဖွင့်နေပါသည်..." else "Opening ${file.name} directly...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("direct_open_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (selectedFormat) {
                            ExportFileFormat.PDF -> PrimaryLight
                            ExportFileFormat.XLSX -> WagesGreenDark
                            ExportFileFormat.CSV -> Slate900
                        }
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = when (selectedFormat) {
                            ExportFileFormat.PDF -> if (isMy) "PDF ဖြင့် တိုက်ရိုက် ဖွင့်ကြည့်မည်" else "OPEN DIRECTLY IN PDF VIEWER"
                            ExportFileFormat.XLSX -> if (isMy) "EXCEL ဖြင့် တိုက်ရိုက် ဖွင့်ကြည့်မည်" else "OPEN DIRECTLY IN EXCEL / SHEETS"
                            ExportFileFormat.CSV -> if (isMy) "CSV ဖိုင်ဖြင့် တိုက်ရိုက် ဖွင့်ကြည့်မည်" else "OPEN DIRECTLY IN CSV VIEWER"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Secondary Action Buttons Row (Share / Copy CSV / Reset Samples)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val result = prepareSelectedFile()
                            if (result != null) {
                                val (file, mime) = result
                                ExcelExportHelper.shareExportFile(
                                    context,
                                    file,
                                    mime,
                                    "Labour Report - ${datePeriodLabel}"
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_report_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(LanguageManager.getString("action_share"), fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(csvContent))
                            Toast.makeText(
                                context,
                                if (isMy) "CSV စာသားများကို ကူးယူပြီးပါပြီ!" else "Copied CSV to clipboard!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_csv_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(LanguageManager.getString("action_copy"), fontSize = 12.sp)
                    }


                }
            }
        }

        // Section 5: Direct In-App Table View (တိုက်ရိုက်ကြည့်ရ​အောင် အသုံးချရလွယ်ကူ​စေရန်)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (viewMode == "table") LanguageManager.getString("view_table") else LanguageManager.getString("view_raw"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = viewMode == "table",
                        onClick = { viewMode = "table" },
                        label = { Text("ဇယား (Table)", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                    FilterChip(
                        selected = viewMode == "raw",
                        onClick = { viewMode = "raw" },
                        label = { Text("နမူနာ (Raw)", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }
            }
        }

        // Table Summary Metrics Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(LanguageManager.getString("records_found"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${filteredAttendance.size} ခု", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Divider(modifier = Modifier.height(28.dp).width(1.dp))
                    Column {
                        Text(LanguageManager.getString("total_mandays"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(String.format(Locale.US, "%.2f", totalManDays), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Divider(modifier = Modifier.height(28.dp).width(1.dp))
                    Column {
                        Text(LanguageManager.getString("total_earned_mmk"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(ExcelExportHelper.formatCurrency(totalEarned), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WagesGreenDark)
                    }
                    Divider(modifier = Modifier.height(28.dp).width(1.dp))
                    Column {
                        Text(LanguageManager.getString("total_advance_mmk"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(ExcelExportHelper.formatCurrency(totalAdvance), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SafetyOrange)
                    }
                    Divider(modifier = Modifier.height(28.dp).width(1.dp))
                    Column {
                        Text(LanguageManager.getString("net_balance_mmk"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(ExcelExportHelper.formatCurrency(netBalanceDue), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryLight)
                    }
                }
            }
        }

        // Direct Table View or Raw Code View
        if (viewMode == "table") {
            if (filteredAttendance.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.EventBusy, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (isMy) "ရွေးချယ်ထားသော ရက်စွဲအတွက် စာရင်းမရှိပါ" else "No attendance records found for selected date",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { dateFilterMode = DateFilterMode.ALL },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isMy) "ရက်စွဲအားလုံး ပြန်ကြည့်မည်" else "View All Dates")
                            }
                        }
                    }
                }
            } else {
                // Interactive Scrollable Horizontal Data Table
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Column {
                                // Table Header Row
                                Surface(
                                    color = Slate900,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("ရက်စွဲ (Date)", width = 85.dp, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("ဆိုဒ် (Site)", width = 110.dp, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("လုပ်သား (Worker)", width = 130.dp, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("တက်ရောက်မှု", width = 80.dp, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("နေ့တွက်ခ (MMK)", width = 95.dp, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("OT နာရီ/ကြေး", width = 85.dp, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("စုစုပေါင်းရငွေ", width = 100.dp, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("ကြိုထုတ်ငွေ", width = 90.dp, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("ယနေ့ပေးငွေ", width = 90.dp, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("လက်ကျန် (MMK)", width = 100.dp, color = ConstructionGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }

                                // Table Body Rows
                                LazyColumn(
                                    modifier = Modifier.fillMaxHeight(),
                                    contentPadding = PaddingValues(bottom = 8.dp)
                                ) {
                                    items(filteredAttendance) { r ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (filteredAttendance.indexOf(r) % 2 == 0) MaterialTheme.colorScheme.surface
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(r.date, width = 85.dp, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                            Text(r.siteName, width = 110.dp, fontSize = 11.sp, maxLines = 1)
                                            Column(modifier = Modifier.width(130.dp)) {
                                                Text(r.workerName, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                                Text(r.workerType, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text("${r.morningAttendance}/${r.eveningAttendance} (${String.format(Locale.US, "%.1f", r.totalDayUnits)})", width = 80.dp, fontSize = 11.sp)
                                            Text(ExcelExportHelper.formatCurrency(r.dailyAmount), width = 95.dp, fontSize = 11.sp)
                                            Text("${String.format(Locale.US, "%.1f", r.otHours)}h / ${ExcelExportHelper.formatCurrency(r.otAmount)}", width = 85.dp, fontSize = 11.sp)
                                            Text(ExcelExportHelper.formatCurrency(r.totalEarned), width = 100.dp, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WagesGreenDark)
                                            Text(ExcelExportHelper.formatCurrency(r.advanceToday), width = 90.dp, fontSize = 11.sp, color = SafetyOrange)
                                            Text(ExcelExportHelper.formatCurrency(r.paidToday), width = 90.dp, fontSize = 11.sp)
                                            Text(ExcelExportHelper.formatCurrency(r.balance), width = 100.dp, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryLight)
                                        }
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Raw Preview
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Slate900,
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = csvContent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }

}

// Helper composable for fixed width Text cells in custom tables
@Composable
private fun Text(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE
) {
    androidx.compose.material3.Text(
        text = text,
        modifier = Modifier.width(width),
        color = color,
        fontWeight = fontWeight,
        fontSize = fontSize,
        maxLines = maxLines
    )
}
