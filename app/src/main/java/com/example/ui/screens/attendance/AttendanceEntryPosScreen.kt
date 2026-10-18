package com.example.ui.screens.attendance

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecord
import com.example.data.model.Site
import com.example.data.model.Worker
import com.example.ui.components.StatusBadge
import com.example.ui.components.TradeIcon
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AttendancePreset
import com.example.ui.viewmodel.LabourViewModel
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.util.AppLanguage
import com.example.util.CategoryManager
import com.example.util.ExcelExportHelper
import com.example.util.LanguageManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceEntryPosScreen(
    viewModel: LabourViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val form by viewModel.posForm.collectAsState()
    val activeWorkers by viewModel.activeWorkers.collectAsState()
    val allWorkers by viewModel.allWorkers.collectAsState()
    val sites by viewModel.allSites.collectAsState()
    val markedWorkerIds by viewModel.markedWorkerIdsForSelectedDate.collectAsState()
    val allAttendance by viewModel.allAttendance.collectAsState()
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()

    val displayWorkers = remember(activeWorkers, allWorkers) {
        if (activeWorkers.isNotEmpty()) activeWorkers else allWorkers
    }

    var showSiteDropdown by remember { mutableStateOf(false) }
    var showWorkerDialog by remember { mutableStateOf(false) }
    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var workerSearchQuery by remember { mutableStateOf("") }
    var filterUnmarkedOnly by remember { mutableStateOf(false) }
    var workerCategoryFilter by remember { mutableStateOf("All") }
    var recordToDelete by remember { mutableStateOf<AttendanceRecord?>(null) }

    androidx.activity.compose.BackHandler {
        if (form.editingRecordId != null) {
            viewModel.resetForm()
        } else {
            viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.DASHBOARD)
        }
    }

    val todayRecords = remember(allAttendance, form.selectedDate, form.selectedSite) {
        allAttendance.filter {
            it.date == form.selectedDate && (form.selectedSite == null || it.siteCode == form.selectedSite?.siteCode)
        }
    }

    // Pre-select top site as default per requirement (3)
    LaunchedEffect(sites) {
        if (form.selectedSite == null && sites.isNotEmpty()) {
            viewModel.selectSite(sites.first())
        }
    }

    val calendar = remember { Calendar.getInstance() }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val datePickerDialog = remember {
        val parts = form.selectedDate.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.YEAR)
        val m = (parts.getOrNull(1)?.toIntOrNull() ?: (calendar.get(Calendar.MONTH) + 1)) - 1
        val d = parts.getOrNull(2)?.toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(context, { _, year, month, dayOfMonth ->
            val formatted = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            viewModel.selectDate(formatted)
        }, y, m, d)
    }

    val isDuplicate = form.selectedWorker != null &&
            form.editingRecordId == null &&
            markedWorkerIds.contains(form.selectedWorker?.workerId)

    val canSubmit = form.selectedWorker != null && !isDuplicate

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("daily_attendance_form_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (form.editingRecordId != null) {
                            if (currentLanguage == AppLanguage.MYANMAR) "တက်ရောက်မှု ပြင်ဆင်ရန်" else "Edit Daily Attendance"
                        } else {
                            if (currentLanguage == AppLanguage.MYANMAR) "နေ့စဉ်တက်ရောက်မှု စာရင်းသွင်းပုံစံ" else "Daily Attendance Form"
                        },
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (currentLanguage == AppLanguage.MYANMAR) "အချိန်နှင့်တပြေးညီ လုပ်အားခ၊ OT နှင့် ကျန်ငွေတွက်ချက်မှု" else "Real-time wage, OT & balance calculation",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (form.editingRecordId != null) {
                        FilledTonalButton(
                            onClick = { viewModel.resetForm() },
                            modifier = Modifier.testTag("cancel_edit_mode_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (currentLanguage == AppLanguage.MYANMAR) "အသစ်စရန်" else "New Form")
                        }
                    } else {
                        Button(
                            onClick = { showBatchDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = OtBlue),
                            modifier = Modifier.testTag("open_batch_attendance_btn")
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (currentLanguage == AppLanguage.MYANMAR) "အားလုံးမှတ်ရန်" else "Batch All",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Editing Mode Banner
        if (form.editingRecordId != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("form_editing_banner"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryContainerLight),
                    border = BorderStroke(1.dp, PrimaryLight.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryLight)
                            Column {
                                Text(
                                    text = if (currentLanguage == AppLanguage.MYANMAR) "တက်ရောက်မှု စာရင်း ပြင်ဆင်နေသည်" else "EDITING ATTENDANCE RECORD",
                                    color = PrimaryLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${form.selectedWorker?.name} • ${form.selectedDate} • ID: ${form.selectedWorker?.workerId}",
                                    fontSize = 11.sp,
                                    color = Slate600
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { viewModel.resetForm() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_cancel_edit_top")
                        ) {
                            Text(if (currentLanguage == AppLanguage.MYANMAR) "မပြင်တော့ပါ" else "Cancel", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Duplicate Worker / Mistake Handler Card
        if (form.duplicateExistingRecord != null) {
            val dup = form.duplicateExistingRecord!!
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("form_duplicate_handler_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AdvanceRedLight),
                    border = BorderStroke(1.dp, AdvanceRed.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AdvanceRed, modifier = Modifier.size(24.dp))
                            Column {
                                Text(
                                    text = if (currentLanguage == AppLanguage.MYANMAR)
                                        "${dup.workerName} (${dup.workerId}) သည် ${dup.date} အတွက် စာရင်းသွင်းပြီးဖြစ်ပါသည်!"
                                    else
                                        "${dup.workerName} is already logged on ${dup.date}!",
                                    color = Color(0xFF991B1B),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (currentLanguage == AppLanguage.MYANMAR)
                                        "ယခင်သွင်းထားမှု- နံနက် (${dup.morningAttendance}) • ညနေ (${dup.eveningAttendance}) • OT: ${dup.otHours}h • ရငွေ: ${ExcelExportHelper.formatCurrency(dup.totalEarned)}"
                                    else
                                        "Existing Log: AM (${dup.morningAttendance}) • PM (${dup.eveningAttendance}) • OT: ${dup.otHours}h • Earned: ${ExcelExportHelper.formatCurrency(dup.totalEarned)}",
                                    fontSize = 11.sp,
                                    color = Slate700
                                )
                            }
                        }

                        Text(
                            text = if (currentLanguage == AppLanguage.MYANMAR)
                                "မှားယွင်းသွင်းမိပါက အောက်ပါခလုတ်ဖြင့် ချက်ချင်း ဖျက်ပစ်နိုင်သည် သို့မဟုတ် ပြင်ဆင်နိုင်ပါသည်-"
                            else
                                "If entered mistakenly, you can delete or edit it right here:",
                            fontSize = 11.sp,
                            color = Slate600
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.editRecord(dup) },
                                modifier = Modifier.weight(1f).height(40.dp).testTag("btn_edit_duplicate_record"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OtBlue)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (currentLanguage == AppLanguage.MYANMAR) "စာရင်းပြင်မည်" else "Edit Record", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { recordToDelete = dup },
                                modifier = Modifier.weight(1f).height(40.dp).testTag("btn_delete_duplicate_record"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AdvanceRed)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (currentLanguage == AppLanguage.MYANMAR) "မှားသွင်းမိ၍ ဖျက်မည်" else "Delete Mistake", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else if (form.errorMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("form_error_banner"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AdvanceRedLight),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AdvanceRed))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AdvanceRed, modifier = Modifier.size(24.dp))
                        Text(
                            text = form.errorMessage ?: "",
                            color = Color(0xFF991B1B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearFormMessage() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = AdvanceRed)
                        }
                    }
                }
            }
        }

        // Success Banner
        if (form.successMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("form_success_banner"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = WagesGreenLight)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = WagesGreenDark, modifier = Modifier.size(24.dp))
                        Text(
                            text = form.successMessage ?: "",
                            color = WagesGreenDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearFormMessage() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = WagesGreenDark)
                        }
                    }
                }
            }
        }

        // 1. SITE & DATE SELECTION CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("site_date_selection_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LanguageManager.getString("section_1"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConstructionGoldDark,
                            letterSpacing = 0.5.sp
                        )
                        Surface(
                            color = PrimaryContainerLight,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.MYANMAR) "${sites.size} ဆိုဒ်ရှိသည်" else "${sites.size} Sites",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryLight,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Site Selection Dropdown Field
                    Box {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSiteDropdown = true }
                                .testTag("site_dropdown_selector"),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(OtBlueLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Apartment, contentDescription = null, tint = OtBlue, modifier = Modifier.size(20.dp))
                                    }
                                    Column {
                                        Text(
                                            text = if (currentLanguage == AppLanguage.MYANMAR) "ဆောက်လုပ်ရေးဆိုဒ် ရွေးချယ်ပါ *" else "Select Construction Site *",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = form.selectedSite?.siteName ?: (if (currentLanguage == AppLanguage.MYANMAR) "ဆိုဒ်ရွေးချယ်ပါ" else "Select Site"),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        if (form.selectedSite != null) {
                                            Text(
                                                text = "${form.selectedSite?.siteCode} • Supervisor: ${form.selectedSite?.supervisor}",
                                                fontSize = 11.sp,
                                                color = Slate500
                                            )
                                        }
                                    }
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Open Sites")
                            }
                        }

                        DropdownMenu(
                            expanded = showSiteDropdown,
                            onDismissRequest = { showSiteDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            sites.forEach { site ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(site.siteName, fontWeight = FontWeight.Bold)
                                            Text("${site.siteCode} • ${site.location} • Sup: ${site.supervisor}", fontSize = 11.sp, color = Slate500)
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.Apartment, contentDescription = null, tint = OtBlue) },
                                    onClick = {
                                        viewModel.selectSite(site)
                                        showSiteDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Date Selector with Quick Day Nav
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                try {
                                    val parsed = dateFormat.parse(form.selectedDate) ?: Date()
                                    val cal = Calendar.getInstance().apply { time = parsed }
                                    cal.add(Calendar.DAY_OF_MONTH, -1)
                                    viewModel.selectDate(dateFormat.format(cal.time))
                                } catch (e: Exception) {}
                            },
                            modifier = Modifier.testTag("btn_prev_date")
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { datePickerDialog.show() }
                                .testTag("btn_date_picker"),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = ConstructionGoldDark, modifier = Modifier.size(18.dp))
                                    Column {
                                        Text(
                                            text = if (currentLanguage == AppLanguage.MYANMAR) "တက်ရောက်မှု နေ့စွဲ" else "Attendance Date",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(form.selectedDate, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                                Text(
                                    text = if (form.selectedDate == viewModel.todayDateString) {
                                        if (currentLanguage == AppLanguage.MYANMAR) "ယနေ့" else "TODAY"
                                    } else {
                                        if (currentLanguage == AppLanguage.MYANMAR) "ရွေးရန်" else "PICK"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryLight
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                try {
                                    val parsed = dateFormat.parse(form.selectedDate) ?: Date()
                                    val cal = Calendar.getInstance().apply { time = parsed }
                                    cal.add(Calendar.DAY_OF_MONTH, 1)
                                    viewModel.selectDate(dateFormat.format(cal.time))
                                } catch (e: Exception) {}
                            },
                            modifier = Modifier.testTag("btn_next_date")
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
                        }
                    }
                }
            }
        }

        // 2. WORKER SELECTION CARD (WITH DUPLICATE VALIDATION)
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("worker_selection_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = LanguageManager.getString("section_2"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ConstructionGoldDark,
                                letterSpacing = 0.5.sp
                            )
                            if (activeWorkers.isNotEmpty()) {
                                Surface(
                                    color = ConstructionGoldLight,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${activeWorkers.size} ဦး",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = { showAddWorkerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ConstructionGold),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("pos_quick_add_worker_btn")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Slate900, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (currentLanguage == AppLanguage.MYANMAR) "+ လုပ်သားအသစ်" else "+ Add Worker",
                                color = Slate900,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Direct Workers List Strip (Ensures all worker names are visible immediately!)
                    if (displayWorkers.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate100),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAddWorkerDialog = true }
                                .testTag("empty_workers_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(ConstructionGold.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = ConstructionGoldDark, modifier = Modifier.size(20.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (currentLanguage == AppLanguage.MYANMAR) "လုပ်သားစာရင်း မရှိသေးပါ" else "No workers registered yet",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Slate800
                                    )
                                    Text(
                                        text = if (currentLanguage == AppLanguage.MYANMAR) "မိမိ၏ လုပ်သားအစစ်များ စတင်ထည့်သွင်းရန် ဤနေရာကို နှိပ်ပါ" else "Tap here to register your workers.",
                                        fontSize = 11.sp,
                                        color = Slate600
                                    )
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (currentLanguage == AppLanguage.MYANMAR) "လုပ်သားအမည်များ (စာရင်းသွင်းရန် နှိပ်ပါ):" else "Select Worker by Name:",
                                    fontSize = 11.sp,
                                    color = Slate600,
                                    fontWeight = FontWeight.SemiBold
                                )
                                TextButton(
                                    onClick = {
                                        workerSearchQuery = ""
                                        showWorkerDialog = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp), tint = OtBlue)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        if (currentLanguage == AppLanguage.MYANMAR) "အသေးစိတ်ရှာရန်" else "Search / Filter",
                                        fontSize = 11.sp,
                                        color = OtBlue
                                    )
                                }
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(displayWorkers, key = { it.workerId }) { w ->
                                    val isSelected = form.selectedWorker?.workerId == w.workerId
                                    val isMarkedToday = markedWorkerIds.contains(w.workerId)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = when {
                                            isSelected -> ConstructionGold
                                            isMarkedToday -> WagesGreenLight
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        },
                                        border = BorderStroke(
                                            if (isSelected) 2.dp else 1.dp,
                                            if (isSelected) ConstructionGoldDark else if (isMarkedToday) WagesGreen else Slate300
                                        ),
                                        modifier = Modifier
                                            .clickable { viewModel.selectWorker(w) }
                                            .testTag("worker_quick_chip_${w.workerId}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                color = if (isSelected) Slate900 else Slate200,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = w.workerId,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else Slate700,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                            Text(
                                                text = w.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isSelected) Slate900 else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "(${CategoryManager.getMyanmarName(w.workerType)})",
                                                fontSize = 10.sp,
                                                color = if (isSelected) Slate800 else Slate500
                                            )
                                            if (isMarkedToday) {
                                                Surface(
                                                    color = WagesGreenDark,
                                                    shape = CircleShape
                                                ) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = "Marked",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp).padding(2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Worker Selector Card / Selected Details
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                workerSearchQuery = ""
                                showWorkerDialog = true
                            }
                            .testTag("worker_dialog_trigger"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (form.selectedWorker == null) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else PrimaryContainerLight.copy(alpha = 0.35f),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (form.selectedWorker != null) ConstructionGold else Slate300
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (form.selectedWorker == null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(ConstructionGold.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.PersonSearch, contentDescription = null, tint = ConstructionGoldDark, modifier = Modifier.size(20.dp))
                                    }
                                    Column {
                                        Text(
                                            text = if (currentLanguage == AppLanguage.MYANMAR) "လုပ်သားရွေးချယ်ပါ *" else "Tap to Select Worker *",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = if (currentLanguage == AppLanguage.MYANMAR) "အထက်ပါ စာရင်းမှ လုပ်သားကို နှိပ်ပါ သို့မဟုတ် ရှာဖွေပါ" else "Tap a worker above or search in directory",
                                            fontSize = 11.sp,
                                            color = Slate500
                                        )
                                    }
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    TradeIcon(trade = form.selectedWorker?.workerType ?: "")
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = form.selectedWorker?.name ?: "",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 16.sp,
                                                color = Slate900
                                            )
                                            StatusBadge(status = CategoryManager.getDisplayName(form.selectedWorker?.workerType ?: ""))
                                        }
                                        Text(
                                            text = "ID: ${form.selectedWorker?.workerId} • Daily: ${ExcelExportHelper.formatCurrency(form.dailyRate)} • OT: ${ExcelExportHelper.formatCurrency(form.otRate)}/h",
                                            fontSize = 11.sp,
                                            color = Slate600
                                        )
                                        if (form.selectedWorker?.address?.isNotBlank() == true) {
                                            Text(
                                                text = "📍 ${form.selectedWorker?.address}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = SafetyOrangeDark
                                            )
                                        }
                                    }
                                }
                                TextButton(onClick = { showWorkerDialog = true }) {
                                    Text(if (currentLanguage == AppLanguage.MYANMAR) "ပြောင်းမည်" else "Change")
                                }
                            }
                        }
                    }

                    // Duplicate Alert Banner
                    if (isDuplicate) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().testTag("duplicate_worker_alert"),
                            shape = RoundedCornerShape(8.dp),
                            color = AdvanceRedLight
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null, tint = AdvanceRed, modifier = Modifier.size(20.dp))
                                Text(
                                    text = if (currentLanguage == AppLanguage.MYANMAR) {
                                        "သတိပေးချက်- ${form.selectedWorker?.name} သည် ${form.selectedDate} အတွက် စာရင်းသွင်းပြီးဖြစ်ပါသည်!"
                                    } else {
                                        "DUPLICATE PREVENTED: ${form.selectedWorker?.name} already has attendance logged for ${form.selectedDate}!"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AdvanceRed
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. MORNING & EVENING ATTENDANCE SHIFT TOGGLES
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("shift_attendance_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LanguageManager.getString("section_3"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConstructionGoldDark,
                            letterSpacing = 0.5.sp
                        )
                        Surface(
                            color = ConstructionGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.MYANMAR) "စုစုပေါင်း: ${form.totalDayUnits} ရက်" else "Total Units: ${form.totalDayUnits}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ConstructionGoldDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Quick 1-Tap Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PresetChip(
                            label = LanguageManager.getString("preset_full_day"),
                            isSelected = form.morningAttendance == "P" && form.eveningAttendance == "P",
                            onClick = { viewModel.applyPreset(AttendancePreset.FULL_DAY) },
                            modifier = Modifier.weight(1f)
                        )
                        PresetChip(
                            label = LanguageManager.getString("preset_half_am"),
                            isSelected = form.morningAttendance == "P" && form.eveningAttendance == "A",
                            onClick = { viewModel.applyPreset(AttendancePreset.HALF_DAY_AM) },
                            modifier = Modifier.weight(1f)
                        )
                        PresetChip(
                            label = LanguageManager.getString("preset_half_pm"),
                            isSelected = form.morningAttendance == "A" && form.eveningAttendance == "P",
                            onClick = { viewModel.applyPreset(AttendancePreset.HALF_DAY_PM) },
                            modifier = Modifier.weight(1f)
                        )
                        PresetChip(
                            label = LanguageManager.getString("preset_absent"),
                            isSelected = form.morningAttendance == "A" && form.eveningAttendance == "A",
                            onClick = { viewModel.applyPreset(AttendancePreset.ABSENT) },
                            modifier = Modifier.weight(1f),
                            selectedBg = AdvanceRed
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Morning Shift
                    Text(LanguageManager.getString("morning_shift"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PosSegmentButton(
                            label = LanguageManager.getString("present_btn"),
                            isSelected = form.morningAttendance == "P",
                            onClick = { viewModel.setMorningAttendance("P") },
                            modifier = Modifier.weight(1f),
                            activeColor = WagesGreenDark
                        )
                        PosSegmentButton(
                            label = LanguageManager.getString("half_btn"),
                            isSelected = form.morningAttendance == "HD",
                            onClick = { viewModel.setMorningAttendance("HD") },
                            modifier = Modifier.weight(1f),
                            activeColor = ConstructionGoldDark
                        )
                        PosSegmentButton(
                            label = LanguageManager.getString("absent_btn"),
                            isSelected = form.morningAttendance == "A",
                            onClick = { viewModel.setMorningAttendance("A") },
                            modifier = Modifier.weight(1f),
                            activeColor = AdvanceRed
                        )
                    }

                    // Evening Shift
                    Text(LanguageManager.getString("evening_shift"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PosSegmentButton(
                            label = LanguageManager.getString("present_btn"),
                            isSelected = form.eveningAttendance == "P",
                            onClick = { viewModel.setEveningAttendance("P") },
                            modifier = Modifier.weight(1f),
                            activeColor = WagesGreenDark
                        )
                        PosSegmentButton(
                            label = LanguageManager.getString("half_btn"),
                            isSelected = form.eveningAttendance == "HD",
                            onClick = { viewModel.setEveningAttendance("HD") },
                            modifier = Modifier.weight(1f),
                            activeColor = ConstructionGoldDark
                        )
                        PosSegmentButton(
                            label = LanguageManager.getString("absent_btn"),
                            isSelected = form.eveningAttendance == "A",
                            onClick = { viewModel.setEveningAttendance("A") },
                            modifier = Modifier.weight(1f),
                            activeColor = AdvanceRed
                        )
                    }
                }
            }
        }

        // 4. OVERTIME (OT) & WAGES
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("ot_wages_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LanguageManager.getString("section_4"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConstructionGoldDark,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (currentLanguage == AppLanguage.MYANMAR) "OT ရငွေ: ${ExcelExportHelper.formatCurrency(form.otAmount)}" else "OT Amount: ${ExcelExportHelper.formatCurrency(form.otAmount)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OtBlue
                        )
                    }

                    // OT Steppers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { viewModel.adjustOtHours(-1.0) },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Text("-1h", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        FilledTonalIconButton(
                            onClick = { viewModel.adjustOtHours(-0.5) },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Text("-½", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedTextField(
                            value = if (form.otHours == 0.0) "" else form.otHours.toString(),
                            onValueChange = { str ->
                                val num = str.toDoubleOrNull() ?: 0.0
                                viewModel.setOtHours(num)
                            },
                            modifier = Modifier.weight(1f).testTag("input_ot_hours"),
                            label = { Text(if (currentLanguage == AppLanguage.MYANMAR) "OT နာရီ" else "OT Hours") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        FilledTonalIconButton(
                            onClick = { viewModel.adjustOtHours(0.5) },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Text("+½", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        FilledTonalIconButton(
                            onClick = { viewModel.adjustOtHours(1.0) },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Text("+1h", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        FilledTonalIconButton(
                            onClick = { viewModel.adjustOtHours(2.0) },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Text("+2h", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Rates row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = if (form.dailyRate == 0.0) "" else form.dailyRate.toInt().toString(),
                            onValueChange = { str ->
                                str.toDoubleOrNull()?.let { viewModel.setDailyRate(it) }
                            },
                            label = { Text(if (currentLanguage == AppLanguage.MYANMAR) "နေ့တွက်ခ (ကျပ်)" else "Daily Rate (Ks)") },
                            modifier = Modifier.weight(1f).testTag("input_daily_rate"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = if (form.otRate == 0.0) "" else form.otRate.toInt().toString(),
                            onValueChange = { str ->
                                str.toDoubleOrNull()?.let { viewModel.setOtRate(it) }
                            },
                            label = { Text(if (currentLanguage == AppLanguage.MYANMAR) "OT ကြေး (ကျပ်/နာရီ)" else "OT Rate (Ks/hr)") },
                            modifier = Modifier.weight(1f).testTag("input_ot_rate"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // 5. ADVANCE & PAID TODAY
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("advance_paid_inputs_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = LanguageManager.getString("section_5"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConstructionGoldDark,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = if (form.advanceToday == 0.0) "" else form.advanceToday.toInt().toString(),
                            onValueChange = { str ->
                                viewModel.setAdvanceToday(str.toDoubleOrNull() ?: 0.0)
                            },
                            label = { Text(if (currentLanguage == AppLanguage.MYANMAR) "ယနေ့ကြိုထုတ်ငွေ (ကျပ်)" else "Advance Today (Ks)") },
                            placeholder = { Text("0") },
                            modifier = Modifier.weight(1f).testTag("input_advance_today"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = if (form.paidToday == 0.0) "" else form.paidToday.toInt().toString(),
                            onValueChange = { str ->
                                viewModel.setPaidToday(str.toDoubleOrNull() ?: 0.0)
                            },
                            label = { Text(if (currentLanguage == AppLanguage.MYANMAR) "ယနေ့ပေးငွေ (ကျပ်)" else "Paid Today (Ks)") },
                            placeholder = { Text("0") },
                            modifier = Modifier.weight(1f).testTag("input_paid_today"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Payment shortcuts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SuggestionChip(
                            onClick = { viewModel.setAdvanceToday(5000.0) },
                            label = { Text(if (currentLanguage == AppLanguage.MYANMAR) "+၅,၀၀၀" else "+5,000 Ks") },
                            modifier = Modifier.weight(1f)
                        )
                        SuggestionChip(
                            onClick = { viewModel.setAdvanceToday(10000.0) },
                            label = { Text(if (currentLanguage == AppLanguage.MYANMAR) "+၁၀,၀၀၀" else "+10,000 Ks") },
                            modifier = Modifier.weight(1f)
                        )
                        SuggestionChip(
                            onClick = { viewModel.setPaidToday(form.totalEarned) },
                            label = { Text(if (currentLanguage == AppLanguage.MYANMAR) "အပြည့်ရှင်း (${ExcelExportHelper.formatCurrency(form.totalEarned)})" else "Pay Full") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = form.remark,
                        onValueChange = { viewModel.setRemark(it) },
                        label = { Text(if (currentLanguage == AppLanguage.MYANMAR) "လုပ်ငန်းမှတ်ချက်" else "Work Remark / Notes") },
                        placeholder = { Text(if (currentLanguage == AppLanguage.MYANMAR) "ဥပမာ- အမိုးလောင်းခြင်း၊ အုတ်စီခြင်း" else "e.g. Slab casting, Column shuttering") },
                        modifier = Modifier.fillMaxWidth().testTag("input_remark"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // 6. REAL-TIME CALCULATION SUMMARY CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("real_time_wage_calculation_card"),
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(WagesGreen))
                            Text(
                                text = LanguageManager.getString("section_6"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ConstructionGold,
                                letterSpacing = 1.sp
                            )
                        }
                        Surface(
                            color = Slate800,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.MYANMAR) "အလိုအလျောက်တွက်သည်" else "Auto Computed",
                                fontSize = 10.sp,
                                color = Slate300,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Divider(color = Slate700)

                    CalculationLine(
                        label = "${LanguageManager.getString("calc_daily_wage")} (${form.totalDayUnits} × ${ExcelExportHelper.formatCurrency(form.dailyRate)})",
                        value = ExcelExportHelper.formatCurrency(form.dailyAmount),
                        color = Color.White
                    )

                    CalculationLine(
                        label = "${LanguageManager.getString("calc_ot_wage")} (${form.otHours}h × ${ExcelExportHelper.formatCurrency(form.otRate)})",
                        value = ExcelExportHelper.formatCurrency(form.otAmount),
                        color = OtBlueLight
                    )

                    CalculationLine(
                        label = LanguageManager.getString("calc_total_earned"),
                        value = ExcelExportHelper.formatCurrency(form.totalEarned),
                        color = WagesGreen,
                        isBold = true
                    )

                    if (form.advanceToday > 0) {
                        CalculationLine(
                            label = LanguageManager.getString("calc_less_adv"),
                            value = "- " + ExcelExportHelper.formatCurrency(form.advanceToday),
                            color = AdvanceRedLight
                        )
                    }

                    if (form.paidToday > 0) {
                        CalculationLine(
                            label = LanguageManager.getString("calc_less_paid"),
                            value = "- " + ExcelExportHelper.formatCurrency(form.paidToday),
                            color = Slate300
                        )
                    }

                    Divider(color = Slate700)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = LanguageManager.getString("calc_balance_due"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ConstructionGold
                            )
                            Text(
                                text = if (currentLanguage == AppLanguage.MYANMAR) "= ရငွေ - ကြိုထုတ်ငွေ - ပေးငွေ" else "= Total Earned - Advance - Paid",
                                fontSize = 10.sp,
                                color = Slate400
                            )
                        }
                        Text(
                            text = ExcelExportHelper.formatCurrency(form.balance),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (form.balance > 0) ConstructionGold else WagesGreen
                        )
                    }
                }
            }
        }

        // 7. ACTION BUTTONS
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (form.editingRecordId != null) {
                    // EDIT MODE ACTION BUTTONS
                    Button(
                        onClick = { viewModel.saveAttendance(advanceToNextWorker = false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("btn_update_editing_record"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ConstructionGold)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Slate900, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LanguageManager.getString("update_record"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val curRecord = allAttendance.find { it.id == form.editingRecordId }
                                    ?: AttendanceRecord(
                                        id = form.editingRecordId ?: 0,
                                        date = form.selectedDate,
                                        siteCode = form.selectedSite?.siteCode ?: "",
                                        siteName = form.selectedSite?.siteName ?: "",
                                        workerId = form.selectedWorker?.workerId ?: "",
                                        workerName = form.selectedWorker?.name ?: "",
                                        workerType = form.selectedWorker?.workerType ?: ""
                                    )
                                recordToDelete = curRecord
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_delete_current_editing_record"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AdvanceRed)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = LanguageManager.getString("delete_record"),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.resetForm() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_cancel_editing_bottom"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(LanguageManager.getString("cancel_edit"), fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // NEW RECORD CREATION BUTTONS
                    Button(
                        onClick = { viewModel.saveAttendance(advanceToNextWorker = true) },
                        enabled = canSubmit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("btn_save_and_next"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ConstructionGold)
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = null, tint = Slate900, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LanguageManager.getString("save_and_next"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.saveAttendance(advanceToNextWorker = false) },
                            enabled = canSubmit,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("btn_save_record"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(LanguageManager.getString("save_only"), fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.resetForm() },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("btn_reset_form"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(LanguageManager.getString("reset"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 8. TODAY'S LOGGED ATTENDANCE RECORDS (Instant Review & Quick Delete/Edit)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_logged_records_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FactCheck, contentDescription = null, tint = ConstructionGoldDark)
                            Column {
                                Text(
                                    text = if (currentLanguage == AppLanguage.MYANMAR)
                                        "ဤရက်စွဲအတွက် စာရင်းသွင်းပြီးသူများ (${todayRecords.size} ဦး)"
                                    else
                                        "Logged Workers for this Date (${todayRecords.size})",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "ရက်စွဲ: ${form.selectedDate} • ဆိုဒ်: ${form.selectedSite?.siteName ?: "ဆိုဒ်အားလုံး"}",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                        }

                        TextButton(
                            onClick = { viewModel.navigateTo(AppScreen.ATTENDANCE_LIST) },
                            modifier = Modifier.testTag("btn_view_full_attendance_log")
                        ) {
                            Text(if (currentLanguage == AppLanguage.MYANMAR) "မှတ်တမ်းအားလုံး" else "View All", fontSize = 12.sp)
                        }
                    }

                    if (todayRecords.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.MYANMAR)
                                    "ဤရက်စွဲအတွက် စာရင်းမသွင်းရသေးပါ။ အထက်တွင် လုပ်သားရွေးချယ်၍ စာရင်းသွင်းနိုင်ပါသည်။"
                                else
                                    "No attendance records for this date yet. Select a worker above to log attendance.",
                                fontSize = 12.sp,
                                color = Slate500,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            todayRecords.forEach { record ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            TradeIcon(trade = record.workerType)
                                            Column {
                                                Text(record.workerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(
                                                    text = "${record.workerId} • ${CategoryManager.getDisplayName(record.workerType)}",
                                                    fontSize = 11.sp,
                                                    color = Slate500
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.padding(top = 2.dp)
                                                ) {
                                                    StatusBadge(status = "AM:${record.morningAttendance}")
                                                    StatusBadge(status = "PM:${record.eveningAttendance}")
                                                    if (record.otHours > 0) {
                                                        StatusBadge(status = "OT:${record.otHours}h")
                                                    }
                                                }
                                                Text(
                                                    text = "ရငွေ: ${ExcelExportHelper.formatCurrency(record.totalEarned)}" +
                                                            if (record.advanceToday > 0) " • ကြိုထုတ်: ${ExcelExportHelper.formatCurrency(record.advanceToday)}" else "",
                                                    fontSize = 11.sp,
                                                    color = WagesGreenDark,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        // Action buttons: Edit & Delete
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = { viewModel.editRecord(record) },
                                                modifier = Modifier.size(36.dp).testTag("pos_edit_record_${record.id}")
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = OtBlue, modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(
                                                onClick = { recordToDelete = record },
                                                modifier = Modifier.size(36.dp).testTag("pos_delete_record_${record.id}")
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AdvanceRed, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // WORKER SELECTION MODAL DIALOG
    if (showWorkerDialog) {
        AlertDialog(
            onDismissRequest = { showWorkerDialog = false },
            title = {
                Column {
                    Text(
                        text = if (currentLanguage == AppLanguage.MYANMAR) "လုပ်သားရွေးချယ်ပါ" else "Select Worker",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text("Date: ${form.selectedDate} (${markedWorkerIds.size} logged)", fontSize = 12.sp, color = Slate500)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
                ) {
                    OutlinedTextField(
                        value = workerSearchQuery,
                        onValueChange = { workerSearchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("worker_search_input"),
                        placeholder = { Text(if (currentLanguage == AppLanguage.MYANMAR) "အမည်၊ ID၊ ကျွမ်းကျင်မှုဖြင့် ရှာဖွေပါ..." else "Search worker name, ID, trade...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Filter chips: All vs Unmarked Only
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val unmarkedCount = displayWorkers.count { !markedWorkerIds.contains(it.workerId) }
                        FilterChip(
                            selected = !filterUnmarkedOnly,
                            onClick = { filterUnmarkedOnly = false },
                            label = { Text(if (currentLanguage == AppLanguage.MYANMAR) "အားလုံး (${displayWorkers.size})" else "All (${displayWorkers.size})") },
                            modifier = Modifier.testTag("filter_all_workers")
                        )
                        FilterChip(
                            selected = filterUnmarkedOnly,
                            onClick = { filterUnmarkedOnly = true },
                            label = { Text(if (currentLanguage == AppLanguage.MYANMAR) "မသွင်းရသေးသူများ ($unmarkedCount)" else "Unmarked Only ($unmarkedCount)") },
                            modifier = Modifier.testTag("filter_unmarked_workers"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ConstructionGold,
                                selectedLabelColor = Slate900
                            )
                        )
                    }

                    // Trade Category Filter chips for rapid filtering by trade
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.MYANMAR) "အမျိုးအစား:" else "Trade:",
                            fontSize = 11.sp,
                            color = Slate500,
                            fontWeight = FontWeight.SemiBold
                        )

                        FilterChip(
                            selected = workerCategoryFilter == "All",
                            onClick = { workerCategoryFilter = "All" },
                            label = { Text("All", fontSize = 11.sp) }
                        )

                        val posFilterTrades = listOf(
                            "Carpenter" to "လက်သမား",
                            "Mason" to "ပန်းရံ",
                            "Helper" to "အကူ",
                            "Bar Bender" to "သံချည်",
                            "Welder" to "ဝရိန်",
                            "Electrician" to "လျှပ်စစ်",
                            "Painter" to "ဆေးသုတ်",
                            "Foreman" to "ဖိုမင်"
                        )

                        posFilterTrades.forEach { (code, mm) ->
                            val isSelected = workerCategoryFilter.equals(code, ignoreCase = true) || workerCategoryFilter.equals(mm, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    workerCategoryFilter = if (isSelected) "All" else code
                                },
                                label = { Text(mm, fontSize = 11.sp) }
                            )
                        }
                    }

                    val filteredWorkers = displayWorkers.filter { worker ->
                        val myanmarTrade = CategoryManager.getMyanmarName(worker.workerType)
                        val matchesSearch = workerSearchQuery.isBlank() ||
                                worker.name.contains(workerSearchQuery, ignoreCase = true) ||
                                worker.workerId.contains(workerSearchQuery, ignoreCase = true) ||
                                worker.workerType.contains(workerSearchQuery, ignoreCase = true) ||
                                myanmarTrade.contains(workerSearchQuery, ignoreCase = true) ||
                                worker.address.contains(workerSearchQuery, ignoreCase = true) ||
                                worker.nrcNo.contains(workerSearchQuery, ignoreCase = true) ||
                                worker.phone.contains(workerSearchQuery)

                        val matchesCategory = workerCategoryFilter == "All" ||
                                worker.workerType.equals(workerCategoryFilter, ignoreCase = true) ||
                                myanmarTrade.equals(workerCategoryFilter, ignoreCase = true)

                        val matchesUnmarked = !filterUnmarkedOnly || !markedWorkerIds.contains(worker.workerId) || (form.editingRecordId != null && form.selectedWorker?.workerId == worker.workerId)
                        matchesSearch && matchesCategory && matchesUnmarked
                    }.sortedWith { a, b -> com.example.data.model.compareWorkerIds(a.workerId, b.workerId) }

                    if (filteredWorkers.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.MYANMAR) {
                                    if (displayWorkers.isEmpty()) "လုပ်သားစာရင်း မရှိသေးပါ"
                                    else if (filterUnmarkedOnly) "ယနေ့အတွက် အားလုံး စာရင်းသွင်းပြီးဖြစ်ပါသည်!"
                                    else "စစ်ထုတ်မှုနှင့် ကိုက်ညီသော လုပ်သား မတွေ့ပါ"
                                } else {
                                    if (displayWorkers.isEmpty()) "No workers registered yet"
                                    else if (filterUnmarkedOnly) "All workers are already marked for today!"
                                    else "No matching workers found"
                                },
                                color = Slate500,
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp
                            )
                            if (displayWorkers.isNotEmpty() && (workerSearchQuery.isNotBlank() || workerCategoryFilter != "All" || filterUnmarkedOnly)) {
                                Button(
                                    onClick = {
                                        workerSearchQuery = ""
                                        workerCategoryFilter = "All"
                                        filterUnmarkedOnly = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ConstructionGold),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (currentLanguage == AppLanguage.MYANMAR) "အားလုံး ပြန်ပြပါ" else "Reset Filters", color = Slate900, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            if (displayWorkers.isEmpty()) {
                                Button(
                                    onClick = {
                                        showWorkerDialog = false
                                        viewModel.navigateTo(AppScreen.WORKER_MASTER)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ConstructionGold),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Slate900, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (currentLanguage == AppLanguage.MYANMAR) "လုပ်သားအသစ် ထည့်မည်" else "Add Worker", color = Slate900, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(filteredWorkers) { worker ->
                                val isMarked = markedWorkerIds.contains(worker.workerId)
                                val isCurrentEditing = form.editingRecordId != null && form.selectedWorker?.workerId == worker.workerId
                                val isSelectable = !isMarked || isCurrentEditing

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.selectWorker(worker)
                                            showWorkerDialog = false
                                        }
                                        .testTag("worker_select_item_${worker.workerId}"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = when {
                                        !isSelectable -> Slate200.copy(alpha = 0.6f)
                                        form.selectedWorker?.workerId == worker.workerId -> ConstructionGold.copy(alpha = 0.2f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    },
                                    border = if (form.selectedWorker?.workerId == worker.workerId) {
                                        CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ConstructionGold))
                                    } else null
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            TradeIcon(trade = worker.workerType)
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = worker.name,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 15.sp,
                                                    lineHeight = 20.sp,
                                                    color = if (isSelectable) MaterialTheme.colorScheme.onSurface else Slate600,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
                                                    Surface(
                                                        color = ConstructionGold,
                                                        shape = RoundedCornerShape(3.dp)
                                                    ) {
                                                        Text(
                                                            text = worker.workerId,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Slate900,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                    Text(
                                                        "• ${CategoryManager.getDisplayName(worker.workerType)} • ${ExcelExportHelper.formatCurrency(worker.dailyRate)}/ရက်",
                                                        fontSize = 11.sp,
                                                        color = if (isSelectable) Slate600 else Slate500,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                if (worker.address.isNotBlank()) {
                                                    Text(
                                                        "📍 ${worker.address}",
                                                        fontSize = 10.sp,
                                                        color = Slate600
                                                    )
                                                }
                                            }
                                        }

                                        if (!isSelectable) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Surface(
                                                    color = AdvanceRed.copy(alpha = 0.85f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                    ) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                        Text(
                                                            text = if (currentLanguage == AppLanguage.MYANMAR) "သွင်းပြီး" else "LOGGED",
                                                            color = Color.White,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                IconButton(
                                                    onClick = {
                                                        val rec = allAttendance.find { it.date == form.selectedDate && it.workerId == worker.workerId }
                                                        if (rec != null) {
                                                            viewModel.editRecord(rec)
                                                        } else {
                                                            viewModel.selectWorker(worker)
                                                        }
                                                        showWorkerDialog = false
                                                    },
                                                    modifier = Modifier.size(28.dp).testTag("dialog_edit_worker_${worker.workerId}")
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = OtBlue, modifier = Modifier.size(16.dp))
                                                }

                                                IconButton(
                                                    onClick = {
                                                        val rec = allAttendance.find { it.date == form.selectedDate && it.workerId == worker.workerId }
                                                        if (rec != null) {
                                                            recordToDelete = rec
                                                            showWorkerDialog = false
                                                        }
                                                    },
                                                    modifier = Modifier.size(28.dp).testTag("dialog_delete_worker_${worker.workerId}")
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AdvanceRed, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        } else {
                                            Surface(
                                                color = WagesGreenLight,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = if (currentLanguage == AppLanguage.MYANMAR) "ရွေးချယ်ရန်" else "SELECT",
                                                    color = WagesGreenDark,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            showWorkerDialog = false
                            showAddWorkerDialog = true
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ConstructionGold,
                            contentColor = Slate900
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (currentLanguage == AppLanguage.MYANMAR) "+ လုပ်သားအသစ်" else "+ Add Worker", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    TextButton(onClick = { showWorkerDialog = false }) {
                        Text(if (currentLanguage == AppLanguage.MYANMAR) "ပိတ်မည်" else "Close")
                    }
                }
            }
        )
    }

    if (showBatchDialog) {
        com.example.ui.components.BatchAttendanceDialog(
            viewModel = viewModel,
            initialSite = form.selectedSite ?: sites.firstOrNull(),
            date = form.selectedDate,
            onDismiss = { showBatchDialog = false }
        )
    }

    // Delete Confirmation Dialog for Mistaken Entries
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
                            "${target.workerName} (${target.workerId}) ၏ ${target.date} ရက်စွဲပါ တက်ရောက်မှု စာရင်းကို ဖျက်မည်မှာ သေချာပါသလား။"
                        else
                            "Are you sure you want to delete the attendance record for ${target.workerName} on ${target.date}?",
                        fontSize = 13.sp
                    )

                    Surface(
                        color = AdvanceRed.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🏢 ဆိုဒ်: ${target.siteName} (${target.siteCode})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("⏰ အလုပ်ချိန်: နံနက် (${target.morningAttendance}) • ညနေ (${target.eveningAttendance}) • OT: ${target.otHours}h", fontSize = 12.sp)
                            Text("💰 စုစုပေါင်းရငွေ: ${ExcelExportHelper.formatCurrency(target.totalEarned)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WagesGreenDark)
                            if (target.advanceToday > 0) {
                                Text("🔻 ကြိုထုတ်ငွေ: ${ExcelExportHelper.formatCurrency(target.advanceToday)}", fontSize = 12.sp, color = AdvanceRed)
                            }
                            Text(
                                text = if (isMy)
                                    "⚠️ ဤစာရင်းကို ဖျက်လိုက်ပါက နေ့စဉ်လုပ်အားခနှင့် ရှင်းရန်ကျန်ငွေများမှ အပြီးအပိုင် ပယ်ဖျက်သွားမည်ဖြစ်ပါသည်။"
                                else
                                    "⚠️ Deleting will permanently remove this from wages and ledger calculations.",
                                fontSize = 11.sp,
                                color = AdvanceRed
                            )
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
                    modifier = Modifier.testTag("confirm_delete_pos_attendance_btn")
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

    if (showAddWorkerDialog) {
        val nextId = remember(activeWorkers) {
            val maxNum = activeWorkers.mapNotNull { it.workerId.removePrefix("W-").toIntOrNull() }.maxOrNull() ?: (activeWorkers.size + 100)
            "W-${maxNum + 1}"
        }
        com.example.ui.screens.workers.WorkerFormDialog(
            initialWorker = Worker(workerId = nextId, name = ""),
            isNew = true,
            existingWorkerTypes = activeWorkers.map { it.workerType }.distinct(),
            existingWorkerIds = activeWorkers.map { it.workerId },
            onDismiss = { showAddWorkerDialog = false },
            onSave = { newWorker ->
                viewModel.addWorker(newWorker, autoSelectInPos = true) {
                    showAddWorkerDialog = false
                    android.widget.Toast.makeText(
                        context,
                        if (currentLanguage == com.example.util.AppLanguage.MYANMAR)
                            "လုပ်သား ${newWorker.name} ကို ထည့်သွင်းပြီး စာရင်းသွင်းရန် အဆင်သင့်ဖြစ်ပါပြီ"
                        else
                            "Added ${newWorker.name} and selected for attendance",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}

@Composable
fun PresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedBg: Color = ConstructionGold
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("preset_chip_${label.replace("\n", "_").lowercase()}"),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) selectedBg else Slate100,
        border = if (isSelected) null else CardDefaults.outlinedCardBorder()
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Slate900 else Slate700,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun PosSegmentButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = ConstructionGold
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("pos_segment_${label.take(2).trim()}"),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) activeColor else Slate100,
        border = if (isSelected) null else CardDefaults.outlinedCardBorder()
    ) {
        Box(
            modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Slate700,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CalculationLine(
    label: String,
    value: String,
    color: Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Slate300,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = if (isBold) 15.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
