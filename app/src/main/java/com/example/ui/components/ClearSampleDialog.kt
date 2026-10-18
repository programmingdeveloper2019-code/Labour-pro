package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.LabourViewModel
import com.example.util.AppLanguage
import com.example.util.LanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearSampleDialog(
    viewModel: LabourViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    val isMy = currentLanguage == AppLanguage.MYANMAR

    var confirmActionType by remember { mutableStateOf<String?>(null) } // "ATTENDANCE_ONLY", "WIPE_ALL", "RELOAD_SAMPLE"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AdvanceRed.copy(alpha = 0.15f)
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = AdvanceRed,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = if (isMy) "နမူနာဒေတာ ဖျက်ရန် / ပြန်လည်စတင်ရန်" else "Clear Sample Data / Reset",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = if (isMy) "လုပ်ငန်းခွင်ဒေတာ အစစ်များ စတင်သွင်းနိုင်ရန်" else "Prepare database for real jobsite work",
                        fontSize = 11.sp,
                        color = Slate500
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Option 1: 1-Click Clear Attendance Only
                Card(
                    onClick = { confirmActionType = "ATTENDANCE_ONLY" },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("clear_attendance_only_btn")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ConstructionGoldLight
                        ) {
                            Icon(
                                Icons.Default.EventBusy,
                                contentDescription = null,
                                tint = ConstructionGoldDark,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isMy) "တက်ရောက်မှု မှတ်တမ်းများ ဖျက်မည် (၁ ချက်နှိပ်)" else "Clear Attendance Records (1-Click)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isMy) "ဆိုဒ်နှင့် လုပ်သားများ မဖျက်ဘဲ ယနေ့မှစ၍ အသစ်မှတ်နိုင်ပါသည်" else "Keeps sites & workers intact, wipes all logs to 0",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate400)
                    }
                }

                // Option 2: 1-Click Wipe All Data Completely
                Card(
                    onClick = { confirmActionType = "WIPE_ALL" },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AdvanceRed.copy(alpha = 0.08f)),
                    modifier = Modifier.testTag("wipe_all_data_btn")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AdvanceRed.copy(alpha = 0.2f)
                        ) {
                            Icon(
                                Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = AdvanceRed,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isMy) "ဒေတာ အားလုံး အပြီးတိုင် ဖျက်မည်" else "Wipe All Data Completely (Clean Slate)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = AdvanceRed
                            )
                            Text(
                                text = if (isMy) "ဆိုဒ်၊ လုပ်သား၊ မှတ်တမ်း အားလုံးကို အပြီးတိုင် သန့်ရှင်းမည်" else "Completely wipes sites, workers, and attendance",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate400)
                    }
                }

                // Option 3: Reload Standard Myanmar Workforce
                Card(
                    onClick = { confirmActionType = "RELOAD_WORKERS" },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ConstructionGold.copy(alpha = 0.15f)),
                    modifier = Modifier.testTag("reload_standard_workforce_btn")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ConstructionGold
                        ) {
                            Icon(
                                Icons.Default.GroupAdd,
                                contentDescription = null,
                                tint = Slate900,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isMy) "အခြေခံ လုပ်သားစာရင်း အမြဲပြပေးမည်" else "Load Standard Workforce",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isMy) "လက်သမား၊ ပန်းရံ၊ သံချည်၊ ဝရိန်၊ လျှပ်စစ်၊ အကူ အပြည့်အစုံ ပြန်ထည့်ပါမည်" else "Restores standard Myanmar construction workforce",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate400)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_clear_dialog_btn")
            ) {
                Text(if (isMy) "ပိတ်မည်" else "Close", fontWeight = FontWeight.Bold)
            }
        }
    )

    // Confirmation Alert
    if (confirmActionType != null) {
        val action = confirmActionType!!
        AlertDialog(
            onDismissRequest = { confirmActionType = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = AdvanceRed) },
            title = {
                Text(
                    text = when (action) {
                        "ATTENDANCE_ONLY" -> if (isMy) "တက်ရောက်မှု မှတ်တမ်းများ ဖျက်မည်လား?" else "Clear All Attendance Records?"
                        "WIPE_ALL" -> if (isMy) "ဒေတာ အားလုံး ဖျက်မည်လား?" else "Wipe All Data Completely?"
                        "RELOAD_WORKERS" -> if (isMy) "အခြေခံ လုပ်သားစာရင်း ပြန်ထည့်မည်လား?" else "Load Standard Workforce?"
                        else -> if (isMy) "နမူနာဒေတာ ပြန်လည်ထည့်မည်လား?" else "Reload Demo Sample Data?"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = when (action) {
                        "ATTENDANCE_ONLY" -> if (isMy) {
                            "တက်ရောက်မှု မှတ်တမ်းအားလုံးကို ဖျက်ပါမည်။ လုပ်သားနှင့် ဆိုဒ်စာရင်းများ မပျက်စီးဘဲ ဆက်လက်တည်ရှိနေပါမည်။"
                        } else {
                            "This will delete all daily attendance records. Your registered workers and sites will be preserved."
                        }
                        "WIPE_ALL" -> if (isMy) {
                            "⚠️ သတိပေးချက်- ဆောက်လုပ်ရေးဆိုဒ်များ၊ လုပ်သားများနှင့် တက်ရောက်မှု မှတ်တမ်းများ အားလုံးကို အပြီးတိုင် ဖျက်ဆီးပစ်ပါမည်။ အသစ်လုံးဝပြန်စပါမည်။"
                        } else {
                            "⚠️ Warning: This will permanently delete ALL sites, workers, and attendance records from the app. You will start with an empty database."
                        }
                        "RELOAD_WORKERS" -> if (isMy) {
                            "လုပ်သားစာရင်း အမြဲပြည့်စုံနေစေရန် လက်သမား၊ ပန်းရံ၊ သံချည်၊ ဝရိန်၊ လျှပ်စစ်၊ အကူ အစရှိသော အခြေခံလုပ်သားစာရင်းကို စနစ်တကျ ပြန်လည်ထည့်သွင်းပေးပါမည်။"
                        } else {
                            "This will populate the standard Myanmar construction workforce so your worker list is always complete."
                        }
                        else -> if (isMy) {
                            "နမူနာဒေတာများကို လက်ရှိဒေတာပေါ်တွင် အစားထိုးထည့်သွင်းပါမည်။"
                        } else {
                            "This will replace current database records with demo construction project samples."
                        }
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (action) {
                            "ATTENDANCE_ONLY" -> {
                                viewModel.clearAttendanceOnly {
                                    Toast.makeText(
                                        context,
                                        if (isMy) "တက်ရောက်မှု မှတ်တမ်းများ အားလုံး ဖျက်ပြီးပါပြီ!" else "All attendance records cleared!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    confirmActionType = null
                                    onDismiss()
                                }
                            }
                            "WIPE_ALL" -> {
                                viewModel.clearAllDataCompletely {
                                    Toast.makeText(
                                        context,
                                        if (isMy) "ဒေတာ အားလုံးကို အပြီးတိုင် ဖျက်ပြီးပါပြီ!" else "All data wiped successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    confirmActionType = null
                                    onDismiss()
                                }
                            }
                            "RELOAD_WORKERS" -> {
                                viewModel.reloadDefaultMyanmarWorkforce {
                                    Toast.makeText(
                                        context,
                                        if (isMy) "အခြေခံ လုပ်သားစာရင်း ပြန်လည်ထည့်သွင်းပြီးပါပြီ!" else "Standard workforce loaded!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    confirmActionType = null
                                    onDismiss()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (action == "WIPE_ALL" || action == "ATTENDANCE_ONLY") AdvanceRed else ConstructionGold
                    ),
                    modifier = Modifier.testTag("confirm_clear_sample_action_btn")
                ) {
                    Text(
                        text = if (isMy) "အတည်ပြု ဖျက်မည်" else "Confirm",
                        color = if (action == "WIPE_ALL" || action == "ATTENDANCE_ONLY") Color.White else Slate900,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmActionType = null }) {
                    Text(if (isMy) "မလုပ်တော့ပါ" else "Cancel")
                }
            }
        )
    }
}
