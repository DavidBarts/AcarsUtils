package info.koosah.acarsutils.wxdecoder;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import info.koosah.acarsutils.*;

/**
 * Decoder for Alaska Airlines (AS).
 *
 * @author David Barts <n5jrn@me.com>
 */
public class DecoderForAS extends WxDecoder {
    private static final TimeZone ZONE = TimeZone.getTimeZone("GMT");

    private GregorianCalendar[] daysToTry;
    private static final Pattern FULL    = Pattern.compile("[NS]\\d{2}\\.\\d{4}.[EW]\\d{3}\\.\\d{4}\\.\\d{6}\\.#\\d{5}\\.[-+\\d]\\d{2}\\.\\d\\.\\d{3}\\.\\d{3}\\.[A-Z]{2}#");
    private static final Pattern PARTIAL = Pattern.compile("[NS]\\d{2}\\.\\d{4}.[EW]\\d{3}\\.\\d{4}\\.\\d{6}\\.#\\d{5}\\.[-+\\d]\\d{2}");

    /**
     * Decode something.
     * @param message     An AcarsMessage.
     * @return            An Iterable<AcarsObservation>, or null
     */
    public Iterable<AcarsObservation> decode(IAcarsMessage message, Date baseTime) {
        // Alaska uses H1 messages from source DF for its observations. If
        // this is not that sort of message, it cannot be an observation.
        if (!message.getLabel().equals("H1") || !message.getSource().equals("DF")) {
            return null;
        }

        // Split the message body into lines.
        String[] lines = message.getMessage().split("\\r?\\n");

        // Messages often contain "runt observations" at the end, because
        // Alaska's firmware is too stupid to know the maximum ACARS message
        // length and truncation often results. So we look for a complete
        // observation to start with then try to parse out as many usable
        // observations as we can.
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

        return found ? ret : null;
    }

    private AcarsObservation makeObs(String line) {
        // Get the basic four coordinates of the observation. These will always
        // be present.
        double sign = line.charAt(0) == 'N' ? 1.0 : -1.0;
        double latitude = sign * Double.parseDouble(line.substring(1, 8));
        sign = line.charAt(9) == 'E' ? 1.0 : -1.0;
        double longitude = sign * Double.parseDouble(line.substring(10, 18));
        Date observed = parseTime(line.substring(19, 25));
        int altitude = Integer.parseInt(line.substring(27, 32));

        // Build the base object.
        AcarsObservation ret = new AcarsObservation(latitude, longitude, altitude, observed);

        // Temperature. OK if it's missing fractional degrees.
        int length = line.length();
        String raw = null;
        if (length < 38)
            raw = line.substring(33);
        else
            raw = line.substring(33, 38);
        ret.setTemperature(Float.parseFloat(raw));

        // Wind direction. Must be complete.
        if (length < 42)
            return ret;
        ret.setWindDirection(Short.parseShort(line.substring(39, 42)));

        // Wind speed. Must be complete.
        if (length < 46)
            return ret;
        ret.setWindSpeed(Short.parseShort(line.substring(43, 46)));
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

    private Date parseTime(String ddhhmm) {
        int dd = Integer.parseInt(ddhhmm.substring(0, 2));
        int hh = Integer.parseInt(ddhhmm.substring(2, 4));
        int mm = Integer.parseInt(ddhhmm.substring(4, 6));
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
