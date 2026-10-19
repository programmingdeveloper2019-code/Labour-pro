package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AttendanceRecord
import com.example.data.model.Site
import com.example.data.model.Worker
import com.example.data.repository.LabourRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.util.AppLanguage
import com.example.util.LanguageManager
import com.example.util.ExcelExportHelper
import com.example.util.BackupManager
import com.example.data.model.LabourBackupData
import kotlinx.coroutines.flow.first

enum class AppScreen {
    DASHBOARD,
    POS_ENTRY,
    ATTENDANCE_LIST,
    WORKER_MASTER,
    SITE_MASTER,
    ADVANCE_LEDGER,
    EXCEL_EXPORT
}

enum class AttendancePreset {
    FULL_DAY,       // Morning P (0.5), Evening P (0.5) = 1.0
    HALF_DAY_AM,    // Morning P (0.5), Evening A (0.0) = 0.5
    HALF_DAY_PM,    // Morning A (0.0), Evening P (0.5) = 0.5
    QUARTER_DAY,    // Morning HD (0.25), Evening A (0.0) = 0.25
    ABSENT          // Morning A (0.0), Evening A (0.0) = 0.0
}

data class PosFormState(
    val selectedDate: String,
    val selectedSite: Site? = null,
    val selectedWorker: Worker? = null,
    val morningAttendance: String = "P",
    val eveningAttendance: String = "P",
    val dailyRate: Double = 20000.0,
    val otHours: Double = 0.0,
    val otRate: Double = 3000.0,
    val advanceToday: Double = 0.0,
    val paidToday: Double = 0.0,
    val remark: String = "",
    val editingRecordId: Long? = null,
    val duplicateExistingRecord: AttendanceRecord? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
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

class LabourViewModel(private val app: Application) : AndroidViewModel(app) {

    private val repository: LabourRepository

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayDateString: String = dateFormat.format(Date())

    private val _currentScreen = MutableStateFlow(AppScreen.POS_ENTRY)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    val allWorkers: StateFlow<List<Worker>>
    val activeWorkers: StateFlow<List<Worker>>
    val allSites: StateFlow<List<Site>>
    val activeSites: StateFlow<List<Site>>
    val allAttendance: StateFlow<List<AttendanceRecord>>

    // POS Form State
    private val _posForm = MutableStateFlow(PosFormState(selectedDate = todayDateString))
    val posForm: StateFlow<PosFormState> = _posForm.asStateFlow()

    // Filter states for reports & table view
    val filterSiteCode = MutableStateFlow<String?>(null)
    val filterWorkerId = MutableStateFlow<String?>(null)
    val filterDate = MutableStateFlow<String?>(todayDateString)

    // Dynamic marked worker IDs for the selected date in POS
    @OptIn(ExperimentalCoroutinesApi::class)
    val markedWorkerIdsForSelectedDate: StateFlow<List<String>>

    init {
        val database = AppDatabase.getDatabase(app, viewModelScope)
        repository = LabourRepository(
            database.workerDao(),
            database.siteDao(),
            database.attendanceDao()
        )

        allWorkers = repository.allWorkers.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

        activeWorkers = repository.activeWorkers.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

        allSites = repository.allSites.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

        activeSites = repository.activeSites.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

        allAttendance = repository.allAttendance.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

        markedWorkerIdsForSelectedDate = _posForm.flatMapLatest { form ->
            repository.getMarkedWorkerIdsForDate(form.selectedDate)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Pre-select first site once sites are available
        viewModelScope.launch {
            repository.activeSites.collect { sites ->
                if (_posForm.value.selectedSite == null && sites.isNotEmpty()) {
                    _posForm.value = _posForm.value.copy(selectedSite = sites.first())
                }
            }
        }

        // Non-destructive permanent worker sync on app launch:
        // Guarantees NO sample workers exist, and only genuine user-entered workers are preserved
        viewModelScope.launch {
            try {
                // 1. Ensure user-entered workers are NEVER purged or deleted.
                // Always sync between Room DB and PermanentWorkerStore so workers never disappear.
                val dbWorkers = repository.getAllWorkersList()
                val permWorkers = com.example.util.PermanentWorkerStore.loadWorkers(app) ?: emptyList()

                if (dbWorkers.isEmpty() && permWorkers.isNotEmpty()) {
                    repository.insertWorkers(permWorkers)
                } else if (dbWorkers.isNotEmpty()) {
                    com.example.util.PermanentWorkerStore.saveWorkers(app, dbWorkers)
                }

                // 2. Ensure Daily Attendance records are NEVER lost
                // Sync between Room DB and PermanentAttendanceStore
                val dbAttendance = repository.getAllAttendanceList()
                val permAttendance = com.example.util.PermanentAttendanceStore.loadAttendance(app) ?: emptyList()

                if (dbAttendance.isEmpty() && permAttendance.isNotEmpty()) {
                    repository.insertAttendanceList(permAttendance)
                } else if (dbAttendance.isNotEmpty()) {
                    com.example.util.PermanentAttendanceStore.saveAttendance(app, dbAttendance)
                }

                // 3. Check for Sites recovery if sites are empty
                val currentSites = repository.allSites.first()
                if (currentSites.isEmpty()) {
                    val vaultBackup = BackupManager.findExistingPersistentVault(app)
                    if (vaultBackup != null && vaultBackup.sites.isNotEmpty()) {
                        for (site in vaultBackup.sites) {
                            repository.insertSite(site)
                        }
                    }
                }

                // 4. If attendance was empty in both DB and PermStore, check Persistent Auto-Vault
                if (dbAttendance.isEmpty() && permAttendance.isEmpty()) {
                    val vaultBackup = BackupManager.findExistingPersistentVault(app)
                    if (vaultBackup != null && vaultBackup.attendance.isNotEmpty()) {
                        repository.insertAttendanceList(vaultBackup.attendance)
                        com.example.util.PermanentAttendanceStore.saveAttendance(app, vaultBackup.attendance)
                    }
                }

                // 5. Keep auto-vault synchronized across layers
                triggerAutoVaultSave()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Persistent Vault & Auto-Recovery State
    private val _detectedPersistentVault = MutableStateFlow<LabourBackupData?>(null)
    val detectedPersistentVault: StateFlow<LabourBackupData?> = _detectedPersistentVault.asStateFlow()

    private val _lastVaultBackupTime = MutableStateFlow(BackupManager.getLastBackupTime(app))
    val lastVaultBackupTime: StateFlow<Long> = _lastVaultBackupTime.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectDate(date: String) {
        _posForm.value = _posForm.value.copy(
            selectedDate = date,
            errorMessage = null,
            duplicateExistingRecord = null
        )
        // Check if selected worker is already marked for this new date
        val currentWorker = _posForm.value.selectedWorker
        if (currentWorker != null && _posForm.value.editingRecordId == null) {
            viewModelScope.launch {
                val existingRecord = repository.getRecordForDateAndWorker(date, currentWorker.workerId)
                val isMy = LanguageManager.currentLanguage.value == AppLanguage.MYANMAR
                if (existingRecord != null) {
                    _posForm.value = _posForm.value.copy(
                        duplicateExistingRecord = existingRecord,
                        errorMessage = if (isMy) {
                            "သတိပေးချက်- ${currentWorker.name} (${currentWorker.workerId}) သည် $date အတွက် စာရင်းသွင်းပြီးဖြစ်ပါသည်!"
                        } else {
                            "Worker ${currentWorker.name} is ALREADY marked on $date!"
                        }
                    )
                } else {
                    _posForm.value = _posForm.value.copy(
                        duplicateExistingRecord = null,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun selectSite(site: Site) {
        _posForm.value = _posForm.value.copy(selectedSite = site)
    }

    fun selectWorker(worker: Worker) {
        val form = _posForm.value
        viewModelScope.launch {
            val existingRecord = repository.getRecordForDateAndWorker(form.selectedDate, worker.workerId)
            val isMy = LanguageManager.currentLanguage.value == AppLanguage.MYANMAR
            val isMarked = existingRecord != null
            val error = if (isMarked && form.editingRecordId == null) {
                if (isMy) {
                    "သတိပေးချက်- ${worker.name} (${worker.workerId}) သည် ${form.selectedDate} အတွက် စာရင်းသွင်းပြီးဖြစ်ပါသည်!"
                } else {
                    "Worker ${worker.name} is ALREADY marked on ${form.selectedDate}!"
                }
            } else null

            _posForm.value = form.copy(
                selectedWorker = worker,
                dailyRate = if (form.editingRecordId != null) form.dailyRate else worker.dailyRate,
                otRate = if (form.editingRecordId != null) form.otRate else worker.otRate,
                duplicateExistingRecord = if (form.editingRecordId == null) existingRecord else null,
                errorMessage = error
            )
        }
    }

    fun setMorningAttendance(status: String) {
        _posForm.value = _posForm.value.copy(morningAttendance = status)
    }

    fun setEveningAttendance(status: String) {
        _posForm.value = _posForm.value.copy(eveningAttendance = status)
    }

    fun applyPreset(preset: AttendancePreset) {
        when (preset) {
            AttendancePreset.FULL_DAY -> {
                _posForm.value = _posForm.value.copy(morningAttendance = "P", eveningAttendance = "P")
            }
            AttendancePreset.HALF_DAY_AM -> {
                _posForm.value = _posForm.value.copy(morningAttendance = "P", eveningAttendance = "A")
            }
            AttendancePreset.HALF_DAY_PM -> {
                _posForm.value = _posForm.value.copy(morningAttendance = "A", eveningAttendance = "P")
            }
            AttendancePreset.QUARTER_DAY -> {
                _posForm.value = _posForm.value.copy(morningAttendance = "HD", eveningAttendance = "A")
            }
            AttendancePreset.ABSENT -> {
                _posForm.value = _posForm.value.copy(morningAttendance = "A", eveningAttendance = "A")
            }
        }
    }

    fun setOtHours(hours: Double) {
        _posForm.value = _posForm.value.copy(otHours = hours.coerceAtLeast(0.0))
    }

    fun adjustOtHours(delta: Double) {
        val newOt = (_posForm.value.otHours + delta).coerceAtLeast(0.0)
        _posForm.value = _posForm.value.copy(otHours = newOt)
    }

    fun setDailyRate(rate: Double) {
        _posForm.value = _posForm.value.copy(dailyRate = rate.coerceAtLeast(0.0))
    }

    fun setOtRate(rate: Double) {
        _posForm.value = _posForm.value.copy(otRate = rate.coerceAtLeast(0.0))
    }

    fun setAdvanceToday(amount: Double) {
        _posForm.value = _posForm.value.copy(advanceToday = amount.coerceAtLeast(0.0))
    }

    fun setPaidToday(amount: Double) {
        _posForm.value = _posForm.value.copy(paidToday = amount.coerceAtLeast(0.0))
    }

    fun setRemark(text: String) {
        _posForm.value = _posForm.value.copy(remark = text)
    }

    fun clearFormMessage() {
        _posForm.value = _posForm.value.copy(errorMessage = null, successMessage = null)
    }

    fun resetForm() {
        val defaultSite = activeSites.value.firstOrNull() ?: allSites.value.firstOrNull()
        _posForm.value = PosFormState(
            selectedDate = _posForm.value.selectedDate,
            selectedSite = defaultSite ?: _posForm.value.selectedSite,
            selectedWorker = null,
            morningAttendance = "P",
            eveningAttendance = "P",
            otHours = 0.0,
            advanceToday = 0.0,
            paidToday = 0.0,
            remark = "",
            editingRecordId = null,
            errorMessage = null,
            successMessage = null
        )
    }

    fun editRecord(record: AttendanceRecord) {
        val worker = allWorkers.value.find { it.workerId == record.workerId }
            ?: Worker(
                workerId = record.workerId,
                name = record.workerName,
                workerType = record.workerType,
                dailyRate = record.dailyRate,
                otRate = record.otRate
            )
        val site = allSites.value.find { it.siteCode == record.siteCode }
            ?: Site(siteCode = record.siteCode, siteName = record.siteName)

        val isMy = LanguageManager.currentLanguage.value == AppLanguage.MYANMAR
        _posForm.value = PosFormState(
            selectedDate = record.date,
            selectedSite = site,
            selectedWorker = worker,
            morningAttendance = record.morningAttendance,
            eveningAttendance = record.eveningAttendance,
            dailyRate = record.dailyRate,
            otHours = record.otHours,
            otRate = record.otRate,
            advanceToday = record.advanceToday,
            paidToday = record.paidToday,
            remark = record.remark,
            editingRecordId = record.id,
            duplicateExistingRecord = null,
            errorMessage = null,
            successMessage = if (isMy) "${record.workerName} ၏ စာရင်းကို ပြင်ဆင်နေပါသည်" else "Editing record for ${record.workerName}"
        )
        _currentScreen.value = AppScreen.POS_ENTRY
    }

    /**
     * Saves attendance. If advanceToNext is true, prepares form for next unmarked worker.
     */
    fun saveAttendance(advanceToNextWorker: Boolean = false) {
        val form = _posForm.value
        val worker = form.selectedWorker
        val isMy = LanguageManager.currentLanguage.value == AppLanguage.MYANMAR

        if (worker == null) {
            _posForm.value = form.copy(
                errorMessage = if (isMy) "ကျေးဇူးပြု၍ လုပ်သားကို အရင်ရွေးချယ်ပါ *" else "Please select a worker first."
            )
            return
        }

        var site = form.selectedSite ?: allSites.value.firstOrNull()
        if (site == null) {
            val defaultSite = Site("S-01", if (isMy) "ပင်မဆောက်လုပ်ရေးဆိုဒ်" else "Main Construction Site", "Yangon", "Supervisor", "Ongoing")
            viewModelScope.launch {
                repository.insertSite(defaultSite)
            }
            site = defaultSite
            _posForm.value = _posForm.value.copy(selectedSite = defaultSite)
        }

        viewModelScope.launch {
            // Check duplicate validation if not editing
            if (form.editingRecordId == null) {
                val alreadyMarked = repository.isWorkerMarkedForDate(form.selectedDate, worker.workerId)
                if (alreadyMarked) {
                    _posForm.value = form.copy(
                        errorMessage = if (isMy) {
                            "သတိပေးချက်- ${worker.name} (${worker.workerId}) သည် ${form.selectedDate} အတွက် စာရင်းသွင်းပြီးဖြစ်ပါသည်! စာရင်းနှစ်ခါထပ်၍မရပါ။"
                        } else {
                            "ERROR: ${worker.name} (${worker.workerId}) is ALREADY recorded for ${form.selectedDate}. Duplicates are prohibited!"
                        }
                    )
                    return@launch
                }
            }

            val record = AttendanceRecord(
                id = form.editingRecordId ?: 0,
                date = form.selectedDate,
                siteCode = site.siteCode,
                siteName = site.siteName,
                workerId = worker.workerId,
                workerName = worker.name,
                workerType = worker.workerType,
                morningAttendance = form.morningAttendance,
                eveningAttendance = form.eveningAttendance,
                dailyRate = form.dailyRate,
                otHours = form.otHours,
                otRate = form.otRate,
                advanceToday = form.advanceToday,
                paidToday = form.paidToday,
                remark = form.remark
            )

            if (form.editingRecordId != null) {
                repository.updateAttendance(record)
            } else {
                repository.insertAttendance(record)
            }
            // Synchronize with PermanentAttendanceStore so daily attendance is never lost
            val currentAllAttendance = repository.getAllAttendanceList()
            com.example.util.PermanentAttendanceStore.saveAttendance(app, currentAllAttendance)

            // Find next available active worker if rapid POS entry requested
            val markedIds = repository.getRecordForDateAndWorker(form.selectedDate, worker.workerId)?.let {
                markedWorkerIdsForSelectedDate.value + worker.workerId
            } ?: (markedWorkerIdsForSelectedDate.value + worker.workerId)

            val nextWorker = if (advanceToNextWorker) {
                activeWorkers.value.firstOrNull { it.workerId !in markedIds && it.workerId != worker.workerId }
            } else null

            _posForm.value = PosFormState(
                selectedDate = form.selectedDate,
                selectedSite = site,
                selectedWorker = nextWorker,
                morningAttendance = "P",
                eveningAttendance = "P",
                dailyRate = nextWorker?.dailyRate ?: 20000.0,
                otHours = 0.0,
                otRate = nextWorker?.otRate ?: 3000.0,
                advanceToday = 0.0,
                paidToday = 0.0,
                remark = "",
                editingRecordId = null,
                errorMessage = null,
                successMessage = if (nextWorker != null) {
                    if (isMy) {
                        "${worker.name} ၏ စာရင်းသွင်းပြီးပါပြီ! နောက်တစ်ဦး ${nextWorker.name} အတွက် အဆင်သင့်ဖြစ်ပါပြီ။"
                    } else {
                        "Saved ${worker.name}! Ready for ${nextWorker.name}."
                    }
                } else {
                    if (isMy) {
                        "${worker.name} ၏ စာရင်းသွင်းမှု အောင်မြင်ပါသည် (စုစုပေါင်းရငွေ: ${ExcelExportHelper.formatCurrency(record.totalEarned)})"
                    } else {
                        "Saved attendance for ${worker.name} successfully (Earned: ${ExcelExportHelper.formatCurrency(record.totalEarned)})."
                    }
                }
            )
            triggerAutoVaultSave()
        }
    }

    fun deleteAttendance(record: AttendanceRecord, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteAttendance(record)
            val remainingAttendance = repository.getAllAttendanceList()
            com.example.util.PermanentAttendanceStore.saveAttendance(
                app,
                remainingAttendance,
                explicitWipe = remainingAttendance.isEmpty()
            )
            triggerAutoVaultSave()
            val isMy = LanguageManager.currentLanguage.value == AppLanguage.MYANMAR
            val msg = if (isMy) {
                "${record.workerName} (${record.date}) ၏ တက်ရောက်မှုစာရင်းကို အောင်မြင်စွာ ဖျက်ပြီးပါပြီ။"
            } else {
                "Deleted attendance for ${record.workerName} on ${record.date}."
            }
            if (_posForm.value.editingRecordId == record.id || _posForm.value.duplicateExistingRecord?.id == record.id) {
                resetForm()
            }
            _posForm.value = _posForm.value.copy(
                successMessage = msg,
                errorMessage = null,
                duplicateExistingRecord = null
            )
            onComplete()
        }
    }

    suspend fun getRecordForDateAndWorker(date: String, workerId: String): AttendanceRecord? {
        return repository.getRecordForDateAndWorker(date, workerId)
    }

    // Auto Vault & Recovery Methods
    fun triggerAutoVaultSave() {
        viewModelScope.launch {
            try {
                val backup = repository.getBackupData(todayDateString)
                BackupManager.autoSaveVault(getApplication(), backup)
                _lastVaultBackupTime.value = System.currentTimeMillis()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun restoreFromDetectedVault(onComplete: (Boolean) -> Unit = {}) {
        val vault = _detectedPersistentVault.value ?: return
        restoreBackup(vault, replaceExisting = true) { success ->
            if (success) {
                _detectedPersistentVault.value = null
            }
            onComplete(success)
        }
    }

    fun dismissDetectedVaultBanner() {
        _detectedPersistentVault.value = null
    }

    // Worker Master actions - 100% persistent, never lost unless explicitly deleted
    fun addWorker(worker: Worker, autoSelectInPos: Boolean = true, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insertWorker(worker)
            val updated = repository.getAllWorkersList()
            com.example.util.PermanentWorkerStore.saveWorkers(getApplication(), updated)
            triggerAutoVaultSave()
            if (autoSelectInPos || _posForm.value.selectedWorker == null) {
                selectWorker(worker)
            }
            onComplete()
        }
    }

    fun updateWorker(worker: Worker, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateWorker(worker)
            val updated = repository.getAllWorkersList()
            com.example.util.PermanentWorkerStore.saveWorkers(getApplication(), updated)
            triggerAutoVaultSave()
            onComplete()
        }
    }

    fun deactivateWorker(worker: Worker, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateWorker(worker.copy(status = "Inactive"))
            val updated = repository.getAllWorkersList()
            com.example.util.PermanentWorkerStore.saveWorkers(getApplication(), updated)
            triggerAutoVaultSave()
            onComplete()
        }
    }

    fun reactivateWorker(worker: Worker, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateWorker(worker.copy(status = "Active"))
            val updated = repository.getAllWorkersList()
            com.example.util.PermanentWorkerStore.saveWorkers(getApplication(), updated)
            triggerAutoVaultSave()
            onComplete()
        }
    }

    fun deleteWorker(worker: Worker, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteWorker(worker)
            val updated = repository.getAllWorkersList()
            com.example.util.PermanentWorkerStore.saveWorkers(getApplication(), updated)
            triggerAutoVaultSave()
            onComplete()
        }
    }

    // Site Master actions
    fun addSite(site: Site, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insertSite(site)
            triggerAutoVaultSave()
            if (_posForm.value.selectedSite == null) {
                _posForm.value = _posForm.value.copy(selectedSite = site)
            }
            onComplete()
        }
    }

    fun updateSite(site: Site, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateSite(site)
            triggerAutoVaultSave()
            if (_posForm.value.selectedSite?.siteCode == site.siteCode) {
                _posForm.value = _posForm.value.copy(selectedSite = site)
            }
            onComplete()
        }
    }

    fun deleteSite(site: Site, deleteAttendanceRecords: Boolean = true, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteSiteWithRecords(site, deleteAttendanceRecords)
            triggerAutoVaultSave()
            if (_posForm.value.selectedSite?.siteCode == site.siteCode) {
                val nextSite = allSites.value.firstOrNull { it.siteCode != site.siteCode }
                _posForm.value = _posForm.value.copy(selectedSite = nextSite)
            }
            onComplete()
        }
    }

    suspend fun getAttendanceCountForSite(siteCode: String): Int {
        return repository.getAttendanceCountForSite(siteCode)
    }

    // 1-Click Clear / Reset Sample Data
    fun clearAttendanceOnly(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearAllAttendance()
            com.example.util.PermanentAttendanceStore.clearAttendance(app)
            triggerAutoVaultSave()
            resetForm()
            val isMy = LanguageManager.currentLanguage.value == AppLanguage.MYANMAR
            _posForm.value = _posForm.value.copy(
                successMessage = if (isMy) "တက်ရောက်မှုမှတ်တမ်းများအားလုံး ဖျက်ပြီးပါပြီ။ စာရင်းအသစ် ပြန်လည်သွင်းယူနိုင်ပါသည်။" else "All attendance records cleared. You can now start fresh daily logging!"
            )
            onComplete()
        }
    }

    fun clearAllDataCompletely(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearAllData()
            com.example.util.PermanentWorkerStore.clearWorkers(app)
            com.example.util.PermanentAttendanceStore.clearAttendance(app)
            triggerAutoVaultSave()
            val isMy = LanguageManager.currentLanguage.value == AppLanguage.MYANMAR
            _posForm.value = PosFormState(
                selectedDate = todayDateString,
                selectedSite = null,
                selectedWorker = null,
                successMessage = if (isMy) "ဒေတာအားလုံးကို အပြီးတိုင် ဖျက်ပြီးပါပြီ။ အချက်အလက်အသစ်များ စတင်ထည့်သွင်းနိုင်ပါပြီ။" else "Database cleared completely. Clean slate ready for your custom data."
            )
            onComplete()
        }
    }

    fun resetDataWithSamples(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.resetWithSampleData()
            val firstSite = com.example.data.local.SampleDataProvider.getInitialSites().firstOrNull()
            _posForm.value = _posForm.value.copy(
                selectedSite = firstSite,
                successMessage = "Demo sample construction data restored successfully."
            )
            onComplete()
        }
    }

    // Batch Attendance for quick jobsite logging
    fun saveBatchAttendance(records: List<AttendanceRecord>, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insertAttendanceList(records)
            val currentAllAttendance = repository.getAllAttendanceList()
            com.example.util.PermanentAttendanceStore.saveAttendance(app, currentAllAttendance)
            triggerAutoVaultSave()
            val isMy = LanguageManager.currentLanguage.value == AppLanguage.MYANMAR
            _posForm.value = _posForm.value.copy(
                successMessage = if (isMy) {
                    "လုပ်သား ${records.size} ဦး၏ နေ့စဉ်တက်ရောက်မှုစာရင်းကို အောင်မြင်စွာ မှတ်တမ်းတင်ပြီးပါပြီ!"
                } else {
                    "Batch attendance saved for ${records.size} workers successfully!"
                }
            )
            onComplete()
        }
    }

    // Backup & Restore
    fun createBackupData(onReady: (com.example.data.model.LabourBackupData) -> Unit) {
        viewModelScope.launch {
            val backup = repository.getBackupData(todayDateString)
            onReady(backup)
        }
    }

    fun restoreBackup(
        backup: com.example.data.model.LabourBackupData,
        replaceExisting: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.restoreData(backup, replaceExisting)
                // Persist restored workers and attendance to permanent storage
                val restoredWorkers = repository.getAllWorkersList()
                com.example.util.PermanentWorkerStore.saveWorkers(app, restoredWorkers)
                val restoredAttendance = repository.getAllAttendanceList()
                com.example.util.PermanentAttendanceStore.saveAttendance(app, restoredAttendance)
                triggerAutoVaultSave()

                val defaultSite = backup.sites.firstOrNull() ?: allSites.value.firstOrNull()
                val isMy = LanguageManager.currentLanguage.value == AppLanguage.MYANMAR
                _posForm.value = _posForm.value.copy(
                    selectedSite = defaultSite,
                    successMessage = if (isMy) {
                        "ဆိုဒ် (${backup.sites.size}) ခု၊ လုပ်သား (${backup.workers.size}) ဦး နှင့် နေ့စဉ်တက်ရောက်မှုမှတ်တမ်း (${backup.attendance.size}) ခု အားလုံးကို အောင်မြင်စွာ ပြန်လည်ရယူပြီးပါပြီ!"
                    } else {
                        "Restored ${backup.sites.size} sites, ${backup.workers.size} workers, and ${backup.attendance.size} daily attendance records successfully!"
                    }
                )
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                _posForm.value = _posForm.value.copy(
                    errorMessage = "Failed to restore backup: ${e.message}"
                )
                onComplete(false)
            }
        }
    }
}
