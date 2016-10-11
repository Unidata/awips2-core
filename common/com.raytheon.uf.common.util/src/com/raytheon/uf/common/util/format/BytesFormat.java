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
 * further licensing information.
 **/
package com.raytheon.uf.common.util.format;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Objects;

/**
 * Object for parsing and formating quantities of digital information, expressed
 * as a number of bytes. The String format is a number followed by a suffix. The
 * suffix used is determined by a standard or convention as defined in
 * {@link Standard}.
 * 
 * BytesFormat objects are not thread safe.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Dec 02, 2016  5992     bsteffen  Initial creation
 * 
 * </pre>
 *
 * @author bsteffen
 */
public class BytesFormat {

    public static final String DEFAULT_BYTES_SUFFIX = "B";

    public static enum Standard {

        /**
         * Binary prefixes as defined by the International Electrotechnical
         * Commission(IEC) and the International Organization for
         * Standardization(ISO) in IEC 80000. In addition to the IEC and ISO
         * this standard is also recommended by the National Institute of
         * Standards and Technology(NIST), the Institute of Electrical and
         * Electronics Engineers (IEEE), the International Bureau of Weights and
         * Measures (BIPM), and the European Committee for Electrotechnical
         * Standardization (CENELEC)
         */
        IEC(1024, "KiB", "MiB", "GiB", "TiB", "PiB", "EiB", "ZiB", "YiB"),

        /**
         * Decimal prefixes defined by the International System of Units(SI).
         * This standard should be used whenever referring to a size using a
         * power of 10 multiplier(1000).
         */
        SI(1000, "kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"),

        /**
         * Suffixes defined by the Joint Electron Device Engineering
         * Council(JEDEC) in JEDEC Standard 100B.01. Although these are commonly
         * used, they are not recommended because they conflict with the SI
         * definitions. The JEDEC only documents them as an existing common
         * practice. These definitions are prohibited by the IEC, BIPM, NIST
         * because they conflict with SI definitions.
         */
        JEDEC(1024, "KB", "MB", "GB"),

        /**
         * This is just an extension of the JEDEC to include larger prefixes
         * borrowed from SI. No standards organization has ever recommended
         * doing this because it can be very confusing but most users and
         * developers ignore standards anyway and continue to use it.
         */
        CUSTOMARY(1024, "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"),

        /**
         * Shorter version of the customary unit that omits the 'B'. This is
         * used in some unix utilities like ls and is also a valid format for
         * some common JVM arguments like -XmX
         */
        SHORT_CUSTOMARY(1024, "K", "M", "G", "T", "P", "E", "Z", "Y"),

        /**
         * Lower case version of the shorter customary unit. This is a valid
         * format for some common JVM arguments like -XmX. This should never be
         * used as the standard for formatting and is included only for
         * comprehensive parsing.
         */
        LOWER_SHORT_CUSTOMARY(1024, "k", "m", "g", "t", "p", "e", "z", "y");

        public final int multiplier;

        public final String[] suffixes;

        private Standard(int multiplier, String... suffixes) {
            this.multiplier = multiplier;
            this.suffixes = suffixes;
        }
    }

    private DecimalFormat format = null;

    private String bytesSuffix = DEFAULT_BYTES_SUFFIX;

    private String separator = " ";

    private BigInteger defaultMultiplier = BigInteger.ONE;

    private final Standard[] standards;

    /**
     * Create a new format that will use all the defined standards. This will
     * provide the most flexibility when parsing values, all formatting will be
     * done using the {@link Standard#IEC} standard.
     */
    public BytesFormat() {
        this.standards = Standard.values();
    }

    /**
     * Creates a new format that will use the provided standards. When
     * formatting values only the first standard is used. For parsing all
     * standards are considered and if there is a conflict the first matching
     * standard is used.
     * 
     * @param standards
     */
    public BytesFormat(Standard... standards) {
        if (standards == null || standards.length == 0) {
            throw new IllegalArgumentException(
                    "At least one standard must be provided.");
        }
        this.standards = standards;
    }

    /**
     * Set a custom format to be used for the numeric portion of any values.
     */
    public BytesFormat setDecimalFormat(DecimalFormat format) {
        this.format = format;
        return this;
    }

    /**
     * Define the suffix to use when formatting a small number of bytes for
     * which the actual value is written. This must not be null but an empty
     * string is allowed. The default suffix is "B"
     */
    public BytesFormat setByteSuffix(String bytesSuffix) {
        this.bytesSuffix = Objects.requireNonNull(bytesSuffix);
        return this;
    }

    /**
     * Define the string to use between the number and the suffix when
     * formatting. Following the recommendation of the BIPM and NIST the default
     * is a space. It is also common to have nothing between the two in which
     * case an empty String may be provided.
     */
    public BytesFormat setSeparator(String separator) {
        this.separator = Objects.requireNonNull(separator);
        return this;
    }

    /**
     * Set the default multiplier for use when parsing a number with no suffix.
     * The default is 1, assuming a number is just the number of bytes. This is
     * especially useful when reading a configuration option which is
     * traditionally specified as a number and a specific unit is assumed. This
     * allows the option to be read with support for suffixes while maintaining
     * backwards compatibility for a raw number.
     */
    public BytesFormat setDefaultMultiplier(long defaultMultiplier) {
        this.defaultMultiplier = BigInteger.valueOf(defaultMultiplier);
        return this;
    }

    /**
     * Convenience method to set the default multiplier using the specified
     * suffix. The suffix is compared against all the standards used in this
     * format and a ParseException is thrown if it is not valid.
     * 
     * @see #setDefaultMultiplier(long)
     */
    public BytesFormat setDefaultSuffix(String suffix) throws ParseException {
        suffix = Objects.requireNonNull(suffix);
        this.defaultMultiplier = getMultiplier(suffix, 0);
        return this;
    }

    /**
     * Parse a size from a string. This can be used for values up
     * {@link Long#MAX_VALUE} which is 8EiB, any string which specifies a larger
     * value will also return Long.MAX_VALUE. This method may calculate values
     * internally using double so precision is limited to 53 bits meaning values
     * larger than 8PiB may not be represented exactly. If it is necessary to
     * perfectly represent values larger than 8PiB then
     * {@link #parseBig(String)} should be used instead.
     * 
     * @param input
     *            the string to parse. This must only contain a number,
     *            optionally followed by a suffix.
     * @return the number of bytes specified by the string.
     * @throws ParseException
     *             if there is no parseable number or an invalid suffix.
     */
    public long parse(String input) throws ParseException {
        DecimalFormat format = getFormat();
        format.setParseBigDecimal(false);
        ParsePosition p = new ParsePosition(0);
        Number valueObj = format.parse(input, p);
        if (valueObj == null) {
            throw new ParseException("Unable to parse bytes " + input,
                    p.getIndex());
        }
        double value = valueObj.doubleValue();
        if (!Double.isFinite(value)) {
            throw new ParseException("Unable to parse bytes " + input, 0);
        } else if (p.getIndex() == input.length()) {
            return (long) (value * defaultMultiplier.doubleValue());
        }
        String suffix = input.substring(p.getIndex()).trim();
        if (suffix.equals(bytesSuffix)) {
            return valueObj.longValue();
        }
        double multiplier = getMultiplier(suffix, p.getIndex()).doubleValue();

        return (long) (multiplier * value);
    }

    /**
     * Parse a size from a string. This method should be used if support is
     * needed for exact values larger than 8PiB.
     * 
     * @param input
     *            the string to parse. This must only contain a number,
     *            optionally followed by a suffix.
     * @return the number of bytes specified by the string.
     * @throws ParseException
     *             if there is no parseable number or an invalid suffix.
     */
    public BigInteger parseBig(String input) throws ParseException {
        DecimalFormat format = getFormat();
        format.setParseBigDecimal(true);
        ParsePosition p = new ParsePosition(0);
        Number valueObj = format.parse(input, p);
        if (!(valueObj instanceof BigDecimal)) {
            throw new ParseException("Unable to parse bytes " + input,
                    p.getIndex());
        }
        BigDecimal value = (BigDecimal) valueObj;
        if (p.getIndex() == input.length()) {
            return value.multiply(new BigDecimal(defaultMultiplier))
                    .toBigInteger();
        }
        String suffix = input.substring(p.getIndex()).trim();
        if (suffix.equals(bytesSuffix)) {
            return value.toBigInteger();
        }

        BigDecimal multiplier = new BigDecimal(
                getMultiplier(suffix, p.getIndex()));

        return value.multiply(multiplier).toBigInteger();
    }

    /**
     * Format the provided number of bytes as a String.
     * 
     * @param numberOfBytes
     *            the numberOfBytes to format
     * @return a string representing the number of Bytes.
     */
    public String format(long numberOfBytes) {
        DecimalFormat format = getFormat();
        Standard standard = standards[0];
        double n = numberOfBytes;
        int reps = -1;
        while ((n >= standard.multiplier)
                && (reps < (standard.suffixes.length - 1))) {
            reps += 1;
            n /= standard.multiplier;
        }
        String suffix = bytesSuffix;
        if (reps > -1) {
            suffix = standard.suffixes[reps];
        }
        if (suffix.isEmpty()) {
            return format.format(n);
        } else {
            return format.format(n) + separator + suffix;
        }
    }

    /**
     * Format the provided number of bytes as a String.
     * 
     * @param numberOfBytes
     *            the numberOfBytes to format
     * @return a string representing the number of Bytes.
     */
    public String formatBig(BigInteger numberOfBytes) {
        DecimalFormat format = getFormat();
        Standard standard = standards[0];
        BigDecimal adjustedSize = new BigDecimal(numberOfBytes);
        BigDecimal multiplier = new BigDecimal(standard.multiplier);

        int reps = -1;
        while ((adjustedSize.compareTo(multiplier) >= 0)
                && (reps < (standard.suffixes.length - 1))) {
            reps += 1;
            adjustedSize = adjustedSize.divide(multiplier);
        }
        String suffix = bytesSuffix;
        if (reps > -1) {
            suffix = standard.suffixes[reps];
        }
        if (suffix.isEmpty()) {
            return format.format(adjustedSize);
        } else {
            return format.format(adjustedSize) + separator + suffix;
        }
    }

    /**
     * Internal getter for the format field, this is to support lazy creation of
     * the default since many users will want to override the default.
     */
    protected DecimalFormat getFormat() {

        if (format == null) {
            format = new DecimalFormat("0.0");
        }
        return format;
    }

    private BigInteger getMultiplier(String suffix, int position)
            throws ParseException {
        for (Standard standard : standards) {
            for (int i = 0; i < standard.suffixes.length; i += 1) {
                String testSuffix = standard.suffixes[i];
                if (suffix.equals(testSuffix)) {
                    return new BigDecimal(standard.multiplier).pow(i + 1)
                            .toBigInteger();
                }

            }
        }
        throw new ParseException("Unsupported suffix " + suffix, position);
    }

    /**
     * Parse a byte value from a system property. If the property is not set or
     * is not parseable then the default value is parsed and used. If the
     * property is set but will not parse then a warning is printed to standard
     * error. If more specific logging or error handling is needed then a
     * BytesFormat should be used directly so ParseExceptions can be caught.
     * 
     * @param propertyName
     *            The name of the system property
     * @param defaultValue
     *            The default value, expressed as a parseable bytes quantity for
     *            readability.
     * @return The number of bytes from the property or from the default value.
     * 
     * @see System#getProperty(String)
     */
    public static long parseSystemProperty(String propertyName,
            String defaultValue) {
        BytesFormat format = new BytesFormat();
        String property = System.getProperty(propertyName);
        if (property != null) {
            try {
                return format.parse(property);
            } catch (ParseException e) {
                System.err.println("WARNING: Cannot parse " + property
                        + " as a byte value for " + propertyName
                        + ". Default value  of " + defaultValue
                        + " will be used.");
            }
        }
        try {
            return format.parse(defaultValue);
        } catch (ParseException e) {
            throw new IllegalStateException(
                    "Default Value for " + propertyName + " is unparseable.",
                    e);
        }
    }

}
