package io.avaje.logback.encoder;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

public final class TimeZoneUtils {
  /** Keyword used by {@link #setTimeZone(String)} to denote the system default time zone. */
  private static final String DEFAULT_TIMEZONE_KEYWORD = "[DEFAULT]";

  private TimeZoneUtils() {}

  /**
   * Parse a string into the corresponding {@link TimeZone} using the format described by {@link
   * TimeZone#getTimeZone(String)}.
   *
   * <p>The value of the {@code timeZone} can be any string accepted by java's {@link
   * TimeZone#getTimeZone(String)} method. For example "America/Los_Angeles" or "GMT+10".
   *
   * @param str the string to parse into a valid {@link TimeZone}.
   * @return the {@link TimeZone} corresponding to the input string
   * @throws IllegalArgumentException thrown when the string is not a valid TimeZone textual
   *     representation.
   */
  public static TimeZone parseTimeZone(String str) {
    if (str == null || str.isEmpty() || DEFAULT_TIMEZONE_KEYWORD.equalsIgnoreCase(str)) {
      return TimeZone.getDefault();
    }

    TimeZone tz = TimeZone.getTimeZone(str);

    /*
     * Instead of throwing an exception when it fails to parse the string into a valid
     * TimeZone, getTimeZone() returns a TimeZone with id "GMT".
     *
     * If the returned TimeZone is GMT but the input string is not, then the input string
     * was not a valid time zone representation.
     */
    if ("GMT".equals(tz.getID()) && !"GMT".equals(str)) {
      throw new IllegalArgumentException("Invalid TimeZone value (was '" + str + "')");
    }

    return tz;
  }

  public static DateTimeFormatter formatter(String pattern, ZoneId zoneId) {
    if (pattern == null) {
      return DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(zoneId);
    }

    try {
      TimePattern time = TimePattern.valueOf(pattern.toUpperCase());
      DateTimeFormatter format = null;
      switch (time) {
        case ISO_OFFSET_DATE_TIME:
          format = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(zoneId);
          break;
        case ISO_ZONED_DATE_TIME:
          format = DateTimeFormatter.ISO_ZONED_DATE_TIME.withZone(zoneId);
          break;
        case ISO_LOCAL_DATE_TIME:
          format = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(zoneId);
          break;
        case ISO_DATE_TIME:
          format = DateTimeFormatter.ISO_DATE_TIME.withZone(zoneId);
          break;
        case ISO_INSTANT:
          format = DateTimeFormatter.ISO_INSTANT.withZone(zoneId);
          break;
      }
      return format;

    } catch (IllegalArgumentException e) {
      return DateTimeFormatter.ofPattern(pattern);
    }
  }
}
