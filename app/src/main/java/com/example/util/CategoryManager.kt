package com.example.util

import android.content.Context
import com.example.data.model.ConstructionTrade
import org.json.JSONArray
import org.json.JSONObject

object CategoryManager {

    private const val PREFS_NAME = "labour_category_prefs"
    private const val KEY_CUSTOM_TRADES = "custom_trades_json"

    val STANDARD_TRADES = listOf(
        ConstructionTrade(
            code = "Carpenter",
            englishName = "Carpenter",
            myanmarName = "လက်သမား",
            myanmarDetail = "သစ် / ဘောင် / ဖောင်လုပ်ငန်း",
            defaultDailyRate = 28000.0,
            defaultOtRate = 4000.0
        ),
        ConstructionTrade(
            code = "Mason",
            englishName = "Mason",
            myanmarName = "ပန်းရံ",
            myanmarDetail = "အုတ်စီ / အင်္ဂတေချော / အုတ်ရိုး",
            defaultDailyRate = 28000.0,
            defaultOtRate = 4000.0
        ),
        ConstructionTrade(
            code = "Helper",
            englishName = "Helper",
            myanmarName = "အကူ / အလုပ်ကြမ်း",
            myanmarDetail = "ကျပန်း / ပစ္စည်းသယ် / ဖျော်စက်",
            defaultDailyRate = 18000.0,
            defaultOtRate = 2500.0
        ),
        ConstructionTrade(
            code = "Bar Bender",
            englishName = "Bar Bender",
            myanmarName = "သံချည်သံကွေး",
            myanmarDetail = "သံချောင်း / ကော်လံ / ဘင်း / ဖောင်ဒေးရှင်း",
            defaultDailyRate = 28000.0,
            defaultOtRate = 4000.0
        ),
        ConstructionTrade(
            code = "Welder",
            englishName = "Welder",
            myanmarName = "ဝရိန် / သံဂဟေ",
            myanmarDetail = "သံထည် / ထရက်စ် / အမိုး / ဂဟေဆော်",
            defaultDailyRate = 30000.0,
            defaultOtRate = 4500.0
        ),
        ConstructionTrade(
            code = "Electrician",
            englishName = "Electrician",
            myanmarName = "လျှပ်စစ်ကျွမ်းကျင်",
            myanmarDetail = "မီးလိုင်း / ဝါယာကြိုး / ဘရိတ်ကာ",
            defaultDailyRate = 30000.0,
            defaultOtRate = 4500.0
        ),
        ConstructionTrade(
            code = "Plumber",
            englishName = "Plumber",
            myanmarName = "ရေပိုက်ကျွမ်းကျင်",
            myanmarDetail = "ရေလိုင်း / မိလ္လာ / ပိုက်လိုင်းဆက်",
            defaultDailyRate = 28000.0,
            defaultOtRate = 4000.0
        ),
        ConstructionTrade(
            code = "Painter",
            englishName = "Painter",
            myanmarName = "ဆေးသုတ်",
            myanmarDetail = "အတွင်း / အပြင် / ဆေးကြော / အလှဆင်",
            defaultDailyRate = 26000.0,
            defaultOtRate = 3500.0
        ),
        ConstructionTrade(
            code = "Tile Layer",
            englishName = "Tile Layer",
            myanmarName = "ကြွေပြားကပ်",
            myanmarDetail = "ကြမ်းခင်း / နံရံကပ် / စကျင်ကျောက်",
            defaultDailyRate = 32000.0,
            defaultOtRate = 4500.0
        ),
        ConstructionTrade(
            code = "Aluminium",
            englishName = "Aluminium Fabricator",
            myanmarName = "အလူမီနီယမ်လက်သမား",
            myanmarDetail = "ပြတင်းပေါက် / တံခါး / မှန်ဘောင်",
            defaultDailyRate = 30000.0,
            defaultOtRate = 4500.0
        ),
        ConstructionTrade(
            code = "Ceiling Worker",
            englishName = "Ceiling Worker",
            myanmarName = "မျက်နှာကြက်ကပ်",
            myanmarDetail = "ဂျစ်ပဆမ် / တီဘား / မျက်နှာကြက်အလှ",
            defaultDailyRate = 28000.0,
            defaultOtRate = 4000.0
        ),
        ConstructionTrade(
            code = "Concreter",
            englishName = "Concreter",
            myanmarName = "ကွန်ကရစ်လောင်း",
            myanmarDetail = "ဖောင်ဒေးရှင်း / ကြမ်းခင်း / ကွန်ကရစ်ဖျော်",
            defaultDailyRate = 22000.0,
            defaultOtRate = 3000.0
        ),
        ConstructionTrade(
            code = "Equipment Operator",
            englishName = "Equipment Operator",
            myanmarName = "စက်ယန္တရားမောင်း",
            myanmarDetail = "ကရိန်း / ဘက်ဖိုး / ဖျော်စက်မောင်း",
            defaultDailyRate = 35000.0,
            defaultOtRate = 5000.0
        ),
        ConstructionTrade(
            code = "Foreman",
            englishName = "Foreman / Supervisor",
            myanmarName = "ဖိုမင် / ဆိုဒ်အုပ် / ခေါင်းဆောင်",
            myanmarDetail = "ဆိုဒ်ကြီးကြပ်ရေးမှူး / အင်ဂျင်နီယာ",
            defaultDailyRate = 40000.0,
            defaultOtRate = 6000.0
        )
    )

    fun getCustomTrades(context: Context): List<ConstructionTrade> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_CUSTOM_TRADES, null) ?: return emptyList()
        val list = mutableListOf<ConstructionTrade>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ConstructionTrade(
                        code = obj.optString("code", ""),
                        englishName = obj.optString("englishName", ""),
                        myanmarName = obj.optString("myanmarName", ""),
                        myanmarDetail = obj.optString("myanmarDetail", ""),
                        defaultDailyRate = obj.optDouble("defaultDailyRate", 25000.0),
                        defaultOtRate = obj.optDouble("defaultOtRate", 3500.0),
                        isCustom = true
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveCustomTrade(context: Context, trade: ConstructionTrade): Boolean {
        val current = getCustomTrades(context).toMutableList()
        // If already exists with same code, replace it
        current.removeAll { it.code.equals(trade.code, ignoreCase = true) || it.myanmarName.equals(trade.myanmarName, ignoreCase = true) }
        current.add(trade.copy(isCustom = true))
        return persistTrades(context, current)
    }

    fun deleteCustomTrade(context: Context, codeOrName: String): Boolean {
        val current = getCustomTrades(context).toMutableList()
        val removed = current.removeAll { 
            it.code.equals(codeOrName, ignoreCase = true) || 
            it.englishName.equals(codeOrName, ignoreCase = true) || 
            it.myanmarName.equals(codeOrName, ignoreCase = true)
        }
        if (removed) {
            persistTrades(context, current)
        }
        return removed
    }

    private fun persistTrades(context: Context, list: List<ConstructionTrade>): Boolean {
        try {
            val arr = JSONArray()
            for (t in list) {
                val obj = JSONObject().apply {
                    put("code", t.code)
                    put("englishName", t.englishName)
                    put("myanmarName", t.myanmarName)
                    put("myanmarDetail", t.myanmarDetail)
                    put("defaultDailyRate", t.defaultDailyRate)
                    put("defaultOtRate", t.defaultOtRate)
                }
                arr.put(obj)
            }
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_CUSTOM_TRADES, arr.toString()).apply()
            return true
        } catch (_: Exception) {
            return false
        }
    }

    fun getAllTrades(context: Context, existingWorkerTypes: List<String> = emptyList()): List<ConstructionTrade> {
        val custom = getCustomTrades(context)
        val combined = (STANDARD_TRADES + custom).toMutableList()

        // Also check if any existing workers have workerType that is not yet in combined
        for (wType in existingWorkerTypes) {
            if (wType.isNotBlank() && combined.none { 
                it.code.equals(wType, ignoreCase = true) || 
                it.englishName.equals(wType, ignoreCase = true) || 
                it.myanmarName.equals(wType, ignoreCase = true) 
            }) {
                combined.add(
                    ConstructionTrade(
                        code = wType,
                        englishName = wType,
                        myanmarName = getMyanmarName(wType),
                        myanmarDetail = "ထည့်သွင်းထားသော အမျိုးအစား",
                        defaultDailyRate = 25000.0,
                        defaultOtRate = 3500.0,
                        isCustom = true
                    )
                )
            }
        }
        return combined
    }

    fun getTradeForCode(code: String, context: Context? = null): ConstructionTrade {
        val trimmed = code.trim()
        val std = STANDARD_TRADES.find { 
            it.code.equals(trimmed, ignoreCase = true) || 
            it.englishName.equals(trimmed, ignoreCase = true) ||
            it.myanmarName.equals(trimmed, ignoreCase = true)
        }
        if (std != null) return std

        if (context != null) {
            val custom = getCustomTrades(context).find {
                it.code.equals(trimmed, ignoreCase = true) ||
                it.englishName.equals(trimmed, ignoreCase = true) ||
                it.myanmarName.equals(trimmed, ignoreCase = true)
            }
            if (custom != null) return custom
        }

        return ConstructionTrade(
            code = trimmed,
            englishName = trimmed,
            myanmarName = getMyanmarName(trimmed),
            defaultDailyRate = 25000.0,
            defaultOtRate = 3500.0,
            isCustom = true
        )
    }

    fun getMyanmarName(trade: String): String {
        val lower = trade.lowercase().trim()
        return when {
            lower.contains("carpenter") || lower.contains("လက်သမား") -> "လက်သမား"
            lower.contains("mason") || lower.contains("ပန်းရံ") -> "ပန်းရံ"
            lower.contains("helper") || lower.contains("အကူ") || lower.contains("အလုပ်ကြမ်း") -> "အကူ/အလုပ်ကြမ်း"
            lower.contains("bar bender") || lower.contains("သံချည်") -> "သံချည်သံကွေး"
            lower.contains("welder") || lower.contains("ဝရိန်") || lower.contains("ဝဟ်") -> "ဝရိန်/သံဂဟေ"
            lower.contains("electrician") || lower.contains("လျှပ်စစ်") -> "လျှပ်စစ်ကျွမ်းကျင်"
            lower.contains("plumber") || lower.contains("ရေပိုက်") -> "ရေပိုက်ကျွမ်းကျင်"
            lower.contains("painter") || lower.contains("ဆေးသုတ်") -> "ဆေးသုတ်"
            lower.contains("tile") || lower.contains("ကြွေပြား") -> "ကြွေပြားကပ်"
            lower.contains("aluminium") || lower.contains("အလူမီနီယမ်") -> "အလူမီနီယမ်လက်သမား"
            lower.contains("ceiling") || lower.contains("မျက်နှာကြက်") -> "မျက်နှာကြက်ကပ်"
            lower.contains("concreter") || lower.contains("ကွန်ကရစ်") -> "ကွန်ကရစ်လောင်း"
            lower.contains("operator") || lower.contains("စက်မောင်း") -> "စက်ယန္တရားမောင်း"
            lower.contains("foreman") || lower.contains("ဖိုမင်") || lower.contains("ခေါင်းဆောင်") || lower.contains("အင်ဂျင်နီယာ") -> "ဖိုမင်/ခေါင်းဆောင်"
            else -> trade
        }
    }

    fun getDisplayName(trade: String): String {
        val myName = getMyanmarName(trade)
        return if (myName != trade && trade.isNotBlank()) {
            "$myName ($trade)"
        } else {
            myName
        }
    }
}
