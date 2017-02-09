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
package com.raytheon.uf.common.jms;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.qpid.client.AMQConnectionURL;
import org.apache.qpid.url.URLSyntaxException;

/**
 * 
 * Extension of {@link AMQConnectionURL} that is configurable as a spring bean.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Feb 02, 2017  6085     bsteffen    Initial creation
 *
 * </pre>
 *
 * @author bsteffen
 */
public class AMQConnectionURLBean extends AMQConnectionURL {

    private static final long serialVersionUID = 1L;

    public AMQConnectionURLBean(String fullURL) throws URLSyntaxException {
        super(fullURL);
    }

    public void setOptions(Map<String, String> options) {
        for (Entry<String, String> option : options.entrySet()) {
            setOption(option.getKey(), option.getValue());
        }
    }

}
