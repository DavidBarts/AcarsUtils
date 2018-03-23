package info.koosah.acarsutils;

/**
 * Defines standard getters for the basic parts of an ACARS message.
 * @author David Barts <n5jrn@me.com>
 */
public interface IAcarsMessage {
    /* aircraft registration (aka tail number) */
    public String getRegistration();

    public String getFlightId();

    public String getLabel();

    public char getMode();

    public char getBlockId();

    public char getAcknowledge();

    public String getMessageId();

    /* for H1 labeled messages only */
    public String getSource();

    /* the body of the message */
    public String getMessage();
}
