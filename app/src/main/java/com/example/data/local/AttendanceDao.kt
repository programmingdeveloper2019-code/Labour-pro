package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AttendanceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY date DESC, workerName ASC")
    fun getAllAttendance(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE date = :date ORDER BY workerName ASC")
    fun getAttendanceForDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE siteCode = :siteCode ORDER BY date DESC, workerName ASC")
    fun getAttendanceForSite(siteCode: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE workerId = :workerId ORDER BY date DESC")
    fun getAttendanceForWorker(workerId: String): Flow<List<AttendanceRecord>>

    @Query("SELECT workerId FROM attendance_records WHERE date = :date")
    fun getMarkedWorkerIdsForDate(date: String): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE date = :date AND workerId = :workerId")
    suspend fun isWorkerMarkedForDate(date: String, workerId: String): Int

    @Query("SELECT * FROM attendance_records WHERE date = :date AND workerId = :workerId LIMIT 1")
    suspend fun getRecordForDateAndWorker(date: String, workerId: String): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Long): AttendanceRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceList(records: List<AttendanceRecord>)

    @Update
    suspend fun updateAttendance(record: AttendanceRecord)

    @Delete
    suspend fun deleteAttendance(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM attendance_records")
    suspend fun getTotalRecordCount(): Int

    @Query("SELECT * FROM attendance_records WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getAttendanceBetweenDates(startDate: String, endDate: String): Flow<List<AttendanceRecord>>

    @Query("DELETE FROM attendance_records")
    suspend fun clearAllAttendance()

    @Query("DELETE FROM attendance_records WHERE siteCode = :siteCode")
    suspend fun deleteAttendanceBySite(siteCode: String)

    @Query("SELECT COUNT(*) FROM attendance_records WHERE siteCode = :siteCode")
    suspend fun getAttendanceCountForSite(siteCode: String): Int

    @Query("UPDATE attendance_records SET siteName = :newSiteName WHERE siteCode = :siteCode")
    suspend fun updateSiteNameInAttendance(siteCode: String, newSiteName: String)

    @Query("SELECT * FROM attendance_records ORDER BY date DESC, id ASC")
    suspend fun getAllAttendanceList(): List<AttendanceRecord>
}
