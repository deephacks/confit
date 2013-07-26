package org.deephacks.confit.test;

import java.util.concurrent.TimeUnit;

/**
 * Represents a duration with inspiration from standard ISO8601 format, but
 * excluding unstable calendar granularity (years, months) of the format to
 * achieve accurate durations, days are excluded as well as milliseconds and
 * finer granularity. Negative durations are supported.
 * <p>
 * Format: PTnHnMnS
 * </p>
 * <p>
 * See "http://www.w3.org/TR/xmlschema-2/#duration" for more information on the
 * subject.
 * </p>
 */
public class DurationTime implements java.io.Serializable {
    private static final String FORMAT = "PTnHnMnS";
    private static final String BAD_DURATION = "Bad duration format, use format [" + FORMAT + "]";
    private static final String BAD_DURATION_SEC = "Bad seconds duration format, use format ["
            + FORMAT + "]";
    private static final String BAD_DURATION_MIN = "Bad minutes duration format, use format ["
            + FORMAT + "]";

    private static final String BAD_DURATION_HOUR = "Bad hour duration format, use format ["
            + FORMAT + "]";
    private static final long serialVersionUID = -3175940178934425650L;
    private boolean isNegative = false;
    private int hours;
    private int minutes;
    private long seconds;

    public DurationTime(boolean negative, int aHours, int aMinutes, long aSeconds) {
        isNegative = negative;
        hours = aHours;
        minutes = aMinutes;
        seconds = aSeconds;
    }

    /**
     * Constructs Duration from a String somewhat according to ISO8601 format -
     * PnHnMnS.
     * 
     */
    public DurationTime(String duration) throws IllegalArgumentException {

        int timePosition = duration.indexOf("T");

        // P is required but P by itself is invalid
        if ((duration.indexOf("P") == -1) || duration.equals("P")) {
            throw new IllegalArgumentException("P parameter missing.");
        }

        // if present, time cannot be empty
        if (duration.lastIndexOf("T") == duration.length() - 1) {
            throw new IllegalArgumentException("T parameter missing.");
        }

        // check the sign
        if (duration.startsWith("-")) {
            isNegative = true;
        }

        // parse time part
        if (timePosition != -1) {
            parseTime(duration.substring(timePosition + 1));
        } else {
            timePosition = duration.length();
        }
    }

    /**
     * This method parses the time portion of a ISO8601 String.
     * 
     * @param time
     *            String that represents nHnMnS
     * @throws IllegalArgumentException
     *             if time does not match format
     * 
     */
    public void parseTime(String time) throws IllegalArgumentException {
        if ((time.length() == 0) || (time.indexOf("-") != -1)) {
            throw new IllegalArgumentException(BAD_DURATION);
        }

        // check if time ends with either H, M, or S
        if (!time.endsWith("H") && !time.endsWith("M") && !time.endsWith("S")) {
            throw new IllegalArgumentException(BAD_DURATION);
        }

        try {
            // parse string and extract hours, minutes, and seconds
            int start = 0;

            // Hours
            int end = time.indexOf("H");
            // if there is H in a string but there is no value for hours,
            // throw an exception
            if (start == end) {
                throw new IllegalArgumentException(BAD_DURATION_HOUR);
            }
            if (end != -1) {
                hours = Integer.parseInt(time.substring(0, end));
                start = end + 1;
            }

            // Minutes
            end = time.indexOf("M");
            // if there is M in a string but there is no value for hours,
            // throw an exception
            if (start == end) {
                throw new IllegalArgumentException(BAD_DURATION_MIN);
            }

            if (end != -1) {
                minutes = Integer.parseInt(time.substring(start, end));
                start = end + 1;
            }

            // Seconds
            end = time.indexOf("S");
            // if there is S in a string but there is no value for hours,
            // throw an exception
            if (start == end) {
                throw new IllegalArgumentException(BAD_DURATION_SEC);
            }

            if (end != -1) {
                seconds = Long.parseLong(time.substring(start, end));
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(BAD_DURATION);
        }
    }

    public boolean isNegative() {
        return isNegative;
    }

    public int getHours() {
        return hours;
    }

    public int getMinutes() {
        return minutes;
    }

    public long getSeconds() {
        return seconds;
    }

    /**
     * Sum parameters to duration in desired unit.
     */
    public long getDurationSumIn(TimeUnit inUnit) {
        return inUnit.convert(hours, TimeUnit.HOURS) + inUnit.convert(minutes, TimeUnit.MINUTES)
                + inUnit.convert(seconds, TimeUnit.SECONDS);
    }

    /**
     * This returns the "ISO8601" time representation of this object.
     */
    @Override
    public String toString() {
        StringBuffer duration = new StringBuffer();

        duration.append("P");
        if ((hours != 0) || (minutes != 0) || (seconds != 0.0)) {
            duration.append("T");

            if (hours != 0) {
                duration.append(hours).append("H");

            }
            if (minutes != 0) {
                duration.append(minutes).append("M");

            }
            if (seconds != 0) {
                if (seconds == (int) seconds) {
                    duration.append((int) seconds).append("S");
                } else {
                    duration.append(seconds).append("S");
                }
            }
        }

        if (duration.length() == 1) {
            duration.append("T0S");
        }

        if (isNegative) {
            duration.insert(0, "-");
        }

        return duration.toString();
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        if (isNegative) {
            hashCode++;
        }
        hashCode += hours;
        hashCode += minutes;
        hashCode += seconds;
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final DurationTime other = (DurationTime) obj;
        if (other.getDurationSumIn(TimeUnit.SECONDS) != this.getDurationSumIn(TimeUnit.SECONDS)) {
            return false;
        }
        return true;
    }
}