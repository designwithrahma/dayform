package com.example.util

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale

@SuppressLint("NewApi")
object DateTimeUtil {

    private const val MILLIS_PER_DAY = 86_400_000L

    private val FULL_MONTH_NAMES = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    private val SHORT_MONTH_NAMES = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    private val SHORT_WEEKDAY_UPPER = arrayOf(
        "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"
    )

    /**
     * Computes local epoch day (days since 1970-01-01 in local timezone)
     * so tasks scheduled for "today" match the user's real local calendar date.
     */
    fun currentLocalEpochDay(nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMillis
        return calendarToEpochDay(cal)
    }

    fun currentMinutesFromMidnight(nowMillis: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMillis
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    fun calendarToEpochDay(cal: Calendar): Long {
        return LocalDate.of(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        ).toEpochDay()
    }

    fun epochDayToCalendar(epochDay: Long): Calendar {
        val c = Calendar.getInstance()
        val utcMillis = epochDay * MILLIS_PER_DAY + (12 * 3600_000L)
        val offset = c.timeZone.getOffset(utcMillis)
        c.timeInMillis = utcMillis - offset
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c
    }

    /**
     * Converts a local calendar [epochDay] (days since 1970-01-01) into Material 3 DatePicker
     * UTC midnight milliseconds (`epochDay * 86_400_000L`).
     * Material 3 DatePicker represents calendar dates as UTC midnight regardless of local timezone.
     */
    fun epochDayToUtcPickerMillis(epochDay: Long): Long {
        return epochDay * MILLIS_PER_DAY
    }

    /**
     * Converts Material 3 DatePicker UTC milliseconds into a calendar [epochDay].
     */
    fun utcPickerMillisToEpochDay(utcMillis: Long): Long {
        return Math.floorDiv(utcMillis, MILLIS_PER_DAY)
    }

    fun getDayOfMonth(epochDay: Long): Int {
        return LocalDate.ofEpochDay(epochDay).dayOfMonth
    }

    fun epochDayAndMinutesToMillis(epochDay: Long, minutesFromMidnight: Int): Long {
        val cal = epochDayToCalendar(epochDay)
        val hours = (minutesFromMidnight / 60).coerceIn(0, 23)
        val mins = (minutesFromMidnight % 60).coerceIn(0, 59)
        cal.set(Calendar.HOUR_OF_DAY, hours)
        cal.set(Calendar.MINUTE, mins)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Formats date pill like "TUE.13.JAN" for the Planned Card on the Home Dashboard.
     */
    fun formatPlannedCardDatePill(epochDay: Long = currentLocalEpochDay()): String {
        val date = LocalDate.ofEpochDay(epochDay)
        val dayOfWeek = SHORT_WEEKDAY_UPPER[(date.dayOfWeek.value - 1).coerceIn(0, 6)]
        val dayOfMonth = if (date.dayOfMonth < 10) "0${date.dayOfMonth}" else date.dayOfMonth.toString()
        val month = SHORT_MONTH_NAMES[(date.monthValue - 1).coerceIn(0, 11)].uppercase(Locale.US)
        return "$dayOfWeek.$dayOfMonth.$month"
    }

    /**
     * Formats date with ordinal like "12th April" matching the reference image.
     */
    fun formatDayWithOrdinalMonth(epochDay: Long): String {
        val date = LocalDate.ofEpochDay(epochDay)
        val day = date.dayOfMonth
        val suffix = getDayOfMonthSuffix(day)
        val month = FULL_MONTH_NAMES[(date.monthValue - 1).coerceIn(0, 11)]
        return "$day$suffix $month"
    }

    fun formatShortMonthDay(epochDay: Long): String {
        val date = LocalDate.ofEpochDay(epochDay)
        val day = date.dayOfMonth
        val suffix = getDayOfMonthSuffix(day)
        val month = SHORT_MONTH_NAMES[(date.monthValue - 1).coerceIn(0, 11)]
        return "$day$suffix $month"
    }

    fun formatFullDateHeader(epochDay: Long): String {
        val today = currentLocalEpochDay()
        val cal = epochDayToCalendar(epochDay)
        val formatted = SimpleDateFormat("EEEE, MMM d", Locale.US).format(cal.time)
        return when (epochDay) {
            today -> "Today • $formatted"
            today + 1L -> "Tomorrow • $formatted"
            today - 1L -> "Yesterday • $formatted"
            else -> formatted
        }
    }

    fun getGreetingForHour(hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): String {
        return when (hourOfDay) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    fun formatTime(minutesFromMidnight: Int, use24Hour: Boolean): String {
        val hours24 = (minutesFromMidnight / 60).coerceIn(0, 23)
        val mins = (minutesFromMidnight % 60).coerceIn(0, 59)
        return if (use24Hour) {
            val hStr = if (hours24 < 10) "0$hours24" else hours24.toString()
            val mStr = if (mins < 10) "0$mins" else mins.toString()
            "$hStr:$mStr"
        } else {
            val amPm = if (hours24 < 12) "am" else "pm"
            val hour12 = when {
                hours24 == 0 -> 12
                hours24 > 12 -> hours24 - 12
                else -> hours24
            }
            val mStr = if (mins < 10) "0$mins" else mins.toString()
            "$hour12:$mStr$amPm"
        }
    }

    /**
     * Formats the due pill text for a task card (e.g. "Due 11:30am" or "Due 12th April").
     */
    fun formatTaskDuePill(
        dueDateEpochDay: Long,
        dueTimeMinutes: Int?,
        use24Hour: Boolean,
        todayEpochDay: Long = currentLocalEpochDay()
    ): String {
        return if (dueDateEpochDay == todayEpochDay && dueTimeMinutes != null) {
            "Due ${formatTime(dueTimeMinutes, use24Hour)}"
        } else if (dueDateEpochDay == todayEpochDay) {
            "Due Today"
        } else if (dueDateEpochDay == todayEpochDay + 1L) {
            if (dueTimeMinutes != null) {
                "Tomorrow, ${formatTime(dueTimeMinutes, use24Hour)}"
            } else {
                "Due Tomorrow"
            }
        } else {
            val datePart = formatDayWithOrdinalMonth(dueDateEpochDay)
            if (dueTimeMinutes != null) {
                "Due $datePart • ${formatTime(dueTimeMinutes, use24Hour)}"
            } else {
                "Due $datePart"
            }
        }
    }

    fun isTaskOverdue(
        dueDateEpochDay: Long,
        dueTimeMinutes: Int?,
        isCompleted: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
        todayEpochDay: Long = currentLocalEpochDay(nowMillis),
        currentMinutes: Int = -1
    ): Boolean {
        if (isCompleted) return false
        if (dueDateEpochDay < todayEpochDay) return true
        if (dueDateEpochDay == todayEpochDay && dueTimeMinutes != null) {
            val mins = if (currentMinutes >= 0) currentMinutes else currentMinutesFromMidnight(nowMillis)
            return mins > dueTimeMinutes
        }
        return false
    }

    /**
     * Iso day of week: 1 = Monday .. 7 = Sunday
     */
    fun getIsoDayOfWeek(epochDay: Long): Int {
        return LocalDate.ofEpochDay(epochDay).dayOfWeek.value
    }

    fun getDayOfWeekShortLabel(isoDay: Int): String {
        return when (isoDay) {
            1 -> "Mon"
            2 -> "Tue"
            3 -> "Wed"
            4 -> "Thu"
            5 -> "Fri"
            6 -> "Sat"
            7 -> "Sun"
            else -> "Day"
        }
    }

    fun computeNextOccurrenceEpochDay(
        currentEpochDay: Long,
        recurrenceTypeName: String,
        customWeekdaysCsv: String,
        anchorDayOfMonth: Int? = null
    ): Long? {
        return when (recurrenceTypeName) {
            "DAILY" -> currentEpochDay + 1L
            "WEEKLY" -> currentEpochDay + 7L
            "MONTHLY" -> {
                val cal = epochDayToCalendar(currentEpochDay)
                val currentDay = cal.get(Calendar.DAY_OF_MONTH)
                val targetAnchorDay = (anchorDayOfMonth ?: currentDay).coerceIn(1, 31)
                // Advance from day 1 of the current month so 31-day months never overflow
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, 1)
                val maxDayInNextMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, targetAnchorDay.coerceAtMost(maxDayInNextMonth))
                calendarToEpochDay(cal)
            }
            "CUSTOM_WEEKDAYS" -> {
                val days = customWeekdaysCsv.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .filter { it in 1..7 }
                    .toSet()
                if (days.isEmpty()) return currentEpochDay + 1L
                for (offset in 1..7) {
                    val candidate = currentEpochDay + offset
                    if (getIsoDayOfWeek(candidate) in days) {
                        return candidate
                    }
                }
                currentEpochDay + 1L
            }
            else -> null
        }
    }

    fun millisToLocalEpochDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return calendarToEpochDay(cal)
    }

    fun getMonthYearTitle(year: Int, monthZeroBased: Int): String {
        val monthName = FULL_MONTH_NAMES[monthZeroBased.coerceIn(0, 11)]
        return "$monthName $year"
    }

    fun buildMonthCalendarCells(
        year: Int,
        monthZeroBased: Int,
        weekStartsOnMonday: Boolean
    ): List<Long?> {
        val firstDate = LocalDate.of(year, (monthZeroBased + 1).coerceIn(1, 12), 1)
        val firstDayEpoch = firstDate.toEpochDay()
        val daysInMonth = firstDate.lengthOfMonth()
        val isoFirstDay = firstDate.dayOfWeek.value // 1=Mon..7=Sun

        val leadingEmpty = if (weekStartsOnMonday) {
            isoFirstDay - 1
        } else {
            isoFirstDay % 7
        }

        val cells = ArrayList<Long?>(leadingEmpty + daysInMonth + 7)
        repeat(leadingEmpty) { cells.add(null) }
        for (dayOffset in 0 until daysInMonth) {
            cells.add(firstDayEpoch + dayOffset)
        }
        while (cells.size % 7 != 0) {
            cells.add(null)
        }
        return cells
    }

    private fun getDayOfMonthSuffix(n: Int): String {
        if (n in 11..13) return "th"
        return when (n % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }
}
