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
 * Decoder for Frontier Airlines (F9). They encode observations in two
 * different ways, but appear to always use both (i.e. an observation
 * gets sent twice, once using one format, then using the other). We
 * only concern ourselves with one of the two formats.
 *
 * @author David Barts <n5jrn@me.com>
 */
public class DecoderForF9 extends WxDecoder {
    private static final TimeZone ZONE = TimeZone.getTimeZone("GMT");

    private HashMap<Integer,GregorianCalendar> hours;
    private static final Pattern MESSAGE = Pattern.compile("POS[NS][ \\d]{3}\\.\\d{3}[EW][ \\d]{3}\\.\\d{3}, *\\d+,\\d{6},\\d+, *\\d+, *\\d+,[ -] *\\d+,\\d{6},\\w{4}");

    private static final int LAT_LONG = 0;
    private static final int WIND_DIR = 1;
    private static final int HHMMSS = 2;
    private static final int ALTITUDE = 3;
    private static final int WIND_SPEED = 5;
    private static final int TEMPERATURE = 6;

    /**
     * Decode something.
     * @param message     An AcarsMessage.
     * @return            An Iterable<AcarsObservation>, or null
     */
    public Iterable<AcarsObservation> decode(IAcarsMessage message, Date baseTime)
    {
        // Frontier uses label 21 messages for observations.
        if (!message.getLabel().equals("21"))
            return null;

        // Only one observation per message!
        String body = message.getMessage().trim();
        if (!MESSAGE.matcher(body).matches())
            return null;

        initBase(baseTime);
        AcarsObservation obs = makeObs(body);
        if (obs == null)
            return null;
        ArrayList<AcarsObservation> ret = new ArrayList<AcarsObservation>(1);
        ret.add(obs);
        return ret;
    }

    private AcarsObservation makeObs(String line)
    {
        // Break the observation into its fields
        String[] fields = line.split(",");

        // Get the basic four coordinates of the observation. These will always
        // be present.
        double latitude = 0.0, longitude = 0.0;
        try {
            double sign = fields[LAT_LONG].charAt(3) == 'N' ? 1.0 : -1.0;
            latitude = sign *
                Double.parseDouble(fields[LAT_LONG].substring(4, 11).trim());
            sign = fields[LAT_LONG].charAt(11) == 'E' ? 1.0 : -1.0;
            longitude = sign *
                Double.parseDouble(fields[LAT_LONG].substring(12).trim());
        } catch (NumberFormatException e) {
            return null;
        }
        Date observed = parseTime(fields[HHMMSS]);
        if (observed == null)
            return null;
        int altitude = Integer.parseInt(fields[ALTITUDE]);

        // Build the base object.
        AcarsObservation ret = new AcarsObservation(latitude, longitude, altitude, observed);

        // Actually, all the fields will be here, because there should never
        // be runt observations in this format.
        ret.setTemperature(Float.parseFloat(fields[TEMPERATURE].replace(" ", "")));
        ret.setWindDirection(Short.parseShort(fields[WIND_DIR].trim()));
        ret.setWindSpeed(Short.parseShort(fields[WIND_SPEED].trim()));
        return ret;
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
        int mm = Integer.parseInt(hhmmss.substring(2, 4));
        int ss = Integer.parseInt(hhmmss.substring(4, 6));
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
