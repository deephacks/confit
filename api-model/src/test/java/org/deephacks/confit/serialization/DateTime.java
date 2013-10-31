package org.deephacks.confit.serialization;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a date/time closely to xs:dateTime ISO8601, or a date without a
 * time. Optionally includes a time zone.
 *
 * See: http://www.w3.org/TR/xmlschema-2/#isoformats
 */
public final class DateTime {

    /** date/time pattern. */
    private static final Pattern dateTimePattern = Pattern
            .compile("(\\d\\d\\d\\d)\\-(\\d\\d)\\-(\\d\\d)[Tt]"
                    + "(\\d\\d):(\\d\\d):(\\d\\d)(\\.(\\d+))?"
                    + "([Zz]|((\\+|\\-)(\\d\\d):(\\d\\d)))?");

    /** date pattern. */
    private static final Pattern datePattern = Pattern
            .compile("(\\d\\d\\d\\d)\\-(\\d\\d)\\-(\\d\\d)"
                    + "([Zz]|((\\+|\\-)(\\d\\d):(\\d\\d)))?");

    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    private String dateTimeString;

    public DateTime(String dateTime) {
        this.dateTimeString = dateTime;
    }

    public String getDateTimeString() {
        return dateTimeString;
    }

    public Date parseDateTimeOrDate() throws NumberFormatException {
        NumberFormatException exception;

        try {
            return parseDateTime();
        } catch (NumberFormatException e) {
            exception = e;
        }

        try {
            return parseDate();
        } catch (NumberFormatException e) {
            exception = e;
        }

        throw exception;
    }

    /**
     * Parses an xs:dateTime string. If the time zone is specified, this value
     * is normalized to UTC, so to format this date/time value, the time zone
     * shift has to be applied. If no time zone or shifts is specified, the
     * local time is assumed.
     */
    public Date parseDateTime() throws NumberFormatException {

        Matcher m = dateTimeString == null ? null : dateTimePattern.matcher(dateTimeString);

        if (dateTimeString == null || !m.matches()) {
            throw new NumberFormatException("Invalid date/time format.");
        }
        String year = m.group(1);
        String month = m.group(2);
        String day = m.group(3);
        String hour = m.group(4);
        String minute = m.group(5);
        String sec = m.group(6);
        String secFraction = m.group(8);
        String timezone = m.group(9);
        String timezoneShift = m.group(11);
        String timezoneHour = m.group(12);
        String timezoneMinute = m.group(13);
        /**
         * Time zone shift from UTC in minutes.
         */
        Integer tzShift;
        Calendar dateTime = null;

        if (timezone == null) {
            // No time zone specified. Use local time.
            dateTime = new GregorianCalendar();
            tzShift = 0;
        } else if (timezone.equalsIgnoreCase("Z")) {
            // Use UTC, no shift
            dateTime = new GregorianCalendar(GMT);
            tzShift = 0;
        } else {
            // shift from UTC
            dateTime = new GregorianCalendar(GMT);
            tzShift = new Integer(
                    (Integer.valueOf(timezoneHour) * 60 + Integer.valueOf(timezoneMinute)));
            if (timezoneShift.equals("-")) {
                tzShift = new Integer(-tzShift.intValue());
            }
        }
        dateTime.clear();
        dateTime.set(Integer.valueOf(year), Integer.valueOf(month) - 1, Integer.valueOf(day),
                Integer.valueOf(hour), Integer.valueOf(minute), Integer.valueOf(sec));
        if (secFraction != null && secFraction.length() > 0) {
            final BigDecimal bd = new BigDecimal("0." + secFraction);
            // we care only for milliseconds, so movePointRight(3)
            dateTime.set(Calendar.MILLISECOND, bd.movePointRight(3).intValue());
        }

        long time = dateTime.getTimeInMillis();

        time -= tzShift.intValue() * 60000;

        return new Date(time);
    }

    /** Parses an xs:date string. */
    public Date parseDate() throws NumberFormatException {

        Matcher m = dateTimeString == null ? null : datePattern.matcher(dateTimeString);

        if (dateTimeString == null || !m.matches()) {
            throw new NumberFormatException("Invalid date format.");
        }

        String year = m.group(1);
        String month = m.group(2);
        String day = m.group(3);
        String timezone = m.group(4);
        String timezoneShift = m.group(6);
        String timezoneHour = m.group(7);
        String timezoneMinute = m.group(8);
        Integer tzShift = null;

        Calendar dateTime = null;

        if (timezone == null) {
            // No time zone specified.
            dateTime = new GregorianCalendar();
            tzShift = 0;
        } else if (timezone.equalsIgnoreCase("Z")) {
            tzShift = 0;
            dateTime = new GregorianCalendar(GMT);
        } else {
            tzShift = new Integer(
                    (Integer.valueOf(timezoneHour) * 60 + Integer.valueOf(timezoneMinute)));
            if (timezoneShift.equals("-")) {
                tzShift = new Integer(-tzShift.intValue());
            }
            dateTime = new GregorianCalendar(GMT);
        }

        dateTime.clear();
        dateTime.set(Integer.valueOf(year), Integer.valueOf(month) - 1, Integer.valueOf(day));

        long time = dateTime.getTimeInMillis();
        time -= tzShift.intValue() * 60000;

        return new Date(time);
    }

    @Override
    public String toString() {
        return dateTimeString;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dateTimeString == null) ? 0 : dateTimeString.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DateTime other = (DateTime) obj;
        if (dateTimeString == null) {
            if (other.dateTimeString != null)
                return false;
        } else if (!dateTimeString.equals(other.dateTimeString))
            return false;
        return true;
    }

}