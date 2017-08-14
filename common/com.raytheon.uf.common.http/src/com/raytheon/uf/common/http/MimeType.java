/**
 * Copyright 09/24/12 Raytheon Company.
 *
 * Unlimited Rights
 * This software was developed pursuant to Contract Number 
 * DTFAWA-10-D-00028 with the US Government. The US Government’s rights 
 * in and to this copyrighted software are as specified in DFARS
 * 252.227-7014 which was made part of the above contract. 
 */
package com.raytheon.uf.common.http;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HeaderElement;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeaderElement;
import org.apache.http.message.BasicHeaderValueFormatter;
import org.apache.http.message.BasicHeaderValueParser;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.message.ParserCursor;
import org.apache.http.util.CharArrayBuffer;

/**
 * Data object representing a MIME type used in http requests
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 29, 2012            bclement     Initial creation
 * Feb 14, 2014 2756       bclement     moved to common http from ogc common
 * Jan 08, 2015 3789       bclement     refactored to use HeaderValueParser
 * Aug 14, 2017 5731       bsteffen     Add accept() method.
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */

public class MimeType {

    private static final String WILDCARD = "*";

    private static final String SUBTYPE_SEPARATOR = "/";

    protected final String type;

    protected final String subtype;

    protected final Map<String, String> parameters;

    public MimeType(String mime) {
        CharArrayBuffer buffer = new CharArrayBuffer(mime.length());
        buffer.append(mime);
        BasicHeaderValueParser parser = new BasicHeaderValueParser();
        HeaderElement[] elements = parser.parseElements(buffer,
                new ParserCursor(0, buffer.length()));
        if (elements == null || elements.length < 1) {
            throw new IllegalArgumentException(
                    "Invalid mime type string: " + mime);
        }
        HeaderElement mimeElement = elements[0];
        String mimeType = mimeElement.getName();
        int separatorIndex = mimeType.indexOf(SUBTYPE_SEPARATOR);
        if (separatorIndex > 0) {
            type = mimeType.substring(0, separatorIndex);
            subtype = mimeType.substring(separatorIndex + 1);
        } else {
            type = mimeType;
            subtype = null;
        }
        NameValuePair[] params = mimeElement.getParameters();
        if (params != null && params.length > 0) {
            this.parameters = new LinkedHashMap<>(params.length);
            for (NameValuePair param : params) {
                this.parameters.put(param.getName().toLowerCase(),
                        param.getValue());
            }
        } else {
            this.parameters = Collections.emptyMap();
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((parameters == null) ? 0 : parameters.hashCode());
        result = prime * result + ((subtype == null) ? 0 : subtype.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MimeType other = (MimeType) obj;
        if (parameters == null) {
            if (other.parameters != null) {
                return false;
            }
        } else {
            if (other.parameters == null) {
                return false;
            }
            if (parameters.size() != other.parameters.size()) {
                return false;
            }
            for (Entry<String, String> e : parameters.entrySet()) {
                String s = e.getKey();
                String val = e.getValue();
                if (!val.equalsIgnoreCase(other.parameters.get(s))) {
                    return false;
                }
            }
        }
        if (subtype == null) {
            if (other.subtype != null) {
                return false;
            }
        } else if (!subtype.equalsIgnoreCase(other.subtype)) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equalsIgnoreCase(other.type)) {
            return false;
        }
        return true;
    }

    /**
     * @param other
     * @return true if the mime type equals this one ignoring any parameters
     */
    public boolean equalsIgnoreParams(MimeType other) {
        if (other == null) {
            return false;
        }
        if (subtype == null) {
            if (other.subtype != null) {
                return false;
            }
        } else if (!subtype.equalsIgnoreCase(other.subtype)) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equalsIgnoreCase(other.type)) {
            return false;
        }
        return true;
    }

    /**
     * Determine of another mimetype is an acceptable match for this mimetype.
     * This is similar to {@link #equalsIgnoreParams(MimeType)}, except this
     * also handles the case where this mimetype contains a type and/or subtype
     * of '*'. For example if this mimetype represents 'text/*' then all
     * mimetypes with a type of text will be acceptable, ignoring subtype.
     */
    public boolean accept(MimeType other) {
        if (other == null) {
            return false;
        }
        if (subtype == null) {
            if (other.subtype != null) {
                return false;
            }
        } else if (!subtype.equalsIgnoreCase(other.subtype)
                && !WILDCARD.equals(subtype)) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equalsIgnoreCase(other.type)
                && !WILDCARD.equals(type)) {
            return false;
        }
        return true;

    }

    /**
     * @param paramName
     * @return null if parameter is not found
     */
    public String getParam(String paramName) {
        return parameters.get(paramName.toLowerCase());
    }

    /**
     * @return the number of parameters this mime type has
     */
    public int getNumParams() {
        return parameters.size();
    }

    @Override
    public String toString() {
        String rval = toStringWithoutParams();
        if (!parameters.isEmpty()) {
            BasicHeaderElement mimeElement = new BasicHeaderElement(rval, null);
            CharArrayBuffer buffer = new CharArrayBuffer(32);
            BasicHeaderValueFormatter formatter = new BasicHeaderValueFormatter();
            formatter.formatHeaderElement(buffer, mimeElement, false);
            buffer.append("; ");
            NameValuePair[] pairs = new NameValuePair[parameters.size()];
            Iterator<Entry<String, String>> iter = parameters.entrySet()
                    .iterator();
            for (int i = 0; i < pairs.length; ++i) {
                Entry<String, String> param = iter.next();
                pairs[i] = new BasicNameValuePair(param.getKey(),
                        param.getValue());
            }
            formatter.formatParameters(buffer, pairs, false);
            rval = buffer.toString();
        }
        return rval;
    }

    /**
     * Format mime type excluding any parameters
     * 
     * @return
     */
    public String toStringWithoutParams() {
        StringBuilder sb = new StringBuilder(this.type);
        if (this.subtype != null) {
            sb.append(SUBTYPE_SEPARATOR);
            sb.append(this.subtype);
        }
        return sb.toString();
    }

    public String getType() {
        return type;
    }

    public String getSubtype() {
        return subtype;
    }

}
