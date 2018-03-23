package info.koosah.acarsutils.wxdecoder;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import info.koosah.acarsutils.*;

/**
 * Decoder for Northwest Airlines (NW). They've been bought out by Delta
 * years ago, but the NW flight prefix is still in use and such planes use
 * a distinct and different format.
 *
 * @author David Barts <n5jrn@me.com>
 */
public class DecoderForNW extends WxDecoder {
    private static final TimeZone ZONE = TimeZone.getTimeZone("GMT");

    private HashMap<Integer,GregorianCalendar> hours;
    private static final Pattern FULL    = Pattern.compile("[NS]\\d{5}[EW]\\d{13}[-+\\d]\\d{11}");
    private static final Pattern PARTIAL = Pattern.compile("[NS]\\d{5}[EW]\\d{13}[-+\\d]\\d{2}");
    private static final String PREFIX = "#DFB*WXR";

    /**
     * Decode something.
     * @param message     An AcarsMessage.
     * @return            An Iterable<AcarsObservation>, or null
     */
    public Iterable<AcarsObservation> decode(IAcarsMessage message, Date baseTime) {
        // Northwest uses H1 messages that start "#DFB*WXR" for their
        // weather reports.
        String body = message.getMessage();
        if (!message.getLabel().equals("H1") || !body.startsWith(PREFIX)) {
            return null;
        }

        // Get the reports themselves. There may be some junk whitespace
        // before they start.
        int length = body.length();
        int start;
        for (start=PREFIX.length(); start<length; start++) {
            if (!Character.isWhitespace(body.charAt(start)))
                break;
        }
        String[] lines = body.substring(start).split("\\r?\\n");

        // All lines SHOULD be complete observations, but be lenient.
        boolean found = false;
        ArrayList<AcarsObservation> ret = new ArrayList<AcarsObservation>();
        initBase(baseTime);
        for (String line : lines) {
            AcarsObservation obs = null;
            if (FULL.matcher(line).matches()) {
                found = true;
                obs = makeObs(line);
            } else if (found && PARTIAL.matcher(line).lookingAt()) {
                obs = makeObs(line);
            }
            if (obs != null)
                ret.add(obs);
        }

        return ret;
    }

    private AcarsObservation makeObs(String line) {
        // Get the basic four coordinates of the observation. These will always
        // be present.
        double sign = line.charAt(0) == 'N' ? 1.0 : -1.0;
        double latitude = sign * Double.parseDouble(line.substring(1, 6)) / 1000.0;
        sign = line.charAt(6) == 'E' ? 1.0 : -1.0;
        double longitude = sign * Double.parseDouble(line.substring(7, 13)) / 1000.0;
        Date observed = parseTime(line.substring(13, 17));
        int altitude = Integer.parseInt(line.substring(17, 20)) * 100;

        // Build the base object.
        AcarsObservation ret = new AcarsObservation(latitude, longitude, altitude, observed);

        // Temperature. No fractional degrees, so must be complete.
        int length = line.length();
        if (length < 23)
            return ret;
        ret.setTemperature(Float.parseFloat(line.substring(20, 23)));

        // Wind direction. Must be complete.
        if (length < 27)
            return ret;
        ret.setWindDirection(Short.parseShort(line.substring(24, 27)));

        // Wind speed. Must be complete.
        if (length < 30)
            return ret;
        ret.setWindSpeed(Short.parseShort(line.substring(27, 30)));
        return ret;
    }

    /* we match the base hour, previous hours back 22, and 1 future
       hour */
    private void initBase(Date baseTime) {
        hours = new HashMap<Integer,GregorianCalendar>();
        GregorianCalendar base = new GregorianCalendar(ZONE);
        base.setTime(baseTime);
        base.set(GregorianCalendar.SECOND, 0);
        base.set(GregorianCalendar.MILLISECOND, 0);
        base.add(GregorianCalendar.HOUR_OF_DAY, -22);
        for (int i=0; i<24; i++) {
            GregorianCalendar c = (GregorianCalendar) base.clone();
            c.add(GregorianCalendar.HOUR_OF_DAY, i);
            hours.put(c.get(GregorianCalendar.HOUR_OF_DAY), c);
        }
    }

    private Date parseTime(String hhmm) {
        int hh = Integer.parseInt(hhmm.substring(0, 2));
        int mm = Integer.parseInt(hhmm.substring(2, 4));
        GregorianCalendar ret = hours.get(hh);
        if (ret == null)
            throw new IllegalArgumentException("Observation not within supported window.");
        ret = (GregorianCalendar) ret.clone();
        ret.set(GregorianCalendar.MINUTE, mm);
        return ret.getTime();
    }
}
