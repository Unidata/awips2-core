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
package com.raytheon.uf.viz.core.drawables.ext;

import com.raytheon.uf.viz.core.IGraphicsTarget;

/**
 * Base class for all implementations of {@link IGraphicsExtensionInterface}
 * that are loaded through extension points using the
 * {@link GraphicsExtensionManager}. This class is intended to be used
 * internally by an {@link IGraphicsTarget} through a GraphicsExtensionManager.
 * 
 * For code that is using a graphics extension it should not be necessary to use
 * anything in the GraphicsExtension class, instead any capability should be
 * exposed through interfaces that extend {@link IGraphicsExtensionInterface}.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 06, 2011            bsteffen    Initial creation
 * Jul 27, 2016  5759      njensen     Updated javadoc from bsteffen
 * 
 * </pre>
 * 
 * @author bsteffen
 */
public abstract class GraphicsExtension<T extends IGraphicsTarget> {

    /**
     * Interface that other interfaces should extend if they want to be used as
     * a graphics extension
     */
    public static interface IGraphicsExtensionInterface {
        // intentionally empty
    }

    /**
     * Some default values that can be used with
     * {@link GraphicsExtension#getCompatibilityValue(IGraphicsTarget)}. For
     * most {@link IGraphicsExtensionInterface} there will be a GENERIC
     * implementation and some targets may choose to implement more efficient
     * TARGET_COMPATIBLE values.
     * 
     * In the event that many extensions exist for a single interface it is
     * likely that all of these values will be used and new extensions will have
     * to pick new values. For example if there is already a GENERIC and a
     * TARGET_COMPATIBLE extension, it is still possible to write a new generic
     * extension that uses other optional extensions to render more efficiently.
     * Then the new generic extension would return a value somewhere between
     * GENERIC and TARGET_COMPATIBLE to indicate it is better then a simple
     * GENERIC implementation but not as good as an implementation custom
     * tailored to the target. When choosing a number it is better to base it
     * off of these constants rather than hardcoding a number directly. For
     * example the better generic extension above could return a compatibility
     * of GENERIC + 1 or GENERIC + TARGET_COMPATIBLE / 2.
     */
    public static class Compatibilty {
        /**
         * Value to indicate an extension is not Compatible with a specific
         * target.
         */
        public static final int INCOMPATIBLE = -1;

        /**
         * Whenever possible a default implementation of any
         * {@link IGraphicsExtensionInterface} should be provided that uses
         * other capabilities of the target to implement the basic
         * functionality. Such implementations are generally slow and sometimes
         * produce lower quality, these default implementations should return
         * GENERIC.
         */
        public static final int GENERIC = 0;

        /**
         * When an implementation of an {@link IGraphicsExtensionInterface} has
         * been tailored for a specific type of target then TARGET_COMPATIBLE
         * should be used as the compatibility value. This will cause the
         * extension to be preferred over any GENERIC extension.
         */
        public static final int TARGET_COMPATIBLE = 1000;

        /**
         * When a TARGET_COMPATIBLE extension already exists but a better
         * extension can be written for a specific target that takes advantage
         * of more advanced capabilities, then ENHANCED_TARGET_COMPATIBLE
         * compatibility should be used. For example it might be possible to
         * write a graphics extension for GL that uses simple GL capabilities
         * available in OpenGL 1.0 and a more efficient extension for the same
         * interface that uses newer capabilities of OpenGL 4.0. In this case
         * the simpler OpenGL 1.0 would return TARGET_COMPATIBLE and the more
         * advanced extension would check for the availability of OpenGL 4.0 and
         * return ENHANCED_TARGET_COMPATIBLE if OpenGL 4.0 is supported or
         * INCOMPATIBLE if it is not.
         */
        public static final int ENHANCED_TARGET_COMPATIBLE = 2000;
    }

    protected T target;

    /**
     * Used to prepare the extension for use with the specified target and to
     * determine which extension implementation should be used when multiple are
     * available. The return value represents an arbitrary
     * priority/compatibility value of this extension. If multiple extensions
     * are registered with the target it will use the one with the highest
     * priority. If a target is incompatible with an extension implementation
     * this function should return a negative number and it will not be used.
     * 
     * @param target
     * @return compatibility value
     */
    @SuppressWarnings("unchecked")
    public final int setTarget(IGraphicsTarget target) {
        try {
            this.target = (T) target;
            return getCompatibilityValue(this.target);
        } catch (ClassCastException e) {
            this.target = null;
            return Compatibilty.INCOMPATIBLE;
        }
    }

    /**
     * Get the target compability value.
     * 
     * @param target
     * @return
     */
    public abstract int getCompatibilityValue(T target);

    /**
     * Diposes the extension. Extensions should dispose any graphics objects
     * that need disposing. Default implementation does nothing
     */
    public void dispose() {
        // Default does nothing
    }
}
