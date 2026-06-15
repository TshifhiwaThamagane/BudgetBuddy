package com.budgetbuddy.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateUtils {
    private val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    private val sortableFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun today(): String = displayFormat.format(Calendar.getInstance().time)

    fun currentMonthPattern(): String = monthFormat.format(Calendar.getInstance().time)

    fun formatDisplay(calendar: Calendar): String = displayFormat.format(calendar.time)

    /** Converts display date (dd MMM yyyy) to sortable (yyyy-MM-dd) for range queries. */
    fun toSortable(displayDate: String): String {
        return try {
            val parsed = displayFormat.parse(displayDate) ?: return displayDate
            sortableFormat.format(parsed)
        } catch (e: Exception) {
            displayDate
        }
    }

    /** Converts sortable date back to display format. */
    fun toDisplay(sortableDate: String): String {
        return try {
            val parsed = sortableFormat.parse(sortableDate) ?: return sortableDate
            displayFormat.format(parsed)
        } catch (e: Exception) {
            sortableDate
        }
    }

    fun startOfMonth(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return toSortable(formatDisplay(cal))
    }

    fun endOfMonth(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        return toSortable(formatDisplay(cal))
    }

    fun startOfWeek(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        return toSortable(formatDisplay(cal))
    }

    fun endOfWeek(): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.add(Calendar.DAY_OF_WEEK, 6)
        return toSortable(formatDisplay(cal))
    }

    fun daysBetween(date1: String, date2: String): Int {
        return try {
            val d1 = displayFormat.parse(date1) ?: return 0
            val d2 = displayFormat.parse(date2) ?: return 0
            val diff = kotlin.math.abs(d2.time - d1.time)
            (diff / (1000 * 60 * 60 * 24)).toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun isConsecutiveDay(previousDate: String?, currentDate: String): Boolean {
        if (previousDate == null) return false
        return daysBetween(previousDate, currentDate) == 1
    }

    fun isSameDay(date1: String?, date2: String): Boolean {
        return date1 == date2
    }

    fun daysInCurrentMonth(): Int {
        return Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun dayOfMonth(): Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    fun isInRange(date: String, startSortable: String, endSortable: String): Boolean {
        val sortable = toSortable(date)
        return sortable >= startSortable && sortable <= endSortable
    }
}
