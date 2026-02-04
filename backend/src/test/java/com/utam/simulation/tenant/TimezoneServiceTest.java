package com.utam.simulation.tenant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TimezoneService.
 */
class TimezoneServiceTest {

    private TimezoneService timezoneService;

    @BeforeEach
    void setUp() {
        timezoneService = new TimezoneService();
    }

    @Test
    @DisplayName("Should convert UTC to tenant timezone")
    void shouldConvertUtcToTenantTimezone() {
        Instant utcTime = Instant.parse("2025-01-20T12:00:00Z");
        
        ZonedDateTime kolkataTime = timezoneService.toTenantTime(utcTime, "Asia/Kolkata");
        ZonedDateTime brisbaneTime = timezoneService.toTenantTime(utcTime, "Australia/Brisbane");
        
        // Kolkata is UTC+5:30
        assertEquals(17, kolkataTime.getHour());
        assertEquals(30, kolkataTime.getMinute());
        
        // Brisbane is UTC+10
        assertEquals(22, brisbaneTime.getHour());
    }

    @Test
    @DisplayName("Should convert tenant time to UTC")
    void shouldConvertTenantTimeToUtc() {
        ZonedDateTime kolkataTime = ZonedDateTime.parse("2025-01-20T17:30:00+05:30");
        
        Instant utc = timezoneService.toUtc(kolkataTime);
        
        assertEquals(Instant.parse("2025-01-20T12:00:00Z"), utc);
    }

    @Test
    @DisplayName("Should identify peak hours")
    void shouldIdentifyPeakHours() {
        // 8 AM Kolkata (peak)
        Instant peakMorning = Instant.parse("2025-01-20T02:30:00Z"); // 8 AM in Kolkata
        assertTrue(timezoneService.isPeakHour(peakMorning, "Asia/Kolkata"));
        
        // 6 PM Kolkata (peak)
        Instant peakEvening = Instant.parse("2025-01-20T12:30:00Z"); // 6 PM in Kolkata
        assertTrue(timezoneService.isPeakHour(peakEvening, "Asia/Kolkata"));
        
        // 2 PM Kolkata (not peak)
        Instant offPeak = Instant.parse("2025-01-20T08:30:00Z"); // 2 PM in Kolkata
        assertFalse(timezoneService.isPeakHour(offPeak, "Asia/Kolkata"));
    }

    @Test
    @DisplayName("Should identify off-peak hours")
    void shouldIdentifyOffPeakHours() {
        // 11 PM Kolkata (off-peak)
        Instant night = Instant.parse("2025-01-20T17:30:00Z"); // 11 PM in Kolkata
        assertTrue(timezoneService.isOffPeakHour(night, "Asia/Kolkata"));
        
        // 3 AM Kolkata (off-peak)
        Instant earlyMorning = Instant.parse("2025-01-20T21:30:00Z"); // 3 AM in Kolkata (next day)
        assertTrue(timezoneService.isOffPeakHour(earlyMorning, "Asia/Kolkata"));
    }

    @Test
    @DisplayName("Should identify daytime")
    void shouldIdentifyDaytime() {
        // 2 PM Kolkata (daytime)
        Instant afternoon = Instant.parse("2025-01-20T08:30:00Z");
        assertTrue(timezoneService.isDaytime(afternoon, "Asia/Kolkata"));
        
        // 10 PM Kolkata (nighttime)
        Instant night = Instant.parse("2025-01-20T16:30:00Z");
        assertFalse(timezoneService.isDaytime(night, "Asia/Kolkata"));
    }

    @Test
    @DisplayName("Should get hour of day")
    void shouldGetHourOfDay() {
        Instant time = Instant.parse("2025-01-20T12:00:00Z");
        
        assertEquals(17, timezoneService.getHourOfDay(time, "Asia/Kolkata")); // 5:30 PM
        assertEquals(22, timezoneService.getHourOfDay(time, "Australia/Brisbane")); // 10 PM
    }

    @Test
    @DisplayName("Should get day of week")
    void shouldGetDayOfWeek() {
        // Monday UTC, should still be Monday in Kolkata for this time
        Instant monday = Instant.parse("2025-01-20T12:00:00Z");
        
        assertEquals(1, timezoneService.getDayOfWeek(monday, "Asia/Kolkata")); // Monday
    }

    @Test
    @DisplayName("Should identify weekend")
    void shouldIdentifyWeekend() {
        Instant saturday = Instant.parse("2025-01-18T12:00:00Z");
        Instant monday = Instant.parse("2025-01-20T12:00:00Z");
        
        assertTrue(timezoneService.isWeekend(saturday, "Asia/Kolkata"));
        assertFalse(timezoneService.isWeekend(monday, "Asia/Kolkata"));
    }

    @Test
    @DisplayName("Should format time for display")
    void shouldFormatTimeForDisplay() {
        Instant time = Instant.parse("2025-01-20T12:00:00Z");
        
        String formatted = timezoneService.formatForDisplay(time, "Asia/Kolkata");
        
        assertTrue(formatted.contains("2025-01-20"));
        assertTrue(formatted.contains("+05:30"));
    }

    @Test
    @DisplayName("Should get local time string")
    void shouldGetLocalTimeString() {
        Instant time = Instant.parse("2025-01-20T12:00:00Z");
        
        String localTime = timezoneService.getLocalTimeString(time, "Asia/Kolkata");
        
        assertEquals("17:30:00", localTime);
    }

    @Test
    @DisplayName("Should get UTC offset hours")
    void shouldGetUtcOffsetHours() {
        assertEquals(5, timezoneService.getUtcOffsetHours("Asia/Kolkata")); // +5:30 -> 5
        assertEquals(10, timezoneService.getUtcOffsetHours("Australia/Brisbane"));
    }
}
