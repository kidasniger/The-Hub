package com.thehub.hb

import com.thehub.hb.ui.components.formatBirthdate
import com.thehub.hb.ui.components.parseBirthdateOrDefault
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WheelDatePickerUnitTest {

    @Test
    fun testParseBirthdateValid() {
        val parsed = parseBirthdateOrDefault("25/12/1995")
        assertEquals(1995, parsed.year)
        assertEquals(12, parsed.monthValue)
        assertEquals(25, parsed.dayOfMonth)
    }

    @Test
    fun testParseBirthdateFallback() {
        val fallback = LocalDate.of(2000, 1, 1)
        val parsedEmpty = parseBirthdateOrDefault("", fallback)
        assertEquals(fallback, parsedEmpty)

        val parsedInvalid = parseBirthdateOrDefault("invalid-date", fallback)
        assertEquals(fallback, parsedInvalid)
    }

    @Test
    fun testFormatBirthdate() {
        val date = LocalDate.of(1998, 7, 9)
        val formatted = formatBirthdate(date)
        assertEquals("09/07/1998", formatted)
    }

    @Test
    fun testLeapYearFebruaryLength() {
        val leapYearFeb = LocalDate.of(2024, 2, 1).lengthOfMonth()
        assertEquals(29, leapYearFeb)

        val nonLeapYearFeb = LocalDate.of(2023, 2, 1).lengthOfMonth()
        assertEquals(28, nonLeapYearFeb)
    }
}
