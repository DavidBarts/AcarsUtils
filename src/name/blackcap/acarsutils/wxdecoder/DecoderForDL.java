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
 * Decoder for Delta Airlines (DL).
 *
 * @author David Barts <n5jrn@me.com>
 */
public class DecoderForNW extends WxDecoder {
    private static final TimeZone ZONE = TimeZone.getTimeZone("GMT");

    private HashMap<Integer,GregorianCalendar> hours;
    private static final Pattern FULL    = Pattern.compile("[NS]\\d{5}[EW]\\d{14}[MP]\\d{2}.\\d{6}.{14}");
    private static final Pattern PARTIAL = Pattern.compile("[NS]\\d{5}[EW]\\d{14}[MP]\\d{2}");

    /**
     * Decode something.
     * @param message     An AcarsMessage.
     * @return            An Iterable<AcarsObservation>, or null
     */
    public Iterable<AcarsObservation> decode(IAcarsMessage message, Date baseTime) {
        // Delta uses H1 messages from source DF for its observations. If
        // this is not that sort of message, it cannot be an observation.
        String body = message.getMessage();
        if (!message.getLabel().equals("H1") || !message.getSource().equals("DF")) {
            return null;
        }

        // Split the message body into lines.
        String[] lines = message.getMessage().split("\\r?\\n");

        // We simply assume any H1/DF message might contain weather reports
        // and treat anything that looks like one as a report.
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
        int altitude = Integer.parseInt(line.substring(17, 21)) * 10;

        // If the timestamp is mangled, silently ignore this observation
        if (observed == null)
            return null;

        // Build the base object.
        AcarsObservation ret = new AcarsObservation(latitude, longitude, altitude, observed);

        // Temperature. No fractional degrees, so must be complete.
        int length = line.length();
        if (length < 24)
            return ret;
        int isign = 0;
        switch (line.charAt(21)) {
        case 'M':
            isign = -1;
            break;
        case 'P':
            isign = 1;
            break;
        default:
            return null;
        }
        ret.setTemperature((float) (isign * Integer.parseInt(line.substring(22, 24)));

        // Wind direction. Must be complete.
        if (length < 28)
            return ret;
        ret.setWindDirection(Short.parseShort(line.substring(25, 28)));

        // Wind speed. Must be complete.
        if (length < 31)
            return ret;
        ret.setWindSpeed(Short.parseShort(line.substring(28, 31)));
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
        /* Delta observations sometimes have mangled timestamps. We ignore
           those. */
        if (hh < 0 || hh > 23 || mm < 0 || mm > 59)
            return null;
        GregorianCalendar ret = hours.get(hh);
        if (ret == null)
            throw new IllegalArgumentException("Observation not within supported window.");
        ret = (GregorianCalendar) ret.clone();
        ret.set(GregorianCalendar.MINUTE, mm);
        return ret.getTime();
    }
}
