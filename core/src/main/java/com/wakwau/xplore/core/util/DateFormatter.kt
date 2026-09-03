package com.wakwau.xplore.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {
    private val fullDateTimeFormat = SimpleDateFormat("d MMM yyyy HH.mm.ss", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun format(timestamp: Long): String {
        if (timestamp <= 0L) return "-"
        return fullDateTimeFormat.format(Date(timestamp))
    }

    fun formatShort(timestamp: Long): String {
        if (timestamp <= 0L) return "-"
        return shortDateFormat.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        if (timestamp <= 0L) return "-"
        return timeFormat.format(Date(timestamp))
    }
}
