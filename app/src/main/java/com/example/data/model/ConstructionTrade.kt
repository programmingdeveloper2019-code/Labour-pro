package com.example.data.model

data class ConstructionTrade(
    val code: String,
    val englishName: String,
    val myanmarName: String,
    val myanmarDetail: String = "",
    val defaultDailyRate: Double = 25000.0,
    val defaultOtRate: Double = 3500.0,
    val isCustom: Boolean = false
) {
    val displayLabel: String
        get() = if (myanmarName.isNotBlank() && englishName.isNotBlank() && myanmarName != englishName) {
            "$myanmarName ($englishName)"
        } else if (myanmarName.isNotBlank()) {
            myanmarName
        } else {
            englishName
        }
}
