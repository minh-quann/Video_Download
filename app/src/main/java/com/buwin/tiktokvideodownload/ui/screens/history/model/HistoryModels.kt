package com.buwin.tiktokvideodownload.ui.screens.history.model

import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Represents a calendar date section containing its corresponding download items.
 */
data class HistoryDateGroup(
    val dateKey: String,
    val dateTitle: String,
    val items: List<DownloadRecord>
)

object HistoryGroupUtils {

    /**
     * Groups download records into chronological date groups.
     */
    fun groupByDate(records: List<DownloadRecord>): List<HistoryDateGroup> {
        val dayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val todayCal = Calendar.getInstance()
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val todayKey = dayFormat.format(todayCal.time)
        val yesterdayKey = dayFormat.format(yesterdayCal.time)

        return records
            .groupBy { record ->
                if (record.timestamp > 0) {
                    dayFormat.format(Date(record.timestamp))
                } else {
                    todayKey
                }
            }
            .map { (dateKey, items) ->
                val firstTimestamp = items.firstOrNull { it.timestamp > 0 }?.timestamp ?: System.currentTimeMillis()
                val fullDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(firstTimestamp))
                val title = when (dateKey) {
                    todayKey -> "Hôm nay • $fullDateStr"
                    yesterdayKey -> "Hôm qua • $fullDateStr"
                    else -> fullDateStr
                }
                HistoryDateGroup(
                    dateKey = dateKey,
                    dateTitle = title,
                    items = items
                )
            }
    }
}
