package name.blackcap.acarsutils.wxdecoder;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.blackcap.acarsutils.*;

/**
 * Decoder for American Airlines (AA).
 *
 * @author David Barts <n5jrn@me.com>
 */
public class DecoderForAA extends WxDecoder {
    private static final TimeZone ZONE = TimeZone.getTimeZone("GMT");

    private GregorianCalendar[] daysToTry;
    private static final String PREFIX = "#DFBA3";
    private static final int PRE_LENGTH = 22;
    private static final int OBS_LENGTH = 32;
    private static final Pattern AA_PRE = Pattern.compile("#DFBA3.{2}\\d{6}[A-Z]{3}[A-Z ][A-Z]{3}[A-Z ]");
    private static final Pattern AA_OBS = Pattern.compile("[NS]\\d{5}[EW]\\d{10}[PM]\\d{9}[A-Z]\\d{4}");

    /**
     * Decode something.
     * @param message     An AcarsMessage.
     * @return            An Iterable<AcarsObservation>, or null
     */
    public Iterable<AcarsObservation> decode(IAcarsMessage message, Date baseTime) {
        // American uses H1 messages that start with a specific prefix for
        // their weather reports.
        if (!message.getLabel().equals("H1") || !message.getMessage().startsWith(PREFIX))
            return null;

        // Get message length and verify it's more than just a bare preamble.
        int len = message.getMessage().length();
        if (len <= PRE_LENGTH)
            return null;

        // Get preamble, verify it looks valid, then get time stamp from it.
        // The time *may* be bad in a "fooler" message.
        String preamble = message.getMessage().substring(0, PRE_LENGTH);
        if (!AA_PRE.matcher(preamble).matches())
            return null;
        initBase(baseTime);
        Date date = null;
        try {
            date = parseTime(preamble.substring(8, 14));
        } catch (IllegalArgumentException e) {
            return null;
        }

        // Throw out the initial preamble and remove any \r or \n
        // characters (AA's systems apparently put the latter in just to
        // limit line length; they signify no meaning). Actually we
        // just throw away all ASCII control characters.
        StringBuilder buf = new StringBuilder(Math.max(16, len - PRE_LENGTH));
        for(int i = PRE_LENGTH; i < len; i++) {
            char ch = message.getMessage().charAt(i);
            if (ch >= ' ')
                buf.append(ch);
        }

        // Break the message into OBS_LENGTH size chunks; each one is an
        // observation. Note that an occasional message looks a lot at first
        // like observations, but isn't, so we do pattern matching to verify.
        boolean found = false;
        ArrayList<AcarsObservation> ret = new ArrayList<AcarsObservation>();
        int nobs = buf.length() / OBS_LENGTH;
        for (int i = 0; i < nobs; i++) {
            String raw = buf.substring(i * OBS_LENGTH, (i + 1) * OBS_LENGTH);
            if (!AA_OBS.matcher(raw).matches())
                continue;
            AcarsObservation obs = makeObs(date, raw);
            if (obs != null) {
                found = true;
                ret.add(obs);
            }
        }

        return found ? ret : null;
    }

    private AcarsObservation makeObs(Date observed, String line) {
        // Get the basic four coordinates of the observation. These will always
        // be present.
        double sign = line.charAt(0) == 'N' ? 1.0 : -1.0;
        double latitude = sign * Double.parseDouble(line.substring(1, 6)) / 1000.0;
        sign = line.charAt(6) == 'E' ? 1.0 : -1.0;
        double longitude = sign * Double.parseDouble(line.substring(7, 13)) / 1000.0;
        int altitude = Integer.parseInt(line.substring(13, 17)) * 10;

        // Build the base object.
        AcarsObservation ret = new AcarsObservation(latitude, longitude, altitude, observed);

        // Temperature.
        float tsign = line.charAt(17) == 'P' ? 1.0f : -1.0f;
        ret.setTemperature(tsign * Float.parseFloat(line.substring(18, 20)));

        // Wind direction and speed
        ret.setWindDirection(Short.parseShort(line.substring(21, 24)));
        ret.setWindSpeed(Short.parseShort(line.substring(24, 27)));
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
