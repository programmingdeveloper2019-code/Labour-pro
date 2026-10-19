package com.example.util

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.data.model.AttendanceRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Bulletproof persistent store for Daily Attendance Records.
 * Ensures daily attendance entered by the user is NEVER lost on restarts or app updates.
 * Uses multiple independent storage layers:
 * 1. Dedicated internal JSON file in context.filesDir (atomic write)
 * 2. SharedPreferences backup with synchronous commit
 * 3. External app storage mirror (if available)
 */
object PermanentAttendanceStore {

    private const val TAG = "PermanentAttendanceStore"
    private const val PREFS_NAME = "permanent_attendance_vault_prefs"
    private const val KEY_ATTENDANCE_JSON = "key_permanent_attendance_json"
    private const val FILE_NAME = "permanent_attendance_registry.json"

    fun saveAttendance(context: Context, records: List<AttendanceRecord>, explicitWipe: Boolean = false) {
        if (records.isEmpty()) {
            if (explicitWipe) {
                clearAttendance(context)
            }
            return
        }
        try {
            val jsonArray = JSONArray()
            for (rec in records) {
                val obj = JSONObject().apply {
                    put("id", rec.id)
                    put("date", rec.date)
                    put("siteCode", rec.siteCode)
                    put("siteName", rec.siteName)
                    put("workerId", rec.workerId)
                    put("workerName", rec.workerName)
                    put("workerType", rec.workerType)
                    put("morningAttendance", rec.morningAttendance)
                    put("eveningAttendance", rec.eveningAttendance)
                    put("dailyRate", rec.dailyRate)
                    put("otHours", rec.otHours)
                    put("otRate", rec.otRate)
                    put("advanceToday", rec.advanceToday)
                    put("paidToday", rec.paidToday)
                    put("remark", rec.remark)
                    put("createdAt", rec.createdAt)
                }
                jsonArray.put(obj)
            }
            val jsonString = jsonArray.toString(2)

            // Layer 1: Internal storage private file (atomic write)
            val file = File(context.filesDir, FILE_NAME)
            val tempFile = File(context.filesDir, "$FILE_NAME.tmp")
            FileOutputStream(tempFile).use { out ->
                out.write(jsonString.toByteArray(Charsets.UTF_8))
                out.flush()
            }
            if (tempFile.exists()) {
                if (file.exists()) file.delete()
                tempFile.renameTo(file)
            }

            // Layer 2: SharedPreferences (synchronous write)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_ATTENDANCE_JSON, jsonString)
                .putLong("last_save_time", System.currentTimeMillis())
                .commit()

            // Layer 3: External app storage mirror
            try {
                val extDir = context.getExternalFilesDir("vault")
                if (extDir != null) {
                    if (!extDir.exists()) extDir.mkdirs()
                    val extFile = File(extDir, "permanent_attendance_vault.json")
                    FileOutputStream(extFile).use { out ->
                        out.write(jsonString.toByteArray(Charsets.UTF_8))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "External mirror save notice: ${e.message}")
            }

            // Layer 4: Public Documents directory (survives app re-install)
            try {
                val publicDocsDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "LabourMaster"
                )
                if (!publicDocsDir.exists()) publicDocsDir.mkdirs()
                val publicVault = File(publicDocsDir, "permanent_attendance_vault.json")
                FileOutputStream(publicVault).use { out ->
                    out.write(jsonString.toByteArray(Charsets.UTF_8))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Public docs mirror notice: ${e.message}")
            }

            Log.d(TAG, "Successfully permanently saved ${records.size} attendance records.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save attendance permanently", e)
        }
    }

    fun clearAttendance(context: Context) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) file.delete()
            val tempFile = File(context.filesDir, "$FILE_NAME.tmp")
            if (tempFile.exists()) tempFile.delete()

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_ATTENDANCE_JSON).remove("last_save_time").commit()

            val extDir = context.getExternalFilesDir("vault")
            if (extDir != null) {
                val extFile = File(extDir, "permanent_attendance_vault.json")
                if (extFile.exists()) extFile.delete()
            }

            try {
                val publicDocsFile = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "LabourMaster/permanent_attendance_vault.json"
                )
                if (publicDocsFile.exists()) publicDocsFile.delete()
            } catch (e: Exception) {
                Log.w(TAG, "Public docs clear notice: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear attendance store", e)
        }
    }

    fun loadAttendance(context: Context): List<AttendanceRecord>? {
        // Try Layer 1: Internal storage file
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists() && file.length() > 0) {
                val content = file.readText(Charsets.UTF_8)
                val list = parseAttendanceJson(content)
                if (list.isNotEmpty()) {
                    Log.d(TAG, "Loaded ${list.size} attendance records from internal file.")
                    return list
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading internal file: ${e.message}")
        }

        // Try Layer 2: SharedPreferences
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_ATTENDANCE_JSON, null)
            if (!json.isNullOrBlank()) {
                val list = parseAttendanceJson(json)
                if (list.isNotEmpty()) {
                    Log.d(TAG, "Loaded ${list.size} attendance records from SharedPreferences.")
                    return list
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading prefs: ${e.message}")
        }

        // Try Layer 3: External app storage
        try {
            val extDir = context.getExternalFilesDir("vault")
            val extFile = if (extDir != null) File(extDir, "permanent_attendance_vault.json") else null
            if (extFile != null && extFile.exists() && extFile.length() > 0) {
                val list = parseAttendanceJson(extFile.readText(Charsets.UTF_8))
                if (list.isNotEmpty()) {
                    Log.d(TAG, "Loaded ${list.size} attendance records from external storage.")
                    return list
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading external file: ${e.message}")
        }

        // Try Layer 4: Public Documents directory
        try {
            val publicVault = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "LabourMaster/permanent_attendance_vault.json"
            )
            if (publicVault.exists() && publicVault.length() > 0) {
                val list = parseAttendanceJson(publicVault.readText(Charsets.UTF_8))
                if (list.isNotEmpty()) {
                    Log.d(TAG, "Loaded ${list.size} attendance records from Public Documents.")
                    return list
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading public documents vault: ${e.message}")
        }

        return null
    }

    private fun parseAttendanceJson(jsonString: String): List<AttendanceRecord> {
        val result = mutableListOf<AttendanceRecord>()
        val array = JSONArray(jsonString)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
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
        return result
    }
}
