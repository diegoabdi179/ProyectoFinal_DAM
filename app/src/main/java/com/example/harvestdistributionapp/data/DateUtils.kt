package com.example.harvestdistributionapp.data

import java.text.SimpleDateFormat
import java.text.ParsePosition
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val utc: TimeZone
    get() = TimeZone.getTimeZone("UTC")

private fun isoDateFormatter(zone: TimeZone = TimeZone.getDefault()) = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = zone
    isLenient = false
}

fun futureIsoDate(daysFromToday: Int = 1): String {
    val calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, daysFromToday)
    }
    return isoDateFormatter().format(calendar.time)
}

fun epochMillisToIsoDate(epochMillis: Long): String = isoDateFormatter(utc).format(Date(epochMillis))

fun isIsoDateTodayOrFuture(value: String): Boolean {
    val parser = isoDateFormatter()
    val position = ParsePosition(0)
    val selectedDate = parser.parse(value, position)
    if (selectedDate == null || position.index != value.length) return false
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time
    return !selectedDate.before(today)
}
