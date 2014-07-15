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
package com.raytheon.uf.common.serialization;

import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

/**
 * Formatting and output options for JAXB marshallers
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 15, 2014 3373       bclement     Initial creation
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */
public class MarshalOptions {

    /**
     * Preset option to marshal to a complete, formatted XML document
     */
    public static final MarshalOptions FORMATTED = new MarshalOptions(true,
            false);

    /**
     * Preset option to marshal to a complete, unformatted XML document
     */
    public static final MarshalOptions UNFORMATTED = new MarshalOptions(false,
            false);

    public final boolean formatted;

    public final boolean fragment;

    /**
     * @param formatted
     *            True if formatting whitespace should be added.
     * @param fragment
     *            True if preamble should be omitted (<?xml ..>). Otherwise, a
     *            complete, standalone XML document will be generated. Fragments
     *            are useful for generating xml which will ultimately be
     *            included in other documents.
     */
    public MarshalOptions(boolean formatted, boolean fragment) {
        this.formatted = formatted;
        this.fragment = fragment;
    }

    /**
     * Apply options to marshaller
     * 
     * @param msh
     * @throws PropertyException
     */
    public void apply(Marshaller msh) throws PropertyException {
        msh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formatted);
        msh.setProperty(Marshaller.JAXB_FRAGMENT, fragment);
    }

}
