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
package com.raytheon.uf.common.http;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HeaderElement;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeaderValueParser;
import org.apache.http.message.HeaderValueParser;
import org.apache.http.message.ParserCursor;
import org.apache.http.util.CharArrayBuffer;

/**
 * Parses Accept and Accept-Encoding headers for HTTP requests
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 8, 2013  2539       bclement     Initial creation
 * Feb 14, 2014 2756       bclement     moved to common http from ogc common
 * Jan 08, 2015 3789       bclement     refactored to use HeaderValueParser
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */
public class AcceptHeaderParser implements Iterable<AcceptHeaderValue> {

    private static final String QVAL_PARAM_NAME = "q";

    private final List<AcceptHeaderValue> values;

    /**
     * @param input
     *            accept header value
     */
    public AcceptHeaderParser(String input) {
        CharArrayBuffer buffer = new CharArrayBuffer(input.length());
        HeaderValueParser parser = new BasicHeaderValueParser();
        HeaderElement[] elements = parser.parseElements(buffer,
                new ParserCursor(0, buffer.length()));
        this.values = new ArrayList<>(elements.length);
        for (HeaderElement element : elements) {
            String encoding = element.getName();
            Double qvalue = findQvalue(element);
            AcceptHeaderValue value;
            if (qvalue != null) {
                value = new AcceptHeaderValue(encoding, qvalue);
            } else {
                value = new AcceptHeaderValue(encoding);
            }
            this.values.add(value);
        }
    }

    /**
     * @param formatHeader
     * @return null if qvalue isn't found or is malformed
     */
    private static Double findQvalue(HeaderElement formatHeader) {
        Double rval = null;
        NameValuePair qvalPair = formatHeader
                .getParameterByName(QVAL_PARAM_NAME);
        if (qvalPair != null) {
            String qvalString = qvalPair.getValue();
            try {
                rval = Double.parseDouble(qvalString);
            } catch (Exception e) {
                /* invalid q value isn't fatal */
            }
        }
        return rval;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<AcceptHeaderValue> iterator() {
        /*
         * header values used to be parsed one at a time, header is now parsed
         * in the constructor. This method was left in for compatibility.
         */
        return values.iterator();
    }

    /**
     * @return the values
     */
    public List<AcceptHeaderValue> getValues() {
        return values;
    }

}
