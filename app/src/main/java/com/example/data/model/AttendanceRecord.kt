package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance_records",
    indices = [
        Index(value = ["date", "workerId"], unique = true),
        Index(value = ["date"]),
        Index(value = ["siteCode"])
    ]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // Format: "YYYY-MM-DD"
    val siteCode: String,
    val siteName: String,
    val workerId: String,
    val workerName: String,
    val workerType: String = "Helper",
    val morningAttendance: String = "P", // "P" (0.5), "HD" (0.25), "A" (0.0)
    val eveningAttendance: String = "P", // "P" (0.5), "HD" (0.25), "A" (0.0)
    val dailyRate: Double = 20000.0,
    val otHours: Double = 0.0,
    val otRate: Double = 3000.0,
    val advanceToday: Double = 0.0,
    val paidToday: Double = 0.0,
    val remark: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val morningValue: Double
        get() = when (morningAttendance.uppercase()) {
            "P" -> 0.5
            "HD" -> 0.25
            else -> 0.0
        }

    val eveningValue: Double
        get() = when (eveningAttendance.uppercase()) {
            "P" -> 0.5
            "HD" -> 0.25
            else -> 0.0
        }

    val totalDayUnits: Double
        get() = morningValue + eveningValue

    val dailyAmount: Double
        get() = totalDayUnits * dailyRate

    val otAmount: Double
        get() = otHours * otRate

    val totalEarned: Double
        get() = dailyAmount + otAmount

    val balance: Double
        get() = totalEarned - advanceToday - paidToday
}
