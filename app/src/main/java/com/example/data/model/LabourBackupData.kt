package com.example.data.model

data class LabourBackupData(
    val version: Int = 1,
    val appName: String = "LabourMaster",
    val timestamp: Long = System.currentTimeMillis(),
    val exportedDate: String = "",
    val sites: List<Site> = emptyList(),
    val workers: List<Worker> = emptyList(),
    val attendance: List<AttendanceRecord> = emptyList()
)
