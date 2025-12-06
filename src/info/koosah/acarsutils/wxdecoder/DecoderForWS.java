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
 * Decoder for WestJet (WS).
 *
 * @author David Barts <n5jrn@me.com>
 */
public class DecoderForWS extends WxDecoder {
    private static final TimeZone ZONE = TimeZone.getTimeZone("GMT");

    private HashMap<Integer,GregorianCalendar> hours;
    // this fails to match negative altitudes, which is a feature, because
    // observations with those are junk
    private static final Pattern OBSERVATION =
        Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{2},\\w{2},\\d{4},\\d{5},\\d{3}\\.\\d,\\.\\d+,[-\\d]\\d{2}\\.\\d,[-\\d]\\d{2}\\.\\d,[NS]\\d{4}\\.\\d,[EW]\\d{5}\\.\\d,\\d{6}");

    private static final int HHMMSS = 0;
    private static final int ALTITUDE = 3;
    private static final int TEMPERATURE = 6;
    private static final int LATITUDE = 8;
    private static final int LONGITUDE = 9;

    /**
     * Decode something.
     * @param message     An AcarsMessage.
     * @return            An Iterable<AcarsObservation>, or null
     */
    public Iterable<AcarsObservation> decode(IAcarsMessage message, Date baseTime)
    {
        // WestJet uses label H1 messages from source DF for observations.
        if (!message.getLabel().equals("H1") || !message.getSource().equals("DF")) {
            return null;
        }

        // Observation messages have multiple lines, but only one line has
        // observation data. But just to be sure we allow multiple such lines.
        initBase(baseTime);
        String[] lines = message.getMessage().split("\\r?\\n");
        ArrayList<AcarsObservation> ret = new ArrayList<AcarsObservation>();
        for (String line: lines) {
            if (OBSERVATION.matcher(line).matches()) {
                addObs(ret, line);
            }
        }

        return ret.size() == 0 ? null : ret;
    }

    private void addObs(ArrayList<AcarsObservation> accum, String line)
    {
        // Break the observation into its fields
        String[] fields = line.split(",");

        // Get the basic four coordinates of the observation.
        double latitude = 0.0, longitude = 0.0;
        try {
            double sign = fields[LATITUDE].charAt(0) == 'N' ? 1.0 : -1.0;
            latitude = sign *
                Double.parseDouble(fields[LATITUDE].substring(1)) / 100.0;
            sign = fields[LONGITUDE].charAt(0) == 'E' ? 1.0 : -1.0;
            longitude = sign *
                Double.parseDouble(fields[LONGITUDE].substring(1)) / 100.0;
        } catch (NumberFormatException e) {
            return;
        }
        Date observed = parseTime(fields[HHMMSS]);
        if (observed == null)
            return;
        int altitude = Integer.parseInt(fields[ALTITUDE]);

        // Build the base object.
        AcarsObservation ret = new AcarsObservation(latitude, longitude, altitude, observed);
        ret.setTemperature(Float.parseFloat(fields[TEMPERATURE]));
        accum.add(ret);
    }

    /* we match the base hour, previous hours back 22, and 1 future
       hour */
    private void initBase(Date baseTime)
    {
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

    private Date parseTime(String hhmmss)
    {
        int hh = Integer.parseInt(hhmmss.substring(0, 2));
        int mm = Integer.parseInt(hhmmss.substring(3, 5));
        int ss = Integer.parseInt(hhmmss.substring(6, 8));
        if (hh > 23 || mm > 59 || ss > 59)
            return null;
        GregorianCalendar ret = hours.get(hh);
        if (ret == null)
            throw new IllegalArgumentException("Observation not within supported window.");
        ret = (GregorianCalendar) ret.clone();
        ret.set(GregorianCalendar.MINUTE, mm);
        ret.set(GregorianCalendar.SECOND, ss);
        return ret.getTime();
    }
}
