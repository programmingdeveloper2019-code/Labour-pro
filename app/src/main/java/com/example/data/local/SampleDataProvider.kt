package com.example.data.local

import com.example.data.model.AttendanceRecord
import com.example.data.model.Site
import com.example.data.model.Worker

object SampleDataProvider {

    // No sample workers - user's workers must NEVER be deleted or purged
    val SAMPLE_WORKER_NAMES = emptySet<String>()

    fun isSampleWorker(worker: Worker): Boolean {
        return false
    }

    /**
     * Do NOT add any sample workers.
     * Starts with a 100% clean slate so the user can enter their own real workers.
     */
    fun getInitialWorkers(): List<Worker> {
        return emptyList()
    }

    fun getInitialSites(): List<Site> {
        return emptyList()
    }

    /**
     * Do NOT add any sample attendance records.
     */
    fun getInitialAttendance(): List<AttendanceRecord> {
        return emptyList()
    }
}
