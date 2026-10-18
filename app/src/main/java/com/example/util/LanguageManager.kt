package com.example.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    MYANMAR("my", "Burmese", "မြန်မာ")
}

object LanguageManager {
    private val _currentLanguage = MutableStateFlow(AppLanguage.MYANMAR)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
    }

    fun toggleLanguage() {
        _currentLanguage.value = if (_currentLanguage.value == AppLanguage.ENGLISH) {
            AppLanguage.MYANMAR
        } else {
            AppLanguage.ENGLISH
        }
    }

    fun getString(key: String): String {
        val isMy = _currentLanguage.value == AppLanguage.MYANMAR
        return when (key) {
            "app_title" -> if (isMy) "LabourPro" else "LabourPro"
            "dashboard_subtitle" -> if (isMy) "ဆိုဒ်နှင့် လုပ်အားခ စီမံခန့်ခွဲမှု" else "Site & Wages Dashboard"
            "pos_subtitle" -> if (isMy) "နေ့စဉ် အမြန်စာရင်းသွင်း" else "POS Attendance Entry"
            "log_subtitle" -> if (isMy) "တက်ရောက်မှု မှတ်တမ်း" else "Daily Attendance Log"
            "worker_subtitle" -> if (isMy) "လုပ်သား မှတ်ပုံတင်စာရင်း" else "Worker Master Directory"
            "site_subtitle" -> if (isMy) "ဆောက်လုပ်ရေးဆိုဒ် စာရင်း" else "Construction Site Master"
            "ledger_subtitle" -> if (isMy) "ကြိုထုတ်ငွေနှင့် လက်ကျန်စာရင်း" else "Advance & Wage Ledger"
            "export_subtitle" -> if (isMy) "အစီရင်ခံစာ (PDF, Excel, CSV) ထုတ်ယူရန်" else "Report (PDF, Excel, CSV) Export"

            // Bottom Nav
            "nav_home" -> if (isMy) "ပင်မ" else "Home"
            "nav_workers" -> if (isMy) "လုပ်သားစာရင်း" else "Workers"
            "nav_pos" -> if (isMy) "စာရင်းသွင်း" else "POS Entry"
            "nav_log" -> if (isMy) "တက်ရောက်မှု" else "Log"
            "nav_ledger" -> if (isMy) "ငွေစာရင်း" else "Ledger"
            "nav_excel" -> if (isMy) "အစီရင်ခံစာ" else "Report"

            // POS Form Headers
            "section_1" -> if (isMy) "၁။ ဆိုဒ်နှင့် နေ့စွဲ ရွေးချယ်ခြင်း" else "1. CONSTRUCTION SITE & DATE"
            "section_2" -> if (isMy) "၂။ လုပ်သား ရွေးချယ်ခြင်း" else "2. WORKER SELECTION"
            "section_3" -> if (isMy) "၃။ အလုပ်ချိန် တက်ရောက်မှု" else "3. SHIFT ATTENDANCE"
            "section_4" -> if (isMy) "၄။ အချိန်ပို (OT) နာရီနှင့် နှုန်းထား" else "4. OVERTIME (OT) & RATES"
            "section_5" -> if (isMy) "၅။ ကြိုထုတ်ငွေ၊ ပေးငွေနှင့် မှတ်ချက်" else "5. ADVANCES, PAYMENTS & REMARK"
            "section_6" -> if (isMy) "အချိန်နှင့်တပြေးညီ တွက်ချက်မှု" else "REAL-TIME CALCULATION"

            // Presets
            "preset_full_day" -> if (isMy) "တစ်နေ့လုံး\n(၁.၀)" else "Full Day\n(1.0)"
            "preset_half_am" -> if (isMy) "မနက်ဝက်\n(၀.၅)" else "AM Half\n(0.5)"
            "preset_half_pm" -> if (isMy) "ညနေဝက်\n(၀.၅)" else "PM Half\n(0.5)"
            "preset_absent" -> if (isMy) "ပျက်ကွက်\n(၀.၀)" else "Absent\n(0.0)"

            // Shift labels
            "morning_shift" -> if (isMy) "နံနက်ပိုင်း- P (၀.၅) • HD (၀.၂၅) • A (၀)" else "Morning Shift: P (0.5) • HD (0.25) • A (0.0)"
            "evening_shift" -> if (isMy) "ညနေပိုင်း- P (၀.၅) • HD (၀.၂၅) • A (၀)" else "Evening Shift: P (0.5) • HD (0.25) • A (0.0)"
            "present_btn" -> if (isMy) "P - တက်ရောက် (၀.၅)" else "P - Present (0.5)"
            "half_btn" -> if (isMy) "HD - တစ်ဝက် (၀.၂၅)" else "HD - Half Day (0.25)"
            "absent_btn" -> if (isMy) "A - ပျက်ကွက် (၀)" else "A - Absent (0)"

            // Calculation Lines
            "calc_daily_wage" -> if (isMy) "နေ့တွက်လုပ်အားခ" else "Daily Wages"
            "calc_ot_wage" -> if (isMy) "အချိန်ပိုလုပ်အားခ" else "OT Wages"
            "calc_total_earned" -> if (isMy) "စုစုပေါင်းရငွေ (နေ့တွက်ခ + OT)" else "Total Earned (Daily Wages + OT)"
            "calc_less_adv" -> if (isMy) "နုတ် - ယနေ့ကြိုထုတ်ငွေ (-)" else "Less Advance Today (-)"
            "calc_less_paid" -> if (isMy) "နုတ် - ယနေ့ပေးငွေ (-)" else "Less Paid Today (-)"
            "calc_balance_due" -> if (isMy) "ပေးရန်ကျန်ငွေ" else "REMAINING BALANCE DUE"

            // Actions
            "save_and_next" -> if (isMy) "သိမ်းဆည်းပြီး နောက်တစ်ဦး" else "SAVE & NEXT WORKER"
            "save_only" -> if (isMy) "သိမ်းဆည်းမည်" else "Save Only"
            "reset" -> if (isMy) "အသစ်ပြန်စ" else "Reset"
            "duplicate_warning" -> if (isMy) "သတိပေးချက်- ဤလုပ်သားသည် ယနေ့အတွက် စာရင်းသွင်းပြီးဖြစ်ပါသည်!" else "DUPLICATE PREVENTED: Worker is already marked for this date!"

            // Trades in Myanmar
            "Carpenter" -> if (isMy) "လက်သမား" else "Carpenter"
            "Mason" -> if (isMy) "ပန်းရံ" else "Mason"
            "Helper" -> if (isMy) "အကူ/အလုပ်ကြမ်း" else "Helper"
            "Electrician" -> if (isMy) "လျှပ်စစ်ကျွမ်းကျင်" else "Electrician"
            "Plumber" -> if (isMy) "ရေပိုက်ကျွမ်းကျင်" else "Plumber"
            "Painter" -> if (isMy) "ဆေးသုတ်" else "Painter"
            "Welder" -> if (isMy) "ဝရိန်/သံဂဟေ" else "Welder"
            "Bar Bender" -> if (isMy) "သံချည်သံကွေး" else "Bar Bender"
            "Foreman" -> if (isMy) "ဖိုမင်/ခေါင်းဆောင်" else "Foreman"
            "Tile Layer" -> if (isMy) "ကြွေပြားကပ်" else "Tile Layer"
            "Aluminium" -> if (isMy) "အလူမီနီယမ်လက်သမား" else "Aluminium"
            "Ceiling Worker" -> if (isMy) "မျက်နှာကြက်ကပ်" else "Ceiling Worker"
            "Concreter" -> if (isMy) "ကွန်ကရစ်လောင်း" else "Concreter"
            "Equipment Operator" -> if (isMy) "စက်ယန္တရားမောင်း" else "Equipment Operator"

            // Category UI
            "category_title" -> if (isMy) "ကျွမ်းကျင်မှု / ဆောက်လုပ်ရေးအမျိုးအစား" else "Skill / Trade Category"
            "custom_category" -> if (isMy) "စိတ်ကြိုက် အမျိုးအစား" else "Custom Category"
            "add_custom_category" -> if (isMy) "အမျိုးအစားအသစ် ထည့်မည်" else "Add Custom Category"
            "manage_categories" -> if (isMy) "အမျိုးအစားများ စီမံရန်" else "Manage Categories"
            "select_category" -> if (isMy) "အမျိုးအစား ရွေးချယ်ပါ" else "Select Trade Category"
            "all_categories" -> if (isMy) "အမျိုးအစား အားလုံး" else "All Categories"

            // Statuses
            "Active" -> if (isMy) "လက်ရှိ" else "Active"
            "Inactive" -> if (isMy) "ရပ်နား" else "Inactive"
            "Ongoing" -> if (isMy) "လုပ်ဆောင်ဆဲ" else "Ongoing"
            "Completed" -> if (isMy) "ပြီးစီး" else "Completed"
            "On Hold" -> if (isMy) "ဆိုင်းငံ့" else "On Hold"

            // Report and Export strings
            "export_title" -> if (isMy) "အစီရင်ခံစာ ထုတ်ယူခြင်းနှင့် တိုက်ရိုက်ကြည့်ရှုခြင်း" else "Report Export & Direct Viewer"
            "export_description" -> if (isMy) "ရက်စွဲအလိုက် စိစစ်ပြီး PDF, Excel (XLSX) နှင့် CSV ပုံစံများဖြင့် တိုက်ရိုက်ကြည့်ရှုခြင်း သို့မဟုတ် ထုတ်ယူမျှဝေခြင်း ပြုလုပ်နိုင်ပါသည်။" else "Filter by date and export or directly view reports in PDF, Excel (XLSX), and CSV formats."
            "filter_date_heading" -> if (isMy) "ရက်စွဲ ရွေးချယ်မှု (Date Filter)" else "Filter by Date"
            "date_all" -> if (isMy) "ရက်စွဲအားလုံး" else "All Dates"
            "date_today" -> if (isMy) "ယနေ့" else "Today"
            "date_custom_day" -> if (isMy) "သတ်မှတ်ရက်စွဲ" else "Specific Date"
            "date_range" -> if (isMy) "ရက်စွဲအပိုင်းအခြား" else "Date Range"
            "from_date" -> if (isMy) "စတင်ရက်" else "From Date"
            "to_date" -> if (isMy) "ပြီးဆုံးရက်" else "To Date"
            "format_pdf" -> if (isMy) "PDF ဖိုင် (.pdf)" else "PDF Report (.pdf)"
            "format_excel" -> if (isMy) "Excel ဖိုင် (.xlsx)" else "Excel (.xlsx)"
            "format_csv" -> if (isMy) "CSV ဖိုင် (.csv)" else "CSV File (.csv)"
            "action_open_direct" -> if (isMy) "တိုက်ရိုက် ဖွင့်ကြည့်မည်" else "OPEN & VIEW DIRECTLY"
            "action_share" -> if (isMy) "မျှဝေမည်" else "Share Report"
            "action_copy" -> if (isMy) "ကူးယူမည်" else "Copy CSV"
            "view_table" -> if (isMy) "ဇယားပုံစံ တိုက်ရိုက်ကြည့်ရန်" else "Interactive Table View"
            "view_raw" -> if (isMy) "ဖိုင်စာသား နမူနာ" else "Raw File Preview"
            "report_date" -> if (isMy) "ထုတ်သည့်ရက်စွဲ" else "Report Date"
            "period_label" -> if (isMy) "ရက်စွဲကာလ" else "Date Period"
            "records_found" -> if (isMy) "မှတ်တမ်းပေါင်း" else "Total Records"
            "total_mandays" -> if (isMy) "လူအင်အားရက်" else "Man-Days"
            "total_earned_mmk" -> if (isMy) "စုစုပေါင်းရငွေ (ကျပ်)" else "Total Earned (MMK)"
            "total_advance_mmk" -> if (isMy) "ကြိုထုတ်ငွေ (ကျပ်)" else "Advance (MMK)"
            "total_paid_mmk" -> if (isMy) "ပေးငွေ (ကျပ်)" else "Paid Today (MMK)"
            "net_balance_mmk" -> if (isMy) "လက်ကျန်ကျပ်ငွေ" else "Balance Due (MMK)"

            // Backup & Restore
            "backup_title" -> if (isMy) "ဒေတာ အရန်သိမ်းခြင်းနှင့် ပြန်လည်ရယူခြင်း" else "Data Backup & Restore"
            "backup_to_drive" -> if (isMy) "Google Drive သို့ သိမ်းမည် / မျှဝေမည်" else "Backup to Drive / Share"
            "backup_local" -> if (isMy) "စက်တွင်းသို့ သိမ်းဆည်းမည် (Local)" else "Save Backup Locally"
            "restore_from_file" -> if (isMy) "အရန်ဖိုင်မှ ပြန်လည်ရယူမည် (Restore)" else "Restore from Backup File"
            "clear_sample_title" -> if (isMy) "နမူနာဒေတာ ဖျက်ရန် / ပြန်လည်စတင်ရန်" else "Clear Sample Data / Reset"
            "clear_attendance_only" -> if (isMy) "တက်ရောက်မှု မှတ်တမ်းများအားလုံး ဖျက်မည် (၁ ချက်နှိပ်)" else "Clear Attendance Records (1-Click)"
            "clear_all_data" -> if (isMy) "ဒေတာ အားလုံးကို အပြီးတိုင် ဖျက်မည် (သန့်ရှင်းရေး)" else "Wipe All Data Completely"
            "reload_sample_data" -> if (isMy) "နမူနာဒေတာများ ပြန်လည်ထည့်သွင်းမည်" else "Reload Demo Sample Data"
            "batch_attendance" -> if (isMy) "ဆိုဒ်လိုက် အားလုံးတစ်ပြိုင်နက် မှတ်ရန်" else "Batch Attendance (All Workers)"
            "mark_all_present" -> if (isMy) "အားလုံး အပြည့်တက်ရောက် (၁.၀)" else "Mark All Full Day"
            "share_voucher" -> if (isMy) "ဖြတ်ပိုင်း ပို့ရန်" else "Share Slip"
            "share_site_summary" -> if (isMy) "နေ့စဉ် ဆိုဒ်ရှင်းတမ်း ပို့ရန်" else "Share Site Summary"
            "call_worker" -> if (isMy) "ဖုန်းခေါ်မည်" else "Call Worker"
            "top_site_only" -> if (isMy) "ဦးစားပေး ဆိုဒ်" else "Top Priority Site"
            "all_sites" -> if (isMy) "ဆိုဒ်အားလုံး" else "All Sites"
            "today_only" -> if (isMy) "ယနေ့ (Today)" else "Today"
            "yesterday_only" -> if (isMy) "မနေ့က (Yesterday)" else "Yesterday"

            // Deletion & Record Management
            "delete_record_title" -> if (isMy) "တက်ရောက်မှု မှတ်တမ်း ဖျက်ရန်" else "Delete Attendance Record"
            "delete_record_confirm_msg" -> if (isMy) "ဤမှတ်တမ်းကို ဖျက်မည်မှာ သေချာပါသလား။ လုပ်အားခနှင့် ကြိုထုတ်ငွေ တွက်ချက်မှုများမှ အပြီးအပိုင် ပယ်ဖျက်သွားပါမည်။" else "Are you sure you want to delete this attendance record? It will be permanently removed from wages and balances."
            "delete_btn" -> if (isMy) "ဖျက်မည်" else "Delete"
            "cancel_btn" -> if (isMy) "မဖျက်ပါ" else "Cancel"
            "delete_mistaken_entry" -> if (isMy) "မှားသွင်းမိ၍ ဖျက်မည်" else "Delete Mistaken Entry"
            "edit_existing_entry" -> if (isMy) "စာရင်းကို ပြင်ဆင်မည်" else "Edit Existing Entry"
            "today_logged_title" -> if (isMy) "ဤရက်စွဲအတွက် စာရင်းသွင်းပြီးသူများ" else "Logged Workers for this Date"
            "update_record" -> if (isMy) "အပြောင်းအလဲ သိမ်းမည်" else "UPDATE RECORD"
            "delete_record" -> if (isMy) "ဤမှတ်တမ်း ဖျက်မည်" else "DELETE RECORD"
            "cancel_edit" -> if (isMy) "ပြင်ဆင်မှု ပယ်ဖျက်မည်" else "Cancel Edit"
            "editing_badge" -> if (isMy) "စာရင်း ပြင်ဆင်နေသည်" else "EDITING RECORD"
            "search_placeholder" -> if (isMy) "အမည်၊ ID၊ အလုပ်အမျိုးအစားဖြင့် ရှာပါ..." else "Search name, ID, trade..."

            // Defaults
            else -> key
        }
    }
}
