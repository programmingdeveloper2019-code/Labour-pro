package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import com.example.data.model.LabourBackupData
import com.example.ui.theme.*
import com.example.ui.viewmodel.LabourViewModel
import com.example.util.AppLanguage
import com.example.util.BackupManager
import com.example.util.LanguageManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreDialog(
    viewModel: LabourViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    val isMy = currentLanguage == AppLanguage.MYANMAR

    val sites by viewModel.allSites.collectAsState()
    val workers by viewModel.allWorkers.collectAsState()
    val attendance by viewModel.allAttendance.collectAsState()

    var pendingRestoreData by remember { mutableStateOf<LabourBackupData?>(null) }
    var isBackingUp by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // SAF Create Document for Local Backup
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.createBackupData { backup ->
                val json = BackupManager.serializeToJson(backup)
                val success = BackupManager.writeBackupToUri(context, uri, json)
                if (success) {
                    Toast.makeText(
                        context,
                        if (isMy) "ဆိုဒ် (${backup.sites.size}) ခု၊ လုပ်သား (${backup.workers.size}) ဦး နှင့် နေ့စဉ်တက်ရောက်မှု (${backup.attendance.size}) ခု အားလုံးကို အောင်မြင်စွာ အရန်သိမ်းဆည်းပြီးပါပြီ!" 
                            else "Backup saved successfully (${backup.sites.size} sites, ${backup.workers.size} workers, ${backup.attendance.size} attendance records)!",
                        Toast.LENGTH_LONG
                    ).show()
                    statusMessage = if (isMy) "နေ့စဉ်တက်ရောက်မှုမှတ်တမ်း (${backup.attendance.size}) ခု ပါဝင်သည့် အရန်ဖိုင် သိမ်းဆည်းပြီးပါပြီ" 
                        else "Local backup created with ${backup.attendance.size} daily attendance records."
                } else {
                    Toast.makeText(context, "Failed to write backup file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // SAF Open Document for Restore
    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val backup = BackupManager.readBackupFromUri(context, uri)
            if (backup != null) {
                pendingRestoreData = backup
            } else {
                Toast.makeText(
                    context,
                    if (isMy) "အရန်ဖိုင် ဖတ်ရှု၍ မရပါ (Invalid JSON backup)" else "Could not read backup file (Invalid JSON)",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ConstructionGold
                ) {
                    Icon(
                        Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = Slate900,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = if (isMy) "ဒေတာ အရန်သိမ်းခြင်းနှင့် ပြန်လည်ရယူခြင်း" else "Data Backup & Restore",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = if (isMy) "နေ့စဉ်တက်ရောက်မှု၊ လုပ်သား၊ ဆိုဒ်များအားလုံး သိမ်းဆည်းနိုင်ပါသည်" else "Includes all daily attendance, workers & sites",
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
                // Live summary card showing exact records that will be included in the backup
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, PrimaryContainerLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isMy) "📦 အရန်သိမ်းမည့် ဒေတာစာရင်း (Data in Backup):" else "📦 Included in Full Backup:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🏢 ${if (isMy) "ဆိုဒ်များ" else "Sites"}: ${sites.size}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("👷 ${if (isMy) "လုပ်သား" else "Workers"}: ${workers.size}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "📝 ${if (isMy) "တက်ရောက်မှု" else "Attendance"}: ${attendance.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = OtBlue
                            )
                        }
                    }
                }

                if (statusMessage != null) {
                    Surface(
                        color = WagesGreenLight,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = statusMessage!!,
                            color = WagesGreenDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Option 1: Backup & Send to Google Drive / Share
                Card(
                    onClick = {
                        isBackingUp = true
                        viewModel.createBackupData { backup ->
                            val file: File = BackupManager.createBackupFile(context, backup)
                            BackupManager.shareBackupToDriveOrApps(context, file)
                            isBackingUp = false
                            statusMessage = if (isMy) {
                                "နေ့စဉ်တက်ရောက်မှုမှတ်တမ်း (${backup.attendance.size}) ခု ပါဝင်သည့် အရန်ဖိုင် Drive သို့ မျှဝေရန် အသင့်ဖြစ်ပါပြီ"
                            } else {
                                "Backup file ready with ${backup.attendance.size} daily attendance records."
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("backup_to_drive_btn")
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
                            color = OtBlueLight
                        ) {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = OtBlue,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isMy) "Google Drive သို့ သိမ်းမည် / မျှဝေမည်" else "Backup & Save to Google Drive",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isMy) "Google Drive, Gmail, WhatsApp သို့ အရန်ဖိုင် (.json) တိုက်ရိုက်ပို့ပါ" else "Uploads full JSON backup to Drive, Gmail, WhatsApp",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate400)
                    }
                }

                // Option 2: Save Backup Locally
                Card(
                    onClick = {
                        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                        createDocLauncher.launch("LabourMaster_Backup_$timeStamp.json")
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("backup_local_btn")
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
                                Icons.Default.SaveAlt,
                                contentDescription = null,
                                tint = ConstructionGoldDark,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isMy) "စက်တွင်း ဖိုင်သိမ်းဆည်းမည် (Local Backup)" else "Save Backup to Local Storage",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isMy) "ဖုန်း၏ Downloads သို့မဟုတ် ဖိုဒါထဲသို့ သိမ်းပါ" else "Save JSON backup to device Downloads/Documents",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate400)
                    }
                }

                // Option 3: Restore Backup
                Card(
                    onClick = {
                        openDocLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("restore_backup_btn")
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
                            color = WagesGreenLight
                        ) {
                            Icon(
                                Icons.Default.SettingsBackupRestore,
                                contentDescription = null,
                                tint = WagesGreenDark,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isMy) "အရန်ဖိုင်မှ ပြန်လည်ရယူမည် (Restore Data)" else "Restore Data from Backup File",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isMy) "Drive သို့မဟုတ် ဖုန်းထဲရှိ Backup .json ကို ပြန်သွင်းပါ" else "Select previous .json backup to restore all records",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate400)
                    }
                }

                // Option 4: Quick Restore from Device Persistent Auto-Vault
                Card(
                    onClick = {
                        val vault = BackupManager.findExistingPersistentVault(context)
                        if (vault != null && (vault.workers.isNotEmpty() || vault.attendance.isNotEmpty() || vault.sites.isNotEmpty())) {
                            pendingRestoreData = vault
                        } else {
                            Toast.makeText(
                                context,
                                if (isMy) "စက်တွင်း Persistent Auto-Vault မတွေ့ရှိပါ" else "No persistent auto-vault found on device",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("restore_from_vault_btn")
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
                                Icons.Default.FolderZip,
                                contentDescription = null,
                                tint = ConstructionGoldDark,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isMy) "စက်တွင်း Auto-Vault မှ ပြန်လည်ရယူမည်" else "Restore from Device Auto-Vault",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isMy) "Uninstall ပြန်လုပ်ထားသော်လည်း ကျန်ရှိနေသော နေ့စဉ်တက်ရောက်မှုနှင့် ဒေတာများကို ရယူပါ" else "Restores auto-backed-up attendance, workers and records",
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
                modifier = Modifier.testTag("close_backup_dialog_btn")
            ) {
                Text(if (isMy) "ပိတ်မည်" else "Close", fontWeight = FontWeight.Bold)
            }
        }
    )

    // Secondary Confirmation Dialog when user picks a file to restore
    if (pendingRestoreData != null) {
        val backup = pendingRestoreData!!
        AlertDialog(
            onDismissRequest = { pendingRestoreData = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = ConstructionGoldDark) },
            title = {
                Text(
                    text = if (isMy) "ဒေတာ ပြန်လည်ထည့်သွင်းရန် အတည်ပြုပါ" else "Confirm Data Restore",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isMy) "အောက်ပါ ဒေတာများကို အရန်ဖိုင်ထဲတွင် တွေ့ရှိရပါသည်-" else "The following records were found in the backup file:"
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🏢 ${if (isMy) "ဆိုဒ်များ" else "Sites"}: ${backup.sites.size}")
                            Text("👷 ${if (isMy) "လုပ်သားများ" else "Workers"}: ${backup.workers.size}")
                            Text("📝 ${if (isMy) "နေ့စဉ်တက်ရောက်မှု မှတ်တမ်းများ (Daily Attendance)" else "Daily Attendance Records"}: ${backup.attendance.size}", fontWeight = FontWeight.Bold, color = OtBlue)
                            if (backup.exportedDate.isNotBlank()) {
                                Text("📅 ${if (isMy) "ထုတ်ယူခဲ့သည့်ရက်" else "Date"}: ${backup.exportedDate}", fontSize = 11.sp, color = Slate500)
                            }
                        }
                    }
                    Text(
                        text = if (isMy) "ဤဖိုင်ဖြင့် ရှိပြီးသားဒေတာများကို အစားထိုး ပြန်လည်ရယူလိုပါသလား?" else "Restore and replace current data with these backup records?",
                        fontSize = 12.sp,
                        color = Slate600
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreBackup(backup, replaceExisting = true) { success ->
                            if (success) {
                                Toast.makeText(
                                    context,
                                    if (isMy) "ဒေတာများ ပြန်လည်ရယူခြင်း အောင်မြင်ပါသည်!" else "Restore completed successfully!",
                                    Toast.LENGTH_LONG
                                ).show()
                                pendingRestoreData = null
                                onDismiss()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ConstructionGold),
                    modifier = Modifier.testTag("confirm_restore_action_btn")
                ) {
                    Text(
                        if (isMy) "အစားထိုး ပြန်လည်ရယူမည်" else "Confirm & Restore",
                        color = Slate900,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreData = null }) {
                    Text(if (isMy) "မလုပ်တော့ပါ" else "Cancel")
                }
            }
        )
    }
}
