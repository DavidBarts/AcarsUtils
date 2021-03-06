package info.koosah.acarsutils.wxdecoder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import info.koosah.acarsutils.AcarsObservation;
import info.koosah.acarsutils.FakeAcarsMessage;

/**
 * Tests for all weather decoders are here, because we want to test decoders
 * with packets from other airlines.
 */
public class WxDecoderTest {
    /* time stamps, chosen so we can exercise end-of-year rollaround */
    private static final SimpleDateFormat UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    static {
        UTC.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    private static final Date OLD_YEAR = parseDate("2017-12-31T23:58:26Z");
    private static final Date NEW_YEAR = parseDate("2018-01-01T00:08:43Z");

    /* messages; observations must be crafted so as to be on 31 Dec
       near midnight */
    private FakeAcarsMessage asObs, asNonObs, asAck;
    private FakeAcarsMessage wnObs, wnNonObs, wnAck;
    private FakeAcarsMessage nwObs, nwNonObs, nwAck;
    private FakeAcarsMessage fxObsA, fxObsB, fxObsC, fxNonObs, fxAck;
    private FakeAcarsMessage dlObs, dlNonObs, dlBadObs;
    private FakeAcarsMessage aaObs, aaNonObs, aaAck;
    private FakeAcarsMessage amObs1, amObs2, amBadObs, amNonObs;
    private FakeAcarsMessage f9Obs, f9NonObs, f9Ack;
    private FakeAcarsMessage acObs, acNonObs, rvObs, rvNonObs;

    private FakeAcarsMessage[] allMessages;

    @Before
    public void getFreshVars() {
        asObs = new FakeAcarsMessage();
        asObs.setRegistration(".N794AS").setFlightId("AS0066").setLabel("H1")
            .setMode('2').setBlockId('6').setAcknowledge('\u0015')
            .setMessageId("D38B").setSource("DF")
            .setMessage("#DFB.2.224.059.CR#\r\n" +
                "N48.8162.W123.5620.312343.#35004.-50.5.227.055.CR#\r\n" +
                "N48.6191.W123.3835.312345.#34996.-49.7.227.053.CR#\r\n" +
                "N48.4227.W123.2013.312347.#31824.-44.2.216.046.DC#");

        asNonObs = new FakeAcarsMessage();
        asNonObs.setRegistration(".N627AS").setFlightId("AS7003").setLabel("H1")
            .setMode('2').setBlockId('0').setAcknowledge('\u0015')
            .setMessageId("D69A").setSource("DF")
            .setMessage("#DFB\r\nTRP#201115#047.6906#-122.3152#048#109.5#\r\n" +
                "239#0007314#0.03514#0011.00#0248.20#\r\n" +
                "01.127#00.901#00.010#-0.006#008#03#00#01#\r\n225#8#0003731#");

        asAck = new FakeAcarsMessage();
        asAck.setRegistration(".N627AS").setFlightId("AS7003").setLabel("_\u007f")
            .setMode('2').setBlockId('7').setAcknowledge('C')
            .setMessageId("S23A");

        wnObs = new FakeAcarsMessage();
        wnObs.setRegistration(".N496WN").setFlightId("WN3289").setLabel("H1")
            .setMode('2').setBlockId('3').setAcknowledge('\u0015')
            .setMessageId("D43B").setSource("DF")
            .setMessage("#DFB23.8,021,003,TO,11931,1,\r\n" +
                "N4727.0,W12218.4,312350,01353,024.2,047,005,TO,11582,1,\r\n" +
                "N4728.0,W12218.4,312350,01623,024.2,038,007,TO,11146,1,\r\n" +
                "N4728.0,W12218.4,312350,01854,024.0,037,009,TO,09895,1,\r\n:");

        wnNonObs = new FakeAcarsMessage();
        wnNonObs.setRegistration(".N288WN").setFlightId("WN0657").setLabel("H1")
            .setMode('2').setBlockId('3').setAcknowledge('\u0015')
            .setMessageId("D07A").setSource("DF")
            .setMessage("#DFB74102,288,B737-700,171007,WN0657,KSMF,KSEA,1265,SW716F\r\n" +
                "19.58.25,CR,0356,40010,239.4,.792,-55.8,-28.5,N4227.0,W12212.4,114920\r\n" +
                "5000,01.2\r\n" +
                "086.59,091.3,651.0,2219,0061,13.8,44,106,001.8\r\n" +
                "086.62,091.4,618.0,2");

        wnAck = new FakeAcarsMessage();
        wnAck.setRegistration(".N288WN").setFlightId("WN0657").setLabel("_\u007f")
            .setMode('2').setBlockId('7').setAcknowledge('C')
            .setMessageId("S23A");

        nwObs = new FakeAcarsMessage();
        nwObs.setRegistration(".N548US").setFlightId("NW1598").setLabel("H1")
            .setMode('2').setBlockId('9').setAcknowledge('\u0015')
            .setMessageId("D78A").setSource("DF")
            .setMessage("#DFB*WXRN47256W1212922348184-13022002400\r\n" +
                "N47255W1212152349199-16521702300\r\n" +
                "N47254W1211662350208-19022102200");

        nwNonObs = new FakeAcarsMessage().setRegistration(".N828NW")
            .setFlightId("NW0034").setMode('2').setLabel("H1").setBlockId('7')
            .setAcknowledge('\u0015').setMessageId("C98A").setSource("CF")
            .setMessage("#CFB.1/FLR/FR1710072050  24000006ECMU1  1,,,,,,,POWER SUPPLY INTERRUPT,INTERMITTENT");

        nwAck = new FakeAcarsMessage();
        nwAck.setRegistration(".N548US").setFlightId("NW1598").setLabel("_\u007f")
            .setMode('2').setBlockId('7').setAcknowledge('C')
            .setMessageId("S23A");

        fxObsA = new FakeAcarsMessage();
        fxObsA.setRegistration(".N674FE").setFlightId("FX1351").setLabel("H1")
            .setMode('2').setBlockId('9').setAcknowledge('\u0015')
            .setMessageId("D53A").setSource("DF")
            .setMessage("#DFBR12/N674FE27JUN010735135143109-120491612\r\n" +
                "/235717N4738W1223003374+1342850050\r\n" +
                "/235723N4738W1223003880+1313440080\r\n" +
                "/235729N4737W1223004286+1313430070\r\n" +
                "/235735N4736W1223004705+1342870060");

        fxObsB = new FakeAcarsMessage();
        fxObsB.setRegistration(".N998FD").setFlightId("FX6969").setLabel("H1")
            .setMode('2').setBlockId('0').setAcknowledge('\u0015')
            .setMessageId("D05A").setSource("DF")
            .setMessage("#DFB5WT4062914510079-164206596891460000414\r\n" +
                "2350280468800-1220479020659-1643590220\r\n" +
                "2351030468127-1220032022209-1903540320\r\n" +
                "2351380467425-1219567023607-2243450330\r\n" +
                "2351510467161-1219392024179-2393430340");

        fxObsC = new FakeAcarsMessage();
        fxObsC.setRegistration(".N560FE").setFlightId("FX1802").setLabel("H1")
            .setMode('2').setBlockId('2').setAcknowledge('\u0015')
            .setMessageId("D85A").setSource("DF")
            .setMessage("#DFB5WT4062914510079-164206596891460000414\r\n" +
                "235028N4737W12224007043-0483500060\r\n" +
                "235103N4737W12227008522-0790220030\r\n" +
                "235138N4736W12230009905-1003410120\r\n" +
                "235151N4734W12233010620-1133390170");

        fxNonObs = new FakeAcarsMessage();
        fxNonObs.setRegistration(".N998FD").setFlightId("FX6969").setLabel("H1")
            .setMode('2').setBlockId('0').setAcknowledge('\u0015')
            .setMessageId("D05A").setSource("DF")
            .setMessage("#DFB5WT NOT AN OBSERVATION!");

        fxAck = new FakeAcarsMessage();
        fxAck.setRegistration(".N998FD").setFlightId("FX6969").setLabel("_\u007f")
            .setMode('2').setBlockId('7').setAcknowledge('C')
            .setMessageId("S23A");

        dlObs = new FakeAcarsMessage();
        dlObs.setRegistration(".N1610D").setFlightId("DL0589").setLabel("H1")
            .setMode('7').setBlockId('4').setAcknowledge('\u0015')
            .setMessageId("D84A").setSource("DF")
            .setMessage("#DFB\r\n" +
                "N47628W12288323552151M3402250590XXXX24::RGYY(\r\n" +
                "N47714W12298303062348M3872230570XXXX2400B0/9W");

        dlBadObs = new FakeAcarsMessage();
        dlBadObs.setRegistration(".N1611B").setFlightId("DL0129").setLabel("H1")
            .setMode('2').setBlockId('7').setAcknowledge('\u0015')
            .setMessageId("D27B").setSource("DF")
            .setMessage("#DFB/WX02EN--KSEAZSPD\r\n" +
                "N47347W12230831630402P0472600260XXXX2600ZOA-P");

        dlNonObs = new FakeAcarsMessage();
        dlNonObs.setRegistration(".N1612T").setFlightId("DL0588").setLabel("H1")
            .setMode('2').setBlockId('0').setAcknowledge('\u0015')
            .setMessageId("D25A").setSource("DF")
            .setMessage("#DFB239N1612 0588111617154817301 4761-12232 34  5  0206 21AK-196 14320\r\n" +
                " 187 177 0961543100B8W/XZSPDKSEA");

        aaObs = new FakeAcarsMessage();
        aaObs.setRegistration(".N955AN").setFlightId("AA1624").setLabel("H1")
            .setMode('2').setBlockId('4').setAcknowledge('\u0015')
            .setMessageId("D20A").setSource("DF")
            .setMessage("#DFBA3E6312355SEA ORD N47254W1211872042M327210016G0009N47254W121154214\r\n" +
                "2M352188021G0009N47253W1211212219M380188020G0009N47253W1210882279M395\r\n" +
                "196020G0009N47252W1210552329M410201024G0009A2006");

        aaNonObs = new FakeAcarsMessage();
        aaNonObs.setRegistration(".N974AN").setFlightId("AA0208").setLabel("H1")
            .setMode('2').setBlockId('8').setAcknowledge('\u0015')
            .setMessageId("D09A").setSource("DF")
            .setMessage("#DFBA3M1161644SEA PHX 0284P005222028G00090300P002224029G00090323M00022\r\n" +
                "4026G00090351M005220026G00090375M010219023G00090401M012227019G0009042\r\n" +
                "7M017227017G00090457M025227018G0009/A2006");

        aaAck = new FakeAcarsMessage();
        aaAck.setRegistration(".N931AN").setFlightId("AA2261").setLabel("_\u007f")
            .setMode('2').setBlockId('2').setAcknowledge('W')
            .setMessageId("S07A");

        amObs1 = new FakeAcarsMessage();
        amObs1.setRegistration(".EI-DRC").setFlightId("AM0494").setLabel("H2")
            .setMode('2').setBlockId('0').setAcknowledge('\u0015')
            .setMessageId("M61A")
            .setMessage("02D15MMMXKSEAN47061W12134923501843M257226040G    " +
                "QN47090W12141223511742M252234039G    " +
                "QN47117W12147423521578M212232040G    " +
                "QN47145W12153323531453M182226042G    " +
                "QN47171W12159423541363M160216040G    QN47200W12205");

        amObs2 = new FakeAcarsMessage();
        amObs2.setRegistration(".EI-DRC").setFlightId("AM0494").setLabel("H2")
            .setMode('2').setBlockId('4').setAcknowledge('\u0015')
            .setMessageId("M63A")
            .setMessage("02D15MMMXKSEAN47360W1222902350 706M037198054G    " +
                "QN47405W1222882351 621M025196057G    " +
                "QN47447W1222872352 514M012186051G    " +
                "QN47473W1222522353 500M007183053G    " +
                "QN47471W1222122354 413P017180053G    QN47451W12220");

        amBadObs = new FakeAcarsMessage();
        amBadObs.setRegistration(".XA-ZAM").setFlightId("AM0495").setLabel("H2")
            .setMode('2').setBlockId('0').setAcknowledge('\u0015')
            .setMessageId("M98C")
            .setMessage("41007G    N47367W122233 497P105202011G    " +
                "N47371W122252 533P100170008G    " +
                "N47369W122273 599P092147011G    " +
                "N47357W122286 674P072126010G    " +
                "N47341W122287 740P077091011G    " +
                "N47327W122280 804P067085007G    N47313W1");

        amNonObs = new FakeAcarsMessage();
        amNonObs.setRegistration(".AM0494").setFlightId("AM0494").setLabel("Q0")
            .setMode('2').setBlockId('9').setAcknowledge('\u0015')
            .setMessageId("S18A");

        f9Obs = new FakeAcarsMessage();
        f9Obs.setRegistration(".N949FR").setFlightId("F90142").setLabel("21")
            .setMode('2').setBlockId('4').setAcknowledge('\u0015')
            .setMessageId("M33A")
            .setMessage("POSN 47.665W122.352, 107,235456,3714, 4262,   8,  3,173618,KSEA");

        /* actually an obs, but we treat as a non-obs */
        f9NonObs = new FakeAcarsMessage();
        f9NonObs.setRegistration(".N910FR").setFlightId("F90137").setLabel("22")
            .setMode('2').setBlockId('7').setAcknowledge('\u0015')
            .setMessageId("M35A")
            .setMessage("N 465544W1220108,-------,013130,21506, ,      , ,M 28,  634  69, 252,");

        f9Ack = new FakeAcarsMessage();
        f9Ack.setRegistration(".N709FR").setFlightId("F90681").setLabel("_\u007f")
            .setMode('2').setBlockId('4').setAcknowledge('5')
            .setMessageId("S65A");

        acObs = new FakeAcarsMessage();
        acObs.setRegistration(".C-FDCA").setFlightId("AC0541").setLabel("4T")
            .setMode('2').setBlockId('9').setAcknowledge('N')
            .setMessageId("M38A")
            .setMessage("AGFSR AC0541/31/31/YYZSEA/2349Z/405/4751.8N12145.1W/101/      /0039/0133/M06/195018/0290/   /---/2337/2357/----/----");
        acNonObs = new FakeAcarsMessage();
        acNonObs.setRegistration(".C-FLSS").setFlightId("AC0540").setLabel("H1")
            .setMode('2').setBlockId('8').setAcknowledge('G')
            .setMessageId("D36A")
            .setMessage("#DFBA04/A32004,1,1/CCC-FLSS,OCT18,141818,KSEA,CYYZ,0540/C105,40031,4000,54,0010,0,0100,54,X/CE0177,01754,169,263,6947,334,C13006/EC731584,00759,04818,00372,D3/EE731966,60611,12812,");

        rvObs = acObs.clone().setFlightId("RV1896");
        rvNonObs = acNonObs.clone().setFlightId("RV1896");

        allMessages = new FakeAcarsMessage[] { asObs, asNonObs, asAck, wnObs,
            wnNonObs, wnAck, dlObs, nwObs, nwNonObs, nwAck, fxObsA, fxObsB,
            fxObsC, fxNonObs, fxAck, dlObs, dlNonObs, dlBadObs, aaObs, aaAck,
            aaNonObs, amObs1, amObs2, amBadObs, amNonObs, f9Obs, f9NonObs,
            f9Ack };
    }

    /* a given airline's decoder should only decode its messages */
    public void onlyGetsMine(String airline) {
        WxDecoder dec = decoderForName(airline);
        for (FakeAcarsMessage msg : allMessages) {
            if (!dec.name().equals(airline))
                assertNull(dec.decode(msg));
        }
    }

    /* utility function, asserts iterators of observations equal */
    public void assertEqual(Iterator<AcarsObservation> i1, Iterator<AcarsObservation> i2) {
        String nl = System.lineSeparator();
        while (i1.hasNext() && i2.hasNext()) {
            AcarsObservation o1 = i1.next();
            AcarsObservation o2 = i2.next();
            String msg = "Mismatch between " + o1 + nl + "and " + o2;
            assertTrue(msg, o1.equals(o2));
        }
        assertTrue("Iterators of unequal length!", i1.hasNext() == i2.hasNext());
    }

    /* all decoders should handle timestamps when day, month, or year change */
    public void wrapsAround(FakeAcarsMessage message) {
        WxDecoder dec = decoderForName(message.getFlightId());
        Iterator<AcarsObservation> t1 = dec.decode(message, OLD_YEAR).iterator();
        Iterator<AcarsObservation> t2 = dec.decode(message, NEW_YEAR).iterator();
        assertEqual(t1, t2);
    }

    /* all should work whether \n or \r\n delimit lines */
    public void bothNewlinesWork(FakeAcarsMessage message) {
        WxDecoder dec = decoderForName(message.getFlightId());
        Iterator<AcarsObservation> m1 = dec.decode(message, OLD_YEAR).iterator();
        String old = message.getMessage();
        message.setMessage(old.replaceAll("\r\n", "\n"));
        Iterator<AcarsObservation> m2 = dec.decode(message, OLD_YEAR).iterator();
        message.setMessage(old);  /* restore */
        assertEqual(m1, m2);
    }

    /* all should work regardless of trailing newlines */
    public void worksWithTrailingNewline(FakeAcarsMessage message) {
        WxDecoder dec = decoderForName(message.getFlightId());
        Iterator<AcarsObservation> m1 = dec.decode(message, OLD_YEAR).iterator();
        String old = message.getMessage();
        message.setMessage(old + "\r\n");
        Iterator<AcarsObservation> m2 = dec.decode(message, OLD_YEAR).iterator();
        message.setMessage(old);  /* restore */
        assertEqual(m1, m2);
    }

    /* works with a truncation that leaves the final observation with some
       null fields, but still there */
    public void worksWithHarmlessTruncation(FakeAcarsMessage message, int truncBy) {
        WxDecoder dec = decoderForName(message.getFlightId());
        Iterator<AcarsObservation> i1 = dec.decode(message, OLD_YEAR).iterator();
        String old = message.getMessage();
        message.setMessage(old.substring(0, old.length() - truncBy));
        Iterator<AcarsObservation> i2 = dec.decode(message, OLD_YEAR).iterator();
        message.setMessage(old);  /* restore */

        /* they should all be equal except for the final one ... */
        AcarsObservation o1 = null, o2 = null;
        while (i1.hasNext() && i2.hasNext()) {
            o1 = i1.next();
            o2 = i2.next();
            if (!o1.equals(o2))
                break;
        }

        /* ... which should be equal for all non-null fields */
        assertTrue(o1.getLatitude().equals(o2.getLatitude()));
        assertTrue(o1.getLongitude().equals(o2.getLongitude()));
        assertTrue(o1.getAltitude().equals(o2.getAltitude()));
        assertTrue(o1.getObserved().equals(o2.getObserved()));
        assertTrue(o2.getWindSpeed() == null || o1.getWindSpeed().equals(o2.getWindSpeed()));
        assertTrue(o2.getWindDirection() == null || o1.getWindDirection().equals(o2.getWindDirection()));
        assertTrue(o2.getTemperature() == null || o1.getTemperature().equals(o2.getTemperature()));
    }

    /* works with a truncation that renders the final observation useless */
    public void worksWithHarmfulTruncation(FakeAcarsMessage message, int truncBy) {
        WxDecoder dec = decoderForName(message.getFlightId());
        Iterator<AcarsObservation> i1 = dec.decode(message, OLD_YEAR).iterator();
        String old = message.getMessage();
        message.setMessage(old.substring(0, old.length() - truncBy));
        Iterator<AcarsObservation> i2 = dec.decode(message, OLD_YEAR).iterator();
        message.setMessage(old);  /* restore */

        /* they should all be equal ... */
        AcarsObservation o1 = null, o2 = null;
        while (i1.hasNext() && i2.hasNext()) {
            o1 = i1.next();
            o2 = i2.next();
            if (!o1.equals(o2))
                break;
        }

        /* ... but the second set should be short */
        assertTrue(i1.hasNext() && !i2.hasNext());
    }

    @Test
    public void canLookupAirline() {
        WxDecoder decoder = decoderForName("AS");
        assertEquals(decoder.name(), "AS");
    }

    @Test
    public void canLookupFlight() {
        WxDecoder decoder = decoderForName("AS0086");
        assertEquals(decoder.name(), "AS");
    }

    @Test
    public void caseInsensitive() {
        WxDecoder dec1 = decoderForName("as");
        WxDecoder dec2 = decoderForName("AS0086");
        /* they should compare as equivalent */
        assertEquals(dec1.compareTo(dec2), 0);
        /* but they SHOULD NOT be exactly the same object */
        assertNotSame(dec1, dec2);
    }

    @Test
    public void alaska() {
        onlyGetsMine("AS");
        wrapsAround(asObs);
        bothNewlinesWork(asObs);
        worksWithTrailingNewline(asObs);
        worksWithHarmlessTruncation(asObs, 12);
        worksWithHarmfulTruncation(asObs, 22);

        AcarsObservation shouldBe = new AcarsObservation(
            48.8162, -123.5620, 35004, parseDate("2017-12-31T23:43:00Z"));
        shouldBe.setTemperature(-50.5f).setWindDirection((short) 227)
            .setWindSpeed((short) 55);
        WxDecoder dec = decoderForName("AS");
        AcarsObservation actuallyIs = dec.decode(asObs, OLD_YEAR).iterator().next();
        assertTrue(actuallyIs.equals(shouldBe));
        assertNull(dec.decode(asNonObs));
    }

    @Test
    public void southwest() {
        onlyGetsMine("WN");
        wrapsAround(wnObs);
        bothNewlinesWork(wnObs);
        worksWithTrailingNewline(wnObs);
        worksWithHarmlessTruncation(wnObs, 13);
        worksWithHarmfulTruncation(wnObs, 28);

        AcarsObservation shouldBe = new AcarsObservation(
            47.27, -122.18, 1353, parseDate("2017-12-31T23:50:00Z"));
        shouldBe.setTemperature(24.2f).setWindDirection((short) 47)
            .setWindSpeed((short) 5);
        WxDecoder dec = decoderForName("WN");
        AcarsObservation actuallyIs = dec.decode(wnObs, OLD_YEAR).iterator().next();
        assertTrue(actuallyIs.equals(shouldBe));
        assertNull(dec.decode(wnNonObs));
    }

    @Test
    public void northwest() {
        onlyGetsMine("NW");
        wrapsAround(nwObs);
        bothNewlinesWork(nwObs);
        worksWithTrailingNewline(nwObs);
        worksWithHarmlessTruncation(nwObs, 4);
        worksWithHarmfulTruncation(nwObs, 17);

        AcarsObservation shouldBe = new AcarsObservation(
            47.256, -121.292, 18400, parseDate("2017-12-31T23:48:00Z"));
        shouldBe.setTemperature(-13.0f).setWindDirection((short) 220)
            .setWindSpeed((short) 24);
        WxDecoder dec = decoderForName("NW");
        AcarsObservation actuallyIs = dec.decode(nwObs, OLD_YEAR).iterator().next();
        assertTrue(actuallyIs.equals(shouldBe));

        /* there can be a newline after the DXB*WXR */
        AcarsObservation orig = shouldBe;
        nwObs.setMessage("#DFB*WXR\r\n" + nwObs.getMessage().substring(8));
        AcarsObservation withNl = dec.decode(nwObs, OLD_YEAR).iterator().next();
        assertTrue(orig.equals(withNl));

        /* this format might encode no observations */
        nwObs.setMessage("#DFB*WXR");
        Iterator<AcarsObservation> empty = dec.decode(nwObs, OLD_YEAR).iterator();
        assertFalse(empty.hasNext());
        nwObs.setMessage("#DFB*WXR\r\n");
        empty = dec.decode(nwObs, OLD_YEAR).iterator();
        assertFalse(empty.hasNext());
        assertNull(dec.decode(nwNonObs));
    }

    @Test
    public void fedEx() {
        onlyGetsMine("FX");

        wrapsAround(fxObsA);
        bothNewlinesWork(fxObsA);
        worksWithTrailingNewline(fxObsA);
        worksWithHarmlessTruncation(fxObsA, 5);
        worksWithHarmfulTruncation(fxObsA, 14);

        AcarsObservation shouldBe = new AcarsObservation(
            47.38, -122.30, 3374, parseDate("2017-12-31T23:57:17Z"));
        shouldBe.setTemperature(13.0f).setWindDirection((short) 285)
            .setWindSpeed((short) 5);
        WxDecoder dec = decoderForName("FX");
        AcarsObservation actuallyIs = dec.decode(fxObsA, OLD_YEAR).iterator().next();
        assertTrue(actuallyIs.equals(shouldBe));

        wrapsAround(fxObsB);
        bothNewlinesWork(fxObsB);
        worksWithTrailingNewline(fxObsB);
        worksWithHarmlessTruncation(fxObsB, 5);
        worksWithHarmfulTruncation(fxObsB, 14);

        shouldBe = new AcarsObservation(
            46.8800, -122.0479, 20659, parseDate("2017-12-31T23:50:28Z"));
        shouldBe.setTemperature(-16.0f).setWindDirection((short) 359)
            .setWindSpeed((short) 22);
        actuallyIs = dec.decode(fxObsB, OLD_YEAR).iterator().next();
        assertTrue(actuallyIs.equals(shouldBe));

        wrapsAround(fxObsC);
        bothNewlinesWork(fxObsC);
        worksWithTrailingNewline(fxObsC);
        worksWithHarmlessTruncation(fxObsC, 5);
        worksWithHarmfulTruncation(fxObsC, 14);

        shouldBe = new AcarsObservation(
            47.37, -122.24, 7043, parseDate("2017-12-31T23:50:28Z"));
        shouldBe.setTemperature(-4.0f).setWindDirection((short) 350)
            .setWindSpeed((short) 6);
        actuallyIs = dec.decode(fxObsC, OLD_YEAR).iterator().next();
        assertTrue(actuallyIs.equals(shouldBe));

        assertNull(dec.decode(fxNonObs));
    }

    @Test
    public void delta() {
        onlyGetsMine("DL");

        wrapsAround(dlObs);
        bothNewlinesWork(dlObs);
        worksWithTrailingNewline(dlObs);
        worksWithHarmlessTruncation(dlObs, 18);
        worksWithHarmfulTruncation(dlObs, 22);

        AcarsObservation shouldBe = new AcarsObservation(
            47.628, -122.883, 21510, parseDate("2017-12-31T23:55:00Z"));
        shouldBe.setTemperature(-34.0f).setWindDirection((short) 225)
            .setWindSpeed((short) 59);
        WxDecoder dec = decoderForName("DL");
        AcarsObservation actuallyIs = dec.decode(dlObs, OLD_YEAR).iterator().next();
        assertTrue(actuallyIs.equals(shouldBe));

        assertFalse(dec.decode(dlBadObs).iterator().hasNext());
        assertNull(dec.decode(dlNonObs));
    }

    @Test
    public void american() {
        onlyGetsMine("AA");

        wrapsAround(aaObs);
        bothNewlinesWork(aaObs);
        worksWithTrailingNewline(aaObs);
        worksWithHarmfulTruncation(aaObs, 13);

        AcarsObservation shouldBe = new AcarsObservation(
            47.254, -121.187, 20420, parseDate("2017-12-31T23:55:00Z"));
        shouldBe.setTemperature(-32.0f).setWindDirection((short) 210)
            .setWindSpeed((short) 16);
        WxDecoder dec = decoderForName("AA");
        AcarsObservation actuallyIs = dec.decode(aaObs, OLD_YEAR).iterator().next();
        assertTrue(actuallyIs.equals(shouldBe));

        assertNull(dec.decode(aaNonObs));
        assertNull(dec.decode(aaAck));
    }

    @Test
    public void aeromexico() {
        onlyGetsMine("AM");

        wrapsAround(amObs1);
        bothNewlinesWork(amObs1);
        worksWithTrailingNewline(amObs1);
        worksWithHarmfulTruncation(amObs1, 32);

        AcarsObservation shouldBe = new AcarsObservation(
            47.061, -121.349, 18430, parseDate("2017-12-31T23:50:00Z"));
        shouldBe.setTemperature(-25.0f).setWindDirection((short) 226)
            .setWindSpeed((short) 40);
        WxDecoder dec = decoderForName("AM");
        AcarsObservation actuallyIs = dec.decode(amObs1, OLD_YEAR).iterator().next();
        assertTrue(actuallyIs.equals(shouldBe));

        wrapsAround(amObs2);
        bothNewlinesWork(amObs2);
        worksWithTrailingNewline(amObs2);
        worksWithHarmfulTruncation(amObs2, 32);

        shouldBe = new AcarsObservation(
            47.360, -122.290, 7060, parseDate("2017-12-31T23:50:00Z"));
        shouldBe.setTemperature(-3.0f).setWindDirection((short) 198)
            .setWindSpeed((short) 54);
        actuallyIs = dec.decode(amObs2, OLD_YEAR).iterator().next();
        assertTrue(actuallyIs.equals(shouldBe));

        assertNull(dec.decode(amNonObs));
        assertNull(dec.decode(amBadObs));
    }

    @Test
    public void frontier() {
        onlyGetsMine("F9");
        wrapsAround(f9Obs);
        worksWithTrailingNewline(f9Obs);

        AcarsObservation shouldBe = new AcarsObservation(
            47.665, -122.352, 3714, parseDate("2017-12-31T23:54:56Z"));
        shouldBe.setTemperature(3.0f).setWindDirection((short) 107)
            .setWindSpeed((short) 8);
        WxDecoder dec = decoderForName("F9");
        AcarsObservation actuallyIs = dec.decode(f9Obs, OLD_YEAR).iterator().next();
        assertTrue(actuallyIs.equals(shouldBe));
    }

    @Test
    public void airCanada() {
        /* this one validates length, so we can't do the truncation tests */
        onlyGetsMine("AC");
        wrapsAround(acObs);
        bothNewlinesWork(acObs);
        worksWithTrailingNewline(acObs);
        /* strange expressions for lat/long to mirror parsing logic */
        AcarsObservation shouldBe = new AcarsObservation(
            4751.8/100.0, -12145.1/100.0, 10100, parseDate("2017-12-31T23:49:00Z"));
        shouldBe.setTemperature(-6.0f).setWindDirection((short) 195)
            .setWindSpeed((short) 18);
        WxDecoder dec = decoderForName("AC");
        AcarsObservation actuallyIs = dec.decode(acObs, OLD_YEAR).iterator().next();
        assertTrue(actuallyIs.equals(shouldBe));
        assertNull(dec.decode(acNonObs));

        onlyGetsMine("RV");
        wrapsAround(rvObs);
        bothNewlinesWork(rvObs);
        worksWithTrailingNewline(rvObs);
        dec = decoderForName("AC");
        actuallyIs = dec.decode(rvObs, OLD_YEAR).iterator().next();
        assertTrue(actuallyIs.equals(shouldBe));
        assertNull(dec.decode(rvNonObs));
    }

    private static Date parseDate(String s) {
        try {
            return UTC.parse(s);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private static WxDecoder decoderForName(String s) {
        try {
            return WxDecoder.forName(s);
        } catch (WxDecoder.UnknownAirlineException e) {
            throw new RuntimeException(e);
        }
    }
}
