package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workers")
data class Worker(
    @PrimaryKey
    val workerId: String, // e.g. "W-101"
    val name: String,
    val gender: String = "Male", // "Male", "Female", "Other"
    val workerType: String = "Helper", // "Carpenter", "Mason", "Helper", "Electrician", "Plumber", "Painter", "Welder", "Bar Bender", "Foreman"
    val dailyRate: Double = 20000.0,
    val otRate: Double = 3000.0, // per hour
    val status: String = "Active", // "Active", "Inactive", "Archived"
    val phone: String = "",
    val address: String = "", // နေရပ်လိပ်စာ / ဇာတိ e.g. "ပဲခူးတိုင်း၊ ညောင်လေးပင်"
    val nrcNo: String = "", // မှတ်ပုံတင်အမှတ် e.g. "၁၂/လမတ(နိုင်)၁၂၃၄၅၆"
    val emergencyContactName: String = "", // အရေးပေါ် ဆက်သွယ်ရန် အမည်
    val emergencyPhone: String = "", // အရေးပေါ် ဆက်သွယ်ရန် ဖုန်းနံပါတ်
    val bloodType: String = "", // သွေးအမျိုးအစား (A, B, AB, O)
    val skillLevel: String = "Skilled", // "Foreman", "Skilled", "Semi-Skilled", "Helper"
    val paymentMethod: String = "Cash", // "Cash", "KBZPay", "WavePay", "Bank"
    val paymentAccount: String = "", // KPay/Wave wallet account
    val joinedDate: String = "",
    val notes: String = ""
)

/**
 * Natural comparator strictly for Worker ID sorting.
 * Ensures workers are ordered strictly by Worker ID (e.g. W-101, W-102, W-103) and NEVER by name.
 */
fun compareWorkerIds(id1: String, id2: String): Int {
    val regex = Regex("""^(.*?)(\d+)$""")
    val m1 = regex.matchEntire(id1.trim())
    val m2 = regex.matchEntire(id2.trim())
    if (m1 != null && m2 != null && m1.groupValues[1].equals(m2.groupValues[1], ignoreCase = true)) {
        val num1 = m1.groupValues[2].toLongOrNull() ?: 0L
        val num2 = m2.groupValues[2].toLongOrNull() ?: 0L
        val cmp = num1.compareTo(num2)
        if (cmp != 0) return cmp
    }
    val lenCmp = id1.trim().length.compareTo(id2.trim().length)
    return if (lenCmp != 0) lenCmp else id1.compareTo(id2, ignoreCase = true)
}

fun List<Worker>.sortedByWorkerId(): List<Worker> {
    return this.sortedWith { a, b -> compareWorkerIds(a.workerId, b.workerId) }
}

