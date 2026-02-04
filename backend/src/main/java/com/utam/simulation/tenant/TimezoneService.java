package com.utam.simulation.tenant;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for handling timezone conversions and tenant-local time operations.
 * Implements FR-032 to FR-035: Timezone-aware generation.
 */
@Service
public class TimezoneService {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter LOCAL_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter LOCAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Convert UTC instant to tenant's local time.
     */
    public ZonedDateTime toTenantTime(Instant utcTime, String timezone) {
        ZoneId zoneId = ZoneId.of(timezone);
        return utcTime.atZone(zoneId);
    }

    /**
     * Get current time in a specific timezone.
     */
    public ZonedDateTime getCurrentTimeInZone(String timezone) {
        return ZonedDateTime.now(ZoneId.of(timezone));
    }

    /**
     * Convert tenant local time to UTC.
     */
    public Instant toUtc(ZonedDateTime tenantTime) {
        return tenantTime.toInstant();
    }

    /**
     * Format time for display in tenant's timezone.
     */
    public String formatForDisplay(Instant time, String timezone) {
        ZonedDateTime localTime = toTenantTime(time, timezone);
        return localTime.format(ISO_FORMATTER);
    }

    /**
     * Get local time string (HH:mm:ss) for a tenant.
     */
    public String getLocalTimeString(Instant time, String timezone) {
        ZonedDateTime localTime = toTenantTime(time, timezone);
        return localTime.format(LOCAL_TIME_FORMATTER);
    }

    /**
     * Get local date string (yyyy-MM-dd) for a tenant.
     */
    public String getLocalDateString(Instant time, String timezone) {
        ZonedDateTime localTime = toTenantTime(time, timezone);
        return localTime.format(LOCAL_DATE_FORMATTER);
    }

    /**
     * Check if time is during peak hours for the timezone (7-9 AM, 5-8 PM local).
     */
    public boolean isPeakHour(Instant time, String timezone) {
        ZonedDateTime localTime = toTenantTime(time, timezone);
        int hour = localTime.getHour();
        return (hour >= 7 && hour < 9) || (hour >= 17 && hour < 20);
    }

    /**
     * Check if time is during off-peak hours (10 PM - 5 AM local).
     */
    public boolean isOffPeakHour(Instant time, String timezone) {
        ZonedDateTime localTime = toTenantTime(time, timezone);
        int hour = localTime.getHour();
        return hour >= 22 || hour < 5;
    }

    /**
     * Check if time is during daytime (6 AM - 9 PM local).
     */
    public boolean isDaytime(Instant time, String timezone) {
        ZonedDateTime localTime = toTenantTime(time, timezone);
        int hour = localTime.getHour();
        return hour >= 6 && hour < 21;
    }

    /**
     * Get hour of day in tenant's timezone.
     */
    public int getHourOfDay(Instant time, String timezone) {
        return toTenantTime(time, timezone).getHour();
    }

    /**
     * Get day of week in tenant's timezone (1=Monday, 7=Sunday).
     */
    public int getDayOfWeek(Instant time, String timezone) {
        return toTenantTime(time, timezone).getDayOfWeek().getValue();
    }

    /**
     * Check if it's a weekend in the tenant's timezone.
     */
    public boolean isWeekend(Instant time, String timezone) {
        int dow = getDayOfWeek(time, timezone);
        return dow == 6 || dow == 7; // Saturday or Sunday
    }

    /**
     * Calculate the timezone offset in hours from UTC.
     */
    public int getUtcOffsetHours(String timezone) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timezone));
        return now.getOffset().getTotalSeconds() / 3600;
    }
}
