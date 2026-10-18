package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.AttendanceRecord
import com.example.data.model.LabourBackupData
import com.example.data.model.Site
import com.example.data.model.Worker
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    private const val TAG = "BackupManager"
    private const val PREFS_NAME = "labour_vault_prefs"
    private const val KEY_VAULT_BACKUP_JSON = "key_vault_backup_json"
    private const val KEY_LAST_BACKUP_TIME = "key_last_backup_time"

    fun serializeToJson(backup: LabourBackupData): String {
        val root = JSONObject()
        root.put("version", backup.version)
        root.put("appName", backup.appName)
        root.put("timestamp", backup.timestamp)
        root.put("exportedDate", backup.exportedDate)

        // Sites
        val sitesArray = JSONArray()
        for (site in backup.sites) {
            val obj = JSONObject()
            obj.put("siteCode", site.siteCode)
            obj.put("siteName", site.siteName)
            obj.put("location", site.location)
            obj.put("supervisor", site.supervisor)
            obj.put("status", site.status)
            sitesArray.put(obj)
        }
        root.put("sites", sitesArray)

        // Workers
        val workersArray = JSONArray()
        for (worker in backup.workers) {
            val obj = JSONObject()
            obj.put("workerId", worker.workerId)
            obj.put("name", worker.name)
            obj.put("gender", worker.gender)
            obj.put("workerType", worker.workerType)
            obj.put("dailyRate", worker.dailyRate)
            obj.put("otRate", worker.otRate)
            obj.put("status", worker.status)
            obj.put("phone", worker.phone)
            obj.put("address", worker.address)
            obj.put("nrcNo", worker.nrcNo)
            obj.put("emergencyContactName", worker.emergencyContactName)
            obj.put("emergencyPhone", worker.emergencyPhone)
            obj.put("bloodType", worker.bloodType)
            obj.put("skillLevel", worker.skillLevel)
            obj.put("paymentMethod", worker.paymentMethod)
            obj.put("paymentAccount", worker.paymentAccount)
            obj.put("joinedDate", worker.joinedDate)
            obj.put("notes", worker.notes)
            workersArray.put(obj)
        }
        root.put("workers", workersArray)

        // Attendance
        val attendanceArray = JSONArray()
        for (rec in backup.attendance) {
            val obj = JSONObject()
            obj.put("id", rec.id)
            obj.put("date", rec.date)
            obj.put("siteCode", rec.siteCode)
            obj.put("siteName", rec.siteName)
            obj.put("workerId", rec.workerId)
            obj.put("workerName", rec.workerName)
            obj.put("workerType", rec.workerType)
            obj.put("morningAttendance", rec.morningAttendance)
            obj.put("eveningAttendance", rec.eveningAttendance)
            obj.put("dailyRate", rec.dailyRate)
            obj.put("otHours", rec.otHours)
            obj.put("otRate", rec.otRate)
            obj.put("advanceToday", rec.advanceToday)
            obj.put("paidToday", rec.paidToday)
            obj.put("remark", rec.remark)
            obj.put("createdAt", rec.createdAt)
            attendanceArray.put(obj)
        }
        root.put("attendance", attendanceArray)

        return root.toString(2)
    }

    fun parseFromJson(jsonString: String): LabourBackupData {
        val root = JSONObject(jsonString)
        val version = root.optInt("version", 2)
        val appName = root.optString("appName", "LabourMaster")
        val timestamp = root.optLong("timestamp", System.currentTimeMillis())
        val exportedDate = root.optString("exportedDate", "")

        val sites = mutableListOf<Site>()
        val sitesArray = root.optJSONArray("sites")
        if (sitesArray != null) {
            for (i in 0 until sitesArray.length()) {
                val obj = sitesArray.getJSONObject(i)
                sites.add(
                    Site(
                        siteCode = obj.optString("siteCode", "S-01"),
                        siteName = obj.optString("siteName", "Site"),
                        location = obj.optString("location", ""),
                        supervisor = obj.optString("supervisor", ""),
                        status = obj.optString("status", "Ongoing")
                    )
                )
            }
        }

        val workers = mutableListOf<Worker>()
        val workersArray = root.optJSONArray("workers")
        if (workersArray != null) {
            for (i in 0 until workersArray.length()) {
                val obj = workersArray.getJSONObject(i)
                workers.add(
                    Worker(
                        workerId = obj.optString("workerId", "W-100"),
                        name = obj.optString("name", "Worker"),
                        gender = obj.optString("gender", "Male"),
                        workerType = obj.optString("workerType", "Helper"),
                        dailyRate = obj.optDouble("dailyRate", 20000.0),
                        otRate = obj.optDouble("otRate", 3000.0),
                        status = obj.optString("status", "Active"),
                        phone = obj.optString("phone", ""),
                        address = obj.optString("address", ""),
                        nrcNo = obj.optString("nrcNo", ""),
                        emergencyContactName = obj.optString("emergencyContactName", ""),
                        emergencyPhone = obj.optString("emergencyPhone", ""),
                        bloodType = obj.optString("bloodType", ""),
                        skillLevel = obj.optString("skillLevel", "Skilled"),
                        paymentMethod = obj.optString("paymentMethod", "Cash"),
                        paymentAccount = obj.optString("paymentAccount", ""),
                        joinedDate = obj.optString("joinedDate", ""),
                        notes = obj.optString("notes", "")
                    )
                )
            }
        }

        val attendance = mutableListOf<AttendanceRecord>()
        val attendanceArray = root.optJSONArray("attendance")
        if (attendanceArray != null) {
            for (i in 0 until attendanceArray.length()) {
                val obj = attendanceArray.getJSONObject(i)
                attendance.add(
                    AttendanceRecord(
                        id = 0,
                        date = obj.optString("date", "2026-01-01"),
                        siteCode = obj.optString("siteCode", ""),
                        siteName = obj.optString("siteName", ""),
                        workerId = obj.optString("workerId", ""),
                        workerName = obj.optString("workerName", ""),
                        workerType = obj.optString("workerType", "Helper"),
                        morningAttendance = obj.optString("morningAttendance", "P"),
                        eveningAttendance = obj.optString("eveningAttendance", "P"),
                        dailyRate = obj.optDouble("dailyRate", 20000.0),
                        otHours = obj.optDouble("otHours", 0.0),
                        otRate = obj.optDouble("otRate", 3000.0),
                        advanceToday = obj.optDouble("advanceToday", 0.0),
                        paidToday = obj.optDouble("paidToday", 0.0),
                        remark = obj.optString("remark", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        return LabourBackupData(
            version = version,
            appName = appName,
            timestamp = timestamp,
            exportedDate = exportedDate,
            sites = sites,
            workers = workers,
            attendance = attendance
        )
    }

    /**
     * Persistently auto-saves the entire workforce and records so that even if the user
     * uninstalls and reinstalls the app, the data can be recovered without re-entering workers.
     */
    fun autoSaveVault(context: Context, backup: LabourBackupData) {
        if (backup.workers.isEmpty() && backup.sites.isEmpty()) return
        try {
            val json = serializeToJson(backup)

            // 1. SharedPreferences (backed up by Android OS Cloud Auto-Backup)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_VAULT_BACKUP_JSON, json)
                .putLong(KEY_LAST_BACKUP_TIME, System.currentTimeMillis())
                .apply()

            // 2. Internal app files directory
            val internalDir = File(context.filesDir, "backups")
            if (!internalDir.exists()) internalDir.mkdirs()
            val internalFile = File(internalDir, "labour_master_autobackup.json")
            FileOutputStream(internalFile).use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            }

            // 3. External App directory
            val extDir = context.getExternalFilesDir("backups")
            if (extDir != null && !extDir.exists()) extDir.mkdirs()
            if (extDir != null) {
                val extFile = File(extDir, "LabourMaster_AutoBackup.json")
                FileOutputStream(extFile).use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
            }

            // 4. Public Documents directory (survives app uninstall on user device)
            try {
                val publicDocsDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "LabourMaster"
                )
                if (!publicDocsDir.exists()) publicDocsDir.mkdirs()
                val publicVault = File(publicDocsDir, "labour_master_vault.json")
                FileOutputStream(publicVault).use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Public docs write notice: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during autoSaveVault", e)
        }
    }

    /**
     * Proactively searches persistent locations on device to restore previously saved workers
     * when the app is freshly reinstalled or when database has no workers.
     */
    fun findExistingPersistentVault(context: Context): LabourBackupData? {
        try {
            // Check 1: Public Documents directory (survives uninstall)
            try {
                val publicVault = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "LabourMaster/labour_master_vault.json"
                )
                if (publicVault.exists() && publicVault.length() > 0) {
                    val content = publicVault.readText(Charsets.UTF_8)
                    val data = parseFromJson(content)
                    if (data.workers.isNotEmpty()) {
                        Log.i(TAG, "Restored vault from Public Documents (${data.workers.size} workers)")
                        return data
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Public vault check error: ${e.message}")
            }

            // Check 2: SharedPreferences (restored by Android OS Google Auto Backup)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val prefsJson = prefs.getString(KEY_VAULT_BACKUP_JSON, null)
            if (!prefsJson.isNullOrBlank()) {
                val data = parseFromJson(prefsJson)
                if (data.workers.isNotEmpty()) {
                    Log.i(TAG, "Restored vault from SharedPreferences (${data.workers.size} workers)")
                    return data
                }
            }

            // Check 3: External app backups dir
            val extDir = context.getExternalFilesDir("backups")
            if (extDir != null) {
                val extFile = File(extDir, "LabourMaster_AutoBackup.json")
                if (extFile.exists() && extFile.length() > 0) {
                    val content = extFile.readText(Charsets.UTF_8)
                    val data = parseFromJson(content)
                    if (data.workers.isNotEmpty()) {
                        return data
                    }
                }
            }

            // Check 4: Internal backups dir
            val internalFile = File(context.filesDir, "backups/labour_master_autobackup.json")
            if (internalFile.exists() && internalFile.length() > 0) {
                val content = internalFile.readText(Charsets.UTF_8)
                val data = parseFromJson(content)
                if (data.workers.isNotEmpty()) {
                    return data
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding existing persistent vault", e)
        }
        return null
    }

    fun getLastBackupTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
    }

    fun createBackupFile(context: Context, backup: LabourBackupData): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "LabourMaster_Backup_$timeStamp.json"
        val file = File(exportDir, fileName)

        val json = serializeToJson(backup)
        FileOutputStream(file).use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
        }

        // Also save to persistent locations
        autoSaveVault(context, backup)

        return file
    }

    fun shareBackupToDriveOrApps(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "LabourMaster Backup - ${file.name}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "LabourMaster Construction Database Backup File. Save this to Google Drive or your secure storage for easy restore."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Backup to Google Drive / Share File")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing backup: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun writeBackupToUri(context: Context, uri: Uri, jsonString: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(jsonString.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun readBackupFromUri(context: Context, uri: Uri): LabourBackupData? {
        return try {
            val sb = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        sb.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            }
            parseFromJson(sb.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
