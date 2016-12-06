/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further 
 */
package com.raytheon.uf.common.util.format;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.ParseException;

import org.junit.Test;

import com.raytheon.uf.common.util.format.BytesFormat.Standard;

/**
 * Test {@link BytesFormat}
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Dec 05, 2016  5992     bsteffen  Initial creation
 * 
 * </pre>
 *
 * @author bsteffen
 */
public class TestBytesFormat {

    /**
     * 8EiB
     * 
     * @see BytesFormat#parse(String)
     */
    private static final long MAX_PRECISE_LONG = 9007199254740992L;

    @Test
    public void testUnscaledParsing() throws ParseException {
        BytesFormat bf = new BytesFormat();
        assertEquals(bf.parse("2"), 2);
        assertEquals(bf.parse("2048"), 2048);
        assertEquals(bf.parse("2560"), 2560);
        assertEquals(bf.parse("2097152"), 2097152);
        assertEquals(bf.parse("2621440"), 2621440);
        assertEquals(bf.parse("2147483648"), 2147483648L);
        assertEquals(bf.parse("2684354560"), 2684354560L);
        assertEquals(bf.parse("2199023255552"), 2199023255552L);
        assertEquals(bf.parse("2748779069440"), 2748779069440L);
        assertEquals(bf.parse("2251799813685248"), 2251799813685248L);
        assertEquals(bf.parse("2814749767106560"), 2814749767106560L);
        assertEquals(bf.parse("9223372036854775807"), 9223372036854775807L);

        assertEquals(bf.parse("2B"), 2);
        assertEquals(bf.parse("2048B"), 2048);
        assertEquals(bf.parse("2560B"), 2560);
        assertEquals(bf.parse("2097152B"), 2097152);
        assertEquals(bf.parse("2621440B"), 2621440);
        assertEquals(bf.parse("2147483648B"), 2147483648L);
        assertEquals(bf.parse("2684354560B"), 2684354560L);
        assertEquals(bf.parse("2199023255552B"), 2199023255552L);
        assertEquals(bf.parse("2748779069440B"), 2748779069440L);
        assertEquals(bf.parse("2251799813685248B"), 2251799813685248L);
        assertEquals(bf.parse("2814749767106560B"), 2814749767106560L);
        assertEquals(bf.parse("9223372036854775807B"), 9223372036854775807L);
    }

    @Test
    public void testSimpleParsing() throws ParseException {
        BytesFormat bf = new BytesFormat();

        assertEquals(bf.parse("2 KiB"), 2048);
        assertEquals(bf.parse("2 MiB"), 2097152);
        assertEquals(bf.parse("2 GiB"), 2147483648L);
        assertEquals(bf.parse("2 TiB"), 2199023255552L);
        assertEquals(bf.parse("2 PiB"), 2251799813685248L);
        assertTrue(bf.parse("2 EiB") > MAX_PRECISE_LONG);
        assertEquals(bf.parse("2 ZiB"), Long.MAX_VALUE);
        assertEquals(bf.parse("2 YiB"), Long.MAX_VALUE);

        assertEquals(bf.parse("2 kB"), 2_000L);
        assertEquals(bf.parse("2 MB"), 2_000_000L);
        assertEquals(bf.parse("2 GB"), 2_000_000_000L);
        assertEquals(bf.parse("2 TB"), 2_000_000_000_000L);
        assertEquals(bf.parse("2 PB"), 2_000_000_000_000_000L);
        assertTrue(bf.parse("2 EB") > 9007199254740992L);
        assertEquals(bf.parse("2 ZB"), Long.MAX_VALUE);
        assertEquals(bf.parse("2 YB"), Long.MAX_VALUE);

        assertEquals(bf.parse("2 KB"), 2048);

        assertEquals(bf.parse("2 K"), 2048);
        assertEquals(bf.parse("2 M"), 2097152);
        assertEquals(bf.parse("2 G"), 2147483648L);
        assertEquals(bf.parse("2 T"), 2199023255552L);
        assertEquals(bf.parse("2 P"), 2251799813685248L);
        assertTrue(bf.parse("2 E") > MAX_PRECISE_LONG);
        assertEquals(bf.parse("2 Z"), Long.MAX_VALUE);
        assertEquals(bf.parse("2 Y"), Long.MAX_VALUE);

        assertEquals(bf.parse("2 k"), 2048);
        assertEquals(bf.parse("2 m"), 2097152);
        assertEquals(bf.parse("2 g"), 2147483648L);
        assertEquals(bf.parse("2 t"), 2199023255552L);
        assertEquals(bf.parse("2 p"), 2251799813685248L);
        assertTrue(bf.parse("2 e") > MAX_PRECISE_LONG);
        assertEquals(bf.parse("2 z"), Long.MAX_VALUE);
        assertEquals(bf.parse("2 y"), Long.MAX_VALUE);
    }

    @Test
    public void testDecimalParsing() throws ParseException {
        BytesFormat bf = new BytesFormat();
        /*
         * These two are a bit ambiguous and may change in the future but for
         * now they parse.
         */
        assertEquals(bf.parse("2.5"), 2);
        assertEquals(bf.parse("2.5 B"), 2);

        assertEquals(bf.parse("2.5 KiB"), 2560);
        assertEquals(bf.parse("2.5 MiB"), 2621440);
        assertEquals(bf.parse("2.5 GiB"), 2684354560L);
        assertEquals(bf.parse("2.5 TiB"), 2748779069440L);
        assertEquals(bf.parse("2.5 PiB"), 2814749767106560L);
        assertTrue(bf.parse("2.5 EiB") > MAX_PRECISE_LONG);
        assertEquals(bf.parse("2.5 ZiB"), Long.MAX_VALUE);
        assertEquals(bf.parse("2.5 YiB"), Long.MAX_VALUE);
    }

    @Test
    public void testConventionalParsing() throws ParseException {
        BytesFormat bf = new BytesFormat(Standard.CUSTOMARY);
        assertEquals(bf.parse("2 KB"), 2048);
        assertEquals(bf.parse("2 MB"), 2097152);
        assertEquals(bf.parse("2 GB"), 2147483648L);
        assertEquals(bf.parse("2 TB"), 2199023255552L);
        assertEquals(bf.parse("2 PB"), 2251799813685248L);
        assertTrue(bf.parse("2 EB") > MAX_PRECISE_LONG);
        assertEquals(bf.parse("2 ZB"), Long.MAX_VALUE);
        assertEquals(bf.parse("2 YB"), Long.MAX_VALUE);
    }

    @Test(expected = ParseException.class)
    public void testThrowsUsingUnspecifiedStandard() throws ParseException {
        new BytesFormat(Standard.CUSTOMARY).parse("2 KiB");
    }

    @Test
    public void testParsingWithSpace() throws ParseException {
        BytesFormat bf = new BytesFormat();

        assertEquals(bf.parse("2 KiB"), 2048);
    }

    public void testParsingDefaultSuffix() throws ParseException {
        BytesFormat bf = new BytesFormat();
        bf.setDefaultSuffix("KiB");

        assertEquals(bf.parse("2"), bf.parse("2 KiB"));
    }
    
    @Test
    public void testBigParsing() throws ParseException {
        BytesFormat bf = new BytesFormat();
        /*
         * These two are a bit ambiguous and may change in the future but for
         * now they parse.
         */
        assertEquals(bf.parseBig("2.5"), new BigInteger("2"));
        assertEquals(bf.parseBig("2.5 B"), new BigInteger("2"));

        assertEquals(bf.parseBig("2"), new BigInteger("2"));
        assertEquals(bf.parseBig("2 B"), new BigInteger("2"));

        assertEquals(bf.parseBig("2 KiB"), new BigInteger("2048"));
        assertEquals(bf.parseBig("2 MiB"), new BigInteger("2097152"));
        assertEquals(bf.parseBig("2 GiB"), new BigInteger("2147483648"));
        assertEquals(bf.parseBig("2 TiB"), new BigInteger("2199023255552"));
        assertEquals(bf.parseBig("2 PiB"), new BigInteger("2251799813685248"));
        assertEquals(bf.parseBig("2 EiB"), new BigInteger("2305843009213693952"));
        assertEquals(bf.parseBig("2 ZiB"), new BigInteger("2361183241434822606848"));
        assertEquals(bf.parseBig("2 YiB"), new BigInteger("2417851639229258349412352"));
    }

    @Test
    public void testDefaultFormating() {
        BytesFormat bf = new BytesFormat();

        assertEquals("2.0 B", bf.format(2));

        assertEquals("2.0 KiB", bf.format(2_000));
        assertEquals("2.0 KiB", bf.format(2048));
        assertEquals("2.4 KiB", bf.format(2_500));
        assertEquals("2.5 KiB", bf.format(2560));

        assertEquals("1.9 MiB", bf.format(2_000_000));
        assertEquals("2.0 MiB", bf.format(2097152));
        assertEquals("2.4 MiB", bf.format(2_500_000));
        assertEquals("2.5 MiB", bf.format(2621440));

        assertEquals("1.9 GiB", bf.format(2_000_000_000));
        assertEquals("2.0 GiB", bf.format(2147483648L));
        assertEquals("2.3 GiB", bf.format(2_500_000_000L));
        assertEquals("2.5 GiB", bf.format(2684354560L));

        assertEquals("1.8 TiB", bf.format(2_000_000_000_000L));
        assertEquals("2.0 TiB", bf.format(2199023255552L));
        assertEquals("2.3 TiB", bf.format(2_500_000_000_000L));
        assertEquals("2.5 TiB", bf.format(2748779069440L));

        assertEquals("1.8 PiB", bf.format(2_000_000_000_000_000L));
        assertEquals("2.0 PiB", bf.format(2251799813685248L));
        assertEquals("2.2 PiB", bf.format(2_500_000_000_000_000L));
        assertEquals("2.5 PiB", bf.format(2814749767106560L));

        assertEquals("1.7 EiB", bf.format(2_000_000_000_000_000_000L));
        assertEquals("2.0 EiB", bf.format(2305843009213693952L));
        assertEquals("2.2 EiB", bf.format(2_500_000_000_000_000_000L));
        assertEquals("2.5 EiB", bf.format(2882303761517117440L));
    }

    @Test
    public void testSIFormating() {
        BytesFormat bf = new BytesFormat(Standard.SI);

        assertEquals("2.0 B", bf.format(2));

        assertEquals("2.0 kB", bf.format(2_000));
        assertEquals("2.0 kB", bf.format(2048));
        assertEquals("2.5 kB", bf.format(2_500));
        assertEquals("2.6 kB", bf.format(2560));

        assertEquals("2.0 MB", bf.format(2_000_000));
        assertEquals("2.1 MB", bf.format(2097152));
        assertEquals("2.5 MB", bf.format(2_500_000));
        assertEquals("2.6 MB", bf.format(2621440));

        assertEquals("2.0 GB", bf.format(2_000_000_000));
        assertEquals("2.1 GB", bf.format(2147483648L));
        assertEquals("2.5 GB", bf.format(2_500_000_000L));
        assertEquals("2.7 GB", bf.format(2684354560L));

        assertEquals("2.0 TB", bf.format(2_000_000_000_000L));
        assertEquals("2.2 TB", bf.format(2199023255552L));
        assertEquals("2.5 TB", bf.format(2_500_000_000_000L));
        assertEquals("2.7 TB", bf.format(2748779069440L));

        assertEquals("2.0 PB", bf.format(2_000_000_000_000_000L));
        assertEquals("2.3 PB", bf.format(2251799813685248L));
        assertEquals("2.5 PB", bf.format(2_500_000_000_000_000L));
        assertEquals("2.8 PB", bf.format(2814749767106560L));

        assertEquals("2.0 EB", bf.format(2_000_000_000_000_000_000L));
        assertEquals("2.3 EB", bf.format(2305843009213693952L));
        assertEquals("2.5 EB", bf.format(2_500_000_000_000_000_000L));
        assertEquals("2.9 EB", bf.format(2882303761517117440L));
    }

    @Test
    public void testCustomaryFormating() {
        BytesFormat bf = new BytesFormat(Standard.CUSTOMARY);

        assertEquals("2.0 B", bf.format(2));

        assertEquals("2.0 KB", bf.format(2_000));
        assertEquals("2.0 KB", bf.format(2048));
        assertEquals("2.4 KB", bf.format(2_500));
        assertEquals("2.5 KB", bf.format(2560));

        assertEquals("1.9 MB", bf.format(2_000_000));
        assertEquals("2.0 MB", bf.format(2097152));
        assertEquals("2.4 MB", bf.format(2_500_000));
        assertEquals("2.5 MB", bf.format(2621440));

        assertEquals("1.9 GB", bf.format(2_000_000_000));
        assertEquals("2.0 GB", bf.format(2147483648L));
        assertEquals("2.3 GB", bf.format(2_500_000_000L));
        assertEquals("2.5 GB", bf.format(2684354560L));

        assertEquals("1.8 TB", bf.format(2_000_000_000_000L));
        assertEquals("2.0 TB", bf.format(2199023255552L));
        assertEquals("2.3 TB", bf.format(2_500_000_000_000L));
        assertEquals("2.5 TB", bf.format(2748779069440L));

        assertEquals("1.8 PB", bf.format(2_000_000_000_000_000L));
        assertEquals("2.0 PB", bf.format(2251799813685248L));
        assertEquals("2.2 PB", bf.format(2_500_000_000_000_000L));
        assertEquals("2.5 PB", bf.format(2814749767106560L));

        assertEquals("1.7 EB", bf.format(2_000_000_000_000_000_000L));
        assertEquals("2.0 EB", bf.format(2305843009213693952L));
        assertEquals("2.2 EB", bf.format(2_500_000_000_000_000_000L));
        assertEquals("2.5 EB", bf.format(2882303761517117440L));
    }

    @Test
    public void testBigFormatting() {
        BytesFormat bf = new BytesFormat();
        assertEquals("2.0 B", bf.formatBig(new BigInteger("2")));
        assertEquals("1.9 MiB", bf.formatBig(new BigInteger("2000000")));
        assertEquals("1.8 PiB",
                bf.formatBig(new BigInteger("2000000000000000")));
        assertEquals("1.7 EiB",
                bf.formatBig(new BigInteger("2000000000000000000")));
        assertEquals("1.7 ZiB",
                bf.formatBig(new BigInteger("2000000000000000000000")));
        assertEquals("1.7 YiB",
                bf.formatBig(new BigInteger("2000000000000000000000000")));
    }

    @Test
    public void testCustomDecimalFormating() {
        BytesFormat bf = new BytesFormat();
        bf.setDecimalFormat(new DecimalFormat());

        assertEquals("1.863 GiB", bf.format(2_000_000_000));
        assertEquals("2 GiB", bf.format(2147483648L));
        assertEquals("2.328 GiB", bf.format(2_500_000_000L));
        assertEquals("2.5 GiB", bf.format(2684354560L));

        assertEquals("1.654 YiB",
                bf.formatBig(new BigInteger("2000000000000000000000000")));
    }

    @Test
    public void testNoB() {
        BytesFormat bf = new BytesFormat();
        bf.setByteSuffix("");

        assertEquals(bf.format(2), "2.0");
        assertEquals(bf.format(2048), "2.0 KiB");

        assertEquals(bf.formatBig(new BigInteger("2")), "2.0");
        assertEquals(bf.formatBig(new BigInteger("2048")), "2.0 KiB");
    }

    @Test
    public void testWithoutSpace() {
        BytesFormat bf = new BytesFormat();
        bf.setSeparator("");

        assertEquals(bf.format(2), "2.0B");
        assertEquals(bf.format(2048), "2.0KiB");

        assertEquals(bf.formatBig(new BigInteger("2")), "2.0B");
        assertEquals(bf.formatBig(new BigInteger("2048")), "2.0KiB");
    }

}
