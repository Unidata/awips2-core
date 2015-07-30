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

package com.raytheon.edex.plugin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base class for plugin decoders.
 * The AbstractDecoder class provides logging support and class variables used
 * in child classes.
 * 
 * @deprecated Does not provide helpful functionality.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 06/14/06                garmendariz Initial check-in
 * 11/02/06     #39        brockwoo    Added wmo header field
 * Jul 10, 2014 2914       garmendariz Remove EnvProperties
 * Jun 25, 2015 4495       njensen     Deprecated
 * 
 * 
 * </pre>
 * @author garmendariz
 * 
 */
@Deprecated
public abstract class AbstractDecoder {

    /** The logger */
    protected Log logger = LogFactory.getLog(getClass());

    /** The file name to parse */
    protected String fileName;

    /** The wmo header to parse */
    protected String wmoHeader;

}
