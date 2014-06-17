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
package com.raytheon.uf.common.util.memory;

/**
 * Utilities for garbage, such as garbage collection or references.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 16, 2014 3274       njensen     Initial creation
 * 
 * 
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class GarbageUtil {

    private GarbageUtil() {

    }

    /**
     * Warning: Calling this will potentially cause the entire JVM to pause.
     * From a user perspective the system may appear to momentarily hang.
     * Therefore this method is dangerous and should only be called if you know
     * what you're doing. Ideally it should only ever be called from somewhere
     * that is already intentionally pausing/blocking (which is very rare).
     * 
     * This methods attempts to induce garbage collection, a finalization of
     * unused objects, and then a second garbage collection. It is roughly
     * equivalent to clicking the trash can icon on the heap monitor in Eclipse
     * or CAVE.
     * 
     * If the garbage collector is specified as G1 (Garbage First), then calling
     * this method can potentially shrink the heap size and release memory back
     * to the OS. This is an undocumented feature of G1, Sun/Oracle's
     * documentation never mention this but in testing the G1 will return memory
     * to the OS whereas other garbage collectors such as ConcurrentMarkSweep
     * will not. It will only release that memory if there is enough free,
     * obviously if the application is using large amounts of memory that still
     * retain references, those references are not garbage and will not be
     * released.
     * 
     * Note that if you want to disable this capability without altering code,
     * you should use the JVM argument -XX:+DisableExplicitGC
     */
    public static void releaseMemoryToOS() {
        System.gc();
        System.runFinalization();

        // this call can release memory to the OS!
        System.gc();
    }

}
