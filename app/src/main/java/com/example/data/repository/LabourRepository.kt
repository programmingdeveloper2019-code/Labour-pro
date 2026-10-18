package com.example.data.repository

import com.example.data.local.AttendanceDao
import com.example.data.local.SampleDataProvider
import com.example.data.local.SiteDao
import com.example.data.local.WorkerDao
import com.example.data.model.AttendanceRecord
import com.example.data.model.Site
import com.example.data.model.Worker
import kotlinx.coroutines.flow.Flow

class LabourRepository(
    private val workerDao: WorkerDao,
    private val siteDao: SiteDao,
    private val attendanceDao: AttendanceDao
) {
    val allWorkers: Flow<List<Worker>> = workerDao.getAllWorkers()
    val activeWorkers: Flow<List<Worker>> = workerDao.getActiveWorkers()

    val allSites: Flow<List<Site>> = siteDao.getAllSites()
    val activeSites: Flow<List<Site>> = siteDao.getActiveSites()

    val allAttendance: Flow<List<AttendanceRecord>> = attendanceDao.getAllAttendance()

    fun getAttendanceForDate(date: String): Flow<List<AttendanceRecord>> =
        attendanceDao.getAttendanceForDate(date)

    fun getMarkedWorkerIdsForDate(date: String): Flow<List<String>> =
        attendanceDao.getMarkedWorkerIdsForDate(date)

    suspend fun isWorkerMarkedForDate(date: String, workerId: String): Boolean =
        attendanceDao.isWorkerMarkedForDate(date, workerId) > 0

    suspend fun getRecordForDateAndWorker(date: String, workerId: String): AttendanceRecord? =
        attendanceDao.getRecordForDateAndWorker(date, workerId)

    suspend fun insertWorker(worker: Worker) = workerDao.insertWorker(worker)
    suspend fun insertWorkers(workers: List<Worker>) = workerDao.insertWorkers(workers)
    suspend fun updateWorker(worker: Worker) = workerDao.updateWorker(worker)
    suspend fun deleteWorker(worker: Worker) = workerDao.deleteWorker(worker)
    suspend fun clearAllWorkers() = workerDao.clearAllWorkers()
    suspend fun deleteWorkersByIds(ids: List<String>) = workerDao.deleteWorkersByIds(ids)
    suspend fun getAllWorkersList(): List<Worker> = workerDao.getAllWorkersList()

    suspend fun insertSite(site: Site) = siteDao.insertSite(site)

    suspend fun updateSite(site: Site) {
        siteDao.updateSite(site)
        attendanceDao.updateSiteNameInAttendance(site.siteCode, site.siteName)
    }

    suspend fun deleteSite(site: Site) {
        siteDao.deleteSite(site)
    }

    suspend fun deleteSiteWithRecords(site: Site, deleteAttendance: Boolean) {
        if (deleteAttendance) {
            attendanceDao.deleteAttendanceBySite(site.siteCode)
        }
        siteDao.deleteSite(site)
    }

    suspend fun getAttendanceCountForSite(siteCode: String): Int =
        attendanceDao.getAttendanceCountForSite(siteCode)

    suspend fun insertAttendance(record: AttendanceRecord): Long =
        attendanceDao.insertAttendance(record)

    suspend fun insertAttendanceList(records: List<AttendanceRecord>) =
        attendanceDao.insertAttendanceList(records)

    suspend fun updateAttendance(record: AttendanceRecord) =
        attendanceDao.updateAttendance(record)

    suspend fun deleteAttendance(record: AttendanceRecord) =
        attendanceDao.deleteAttendance(record)

    suspend fun deleteAttendanceById(id: Long) =
        attendanceDao.deleteById(id)

    // Clear and Reset Actions
    suspend fun clearAllAttendance() =
        attendanceDao.clearAllAttendance()

    suspend fun clearAllData() {
        attendanceDao.clearAllAttendance()
        workerDao.clearAllWorkers()
        siteDao.clearAllSites()
    }

    suspend fun resetWithSampleData() {
        attendanceDao.clearAllAttendance()
        workerDao.clearAllWorkers()
        siteDao.clearAllSites()
        workerDao.insertWorkers(SampleDataProvider.getInitialWorkers())
        siteDao.insertSites(SampleDataProvider.getInitialSites())
        attendanceDao.insertAttendanceList(SampleDataProvider.getInitialAttendance())
    }

    // Backup & Restore
    suspend fun getBackupData(exportedDate: String): com.example.data.model.LabourBackupData {
        val sites = siteDao.getAllSitesList()
        val workers = workerDao.getAllWorkersList()
        val attendance = attendanceDao.getAllAttendanceList()
        return com.example.data.model.LabourBackupData(
            exportedDate = exportedDate,
            sites = sites,
            workers = workers,
            attendance = attendance
        )
    }

    suspend fun restoreData(backup: com.example.data.model.LabourBackupData, replaceExisting: Boolean) {
        if (replaceExisting) {
            attendanceDao.clearAllAttendance()
            workerDao.clearAllWorkers()
            siteDao.clearAllSites()
        }
        if (backup.sites.isNotEmpty()) {
            siteDao.insertSites(backup.sites)
        }
        if (backup.workers.isNotEmpty()) {
            workerDao.insertWorkers(backup.workers)
        }
        if (backup.attendance.isNotEmpty()) {
            attendanceDao.insertAttendanceList(backup.attendance)
        }
    }
}
