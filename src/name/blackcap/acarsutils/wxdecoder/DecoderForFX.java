package name.blackcap.acarsutils.wxdecoder;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.blackcap.acarsutils.*;

/**
 * Decoder for Federal Express (FX), a carrier which thoughtfully encodes
 * observations in at least three different ways.
 *
 * @author David Barts <n5jrn@me.com>
 */
public class DecoderForFX extends WxDecoder {
    private static final TimeZone ZONE = TimeZone.getTimeZone("GMT");

    private HashMap<Integer,GregorianCalendar> hours;
    private static final Pattern A_FULL    = Pattern.compile("/\\d{6}[NS]\\d{4}[EW]\\d{10}[-+\\d]\\d{10}");
    private static final Pattern A_PARTIAL = Pattern.compile("/\\d{6}[NS]\\d{4}[EW]\\d{10}[-+\\d]\\d{2}");
    private static final Pattern B_FULL    = Pattern.compile("\\d{6}[-+\\d]\\d{6}[-+\\d]\\d{13}[-+d]\\d{10}");
    private static final Pattern B_PARTIAL = Pattern.compile("\\d{6}[-+\\d]\\d{6}[-+\\d]\\d{13}[-+d]\\d{2}");
    private static final Pattern C_FULL    = Pattern.compile("\\d{6}[NS]\\d{4}[EW]\\d{11}[-+\\d]\\d{10}");
    private static final Pattern C_PARTIAL = Pattern.compile("\\d{6}[NS]\\d{4}[EW]\\d{11}[-+\\d]\\d{2}");

    /**
     * Decode something.
     * @param message     An AcarsMessage.
     * @return            An Iterable<AcarsObservation>, or null
     */
    public Iterable<AcarsObservation> decode(IAcarsMessage message, Date baseTime) {
        // FedEx uses H1 messages with a source of DF for observations.
        String body = message.getMessage();
        if (!message.getLabel().equals("H1") || !message.getSource().equals("DF")) {
            return null;
        }

        // Split the message body into lines.
        String[] lines = message.getMessage().split("\\r?\\n");

        // No runts have been observed by me "in the wild" but be lenient
        // and allow them anyhow.
        boolean found = false;
        ArrayList<AcarsObservation> ret = new ArrayList<AcarsObservation>();
        initBase(baseTime);
        for (String line : lines) {
            AcarsObservation obs = null;
            if (A_FULL.matcher(line).matches()) {
                found = true;
                obs = makeObsA(line);
            } else if (B_FULL.matcher(line).matches()) {
                found = true;
                obs = makeObsB(line);
            } else if (C_FULL.matcher(line).matches()) {
                found = true;
                obs = makeObsC(line);
            } else if (found) {
                if (A_PARTIAL.matcher(line).lookingAt())
                    obs = makeObsA(line);
                else if (B_PARTIAL.matcher(line).lookingAt())
                    obs = makeObsB(line);
                else if (C_PARTIAL.matcher(line).lookingAt())
                    obs = makeObsC(line);
            }
            if (obs != null)
                ret.add(obs);
        }

        return found ? ret : null;
    }

    private AcarsObservation makeObsA(String line) {
        // Get the basic four coordinates of the observation. These will always
        // be present.
        Date observed = parseTime(line.substring(1, 7));
        double sign = line.charAt(7) == 'N' ? 1.0 : -1.0;
        double latitude = sign * Double.parseDouble(line.substring(8, 12)) / 100.0;
        sign = line.charAt(12) == 'E' ? 1.0 : -1.0;
        double longitude = sign * Double.parseDouble(line.substring(13, 18)) / 100.0;
        int altitude = Integer.parseInt(line.substring(18, 23));

        // Build the base object.
        AcarsObservation ret = new AcarsObservation(latitude, longitude, altitude, observed);

        // Temperature. No fractional degrees, so must be complete.
        int length = line.length();
        if (length < 26)
            return ret;
        ret.setTemperature(Float.parseFloat(line.substring(23, 26)));

        // Wind direction. Must be complete.
        if (length < 30)
            return ret;
        ret.setWindDirection(Short.parseShort(line.substring(27, 30)));

        // Wind speed. Must be complete.
        if (length < 33)
            return ret;
        ret.setWindSpeed(Short.parseShort(line.substring(30, 33)));
        return ret;
    }

    private AcarsObservation makeObsB(String line) {
        // Get the basic four coordinates of the observation. These will always
        // be present.
        Date observed = parseTime(line.substring(0, 6));
        double latitude = Double.parseDouble(line.substring(6, 13)) / 10000.0;
        double longitude = Double.parseDouble(line.substring(13, 21)) / 10000.0;
        int altitude = Integer.parseInt(line.substring(22, 27));

        // Build the base object.
        AcarsObservation ret = new AcarsObservation(latitude, longitude, altitude, observed);

        // Temperature. No fractional degrees, so must be complete.
        int length = line.length();
        if (length < 30)
            return ret;
        ret.setTemperature(Float.parseFloat(line.substring(27, 30)));

        // Wind direction. Must be complete.
        if (length < 34)
            return ret;
        ret.setWindDirection(Short.parseShort(line.substring(31, 34)));

        // Wind speed. Must be complete.
        if (length < 37)
            return ret;
        ret.setWindSpeed(Short.parseShort(line.substring(34, 37)));
        return ret;
    }

    private AcarsObservation makeObsC(String line) {
        // Get the basic four coordinates of the observation. These will always
        // be present.
        Date observed = parseTime(line.substring(0, 6));
        double sign = line.charAt(6) == 'N' ? 1.0 : -1.0;
        double latitude = sign * Double.parseDouble(line.substring(7, 11)) / 100.0;
        sign = line.charAt(11) == 'E' ? 1.0 : -1.0;
        double longitude = sign * Double.parseDouble(line.substring(12, 17)) / 100.0;
        int altitude = Integer.parseInt(line.substring(18, 23));

        // Build the base object.
        AcarsObservation ret = new AcarsObservation(latitude, longitude, altitude, observed);

        // Temperature. No fractional degrees, so must be complete.
        int length = line.length();
        if (length < 26)
            return ret;
        ret.setTemperature(Float.parseFloat(line.substring(23, 26)));

        // Wind direction. Must be complete.
        if (length < 30)
            return ret;
        ret.setWindDirection(Short.parseShort(line.substring(27, 30)));

        // Wind speed. Must be complete.
        if (length < 33)
            return ret;
        ret.setWindSpeed(Short.parseShort(line.substring(30, 33)));
        return ret;
    }

    /* we match the base hour, previous hours back 22, and 1 future
       hour */
    private void initBase(Date baseTime) {
        hours = new HashMap<Integer,GregorianCalendar>();
        GregorianCalendar base = new GregorianCalendar(ZONE);
        base.setTime(baseTime);
        base.set(GregorianCalendar.MILLISECOND, 0);
        base.add(GregorianCalendar.HOUR_OF_DAY, -22);
        for (int i=0; i<24; i++) {
            GregorianCalendar c = (GregorianCalendar) base.clone();
            c.add(GregorianCalendar.HOUR_OF_DAY, i);
            hours.put(c.get(GregorianCalendar.HOUR_OF_DAY), c);
        }
    }

    private Date parseTime(String hhmmss) {
        int hh = Integer.parseInt(hhmmss.substring(0, 2));
        int mm = Integer.parseInt(hhmmss.substring(2, 4));
        int ss = Integer.parseInt(hhmmss.substring(4, 6));
        GregorianCalendar ret = hours.get(hh);
        if (ret == null)
            throw new IllegalArgumentException("Observation not within supported window.");
        ret = (GregorianCalendar) ret.clone();
        ret.set(GregorianCalendar.MINUTE, mm);
        ret.set(GregorianCalendar.SECOND, ss);
        return ret.getTime();
    }
}
