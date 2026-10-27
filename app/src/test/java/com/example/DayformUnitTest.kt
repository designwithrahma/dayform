package com.example

import com.example.util.DateTimeUtil
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.TimeZone

class DayformUnitTest {


    @Test
    fun datePickerUtcMillisRoundTripPreservesEpochDayAcrossAllTimeZones() {
        val originalTimeZone = TimeZone.getDefault()
        val timeZonesToTest = listOf(
            "Asia/Colombo",        // UTC+05:30
            "Pacific/Auckland",    // UTC+12:00 / UTC+13:00
            "UTC",                 // UTC+00:00
            "America/Los_Angeles"  // UTC-08:00 / UTC-07:00
        )
        val datesToTest = listOf(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 6, 15),
            LocalDate.of(2026, 10, 26),
            LocalDate.of(2026, 12, 31),
            LocalDate.of(2028, 2, 29)
        )

        try {
            for (zoneId in timeZonesToTest) {
                TimeZone.setDefault(TimeZone.getTimeZone(zoneId))
                for (localDate in datesToTest) {
                    val epochDay = localDate.toEpochDay()
                    val utcMidnightMillis = DateTimeUtil.epochDayToUtcPickerMillis(epochDay)
                    assertEquals(
                        "UTC midnight millis mismatch in $zoneId for $localDate",
                        epochDay * 86_400_000L,
                        utcMidnightMillis
                    )
                    val roundTrippedEpochDay = DateTimeUtil.utcPickerMillisToEpochDay(utcMidnightMillis)
                    assertEquals(
                        "EpochDay round-trip shifted in $zoneId for $localDate",
                        epochDay,
                        roundTrippedEpochDay
                    )
                }
            }
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }

    @Test
    fun monthlyRecurrencePreservesAnchorDayAcrossShortAndLeapMonths() {
        // 1. Anchor day 31 across non-leap year 2026: Jan 31 -> Feb 28 -> Mar 31 -> Apr 30 -> May 31
        val jan31 = LocalDate.of(2026, 1, 31).toEpochDay()
        val feb28 = DateTimeUtil.computeNextOccurrenceEpochDay(
            currentEpochDay = jan31,
            recurrenceTypeName = "MONTHLY",
            customWeekdaysCsv = "",
            anchorDayOfMonth = 31
        )
        assertEquals(LocalDate.of(2026, 2, 28).toEpochDay(), feb28)

        val mar31 = DateTimeUtil.computeNextOccurrenceEpochDay(
            currentEpochDay = feb28!!,
            recurrenceTypeName = "MONTHLY",
            customWeekdaysCsv = "",
            anchorDayOfMonth = 31
        )
        assertEquals(LocalDate.of(2026, 3, 31).toEpochDay(), mar31)

        val apr30 = DateTimeUtil.computeNextOccurrenceEpochDay(
            currentEpochDay = mar31!!,
            recurrenceTypeName = "MONTHLY",
            customWeekdaysCsv = "",
            anchorDayOfMonth = 31
        )
        assertEquals(LocalDate.of(2026, 4, 30).toEpochDay(), apr30)

        val may31 = DateTimeUtil.computeNextOccurrenceEpochDay(
            currentEpochDay = apr30!!,
            recurrenceTypeName = "MONTHLY",
            customWeekdaysCsv = "",
            anchorDayOfMonth = 31
        )
        assertEquals(LocalDate.of(2026, 5, 31).toEpochDay(), may31)

        // 2. Anchor day 31 across leap year 2028: Jan 31 -> Feb 29 -> Mar 31
        val leapJan31 = LocalDate.of(2028, 1, 31).toEpochDay()
        val leapFeb29 = DateTimeUtil.computeNextOccurrenceEpochDay(
            currentEpochDay = leapJan31,
            recurrenceTypeName = "MONTHLY",
            customWeekdaysCsv = "",
            anchorDayOfMonth = 31
        )
        assertEquals(LocalDate.of(2028, 2, 29).toEpochDay(), leapFeb29)

        val leapMar31 = DateTimeUtil.computeNextOccurrenceEpochDay(
            currentEpochDay = leapFeb29!!,
            recurrenceTypeName = "MONTHLY",
            customWeekdaysCsv = "",
            anchorDayOfMonth = 31
        )
        assertEquals(LocalDate.of(2028, 3, 31).toEpochDay(), leapMar31)

        // 3. Anchor day 30 across Feb -> Mar -> Apr
        val jan30 = LocalDate.of(2026, 1, 30).toEpochDay()
        val feb28From30 = DateTimeUtil.computeNextOccurrenceEpochDay(
            currentEpochDay = jan30,
            recurrenceTypeName = "MONTHLY",
            customWeekdaysCsv = "",
            anchorDayOfMonth = 30
        )
        assertEquals(LocalDate.of(2026, 2, 28).toEpochDay(), feb28From30)

        val mar30From30 = DateTimeUtil.computeNextOccurrenceEpochDay(
            currentEpochDay = feb28From30!!,
            recurrenceTypeName = "MONTHLY",
            customWeekdaysCsv = "",
            anchorDayOfMonth = 30
        )
        assertEquals(LocalDate.of(2026, 3, 30).toEpochDay(), mar30From30)
    }
}
