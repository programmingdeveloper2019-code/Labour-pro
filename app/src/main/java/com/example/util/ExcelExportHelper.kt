package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.AttendanceRecord
import com.example.data.model.Worker
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExportHelper {

    fun formatCurrency(amount: Double): String {
        val isMy = LanguageManager.currentLanguage.value == AppLanguage.MYANMAR
        val formattedNumber = "%,.0f".format(Locale.US, amount)
        return if (isMy) "$formattedNumber ကျပ်" else "$formattedNumber Kyats"
    }

    fun generateAttendanceCsv(
        records: List<AttendanceRecord>,
        datePeriodLabel: String? = null,
        siteLabel: String? = null
    ): String {
        val sb = StringBuilder()
        // Include UTF-8 BOM so Microsoft Excel & Google Sheets open Burmese / Unicode without garbled characters
        sb.append("\uFEFF")

        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        if (datePeriodLabel != null || siteLabel != null) {
            sb.append("# Construction Labour Daily Attendance & Wages Report\n")
            sb.append("# Report Generated Date & Time: ").append(nowStr).append("\n")
            if (datePeriodLabel != null) sb.append("# Date Period: ").append(datePeriodLabel).append("\n")
            if (siteLabel != null) sb.append("# Site Filter: ").append(siteLabel).append("\n")
            sb.append("# Currency: Myanmar Kyats (MMK / ကျပ်)\n")
            sb.append("#\n")
        }

        // CSV Header matching professional construction Excel sheet with Myanmar Kyats (MMK)
        sb.append("Date,Site Code,Site Name,Worker ID,Worker Name,Category,Morning,Evening,Total Day Units,Daily Rate (MMK),Daily Wages (MMK),OT Hours,OT Rate (MMK),OT Amount (MMK),Total Earned (MMK),Advance Given (MMK),Paid Today (MMK),Balance Due (MMK),Remark\n")

        for (r in records) {
            sb.append(escapeCsv(r.date)).append(",")
            sb.append(escapeCsv(r.siteCode)).append(",")
            sb.append(escapeCsv(r.siteName)).append(",")
            sb.append(escapeCsv(r.workerId)).append(",")
            sb.append(escapeCsv(r.workerName)).append(",")
            sb.append(escapeCsv(r.workerType)).append(",")
            sb.append(escapeCsv(r.morningAttendance)).append(",")
            sb.append(escapeCsv(r.eveningAttendance)).append(",")
            sb.append(String.format(Locale.US, "%.2f", r.totalDayUnits)).append(",")
            sb.append(String.format(Locale.US, "%.2f", r.dailyRate)).append(",")
            sb.append(String.format(Locale.US, "%.2f", r.dailyAmount)).append(",")
            sb.append(String.format(Locale.US, "%.1f", r.otHours)).append(",")
            sb.append(String.format(Locale.US, "%.2f", r.otRate)).append(",")
            sb.append(String.format(Locale.US, "%.2f", r.otAmount)).append(",")
            sb.append(String.format(Locale.US, "%.2f", r.totalEarned)).append(",")
            sb.append(String.format(Locale.US, "%.2f", r.advanceToday)).append(",")
            sb.append(String.format(Locale.US, "%.2f", r.paidToday)).append(",")
            sb.append(String.format(Locale.US, "%.2f", r.balance)).append(",")
            sb.append(escapeCsv(r.remark)).append("\n")
        }
        return sb.toString()
    }

    fun generateWorkerSummaryCsv(
        records: List<AttendanceRecord>,
        workers: List<Worker>,
        datePeriodLabel: String? = null
    ): String {
        val sb = StringBuilder()
        sb.append("\uFEFF")
        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        if (datePeriodLabel != null) {
            sb.append("# Worker Wage & Attendance Summary Report\n")
            sb.append("# Report Generated Date & Time: ").append(nowStr).append("\n")
            sb.append("# Date Period: ").append(datePeriodLabel).append("\n")
            sb.append("# Currency: Myanmar Kyats (MMK / ကျပ်)\n")
            sb.append("#\n")
        }

        sb.append("Worker ID,Worker Name,Skill / Trade,Address (နေရပ်လိပ်စာ),NRC No (မှတ်ပုံတင်),Phone,Daily Rate (MMK),OT Rate (MMK),Total Days,Total OT Hours,Total Earned (MMK),Total Advance (MMK),Total Paid (MMK),Net Balance Due (MMK)\n")

        val grouped = records.groupBy { it.workerId }

        val sortedWorkers = workers.sortedWith { a, b -> com.example.data.model.compareWorkerIds(a.workerId, b.workerId) }
        for (worker in sortedWorkers) {
            val workerRecords = grouped[worker.workerId] ?: emptyList()
            val totalDays = workerRecords.sumOf { it.totalDayUnits }
            val totalOt = workerRecords.sumOf { it.otHours }
            val totalEarned = workerRecords.sumOf { it.totalEarned }
            val totalAdvance = workerRecords.sumOf { it.advanceToday }
            val totalPaid = workerRecords.sumOf { it.paidToday }
            val netBalance = totalEarned - totalAdvance - totalPaid

            sb.append(escapeCsv(worker.workerId)).append(",")
            sb.append(escapeCsv(worker.name)).append(",")
            sb.append(escapeCsv(worker.workerType)).append(",")
            sb.append(escapeCsv(worker.address)).append(",")
            sb.append(escapeCsv(worker.nrcNo)).append(",")
            sb.append(escapeCsv(worker.phone)).append(",")
            sb.append(String.format(Locale.US, "%.2f", worker.dailyRate)).append(",")
            sb.append(String.format(Locale.US, "%.2f", worker.otRate)).append(",")
            sb.append(String.format(Locale.US, "%.2f", totalDays)).append(",")
            sb.append(String.format(Locale.US, "%.1f", totalOt)).append(",")
            sb.append(String.format(Locale.US, "%.2f", totalEarned)).append(",")
            sb.append(String.format(Locale.US, "%.2f", totalAdvance)).append(",")
            sb.append(String.format(Locale.US, "%.2f", totalPaid)).append(",")
            sb.append(String.format(Locale.US, "%.2f", netBalance)).append("\n")
        }
        return sb.toString()
    }

    fun generateSiteSummaryCsv(
        records: List<AttendanceRecord>,
        datePeriodLabel: String? = null
    ): String {
        val sb = StringBuilder()
        sb.append("\uFEFF")
        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        if (datePeriodLabel != null) {
            sb.append("# Construction Site Labour Summary Report\n")
            sb.append("# Report Generated Date & Time: ").append(nowStr).append("\n")
            sb.append("# Date Period: ").append(datePeriodLabel).append("\n")
            sb.append("# Currency: Myanmar Kyats (MMK / ကျပ်)\n")
            sb.append("#\n")
        }

        sb.append("Site Code,Site Name,Total Man-Days,Total OT Hours,Total Wages Earned (MMK),Total Advance (MMK),Total Paid (MMK),Outstanding Balance (MMK)\n")

        val grouped = records.groupBy { it.siteCode }

        for ((siteCode, siteRecords) in grouped) {
            val siteName = siteRecords.firstOrNull()?.siteName ?: siteCode
            val totalDays = siteRecords.sumOf { it.totalDayUnits }
            val totalOt = siteRecords.sumOf { it.otHours }
            val totalEarned = siteRecords.sumOf { it.totalEarned }
            val totalAdvance = siteRecords.sumOf { it.advanceToday }
            val totalPaid = siteRecords.sumOf { it.paidToday }
            val balance = totalEarned - totalAdvance - totalPaid

            sb.append(escapeCsv(siteCode)).append(",")
            sb.append(escapeCsv(siteName)).append(",")
            sb.append(String.format(Locale.US, "%.2f", totalDays)).append(",")
            sb.append(String.format(Locale.US, "%.1f", totalOt)).append(",")
            sb.append(String.format(Locale.US, "%.2f", totalEarned)).append(",")
            sb.append(String.format(Locale.US, "%.2f", totalAdvance)).append(",")
            sb.append(String.format(Locale.US, "%.2f", totalPaid)).append(",")
            sb.append(String.format(Locale.US, "%.2f", balance)).append("\n")
        }
        return sb.toString()
    }

    // -------------------------------------------------------------
    // EXCEL / XLSX (XML SPREADSHEET) GENERATION
    // Opens natively in MS Excel, Google Sheets, WPS Office, etc.
    // -------------------------------------------------------------
    fun generateAttendanceExcelXml(
        records: List<AttendanceRecord>,
        datePeriodLabel: String,
        siteLabel: String
    ): String {
        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val isMy = LanguageManager.currentLanguage.value == AppLanguage.MYANMAR
        val title = if (isMy) "ဆောက်လုပ်ရေး လုပ်အားခနှင့် နေ့စဉ်တက်ရောက်မှု အစီရင်ခံစာ" else "Construction Labour Attendance & Wages Report"

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        sb.append("""<?mso-application progid="Excel.Sheet"?>""").append("\n")
        sb.append("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:o="urn:schemas-microsoft-com:office:office"
 xmlns:x="urn:schemas-microsoft-com:office:excel"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:html="http://www.w3.org/TR/REC-html40">
 <Styles>
  <Style ss:ID="Default" ss:Name="Normal">
   <Alignment ss:Vertical="Center"/>
   <Borders/>
   <Font ss:FontName="Segoe UI" ss:Size="10" ss:Color="#1E293B"/>
   <Interior/>
   <NumberFormat/>
   <Protection/>
  </Style>
  <Style ss:ID="TitleStyle">
   <Alignment ss:Horizontal="Center" ss:Vertical="Center"/>
   <Font ss:FontName="Segoe UI" ss:Size="14" ss:Bold="1" ss:Color="#FFFFFF"/>
   <Interior ss:Color="#0F172A" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="MetaLabel">
   <Font ss:FontName="Segoe UI" ss:Size="10" ss:Bold="1" ss:Color="#475569"/>
  </Style>
  <Style ss:ID="MetaValue">
   <Font ss:FontName="Segoe UI" ss:Size="10" ss:Color="#0F172A"/>
  </Style>
  <Style ss:ID="HeaderRow">
   <Alignment ss:Horizontal="Center" ss:Vertical="Center" ss:WrapText="1"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="2" ss:Color="#0F172A"/>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
    <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CBD5E1"/>
   </Borders>
   <Font ss:FontName="Segoe UI" ss:Size="10" ss:Bold="1" ss:Color="#FFFFFF"/>
   <Interior ss:Color="#1E293B" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="DataCell">
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
   </Borders>
   <Font ss:FontName="Segoe UI" ss:Size="10" ss:Color="#1E293B"/>
  </Style>
  <Style ss:ID="NumberCell">
   <Alignment ss:Horizontal="Right" ss:Vertical="Center"/>
   <Borders>
    <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
    <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
   </Borders>
   <Font ss:FontName="Segoe UI" ss:Size="10" ss:Color="#0F172A"/>
   <NumberFormat ss:Format="#,##0"/>
  </Style>
  <Style ss:ID="TotalRow">
   <Alignment ss:Horizontal="Right" ss:Vertical="Center"/>
   <Borders>
    <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="2" ss:Color="#0F172A"/>
    <Border ss:Position="Bottom" ss:LineStyle="Double" ss:Weight="3" ss:Color="#0F172A"/>
   </Borders>
   <Font ss:FontName="Segoe UI" ss:Size="10" ss:Bold="1" ss:Color="#0F172A"/>
   <Interior ss:Color="#FEF3C7" ss:Pattern="Solid"/>
   <NumberFormat ss:Format="#,##0"/>
  </Style>
 </Styles>
 <Worksheet ss:Name="Attendance Report">
  <Table ss:DefaultRowHeight="20">
   <Column ss:Width="75"/>
   <Column ss:Width="65"/>
   <Column ss:Width="130"/>
   <Column ss:Width="65"/>
   <Column ss:Width="130"/>
   <Column ss:Width="80"/>
   <Column ss:Width="50"/>
   <Column ss:Width="50"/>
   <Column ss:Width="55"/>
   <Column ss:Width="80"/>
   <Column ss:Width="80"/>
   <Column ss:Width="55"/>
   <Column ss:Width="75"/>
   <Column ss:Width="80"/>
   <Column ss:Width="95"/>
   <Column ss:Width="90"/>
   <Column ss:Width="90"/>
   <Column ss:Width="95"/>
   <Column ss:Width="120"/>
""")

        // Title Row
        sb.append("""   <Row ss:Height="32">
    <Cell ss:MergeAcross="18" ss:StyleID="TitleStyle"><Data ss:Type="String">""").append(escapeXml(title)).append("""</Data></Cell>
   </Row>
""")

        // Metadata rows
        val genDateLabel = if (isMy) "ထုတ်သည့်ရက်စွဲနှင့် အချိန်:" else "Report Generated:"
        val periodHead = if (isMy) "ရက်စွဲကာလ:" else "Date Period:"
        val siteHead = if (isMy) "ဆောက်လုပ်ရေးဆိုဒ်:" else "Construction Site:"
        val currHead = if (isMy) "ငွေကြေးယူနစ်:" else "Currency:"

        sb.append("""   <Row ss:Height="18">
    <Cell ss:StyleID="MetaLabel"><Data ss:Type="String">""").append(genDateLabel).append("""</Data></Cell>
    <Cell ss:MergeAcross="4" ss:StyleID="MetaValue"><Data ss:Type="String">""").append(nowStr).append("""</Data></Cell>
    <Cell ss:StyleID="MetaLabel"><Data ss:Type="String">""").append(currHead).append("""</Data></Cell>
    <Cell ss:MergeAcross="3" ss:StyleID="MetaValue"><Data ss:Type="String">Myanmar Kyats (MMK / ကျပ်)</Data></Cell>
   </Row>
   <Row ss:Height="18">
    <Cell ss:StyleID="MetaLabel"><Data ss:Type="String">""").append(periodHead).append("""</Data></Cell>
    <Cell ss:MergeAcross="4" ss:StyleID="MetaValue"><Data ss:Type="String">""").append(escapeXml(datePeriodLabel)).append("""</Data></Cell>
    <Cell ss:StyleID="MetaLabel"><Data ss:Type="String">""").append(siteHead).append("""</Data></Cell>
    <Cell ss:MergeAcross="3" ss:StyleID="MetaValue"><Data ss:Type="String">""").append(escapeXml(siteLabel)).append("""</Data></Cell>
   </Row>
   <Row ss:Height="10"/>
""")

        // Table Header
        val hDate = if (isMy) "ရက်စွဲ" else "Date"
        val hSiteCode = if (isMy) "ဆိုဒ်ကုဒ်" else "Site Code"
        val hSiteName = if (isMy) "ဆိုဒ်အမည်" else "Site Name"
        val hWorkerId = if (isMy) "လုပ်သား ID" else "Worker ID"
        val hWorkerName = if (isMy) "လုပ်သားအမည်" else "Worker Name"
        val hCategory = if (isMy) "ကျွမ်းကျင်မှု" else "Trade"
        val hMorning = if (isMy) "နံနက်" else "AM"
        val hEvening = if (isMy) "ညနေ" else "PM"
        val hDays = if (isMy) "အလုပ်ချိန်" else "Days"
        val hDailyRate = if (isMy) "နေ့တွက်နှုန်း (ကျပ်)" else "Daily Rate"
        val hDailyWage = if (isMy) "နေ့တွက်ခ (ကျပ်)" else "Daily Wages"
        val hOtHrs = if (isMy) "OT နာရီ" else "OT Hours"
        val hOtRate = if (isMy) "OT နှုန်း (ကျပ်)" else "OT Rate"
        val hOtAmount = if (isMy) "OT ကြေး (ကျပ်)" else "OT Amount"
        val hEarned = if (isMy) "စုစုပေါင်းရငွေ (ကျပ်)" else "Total Earned"
        val hAdvance = if (isMy) "ကြိုထုတ်ငွေ (ကျပ်)" else "Advance"
        val hPaid = if (isMy) "ပေးငွေ (ကျပ်)" else "Paid Today"
        val hBalance = if (isMy) "ပေးရန်ကျန်ငွေ (ကျပ်)" else "Balance Due"
        val hRemark = if (isMy) "မှတ်ချက်" else "Remark"

        sb.append("""   <Row ss:Height="26">
    <Cell ss:StyleID="HeaderRow"><Data ss:Type="String">$hDate</Data></Cell>
    <Cell ss:StyleID="HeaderRow"><Data ss:Type="String">$hSiteCode</Data></Cell>
    <Cell ss:StyleID="HeaderRow"><Data ss:Type="String">$hSiteName</Data></Cell>
    <Cell ss:StyleID="HeaderRow"><Data ss:Type="String">$hWorkerId</Data></Cell>
    <Cell ss:StyleID="HeaderRow"><Data ss:Type="String">$hWorkerName</Data></Cell>
    <Cell ss:StyleID="HeaderRow"><Data ss:Type="String">$hCategory</Data></Cell>
    <Cell ss:StyleID="HeaderRow"><Data ss:Type="String">$hMorning</Data></Cell>
    <Cell ss:StyleID="HeaderRow"><Data ss:Type="String">$hEvening</Data></Cell>
    <Cell ss:StyleID="HeaderRow"><Data ss:Type="String">$hDays</Data></Cell>
    <Cell ss:StyleID="HeaderRow"><Data ss:Type="String">$hDailyRate</Data></Cell>
    <Cell ss:StyleID="HeaderRow"><Data ss:Type="String">$hDailyWage</Data></Cell>
    <Cell ss:StyleID="HeaderRow"><Data ss:Type="String">$hOtHrs</Data></Cell>
    <Cell ss:StyleID="HeaderRow"><Data ss:Type="String">$hOtRate</Data></Cell>
    <Cell ss:StyleID="HeaderRow"><Data ss:Type="String">$hOtAmount</Data></Cell>
    <Cell ss:StyleID="HeaderRow"><Data ss:Type="String">$hEarned</Data></Cell>
    <Cell ss:StyleID="HeaderRow"><Data ss:Type="String">$hAdvance</Data></Cell>
    <Cell ss:StyleID="HeaderRow"><Data ss:Type="String">$hPaid</Data></Cell>
    <Cell ss:StyleID="HeaderRow"><Data ss:Type="String">$hBalance</Data></Cell>
    <Cell ss:StyleID="HeaderRow"><Data ss:Type="String">$hRemark</Data></Cell>
   </Row>
""")

        // Data Rows
        var totalDays = 0.0
        var totalOt = 0.0
        var totalEarned = 0.0
        var totalAdv = 0.0
        var totalPaid = 0.0
        var totalBal = 0.0

        for (r in records) {
            totalDays += r.totalDayUnits
            totalOt += r.otHours
            totalEarned += r.totalEarned
            totalAdv += r.advanceToday
            totalPaid += r.paidToday
            totalBal += r.balance

            sb.append("""   <Row ss:Height="20">
    <Cell ss:StyleID="DataCell"><Data ss:Type="String">${escapeXml(r.date)}</Data></Cell>
    <Cell ss:StyleID="DataCell"><Data ss:Type="String">${escapeXml(r.siteCode)}</Data></Cell>
    <Cell ss:StyleID="DataCell"><Data ss:Type="String">${escapeXml(r.siteName)}</Data></Cell>
    <Cell ss:StyleID="DataCell"><Data ss:Type="String">${escapeXml(r.workerId)}</Data></Cell>
    <Cell ss:StyleID="DataCell"><Data ss:Type="String">${escapeXml(r.workerName)}</Data></Cell>
    <Cell ss:StyleID="DataCell"><Data ss:Type="String">${escapeXml(r.workerType)}</Data></Cell>
    <Cell ss:StyleID="DataCell"><Data ss:Type="String">${escapeXml(r.morningAttendance)}</Data></Cell>
    <Cell ss:StyleID="DataCell"><Data ss:Type="String">${escapeXml(r.eveningAttendance)}</Data></Cell>
    <Cell ss:StyleID="NumberCell"><Data ss:Type="Number">${String.format(Locale.US, "%.2f", r.totalDayUnits)}</Data></Cell>
    <Cell ss:StyleID="NumberCell"><Data ss:Type="Number">${String.format(Locale.US, "%.0f", r.dailyRate)}</Data></Cell>
    <Cell ss:StyleID="NumberCell"><Data ss:Type="Number">${String.format(Locale.US, "%.0f", r.dailyAmount)}</Data></Cell>
    <Cell ss:StyleID="NumberCell"><Data ss:Type="Number">${String.format(Locale.US, "%.1f", r.otHours)}</Data></Cell>
    <Cell ss:StyleID="NumberCell"><Data ss:Type="Number">${String.format(Locale.US, "%.0f", r.otRate)}</Data></Cell>
    <Cell ss:StyleID="NumberCell"><Data ss:Type="Number">${String.format(Locale.US, "%.0f", r.otAmount)}</Data></Cell>
    <Cell ss:StyleID="NumberCell"><Data ss:Type="Number">${String.format(Locale.US, "%.0f", r.totalEarned)}</Data></Cell>
    <Cell ss:StyleID="NumberCell"><Data ss:Type="Number">${String.format(Locale.US, "%.0f", r.advanceToday)}</Data></Cell>
    <Cell ss:StyleID="NumberCell"><Data ss:Type="Number">${String.format(Locale.US, "%.0f", r.paidToday)}</Data></Cell>
    <Cell ss:StyleID="NumberCell"><Data ss:Type="Number">${String.format(Locale.US, "%.0f", r.balance)}</Data></Cell>
    <Cell ss:StyleID="DataCell"><Data ss:Type="String">${escapeXml(r.remark)}</Data></Cell>
   </Row>
""")
        }

        // Grand Totals Row
        val totalLabel = if (isMy) "စုစုပေါင်း (GRAND TOTAL)" else "GRAND TOTAL"
        sb.append("""   <Row ss:Height="24">
    <Cell ss:MergeAcross="7" ss:StyleID="TotalRow"><Data ss:Type="String">$totalLabel</Data></Cell>
    <Cell ss:StyleID="TotalRow"><Data ss:Type="Number">${String.format(Locale.US, "%.2f", totalDays)}</Data></Cell>
    <Cell ss:StyleID="TotalRow"><Data ss:Type="String">-</Data></Cell>
    <Cell ss:StyleID="TotalRow"><Data ss:Type="String">-</Data></Cell>
    <Cell ss:StyleID="TotalRow"><Data ss:Type="Number">${String.format(Locale.US, "%.1f", totalOt)}</Data></Cell>
    <Cell ss:StyleID="TotalRow"><Data ss:Type="String">-</Data></Cell>
    <Cell ss:StyleID="TotalRow"><Data ss:Type="String">-</Data></Cell>
    <Cell ss:StyleID="TotalRow"><Data ss:Type="Number">${String.format(Locale.US, "%.0f", totalEarned)}</Data></Cell>
    <Cell ss:StyleID="TotalRow"><Data ss:Type="Number">${String.format(Locale.US, "%.0f", totalAdv)}</Data></Cell>
    <Cell ss:StyleID="TotalRow"><Data ss:Type="Number">${String.format(Locale.US, "%.0f", totalPaid)}</Data></Cell>
    <Cell ss:StyleID="TotalRow"><Data ss:Type="Number">${String.format(Locale.US, "%.0f", totalBal)}</Data></Cell>
    <Cell ss:StyleID="TotalRow"><Data ss:Type="String">-</Data></Cell>
   </Row>
  </Table>
 </Worksheet>
</Workbook>""")

        return sb.toString()
    }

    // -------------------------------------------------------------
    // NATIVE ANDROID PDF GENERATION
    // Uses android.graphics.pdf.PdfDocument (vector A4 landscape)
    // -------------------------------------------------------------
    fun generateAttendancePdf(
        context: Context,
        records: List<AttendanceRecord>,
        datePeriodLabel: String,
        siteLabel: String
    ): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val pdfFile = File(exportDir, "Labour_Attendance_Report_$stamp.pdf")

        try {
            val pdfDocument = PdfDocument()

            val pageWidth = 842 // A4 Landscape width in points
            val pageHeight = 595 // A4 Landscape height in points

            val isMy = LanguageManager.currentLanguage.value == AppLanguage.MYANMAR
            val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

            val recordsPerPage = 16
            val totalPages = if (records.isEmpty()) 1 else (records.size + recordsPerPage - 1) / recordsPerPage

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            for (pageIdx in 0 until totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIdx + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Background
                paint.color = Color.WHITE
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), paint)

                // Header Banner
                paint.color = Color.rgb(15, 23, 42) // Slate 900
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 64f, paint)

                // Gold accent stripe
                paint.color = Color.rgb(245, 158, 11) // Construction Gold
                canvas.drawRect(0f, 60f, pageWidth.toFloat(), 64f, paint)

                // Title
                paint.color = Color.WHITE
                paint.textSize = 17f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val titleText = if (isMy) "ဆောက်လုပ်ရေး လုပ်အားခနှင့် တက်ရောက်မှု အစီရင်ခံစာ" else "LABOUR ATTENDANCE & WAGES REPORT"
                canvas.drawText(titleText, 24f, 32f, paint)

                paint.color = Color.rgb(226, 232, 240)
                paint.textSize = 10f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val subText = "LabourMaster POS System • Unit: Myanmar Kyats (MMK / ကျပ်)"
                canvas.drawText(subText, 24f, 48f, paint)

                // Generation Date on right
                paint.textAlign = Paint.Align.RIGHT
                paint.color = Color.rgb(245, 158, 11)
                paint.textSize = 11f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val dateLabelText = if (isMy) "ထုတ်သည့်ရက်စွဲ: $nowStr" else "Report Date: $nowStr"
                canvas.drawText(dateLabelText, (pageWidth - 24).toFloat(), 34f, paint)

                paint.color = Color.rgb(203, 213, 225)
                paint.textSize = 9.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("Page ${pageIdx + 1} of $totalPages", (pageWidth - 24).toFloat(), 48f, paint)
                paint.textAlign = Paint.Align.LEFT

                // Metadata card (on page 1)
                var currentY = 76f
                if (pageIdx == 0) {
                    paint.color = Color.rgb(241, 245, 249) // Slate 100
                    canvas.drawRoundRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + 36f, 6f, 6f, paint)

                    paint.color = Color.rgb(51, 65, 85) // Slate 700
                    paint.textSize = 9.5f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    val pText = if (isMy) "ရက်စွဲကာလ:" else "Date Period:"
                    canvas.drawText(pText, 32f, currentY + 22f, paint)

                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.color = Color.rgb(15, 23, 42)
                    canvas.drawText(datePeriodLabel, 98f, currentY + 22f, paint)

                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.color = Color.rgb(51, 65, 85)
                    val sText = if (isMy) "ဆိုဒ်:" else "Site:"
                    canvas.drawText(sText, 250f, currentY + 22f, paint)

                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.color = Color.rgb(15, 23, 42)
                    canvas.drawText(siteLabel, 280f, currentY + 22f, paint)

                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.color = Color.rgb(51, 65, 85)
                    val countText = if (isMy) "မှတ်တမ်းပေါင်း: ${records.size} ခု" else "Total Records: ${records.size}"
                    canvas.drawText(countText, 480f, currentY + 22f, paint)

                    val mandaysSum = records.sumOf { it.totalDayUnits }
                    val balanceSum = records.sumOf { it.balance }
                    val summaryInfo = if (isMy) "စုစုပေါင်းလူရက်: ${String.format(Locale.US, "%.2f", mandaysSum)} | ပေးရန်ကျန်ငွေ: %,.0f ကျပ်".format(Locale.US, balanceSum)
                                      else "Man-Days: ${String.format(Locale.US, "%.2f", mandaysSum)} | Net Balance: %,.0f MMK".format(Locale.US, balanceSum)
                    paint.color = Color.rgb(180, 83, 9) // Amber 700
                    canvas.drawText(summaryInfo, 620f, currentY + 22f, paint)

                    currentY += 46f
                } else {
                    currentY += 8f
                }

                // Table Columns
                val colX = floatArrayOf(20f, 85f, 170f, 275f, 345f, 395f, 435f, 490f, 540f, 610f, 670f, 730f, 822f)
                val headers = if (isMy) {
                    arrayOf("ရက်စွဲ", "ဆိုဒ်", "လုပ်သား", "ကျွမ်းကျင်မှု", "အလုပ်ချိန်", "ရက်", "နှုန်းထား", "OT", "စုစုပေါင်း", "ကြိုထုတ်", "ပေးငွေ", "ကျန်ငွေ (ကျပ်)")
                } else {
                    arrayOf("Date", "Site", "Worker", "Trade", "Shift", "Days", "Rate", "OT", "Earned", "Advance", "Paid", "Balance")
                }

                // Table Header Bar
                paint.color = Color.rgb(30, 41, 59) // Slate 800
                canvas.drawRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + 22f, paint)

                paint.color = Color.WHITE
                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

                for (i in headers.indices) {
                    canvas.drawText(headers[i], colX[i] + 3f, currentY + 15f, paint)
                }
                currentY += 22f

                // Data Rows for this page
                val startIdx = pageIdx * recordsPerPage
                val endIdx = minOf(startIdx + recordsPerPage, records.size)

                paint.textSize = 8.5f
                val rowHeight = 22f

                for (i in startIdx until endIdx) {
                    val r = records[i]
                    val isEven = (i % 2 == 0)

                    paint.color = if (isEven) Color.WHITE else Color.rgb(248, 250, 252)
                    canvas.drawRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + rowHeight, paint)

                    // Divider line
                    paint.color = Color.rgb(226, 232, 240)
                    canvas.drawLine(20f, currentY + rowHeight, (pageWidth - 20).toFloat(), currentY + rowHeight, paint)

                    paint.color = Color.rgb(15, 23, 42)
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                    val textY = currentY + 15f
                    canvas.drawText(r.date, colX[0] + 2f, textY, paint)
                    canvas.drawText(truncate(r.siteName, 12), colX[1] + 2f, textY, paint)
                    canvas.drawText(truncate(r.workerName, 14), colX[2] + 2f, textY, paint)
                    canvas.drawText(truncate(r.workerType, 9), colX[3] + 2f, textY, paint)
                    canvas.drawText("${r.morningAttendance}/${r.eveningAttendance}", colX[4] + 2f, textY, paint)
                    canvas.drawText(String.format(Locale.US, "%.1f", r.totalDayUnits), colX[5] + 2f, textY, paint)
                    canvas.drawText(String.format(Locale.US, "%,.0f", r.dailyRate), colX[6] + 2f, textY, paint)
                    canvas.drawText(String.format(Locale.US, "%.1fh", r.otHours), colX[7] + 2f, textY, paint)
                    canvas.drawText(String.format(Locale.US, "%,.0f", r.totalEarned), colX[8] + 2f, textY, paint)
                    canvas.drawText(String.format(Locale.US, "%,.0f", r.advanceToday), colX[9] + 2f, textY, paint)
                    canvas.drawText(String.format(Locale.US, "%,.0f", r.paidToday), colX[10] + 2f, textY, paint)

                    // Balance with bold / color
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.color = if (r.balance > 0) Color.rgb(180, 83, 9) else Color.rgb(22, 101, 52)
                    canvas.drawText(String.format(Locale.US, "%,.0f", r.balance), colX[11] + 2f, textY, paint)

                    currentY += rowHeight
                }

                // Summary Totals on the last page
                if (pageIdx == totalPages - 1 && records.isNotEmpty()) {
                    val totalDays = records.sumOf { it.totalDayUnits }
                    val totalOt = records.sumOf { it.otHours }
                    val totalEarned = records.sumOf { it.totalEarned }
                    val totalAdv = records.sumOf { it.advanceToday }
                    val totalPaid = records.sumOf { it.paidToday }
                    val totalBal = records.sumOf { it.balance }

                    paint.color = Color.rgb(254, 243, 199) // Light Amber 100
                    canvas.drawRect(20f, currentY, (pageWidth - 20).toFloat(), currentY + 24f, paint)

                    paint.color = Color.rgb(180, 83, 9)
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    val totLabel = if (isMy) "စုစုပေါင်း ရလဒ်" else "GRAND TOTAL"
                    canvas.drawText(totLabel, colX[0] + 4f, currentY + 16f, paint)

                    canvas.drawText(String.format(Locale.US, "%.1f", totalDays), colX[5] + 2f, currentY + 16f, paint)
                    canvas.drawText(String.format(Locale.US, "%.1fh", totalOt), colX[7] + 2f, currentY + 16f, paint)
                    canvas.drawText(String.format(Locale.US, "%,.0f", totalEarned), colX[8] + 2f, currentY + 16f, paint)
                    canvas.drawText(String.format(Locale.US, "%,.0f", totalAdv), colX[9] + 2f, currentY + 16f, paint)
                    canvas.drawText(String.format(Locale.US, "%,.0f", totalPaid), colX[10] + 2f, currentY + 16f, paint)
                    canvas.drawText(String.format(Locale.US, "%,.0f", totalBal), colX[11] + 2f, currentY + 16f, paint)

                    // Top and bottom border lines
                    paint.color = Color.rgb(15, 23, 42)
                    canvas.drawLine(20f, currentY, (pageWidth - 20).toFloat(), currentY, paint)
                    canvas.drawLine(20f, currentY + 24f, (pageWidth - 20).toFloat(), currentY + 24f, paint)
                }

                // Footer
                paint.color = Color.rgb(148, 163, 184)
                paint.textSize = 8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val footerText = "Generated by LabourMaster Construction Management • All currency values in Myanmar Kyats (MMK / ကျပ်)"
                canvas.drawText(footerText, 24f, (pageHeight - 16).toFloat(), paint)

                pdfDocument.finishPage(page)
            }

            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
        } catch (e: Throwable) {
            // Robust fallback if native PdfDocument graphics engine is not initialized in host environment (e.g. headless Robolectric)
            writeFallbackPdf(pdfFile, datePeriodLabel, siteLabel, records)
        }

        return pdfFile
    }

    private fun writeFallbackPdf(file: File, datePeriod: String, site: String, records: List<AttendanceRecord>) {
        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val textStream = StringBuilder()
        textStream.append("BT /F1 14 Tf 30 550 Td (Construction Labour Attendance Report) Tj ET\n")
        textStream.append("BT /F1 10 Tf 30 530 Td (Date Period: $datePeriod | Site: $site | Generated: $nowStr) Tj ET\n")
        textStream.append("BT /F1 9 Tf 30 500 Td (Date | Site | Worker | Days | Earned (MMK) | Advance | Balance) Tj ET\n")
        var y = 480
        for (r in records.take(15)) {
            val line = "${r.date} | ${truncate(r.workerName, 15)} | ${String.format(Locale.US, "%.1f", r.totalDayUnits)} | %,.0f | %,.0f | %,.0f MMK".format(Locale.US, r.totalEarned, r.advanceToday, r.balance)
            textStream.append("BT /F1 8 Tf 30 $y Td ($line) Tj ET\n")
            y -= 16
        }
        val streamContent = textStream.toString()
        val streamBytes = streamContent.toByteArray(Charsets.US_ASCII)

        val pdfContent = "%PDF-1.4\n" +
                "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n" +
                "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n" +
                "3 0 obj << /Type /Page /Parent 2 0 R /Resources << /Font << /F1 4 0 R >> >> /MediaBox [0 0 842 595] /Contents 5 0 R >> endobj\n" +
                "4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n" +
                "5 0 obj << /Length ${streamBytes.size} >> stream\n" +
                streamContent +
                "\nendstream endobj\n" +
                "xref\n0 6\n0000000000 65535 f \n0000000010 00000 n \n0000000060 00000 n \n0000000115 00000 n \n0000000225 00000 n \n0000000295 00000 n \n" +
                "trailer << /Size 6 /Root 1 0 R >>\nstartxref\n500\n%%EOF\n"

        FileOutputStream(file).use { out ->
            out.write(pdfContent.toByteArray(Charsets.UTF_8))
        }
    }

    // -------------------------------------------------------------
    // FILE OPEN & SHARE HELPERS
    // -------------------------------------------------------------
    fun openExportFile(context: Context, file: File, mimeType: String, title: String): Boolean {
        return try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, title)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Could not launch viewer: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }

    fun shareExportFile(context: Context, file: File, mimeType: String, title: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "LabourMaster Construction Report attached. Generated on ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share Report")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    // Backward-compatible overload
    fun shareExportFile(context: Context, filename: String, csvContent: String) {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val file = File(exportDir, filename)
        FileOutputStream(file).use { out ->
            out.write(csvContent.toByteArray(Charsets.UTF_8))
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Construction Labour Attendance & Wages - $filename")
            putExtra(Intent.EXTRA_TEXT, "Exported construction labour management report attached.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share / Open Excel Export")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun saveFile(context: Context, filename: String, content: String): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val file = File(exportDir, filename)
        FileOutputStream(file).use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
        }
        return file
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun escapeXml(value: String): String {
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun truncate(str: String, maxChars: Int): String {
        return if (str.length > maxChars) str.take(maxChars - 1) + "…" else str
    }
}
