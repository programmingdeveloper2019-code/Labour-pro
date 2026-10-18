package com.example.ui.screens.workers

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Worker
import com.example.ui.components.CategorySelectionDialog
import com.example.ui.components.TradeIcon
import com.example.ui.theme.*
import com.example.util.CategoryManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerFormDialog(
    initialWorker: Worker,
    isNew: Boolean,
    existingWorkerTypes: List<String> = emptyList(),
    existingWorkerIds: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Worker) -> Unit
) {
    val context = LocalContext.current
    var workerId by remember { mutableStateOf(initialWorker.workerId) }
    var name by remember { mutableStateOf(initialWorker.name) }
    var gender by remember { mutableStateOf(initialWorker.gender) }
    var workerType by remember { mutableStateOf(initialWorker.workerType) }
    var dailyRateStr by remember { mutableStateOf(initialWorker.dailyRate.toInt().toString()) }
    var otRateStr by remember { mutableStateOf(initialWorker.otRate.toInt().toString()) }
    var status by remember { mutableStateOf(initialWorker.status) }
    var phone by remember { mutableStateOf(initialWorker.phone) }
    var address by remember { mutableStateOf(initialWorker.address) }
    var nrcNo by remember { mutableStateOf(initialWorker.nrcNo) }
    var emergencyContactName by remember { mutableStateOf(initialWorker.emergencyContactName) }
    var emergencyPhone by remember { mutableStateOf(initialWorker.emergencyPhone) }
    var bloodType by remember { mutableStateOf(initialWorker.bloodType) }
    var skillLevel by remember { mutableStateOf(initialWorker.skillLevel) }
    var paymentMethod by remember { mutableStateOf(initialWorker.paymentMethod) }
    var paymentAccount by remember { mutableStateOf(initialWorker.paymentAccount) }
    var notes by remember { mutableStateOf(initialWorker.notes) }

    var showCategoryPickerDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isNew) Icons.Default.PersonAdd else Icons.Default.Edit,
                    contentDescription = null,
                    tint = ConstructionGoldDark
                )
                Text(
                    text = if (isNew) "Add New Worker (လုပ်သားအသစ်)" else "Edit Worker (လုပ်သားပြင်ဆင်ခြင်း)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 540.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Basic Info Header
                item {
                    Text(
                        text = "အခြေခံ အချက်အလက်များ (Basic Information)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Slate700
                    )
                }

                item {
                    OutlinedTextField(
                        value = workerId,
                        onValueChange = { workerId = it },
                        label = { Text("Worker ID (ကုဒ် e.g. W-101) *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("worker_id_input"),
                        enabled = isNew,
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name (လုပ်သားအမည်) *") },
                        placeholder = { Text("ဥပမာ- ကိုအောင်ကျော်၊ ဦးဘစိန်") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("worker_name_input"),
                        singleLine = true
                    )
                }

                // WORKER ADDRESS / HOMETOWN (Crucial User Requirement)
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Worker Address / Hometown (နေရပ်လိပ်စာ / ဇာတိ) *") },
                        placeholder = { Text("ဥပမာ- ပဲခူးတိုင်း၊ ညောင်လေးပင် (သို့) လှိုင်သာယာ၊ ရန်ကုန်") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = SafetyOrangeDark
                            )
                        },
                        supportingText = {
                            Text("ဘယ်အရပ်ဒေသမှ လာရောက်လုပ်ကိုင်သည်ကို မှတ်သားရန်", fontSize = 11.sp, color = Slate500)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("worker_address_input"),
                        singleLine = false,
                        maxLines = 2
                    )
                }

                // TRADE / CATEGORY
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ဆောက်လုပ်ရေး လုပ်ငန်းခွင်အမျိုးအစား (Trade) *",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate700
                            )
                            TextButton(
                                onClick = { showCategoryPickerDialog = true },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("အားလုံး / စိတ်ကြိုက်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCategoryPickerDialog = true }
                                .testTag("worker_type_picker"),
                            shape = RoundedCornerShape(10.dp),
                            color = ConstructionGold.copy(alpha = 0.15f),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(ConstructionGoldDark)
                            )
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
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    TradeIcon(trade = workerType)
                                    Column {
                                        Text(
                                            text = CategoryManager.getMyanmarName(workerType),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Slate900
                                        )
                                        Text(
                                            text = workerType,
                                            fontSize = 11.sp,
                                            color = Slate600
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("ပြောင်းရန်", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SafetyOrangeDark)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select", tint = SafetyOrangeDark)
                                }
                            }
                        }

                        // Quick-Tap category chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val quickTrades = listOf(
                                "Carpenter" to "လက်သမား",
                                "Mason" to "ပန်းရံ",
                                "Helper" to "အကူ",
                                "Bar Bender" to "သံချည်",
                                "Welder" to "ဝရိန်",
                                "Electrician" to "လျှပ်စစ်",
                                "Painter" to "ဆေးသုတ်",
                                "Foreman" to "ဖိုမင်"
                            )
                            quickTrades.forEach { (code, mm) ->
                                val isSelected = workerType.equals(code, ignoreCase = true) || workerType.equals(mm, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        workerType = code
                                        val std = CategoryManager.getTradeForCode(code, context)
                                        if (isNew || dailyRateStr == "20000" || dailyRateStr.isBlank()) {
                                            dailyRateStr = std.defaultDailyRate.toInt().toString()
                                            otRateStr = std.defaultOtRate.toInt().toString()
                                        }
                                    },
                                    label = { Text(mm, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ConstructionGold,
                                        selectedLabelColor = Slate900
                                    )
                                )
                            }
                        }
                    }
                }

                // Skill Rank / Level
                item {
                    Text("ကျွမ်းကျင်မှု အဆင့် (Skill Rank):", fontSize = 12.sp, color = Slate600)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Foreman" to "ခေါင်းဆောင်/ဆရာကြီး",
                            "Skilled" to "ကျွမ်းကျင်လုပ်သား",
                            "Semi-Skilled" to "အလတ်တန်း",
                            "Helper" to "အကူ/အလုပ်သင်"
                        ).forEach { (rank, mm) ->
                            FilterChip(
                                selected = skillLevel == rank,
                                onClick = { skillLevel = rank },
                                label = { Text(mm, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Rates
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = dailyRateStr,
                            onValueChange = { dailyRateStr = it },
                            label = { Text("Daily Rate (ကျပ်/ရက်)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("daily_rate_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = otRateStr,
                            onValueChange = { otRateStr = it },
                            label = { Text("OT Rate (ကျပ်/နာရီ)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ot_rate_input"),
                            singleLine = true
                        )
                    }
                }

                // Gender & Status
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ကျား/မ (Gender):", fontSize = 11.sp, color = Slate600)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Male" to "ကျား", "Female" to "မ").forEach { (g, mm) ->
                                    FilterChip(
                                        selected = gender == g,
                                        onClick = { gender = g },
                                        label = { Text(mm, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("အခြေအနေ (Status):", fontSize = 11.sp, color = Slate600)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Active" to "လက်ရှိ", "Inactive" to "နားထား").forEach { (s, mm) ->
                                    FilterChip(
                                        selected = status == s,
                                        onClick = { status = s },
                                        label = { Text(mm, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Verification & Emergency Section
                item {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "လုံခြုံရေးနှင့် အရေးပေါ် အချက်အလက် (Safety & Verification)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Slate700
                    )
                }

                // NRC & Phone
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number (လုပ်သားဖုန်းနံပါတ်)") },
                        placeholder = { Text("09xxxxxxxxx") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = WagesGreenDark) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("worker_phone_input"),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = nrcNo,
                        onValueChange = { nrcNo = it },
                        label = { Text("NRC / ID Card No. (မှတ်ပုံတင်အမှတ်)") },
                        placeholder = { Text("ဥပမာ- ၁၂/လမတ(နိုင်)၁၂၃၄၅၆") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = OtBlue) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("worker_nrc_input"),
                        singleLine = true
                    )
                }

                // Emergency Contact Name & Phone
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = emergencyContactName,
                            onValueChange = { emergencyContactName = it },
                            label = { Text("Emergency Contact (အမည်)") },
                            placeholder = { Text("ဥပမာ- ဒေါ်တင်တင် (ဇနီး)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("worker_emergency_name_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = emergencyPhone,
                            onValueChange = { emergencyPhone = it },
                            label = { Text("အရေးပေါ် ဖုန်းနံပါတ်") },
                            placeholder = { Text("09xxxxxxxxx") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("worker_emergency_phone_input"),
                            singleLine = true
                        )
                    }
                }

                // Blood Type & Payment
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = bloodType,
                            onValueChange = { bloodType = it },
                            label = { Text("သွေး (Blood)") },
                            placeholder = { Text("O+, B+, A+") },
                            modifier = Modifier
                                .weight(0.35f)
                                .testTag("worker_blood_type_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = paymentMethod,
                            onValueChange = { paymentMethod = it },
                            label = { Text("Payment (ငွေပေးစနစ်)") },
                            placeholder = { Text("KBZPay, Wave, Cash") },
                            modifier = Modifier
                                .weight(0.65f)
                                .testTag("worker_payment_method_input"),
                            singleLine = true
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = paymentAccount,
                        onValueChange = { paymentAccount = it },
                        label = { Text("Wallet Account / KPay / Wave No.") },
                        placeholder = { Text("09xxxxxxxxx") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("worker_payment_account_input"),
                        singleLine = true
                    )
                }

                // Notes
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (အထူးမှတ်ချက် / အလုပ်အတွေ့အကြုံ)") },
                        placeholder = { Text("ဥပမာ- ငြမ်းဆင်ကျွမ်းကျင်၊ မျက်နှာကြက်ကပ်ရ") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("worker_notes_input"),
                        singleLine = false,
                        maxLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "လုပ်သားအမည် ထည့်သွင်းပေးပါ", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    var effectiveWorkerId = workerId.trim()
                    if (effectiveWorkerId.isBlank()) {
                        val maxNum = existingWorkerIds.mapNotNull { it.removePrefix("W-").toIntOrNull() }.maxOrNull() ?: (existingWorkerIds.size + 100)
                        effectiveWorkerId = "W-${maxNum + 1}"
                    }
                    if (isNew && existingWorkerIds.any { it.equals(effectiveWorkerId, ignoreCase = true) }) {
                        val maxNum = existingWorkerIds.mapNotNull { it.removePrefix("W-").toIntOrNull() }.maxOrNull() ?: (existingWorkerIds.size + 100)
                        effectiveWorkerId = "W-${maxNum + 1}"
                    }
                    onSave(
                        Worker(
                            workerId = effectiveWorkerId,
                            name = name.trim(),
                            gender = gender,
                            workerType = workerType.trim(),
                            dailyRate = dailyRateStr.toDoubleOrNull() ?: 20000.0,
                            otRate = otRateStr.toDoubleOrNull() ?: 3000.0,
                            status = status,
                            phone = phone.trim(),
                            address = address.trim(),
                            nrcNo = nrcNo.trim(),
                            emergencyContactName = emergencyContactName.trim(),
                            emergencyPhone = emergencyPhone.trim(),
                            bloodType = bloodType.trim(),
                            skillLevel = skillLevel.trim(),
                            paymentMethod = paymentMethod.trim(),
                            paymentAccount = paymentAccount.trim(),
                            joinedDate = if (initialWorker.joinedDate.isNotBlank()) {
                                initialWorker.joinedDate
                            } else {
                                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            },
                            notes = notes.trim()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ConstructionGold),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_worker_btn")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Slate900, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isNew) "သိမ်းဆည်းမည် (Save Worker)" else "ပြင်ဆင်ချက် သိမ်းမည်",
                    color = Slate900,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("cancel_worker_btn")
            ) {
                Text("ပယ်ဖျက်မည်")
            }
        }
    )

    if (showCategoryPickerDialog) {
        CategorySelectionDialog(
            currentTradeCode = workerType,
            existingWorkerTypes = existingWorkerTypes,
            onDismiss = { showCategoryPickerDialog = false },
            onSelectTrade = { selectedTrade ->
                workerType = selectedTrade.code
                if (isNew || dailyRateStr == "20000" || dailyRateStr.isBlank()) {
                    dailyRateStr = selectedTrade.defaultDailyRate.toInt().toString()
                    otRateStr = selectedTrade.defaultOtRate.toInt().toString()
                }
                showCategoryPickerDialog = false
            }
        )
    }
}
