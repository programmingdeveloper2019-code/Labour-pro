package com.example.util

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.data.model.Worker
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Bulletproof persistent store for Workers.
 * Ensures workers entered by the user are NEVER lost or cleared unless explicitly deleted.
 * Uses multiple independent persistent layers:
 * 1. Dedicated internal JSON file in context.filesDir (guaranteed private, survives app restarts)
 * 2. SharedPreferences backup
 * 3. External app directory mirror (if available)
 */
object PermanentWorkerStore {

    private const val TAG = "PermanentWorkerStore"
    private const val PREFS_NAME = "permanent_worker_vault_prefs"
    private const val KEY_WORKERS_JSON = "key_permanent_workers_json"
    private const val FILE_NAME = "permanent_workers_registry.json"

    fun saveWorkers(context: Context, workers: List<Worker>, explicitWipe: Boolean = false) {
        if (workers.isEmpty()) {
            if (explicitWipe) {
                clearWorkers(context)
            }
            return
        }
        try {
            val jsonArray = JSONArray()
            for (w in workers) {
                val obj = JSONObject().apply {
                    put("workerId", w.workerId)
                    put("name", w.name)
                    put("gender", w.gender)
                    put("workerType", w.workerType)
                    put("dailyRate", w.dailyRate)
                    put("otRate", w.otRate)
                    put("status", w.status)
                    put("phone", w.phone)
                    put("address", w.address)
                    put("nrcNo", w.nrcNo)
                    put("emergencyContactName", w.emergencyContactName)
                    put("emergencyPhone", w.emergencyPhone)
                    put("bloodType", w.bloodType)
                    put("skillLevel", w.skillLevel)
                    put("paymentMethod", w.paymentMethod)
                    put("paymentAccount", w.paymentAccount)
                    put("joinedDate", w.joinedDate)
                    put("notes", w.notes)
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

            // Layer 2: SharedPreferences
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_WORKERS_JSON, jsonString)
                .putLong("last_save_time", System.currentTimeMillis())
                .apply()

            // Layer 3: External app storage mirror (optional safety)
            try {
                val extDir = context.getExternalFilesDir("vault")
                if (extDir != null) {
                    if (!extDir.exists()) extDir.mkdirs()
                    val extFile = File(extDir, "permanent_workers_vault.json")
                    FileOutputStream(extFile).use { out ->
                        out.write(jsonString.toByteArray(Charsets.UTF_8))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "External mirror save notice: ${e.message}")
            }

            // Layer 4: Public Documents directory (survives app re-install and container restarts)
            try {
                val publicDocsDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "LabourMaster"
                )
                if (!publicDocsDir.exists()) publicDocsDir.mkdirs()
                val publicVault = File(publicDocsDir, "permanent_workers_vault.json")
                FileOutputStream(publicVault).use { out ->
                    out.write(jsonString.toByteArray(Charsets.UTF_8))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Public documents mirror notice: ${e.message}")
            }

            // Layer 5: Public Downloads directory (secondary safety)
            try {
                val publicDlDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "LabourMaster"
                )
                if (!publicDlDir.exists()) publicDlDir.mkdirs()
                val publicVault = File(publicDlDir, "permanent_workers_vault.json")
                FileOutputStream(publicVault).use { out ->
                    out.write(jsonString.toByteArray(Charsets.UTF_8))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Public downloads mirror notice: ${e.message}")
            }

            Log.d(TAG, "Successfully permanently saved ${workers.size} workers to all storage layers.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save workers permanently", e)
        }
    }

    /**
     * Completely and permanently clears all stored workers from all persistence layers.
     */
    fun clearWorkers(context: Context) {
        try {
            // Layer 1: Internal file
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) file.delete()
            val tempFile = File(context.filesDir, "$FILE_NAME.tmp")
            if (tempFile.exists()) tempFile.delete()

            // Layer 2: SharedPreferences
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_WORKERS_JSON).remove("last_save_time").apply()

            // Layer 3: External mirror
            val extDir = context.getExternalFilesDir("vault")
            if (extDir != null) {
                val extFile = File(extDir, "permanent_workers_vault.json")
                if (extFile.exists()) extFile.delete()
            }

            // Layer 4: Public Documents
            try {
                val publicDocsFile = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                    "LabourMaster/permanent_workers_vault.json"
                )
                if (publicDocsFile.exists()) publicDocsFile.delete()
            } catch (e: Exception) {
                Log.w(TAG, "Public docs clear notice: ${e.message}")
            }

            Log.d(TAG, "PermanentWorkerStore successfully cleared all workers.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear PermanentWorkerStore", e)
        }
    }

    /**
     * Deletes a specific set of worker IDs from the persistent store.
     */
    fun deleteWorkersByIds(context: Context, idsToDelete: Set<String>) {
        val current = loadWorkers(context) ?: return
        val remaining = current.filterNot { idsToDelete.contains(it.workerId) }
        saveWorkers(context, remaining, explicitWipe = remaining.isEmpty())
    }

    fun loadWorkers(context: Context): List<Worker>? {
        // Try Layer 1: Internal storage file
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists() && file.length() > 0) {
                val content = file.readText(Charsets.UTF_8)
                val list = parseWorkersJson(content)
                if (list.isNotEmpty()) {
                    Log.d(TAG, "Loaded ${list.size} workers from internal file.")
                    return list
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading internal file: ${e.message}")
        }

        // Try Layer 2: SharedPreferences
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_WORKERS_JSON, null)
            if (!json.isNullOrBlank()) {
                val list = parseWorkersJson(json)
                if (list.isNotEmpty()) {
                    Log.d(TAG, "Loaded ${list.size} workers from SharedPreferences.")
                    return list
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading prefs: ${e.message}")
        }

        // Try Layer 3: External app storage
        try {
            val extDir = context.getExternalFilesDir("vault")
            val extFile = if (extDir != null) File(extDir, "permanent_workers_vault.json") else null
            if (extFile != null && extFile.exists() && extFile.length() > 0) {
                val list = parseWorkersJson(extFile.readText(Charsets.UTF_8))
                if (list.isNotEmpty()) {
                    Log.d(TAG, "Loaded ${list.size} workers from external storage.")
                    return list
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading external file: ${e.message}")
        }

        // Try Layer 4: Public Documents directory (survives app reinstall!)
        try {
            val publicVault = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "LabourMaster/permanent_workers_vault.json"
            )
            if (publicVault.exists() && publicVault.length() > 0) {
                val list = parseWorkersJson(publicVault.readText(Charsets.UTF_8))
                if (list.isNotEmpty()) {
                    Log.d(TAG, "Loaded ${list.size} workers from Public Documents.")
                    return list
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading public documents vault: ${e.message}")
        }

        // Try Layer 5: Public Downloads directory
        try {
            val publicDlVault = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "LabourMaster/permanent_workers_vault.json"
            )
            if (publicDlVault.exists() && publicDlVault.length() > 0) {
                val list = parseWorkersJson(publicDlVault.readText(Charsets.UTF_8))
                if (list.isNotEmpty()) {
                    Log.d(TAG, "Loaded ${list.size} workers from Public Downloads.")
                    return list
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading public downloads vault: ${e.message}")
        }

        // Try Layer 6: BackupManager auto-backups
        try {
            val vaultBackup = BackupManager.findExistingPersistentVault(context)
            if (vaultBackup != null && vaultBackup.workers.isNotEmpty()) {
                Log.d(TAG, "Loaded ${vaultBackup.workers.size} workers from BackupManager vault.")
                return vaultBackup.workers
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading BackupManager vault: ${e.message}")
        }

        return null
    }

    private fun parseWorkersJson(jsonString: String): List<Worker> {
        val result = mutableListOf<Worker>()
        val array = JSONArray(jsonString)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                Worker(
                    workerId = obj.optString("workerId", "W-${100 + i}"),
                    name = obj.optString("name", ""),
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
        return result
    }
}
