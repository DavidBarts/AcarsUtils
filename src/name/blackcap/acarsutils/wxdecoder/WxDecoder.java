package info.koosah.acarsutils.wxdecoder;

import java.util.Date;
import java.util.HashMap;
import info.koosah.acarsutils.AcarsObservation;
import info.koosah.acarsutils.IAcarsMessage;

/**
 * This is the core class of the acarsutils.wxdecoder package, which
 * decodes ACARS weather observations. Because there is no standard for
 * such messages, (officially there is, but the airlines all violate it
 * and use their own proprietary messages), the rules for how to do so
 * vary from airline to airline, so it's best to have each airline's
 * decoder be in its own class. Which is what we do.
 *
 * Derived classes must all be in this package and have names of the
 * form DecoderForXX, where XX is the 2-letter IATA code for the airline.
 *
 * @author David Barts <n5jrn@me.com>
 */
abstract public class WxDecoder implements Comparable<WxDecoder> {
    /**
     * Constructor. Not intended to be called by "mere mortals".
     */
    protected WxDecoder() {}

    /* for efficiency, we cache these. */
    private static HashMap<String,Class> cache = new HashMap<String,Class>();

    /* All codes and flight be these lengths. */
    private static final int CODE_LENGTH = 2;

    /**
     * The exception we throw when we fail to loop up a syntactically valid
     * airline code.
     */
    public static class UnknownAirlineException extends Exception {
        public UnknownAirlineException(String message) {
            super(message);
        }
    }

    /**
     * This the way to get a decoder for a given airline. The passed name
     * is either an IATA airline code or an IATA flight number (the latter
     * begin with the former).
     *
     * @param name        Airline or flight name (see above)
     * @return            A decoder for the airline in question
     * @throws UnknownAirlineException If there is no known decoder for
     *                    the specified airline or flight.
     * @throws IllegalArgumentException If the passed name is null or syntactically invalid.
     */
    public static WxDecoder forName(String name)
                 throws UnknownAirlineException, IllegalArgumentException {
        // Reject bad names
        if (name == null)
            throw new IllegalArgumentException("Name must not be null.");
        for (int i=0; i<name.length(); i++) {
            if (name.charAt(i) >= 128)
                throw new IllegalArgumentException("Name must contain only ASCII characters.");
        }
        if (name.length() < CODE_LENGTH)
            throw new IllegalArgumentException("Invalid airline or flight code length.");

        // Canonicalize the name into an IATA airline code, rejecting
        // bad codes.
        String canonical = name.substring(0, CODE_LENGTH).toUpperCase();
        for (int i=0; i<CODE_LENGTH; i++) {
            char ch = canonical.charAt(i);
            if (!Character.isDigit(ch) && !Character.isAlphabetic(ch))
                throw new IllegalArgumentException("Airline code must be alphanumeric.");
        }

        // If it's cached, avoid the hard work.
        boolean inCache = false;
        Class retClass = null;
        synchronized (cache) {
            inCache = cache.containsKey(canonical);
            if (inCache)
                retClass = cache.get(canonical);
        }
        if (inCache) {
            if (retClass == null)
                throw new UnknownAirlineException("Unknown airline code: " + canonical);
            else
                return createDecoder(retClass);
        }

        // Else gotta get the class from the class loader.
        try {
            retClass = Class.forName(WxDecoder.class.getPackage().getName() + ".DecoderFor" + canonical);
        } catch (ClassNotFoundException e) {
            synchronized (cache) {
                cache.put(canonical, null);  // remember bad ones, too
            }
            throw new UnknownAirlineException("Unknown airline code: " + canonical);
        }
        synchronized (cache) {
            cache.put(canonical, retClass);
        }
        return createDecoder(retClass);
    }

    private static WxDecoder createDecoder(Class c) {
        try {
            return (WxDecoder) c.newInstance();
        } catch (IllegalAccessException|InstantiationException|ExceptionInInitializerError e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Decode an ACARS message into one or more weather observations. If the
     * message is not the right type for a weather observation, return null.
     * If the message is an empty weather observation message, return an
     * empty Iterable.
     *
     * @param message     An AcarsMessage
     * @param baseTime    Absolute time to base any relative timestamps on.
     * @return            An Iterable<AcarsObservation>, or null
     */
    public abstract Iterable<AcarsObservation> decode(IAcarsMessage message, Date baseTime);

    /**
     * Convenience method, based on current date and time.
     * @param message     An AcarsMessage
     * @param baseTime    Absolute time to base any relative timestamps on.
     * @return            An Iterable<AcarsObservation>, or null
     */
    public Iterable<AcarsObservation> decode(IAcarsMessage message) {
        return decode(message, new Date());
    }

    /**
     * Get the name of this decoder. For any WxDecoder object x, calling
     * WxDecoder.forName(x.getName()) should be equivalent to calling
     * x.getClass().getInstance(), i.e. it should return a fresh instance
     * of that type of decoder.
     * @return            A string containing the decoder name.
     */
    public String name() {
        String s = getClass().getName();
        return s.substring(s.length() - 2);
    }

    /**
     * Compare this decoder to another one. They match if their names do.
     * @param o           WxDecoder object to compare this one to.
     */
    public int compareTo(WxDecoder o) {
        return name().compareTo(o.name());
    }
}
