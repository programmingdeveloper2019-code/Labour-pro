package com.example.ui.components

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
import com.example.data.model.ConstructionTrade
import com.example.ui.theme.*
import com.example.util.CategoryManager
import com.example.util.ExcelExportHelper

@Composable
fun CategorySelectionDialog(
    currentTradeCode: String,
    existingWorkerTypes: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSelectTrade: (ConstructionTrade) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val allTrades = remember(refreshTrigger, existingWorkerTypes) {
        CategoryManager.getAllTrades(context, existingWorkerTypes)
    }

    val filteredTrades = remember(allTrades, searchQuery) {
        if (searchQuery.isBlank()) {
            allTrades
        } else {
            allTrades.filter {
                it.myanmarName.contains(searchQuery, ignoreCase = true) ||
                it.englishName.contains(searchQuery, ignoreCase = true) ||
                it.code.contains(searchQuery, ignoreCase = true) ||
                it.myanmarDetail.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "လုပ်သား အမျိုးအစား ရွေးချယ်ရန်",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "ဆောက်လုပ်ရေး လုပ်ငန်းခွင် ကျွမ်းကျင်မှုများ",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ရှာဖွေရန် (ဥပမာ- လက်သမား၊ ပန်းရံ၊ Carpenter...)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate500) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_search_field"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                // Add Custom Category Button
                OutlinedButton(
                    onClick = { showAddCustomDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_custom_category_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SafetyOrangeDark
                    )
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ စိတ်ကြိုက် အမျိုးအစားအသစ် ထည့်မည်", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // List of trades
                if (filteredTrades.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ရှာမတွေ့ပါ",
                                fontWeight = FontWeight.Bold,
                                color = Slate500
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "အထက်ပါ '+ စိတ်ကြိုက်' ခလုတ်ဖြင့် အသစ်ထည့်နိုင်ပါသည်",
                                fontSize = 12.sp,
                                color = Slate400
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredTrades) { trade ->
                            val isSelected = trade.code.equals(currentTradeCode, ignoreCase = true) ||
                                    trade.englishName.equals(currentTradeCode, ignoreCase = true) ||
                                    trade.myanmarName.equals(currentTradeCode, ignoreCase = true)

                            TradeSelectionCard(
                                trade = trade,
                                isSelected = isSelected,
                                onClick = {
                                    onSelectTrade(trade)
                                    onDismiss()
                                },
                                onDelete = if (trade.isCustom) {
                                    {
                                        CategoryManager.deleteCustomTrade(context, trade.code)
                                        refreshTrigger++
                                    }
                                } else null
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ပိတ်မည် (Close)")
            }
        }
    )

    // Add Custom Trade Dialog
    if (showAddCustomDialog) {
        AddCustomTradeDialog(
            onDismiss = { showAddCustomDialog = false },
            onSave = { newTrade ->
                CategoryManager.saveCustomTrade(context, newTrade)
                refreshTrigger++
                showAddCustomDialog = false
                onSelectTrade(newTrade)
                onDismiss()
            }
        )
    }
}

@Composable
fun TradeSelectionCard(
    trade: ConstructionTrade,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("trade_item_${trade.code}"),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) ConstructionGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ConstructionGold))
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                TradeIcon(trade = trade.code)
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = trade.myanmarName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (trade.englishName.isNotBlank() && trade.englishName != trade.myanmarName) {
                            Text(
                                text = "(${trade.englishName})",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }
                        if (trade.isCustom) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SafetyOrangeDark.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "စိတ်ကြိုက်",
                                    fontSize = 10.sp,
                                    color = SafetyOrangeDark,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    if (trade.myanmarDetail.isNotBlank()) {
                        Text(
                            text = trade.myanmarDetail,
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }

                    Text(
                        text = "ပုံမှန်နှုန်း: ${ExcelExportHelper.formatCurrency(trade.defaultDailyRate)}/ရက် • OT: ${ExcelExportHelper.formatCurrency(trade.defaultOtRate)}/နာရီ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WagesGreenDark
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = WagesGreenDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Custom Trade",
                            tint = AdvanceRed.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddCustomTradeDialog(
    onDismiss: () -> Unit,
    onSave: (ConstructionTrade) -> Unit
) {
    var myanmarName by remember { mutableStateOf("") }
    var englishName by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var dailyRateStr by remember { mutableStateOf("25000") }
    var otRateStr by remember { mutableStateOf("3500") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("စိတ်ကြိုက် အမျိုးအစားအသစ် ထည့်ရန်", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = myanmarName,
                    onValueChange = { myanmarName = it },
                    label = { Text("အမျိုးအစား အမည် (မြန်မာ) *") },
                    placeholder = { Text("ဥပမာ- သံကူကွန်ကရစ်၊ ကြွေပြားကပ်၊ စက်မောင်း") },
                    modifier = Modifier.fillMaxWidth().testTag("custom_trade_mm_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = englishName,
                    onValueChange = { englishName = it },
                    label = { Text("အင်္ဂလိပ် အမည် (English Name - Optional)") },
                    placeholder = { Text("e.g. Tile Worker, Steel Fixer") },
                    modifier = Modifier.fillMaxWidth().testTag("custom_trade_en_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text("အသေးစိတ် လုပ်ငန်းအမျိုးအစား (Detail)") },
                    placeholder = { Text("ဥပမာ- ကြမ်းခင်း၊ နံရံကပ်") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dailyRateStr,
                        onValueChange = { dailyRateStr = it },
                        label = { Text("ပုံမှန်နေ့တွက် (ကျပ်)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = otRateStr,
                        onValueChange = { otRateStr = it },
                        label = { Text("OT နှုန်း (ကျပ်/နာရီ)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (myanmarName.isNotBlank()) {
                        val dRate = dailyRateStr.toDoubleOrNull() ?: 25000.0
                        val oRate = otRateStr.toDoubleOrNull() ?: 3500.0
                        val finalEn = if (englishName.isNotBlank()) englishName.trim() else myanmarName.trim()
                        val code = finalEn.replace(" ", "_")
                        onSave(
                            ConstructionTrade(
                                code = code,
                                englishName = finalEn,
                                myanmarName = myanmarName.trim(),
                                myanmarDetail = detail.trim(),
                                defaultDailyRate = dRate,
                                defaultOtRate = oRate,
                                isCustom = true
                            )
                        )
                    }
                },
                enabled = myanmarName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ConstructionGold)
            ) {
                Text("ထည့်သွင်းမည် (Add)", color = Slate900, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("မလုပ်တော့ပါ (Cancel)")
            }
        }
    )
}
