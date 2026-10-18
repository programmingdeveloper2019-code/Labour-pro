package com.example.ui.screens.workers

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Worker
import com.example.ui.components.StatusBadge
import com.example.ui.components.TradeIcon
import com.example.ui.theme.*
import com.example.util.CategoryManager
import com.example.util.ExcelExportHelper

@Composable
fun WorkerDetailDialog(
    worker: Worker,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onMarkAttendance: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TradeIcon(trade = worker.workerType, modifier = Modifier.size(42.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = worker.name,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                lineHeight = 24.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusBadge(status = worker.status)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                color = Slate500
                            )
                        }
                    }
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Worker Address & Hometown (Primary Highlight)
                item {
                    Surface(
                        color = Color(0xFFEFF6FF), // soft blue
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = SafetyOrangeDark,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "နေရပ်လိပ်စာ / ဇာတိ (Address & Hometown)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate600
                                )
                                Text(
                                    text = if (worker.address.isNotBlank()) worker.address else "လိပ်စာ မထည့်သွင်းရသေးပါ (Address not recorded)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (worker.address.isNotBlank()) Slate900 else Slate400
                                )
                            }
                        }
                    }
                }

                // 2. Wage & Skill Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "လုပ်အားခနှင့် ကျွမ်းကျင်မှု အဆင့် (Rates & Skill)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Slate700
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("နေ့တွက်ခ (Daily Rate)", fontSize = 11.sp, color = Slate500)
                                    Text(
                                        ExcelExportHelper.formatCurrency(worker.dailyRate),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WagesGreenDark
                                    )
                                }
                                Column {
                                    Text("အချိန်ပိုခ (OT Rate)", fontSize = 11.sp, color = Slate500)
                                    Text(
                                        "${ExcelExportHelper.formatCurrency(worker.otRate)}/hr",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OtBlue
                                    )
                                }
                                Column {
                                    Text("အဆင့် (Skill Rank)", fontSize = 11.sp, color = Slate500)
                                    Text(
                                        worker.skillLevel,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SafetyOrangeDark
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Contact Phone & Actions
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ဆက်သွယ်ရန် (Contact Information)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Slate700
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = WagesGreenDark, modifier = Modifier.size(20.dp))
                                    Column {
                                        Text(
                                            text = if (worker.phone.isNotBlank()) worker.phone else "ဖုန်းနံပါတ် မရှိပါ",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text("ပင်မ ဖုန်းနံပါတ်", fontSize = 10.sp, color = Slate500)
                                    }
                                }
                                if (worker.phone.isNotBlank()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                                    data = Uri.parse("tel:${worker.phone.replace(" ", "")}")
                                                }
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = "Call", tint = WagesGreenDark)
                                        }
                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                    data = Uri.parse("smsto:${worker.phone.replace(" ", "")}")
                                                }
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Sms, contentDescription = "SMS", tint = OtBlue)
                                        }
                                    }
                                }
                            }

                            // Emergency Contact
                            if (worker.emergencyPhone.isNotBlank() || worker.emergencyContactName.isNotBlank()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = AdvanceRed, modifier = Modifier.size(20.dp))
                                        Column {
                                            Text(
                                                text = "အရေးပေါ်: ${worker.emergencyContactName.ifBlank { "အရေးပေါ်လူကြီး" }}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AdvanceRed
                                            )
                                            Text(
                                                text = worker.emergencyPhone.ifBlank { "ဖုန်းမရှိပါ" },
                                                fontSize = 11.sp,
                                                color = Slate600
                                            )
                                        }
                                    }
                                    if (worker.emergencyPhone.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                                    data = Uri.parse("tel:${worker.emergencyPhone.replace(" ", "")}")
                                                }
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.PhoneCallback, contentDescription = "Emergency Call", tint = AdvanceRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Verification & Legal Data (NRC, Blood Type, Payment)
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "အထောက်အထားနှင့် ငွေပေးချေမှု (Verification & Payment)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Slate700
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("မှတ်ပုံတင်အမှတ် (NRC):", fontSize = 12.sp, color = Slate600)
                                Text(
                                    text = if (worker.nrcNo.isNotBlank()) worker.nrcNo else "မထည့်ရသေးပါ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("သွေးအမျိုးအစား (Blood):", fontSize = 12.sp, color = Slate600)
                                Text(
                                    text = if (worker.bloodType.isNotBlank()) "🩸 ${worker.bloodType}" else "မထည့်ရသေးပါ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AdvanceRed
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("လစာထုတ်စနစ် (Payment):", fontSize = 12.sp, color = Slate600)
                                Text(
                                    text = "${worker.paymentMethod} ${if (worker.paymentAccount.isNotBlank()) "(${worker.paymentAccount})" else ""}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (worker.joinedDate.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("စတင်ဝင်ရောက်ရက်:", fontSize = 12.sp, color = Slate600)
                                    Text(text = worker.joinedDate, fontSize = 12.sp, color = Slate600)
                                }
                            }

                            if (worker.notes.isNotBlank()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                Text("အထူးမှတ်ချက် / အတွေ့အကြုံ:", fontSize = 11.sp, color = Slate500)
                                Text(worker.notes, fontSize = 12.sp, color = Slate800)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ပြင်ဆင်မည်")
                }
                Button(
                    onClick = onMarkAttendance,
                    colors = ButtonDefaults.buttonColors(containerColor = ConstructionGold),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.FactCheck, contentDescription = null, tint = Slate900, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ယနေ့ စာရင်းသွင်းမည်", color = Slate900, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ပိတ်မည်")
            }
        }
    )
}
