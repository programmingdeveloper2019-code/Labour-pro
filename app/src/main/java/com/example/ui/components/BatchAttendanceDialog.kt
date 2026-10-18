package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecord
import com.example.data.model.Site
import com.example.data.model.Worker
import com.example.ui.theme.*
import com.example.ui.viewmodel.LabourViewModel
import com.example.util.AppLanguage
import com.example.util.CategoryManager
import com.example.util.ExcelExportHelper
import com.example.util.LanguageManager

data class WorkerBatchItem(
    val worker: Worker,
    var status: String = "P", // "P" (1.0), "HD" (0.5), "A" (0.0)
    var otHours: Double = 0.0,
    var advance: Double = 0.0,
    var remark: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchAttendanceDialog(
    viewModel: LabourViewModel,
    initialSite: Site?,
    date: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    val isMy = currentLanguage == AppLanguage.MYANMAR
    val activeWorkers by viewModel.activeWorkers.collectAsState()
    val markedWorkerIds by viewModel.markedWorkerIdsForSelectedDate.collectAsState()

    val availableWorkers = remember(activeWorkers, markedWorkerIds) {
        activeWorkers.filter { !markedWorkerIds.contains(it.workerId) }
            .sortedWith { a, b -> com.example.data.model.compareWorkerIds(a.workerId, b.workerId) }
    }

    // State list of workers to mark
    var batchList by remember(availableWorkers) {
        mutableStateOf(availableWorkers.map { WorkerBatchItem(worker = it, status = "P") })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ConstructionGold
                    ) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Slate900,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(22.dp)
                        )
                    }
                    Text(
                        text = if (isMy) "ဆိုဒ်လိုက် အားလုံးတစ်ပြိုင်နက် မှတ်ရန်" else "Quick Batch Attendance",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
                Text(
                    text = "${initialSite?.siteName ?: "Site"} • $date (${batchList.size} ${if (isMy) "ဦး" else "workers"})",
                    fontSize = 12.sp,
                    color = Slate500
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quick Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            batchList = batchList.map { it.copy(status = "P") }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WagesGreenDark),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("mark_all_present_btn")
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isMy) "အားလုံး ၁.၀ တက်ရောက်" else "Mark All 1.0", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            batchList = batchList.map { it.copy(status = "A") }
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(if (isMy) "အားလုံး ပျက်ကွက် (A)" else "Mark All Absent", fontSize = 11.sp)
                    }
                }

                Divider()

                if (batchList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isMy) "ယနေ့အတွက် အားလုံး စာရင်းသွင်းပြီးပါပြီ!" else "All active workers are already recorded for this date!",
                            color = Slate500,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(batchList) { item ->
                            val index = batchList.indexOf(item)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = CardDefaults.outlinedCardBorder(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                item.worker.name,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp,
                                                lineHeight = 20.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                "${item.worker.workerId} • ${CategoryManager.getDisplayName(item.worker.workerType)} • ${ExcelExportHelper.formatCurrency(item.worker.dailyRate)}",
                                                fontSize = 11.sp,
                                                color = Slate500
                                            )
                                        }
                                        // Status Selector (P, HD, A)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf("P" to "1.0", "HD" to "0.5", "A" to "0").forEach { (st, lbl) ->
                                                val isSel = item.status == st
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = when {
                                                        isSel && st == "P" -> WagesGreenDark
                                                        isSel && st == "HD" -> OtBlue
                                                        isSel && st == "A" -> AdvanceRed
                                                        else -> MaterialTheme.colorScheme.surface
                                                    },
                                                    border = if (!isSel) CardDefaults.outlinedCardBorder() else null,
                                                    modifier = Modifier.clickable {
                                                        val updated = batchList.toMutableList()
                                                        updated[index] = item.copy(status = st)
                                                        batchList = updated
                                                    }
                                                ) {
                                                    Text(
                                                        text = "$st ($lbl)",
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // OT & Advance Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = if (item.otHours > 0) item.otHours.toString() else "",
                                            onValueChange = { str ->
                                                val h = str.toDoubleOrNull() ?: 0.0
                                                val updated = batchList.toMutableList()
                                                updated[index] = item.copy(otHours = h)
                                                batchList = updated
                                            },
                                            label = { Text("OT နာရီ", fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = if (item.advance > 0) item.advance.toInt().toString() else "",
                                            onValueChange = { str ->
                                                val adv = str.toDoubleOrNull() ?: 0.0
                                                val updated = batchList.toMutableList()
                                                updated[index] = item.copy(advance = adv)
                                                batchList = updated
                                            },
                                            label = { Text("ကြိုထုတ်ငွေ", fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1.2f),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (initialSite == null) {
                        Toast.makeText(context, "Please select a site first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val records = batchList.map { item ->
                        val (morn, eve) = when (item.status) {
                            "P" -> "P" to "P"
                            "HD" -> "P" to "A"
                            else -> "A" to "A"
                        }
                        AttendanceRecord(
                            date = date,
                            siteCode = initialSite.siteCode,
                            siteName = initialSite.siteName,
                            workerId = item.worker.workerId,
                            workerName = item.worker.name,
                            workerType = item.worker.workerType,
                            morningAttendance = morn,
                            eveningAttendance = eve,
                            dailyRate = item.worker.dailyRate,
                            otHours = item.otHours,
                            otRate = item.worker.otRate,
                            advanceToday = item.advance,
                            paidToday = 0.0,
                            remark = item.remark
                        )
                    }
                    viewModel.saveBatchAttendance(records) {
                        Toast.makeText(
                            context,
                            if (isMy) "${records.size} ဦး၏ စာရင်းကို အောင်မြင်စွာ သိမ်းဆည်းပြီးပါပြီ!" else "Batch attendance saved for ${records.size} workers!",
                            Toast.LENGTH_LONG
                        ).show()
                        onDismiss()
                    }
                },
                enabled = batchList.isNotEmpty() && initialSite != null,
                colors = ButtonDefaults.buttonColors(containerColor = ConstructionGold),
                modifier = Modifier.testTag("save_batch_attendance_btn")
            ) {
                Text(
                    text = if (isMy) "အားလုံး သိမ်းဆည်းမည် (${batchList.size} ဦး)" else "Save All (${batchList.size})",
                    color = Slate900,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isMy) "မလုပ်တော့ပါ" else "Cancel")
            }
        }
    )
}
