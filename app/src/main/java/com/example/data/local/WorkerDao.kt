package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Worker
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers ORDER BY LENGTH(workerId) ASC, workerId ASC")
    fun getAllWorkers(): Flow<List<Worker>>

    @Query("SELECT * FROM workers WHERE status = 'Active' ORDER BY LENGTH(workerId) ASC, workerId ASC")
    fun getActiveWorkers(): Flow<List<Worker>>

    @Query("SELECT * FROM workers WHERE workerId = :workerId LIMIT 1")
    suspend fun getWorkerById(workerId: String): Worker?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: Worker)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkers(workers: List<Worker>)

    @Update
    suspend fun updateWorker(worker: Worker)

    @Delete
    suspend fun deleteWorker(worker: Worker)

    @Query("SELECT COUNT(*) FROM workers")
    suspend fun getWorkerCount(): Int

    @Query("DELETE FROM workers")
    suspend fun clearAllWorkers()

    @Query("DELETE FROM workers WHERE workerId IN (:ids)")
    suspend fun deleteWorkersByIds(ids: List<String>)

    @Query("SELECT * FROM workers ORDER BY LENGTH(workerId) ASC, workerId ASC")
    suspend fun getAllWorkersList(): List<Worker>
}
