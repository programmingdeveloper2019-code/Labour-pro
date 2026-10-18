package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Site
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {
    @Query("SELECT * FROM sites ORDER BY siteName ASC")
    fun getAllSites(): Flow<List<Site>>

    @Query("SELECT * FROM sites WHERE status = 'Ongoing' ORDER BY siteName ASC")
    fun getActiveSites(): Flow<List<Site>>

    @Query("SELECT * FROM sites WHERE siteCode = :siteCode LIMIT 1")
    suspend fun getSiteByCode(siteCode: String): Site?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: Site)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSites(sites: List<Site>)

    @Update
    suspend fun updateSite(site: Site)

    @Delete
    suspend fun deleteSite(site: Site)

    @Query("SELECT COUNT(*) FROM sites")
    suspend fun getSiteCount(): Int

    @Query("DELETE FROM sites")
    suspend fun clearAllSites()

    @Query("DELETE FROM sites WHERE siteCode = :siteCode")
    suspend fun deleteSiteByCode(siteCode: String)

    @Query("SELECT * FROM sites ORDER BY siteCode ASC")
    suspend fun getAllSitesList(): List<Site>
}
