package info.koosah.acarsutils.wxdecoder;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import info.koosah.acarsutils.*;

/**
 * Decoder for Air Canada (AC).
 *
 * @author David Barts <n5jrn@me.com>
 */
public class DecoderForAC extends WxDecoder {
    private static final TimeZone ZONE = TimeZone.getTimeZone("GMT");

    private GregorianCalendar[] daysToTry;
    private static final int LENGTH = 116;
    private static final Pattern PREFIX = Pattern.compile("AGFSR [A-Z]{2}\\d{4}/\\d{2}/\\d{2}/\\w{6}/\\d{4}Z/\\d{3}/\\d{4}\\.\\d[NS]\\d{5}\\.\\d[EW]/\\d{3}/");

    /**
     * Decode something.
     * @param message     An AcarsMessage.
     * @return            An Iterable<AcarsObservation>, or null
     */
    public Iterable<AcarsObservation> decode(IAcarsMessage message, Date baseTime) {
        // Air Canada uses 4T messages that start with a specific prefix for
        // their weather reports. 4T is not a documented ACARS message type.
        // AC seems fond of these. Oh well.
        String body = message.getMessage();
        if (!message.getLabel().equals("4T") || body.length() < LENGTH) {
            return null;
        }
        if (!PREFIX.matcher(body).lookingAt()) {
            return null;
        }

        // Get date/time stamp
        initBase(baseTime);
        Date date = null;
        try {
            String dd = body.substring(16, 18);
            String hhmm = body.substring(26, 30);
            date = parseTime(dd, hhmm);
        } catch (IllegalArgumentException e) {
            return null;
        }

        // Lat/long/alt
        double sign = body.charAt(42) == 'N' ? 1.0 : -1.0;
        double latitude = sign * Double.parseDouble(body.substring(36, 42)) / 100.0;
        sign = body.charAt(50) == 'E' ? 1.0 : -1.0;
        double longitude = sign * Double.parseDouble(body.substring(43, 50)) / 100.0;
        int altitude = Integer.parseInt(body.substring(52, 55)) * 100;

        // Optional stuff
        AcarsObservation o = new AcarsObservation(latitude, longitude, altitude, date);
        float tsign = body.charAt(73) == 'M' ? -1.0f : 1.0f;
        o.setTemperature(tsign * Float.parseFloat(body.substring(74, 76)));
        o.setWindDirection(Short.parseShort(body.substring(77, 80)));
        o.setWindSpeed(Short.parseShort(body.substring(80, 83)));

        // We must return an Iterable
        ArrayList<AcarsObservation> ret = new ArrayList<AcarsObservation>(1);
        ret.add(o);
        return ret;
    }

    private void initBase(Date baseTime) {
        GregorianCalendar today = new GregorianCalendar(ZONE);
        today.setTime(baseTime);
        today.set(GregorianCalendar.SECOND, 0);
        today.set(GregorianCalendar.MILLISECOND, 0);
        GregorianCalendar yesterday = (GregorianCalendar) today.clone();
        yesterday.add(GregorianCalendar.DATE, -1);
        GregorianCalendar tomorrow = (GregorianCalendar) today.clone();
        tomorrow.add(GregorianCalendar.DATE, 1);
        daysToTry = new GregorianCalendar[] { today, yesterday, tomorrow };
    }

    private Date parseTime(String rawDd, String hhmm) {
        int dd = Integer.parseInt(rawDd);
        int hh = Integer.parseInt(hhmm.substring(0, 2));
        int mm = Integer.parseInt(hhmm.substring(2, 4));
        for (GregorianCalendar day : daysToTry)
            if (dd == day.get(GregorianCalendar.DAY_OF_MONTH)) {
                GregorianCalendar ret = (GregorianCalendar) day.clone();
                ret.set(GregorianCalendar.MINUTE, mm);
                ret.set(GregorianCalendar.HOUR_OF_DAY, hh);
                return ret.getTime();
            }
        throw new IllegalArgumentException("Observation not within 24 hrs of base time");
    }
}
