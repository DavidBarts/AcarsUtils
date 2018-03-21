package info.koosah.acarsutils;

/**
 * A class to help visualize what's in a java.lang.String containing ASCII
 * characters, by turning non-printable ASCII into "caret notation". Non-
 * ASCII gets passed as-is. This class uses StringBuilder so is not
 * thread-safe.
 * @author David Barts <n5jrn@me.com>
 */
public class CaretNotator {
    private static final String NL = System.lineSeparator();
    private StringBuilder buf;

    /**
     * Constructor.
     */
    public CaretNotator() {
        buf = new StringBuilder();
    }

    /**
     * Append a single character, uparrow-coding if needed.
     * @param c           Character to append.
     */
    public void append(char c) {
        if (c < '\040' || c == '\177') {
            buf.append('^');
            c ^= 0100;
        }
        buf.append(c);
    }

    /**
     * Append a string, up-arrow encoding characters as needed.
     * @param s           String to append.
     */
    public void append(String s) {
        int len = s.length();
        for (int i=0; i<len; i++)
            append(s.charAt(i));
    }

    /**
     * Append a system-dependent newline sequence, which is not
     * uparrow-encoded.
     */
    public void appendNewline() {
        buf.append(NL);
    }

    /**
     * Append a multi-line message. Newline sequences are recognized and
     * are mapped to the system-dependant newline sequence instead of being
     * uparrow-encoded. Tabs are simply passed verbatim.
     * @param m           Message to append
     */
    public void appendMultiline(String m) {
        int len = m.length();
        int last = len - 1;
        for (int i=0; i<len; i++) {
            char c = m.charAt(i);
            switch(c) {
            case '\t':
                /* tabs get passed verbatim */
                buf.append(c);
                break;
            case '\r':
                /* carriage return before line feed or at end gets deleted */
                if (i != last && m.charAt(i+1) != '\n')
                    append(c);
                break;
            case '\n':
                /* delete LF at end, map others to newline */
                if (i != last)
                    appendNewline();
                break;
            default:
                append(c);
                break;
            }
        }
        appendNewline();
    }

    /**
     * Append a string, doing no uparrow-coding whatsoever.
     * @param s           String to append.
     */
    public void appendRaw(String s) {
        buf.append(s);
    }

    /**
     * Retrieve the printable string.
     * @return      The string.
     */
    public String toString() {
        return buf.toString();
    }
}
