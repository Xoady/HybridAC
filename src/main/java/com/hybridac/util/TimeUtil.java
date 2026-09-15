package com.hybridac.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class TimeUtil {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

    private TimeUtil() {
    }

    public static String formatInstant(Instant instant) {
        return FORMATTER.format(instant);
    }
}
