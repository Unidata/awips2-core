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
package com.raytheon.uf.edex.esb.camel;

import java.io.File;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFile;

/**
 * Provides a capability to transform java.io.File to Strings. This is necessary
 * because camel transforms java.io.Files to byte[] on JMS.
 * <p>
 * To ensure proper processing, this file will need to be stored on a file
 * system accessible to all EDEX cluster members.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 03, 2008            chammack    Initial creation
 * Jun 12, 2012 00609      djohnson    Use EDEXUtil for EDEX_HOME.
 * Oct 16, 2018 #7522      dgilling    Stop moving file.
 *
 * </pre>
 *
 * @author chammack
 */

@Deprecated
public class FileToString implements Processor {

    @Override
    public void process(Exchange arg0) throws Exception {
        File file = (File) ((GenericFile<?>) arg0.getIn().getBody()).getFile();
        arg0.getIn().setBody(file.toString());
        arg0.getIn().setHeader("enqueueTime", System.currentTimeMillis());
    }
}
