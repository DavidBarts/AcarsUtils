package info.koosah.acarsutils;

/**
 * Allows us to spoof an ACARS message, mainly for the purpose of
 * testing decoders.
 */
public class FakeAcarsMessage implements IAcarsMessage, Cloneable {
    private String registration;
    public String getRegistration() {
        return registration;
    }
    public FakeAcarsMessage setRegistration(String value) {
        this.registration = value;
        return this;
    }

    private String flightId;
    public String getFlightId() {
        return flightId;
    }
    public FakeAcarsMessage setFlightId(String value) {
        this.flightId = value;
        return this;
    }

    private String label;
    public String getLabel() {
        return label;
    }
    public FakeAcarsMessage setLabel(String value) {
        this.label = value;
        return this;
    }

    private char mode;
    public char getMode() {
        return mode;
    }
    public FakeAcarsMessage setMode(char value) {
        this.mode = value;
        return this;
    }

    private char blockId;
    public char getBlockId() {
        return blockId;
    }
    public FakeAcarsMessage setBlockId(char value) {
        this.blockId = value;
        return this;
    }

    private char acknowledge;
    public char getAcknowledge() {
        return acknowledge;
    }
    public FakeAcarsMessage setAcknowledge(char value) {
        this.acknowledge = value;
        return this;
    }

    private String messageId;
    public String getMessageId() {
        return messageId;
    }
    public FakeAcarsMessage setMessageId(String value) {
        this.messageId = value;
        return this;
    }

    private String source;
    public String getSource() {
        return source;
    }
    public FakeAcarsMessage setSource(String value) {
        this.source = value;
        return this;
    }

    private String message;
    public String getMessage() {
        return message;
    }
    public FakeAcarsMessage setMessage(String value) {
        this.message = value;
        return this;
    }

    public FakeAcarsMessage() {
        registration = flightId = label = source = message = null;
        mode = blockId = acknowledge = 0;
    }

    public FakeAcarsMessage clone() {
        FakeAcarsMessage ret = new FakeAcarsMessage();
        ret.registration = registration;
        ret.flightId = flightId;
        ret.label = label;
        ret.mode = mode;
        ret.blockId = blockId;
        ret.acknowledge = acknowledge;
        ret.messageId = messageId;
        ret.source = source;
        ret.message = message;
        return ret;
    }
}
