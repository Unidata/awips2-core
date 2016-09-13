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
package com.raytheon.uf.common.python.concurrent;

import jep.JepException;

import com.raytheon.uf.common.python.PythonInterpreter;

/**
 * Interface for a factory class that the {@code PythonJobCoordinator} uses to
 * build each {@code PythonInterpreter} instance for each thread in its thread
 * pool.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 05, 2013            mnash       Initial creation
 * Jun 04, 2013 2041       bsteffen    Improve exception handling for concurrent
 *                                     python.
 * Dec 10, 2015 4816       dgilling    Rewrite as interface, remove maxThreads
 *                                     and name.
 * 
 * </pre>
 * 
 * @author mnash
 * @version 1.0
 */

public interface PythonInterpreterFactory<P extends PythonInterpreter> {

    /**
     * This method will be called on one of the pool's threads and will
     * instantiate the {@code PythonInterpreter}.
     * 
     * @return The {@code PythonInterpreter} instance.
     */
    P createPythonScript() throws JepException;
}
