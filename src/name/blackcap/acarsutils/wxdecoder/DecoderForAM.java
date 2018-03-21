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
 * Decoder for Aeroméxico (AM).
 *
 * @author David Barts <n5jrn@me.com>
 */
public class DecoderForAM extends WxDecoder {
    private static final TimeZone ZONE = TimeZone.getTimeZone("GMT");

    private HashMap<Integer,GregorianCalendar> hours;
    private static final int PRE_LENGTH = 13;
    private static final String DELIM = "    ";
    private static final Pattern AM_PRE = Pattern.compile(".{5}[A-Z]{8}");
    private static final Pattern AM_OBS = Pattern.compile("Q[NS]\\d{5}[EW]\\d{10}[\\d ]{3}\\d[PM]\\d{9}G");

    /**
     * Decode something.
     * @param message     An AcarsMessage.
     * @return            An Iterable<AcarsObservation>, or null
     */
    public Iterable<AcarsObservation> decode(IAcarsMessage message, Date baseTime) {
        // Aeroméxico uses the offical ACARS message label for weather
        // observations (H2). Not many airlines do.
        if (!message.getLabel().equals("H2"))
            return null;

        // Get message length and verify it's more than just a bare preamble.
        int len = message.getMessage().length();
        if (len <= PRE_LENGTH)
            return null;

        // Get preamble, verify it looks valid.
        String preamble = message.getMessage().substring(0, PRE_LENGTH);
        if (!AM_PRE.matcher(preamble).matches())
            return null;

        // Break it into observations and parse all that seem to be of the
        // useful type. There are two variants, one with timestamps (useful)
        // and ones without (useless).
        ArrayList<AcarsObservation> ret = new ArrayList<AcarsObservation>();
        String body = "Q" + message.getMessage().substring(PRE_LENGTH);
        initBase(baseTime);
        for (String raw : body.split(DELIM)) {
            if (!AM_OBS.matcher(raw).matches())
                continue;
            ret.add(makeObs(raw));
        }

        // Because type H2 uniquely identifies WX obs, it's better to return
        // an empty object than null if none were found.
        return ret;
    }

    private AcarsObservation makeObs(String line) {
        // Get the basic four coordinates of the observation. These will always
        // be present.
        double sign = line.charAt(1) == 'N' ? 1.0 : -1.0;
        double latitude = sign * Double.parseDouble(line.substring(2, 7)) / 1000.0;
        sign = line.charAt(7) == 'E' ? 1.0 : -1.0;
        double longitude = sign * Double.parseDouble(line.substring(8, 14)) / 1000.0;
        Date observed = parseTime(line.substring(14, 18));
        int altitude = Integer.parseInt(line.substring(18, 22).trim()) * 10;

        // Build the base object.
        AcarsObservation ret = new AcarsObservation(latitude, longitude, altitude, observed);

        // Temperature.
        float tsign = line.charAt(22) == 'P' ? 1.0f : -1.0f;
        ret.setTemperature(tsign * Float.parseFloat(line.substring(23, 25)));

        // Wind direction and speed
        ret.setWindDirection(Short.parseShort(line.substring(26, 29)));
        ret.setWindSpeed(Short.parseShort(line.substring(29, 32)));
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
