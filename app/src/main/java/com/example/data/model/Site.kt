package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class Site(
    @PrimaryKey
    val siteCode: String, // e.g. "S-01"
    val siteName: String, // e.g. "Metro Tower Phase 2"
    val location: String = "",
    val supervisor: String = "",
    val status: String = "Ongoing" // "Ongoing", "Completed", "On Hold"
)
