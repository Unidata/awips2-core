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

package com.raytheon.uf.viz.core.drawables;

/**
 * Represents a renderable shape.
 * 
 * TODO: Contemplate refocusing, renaming, removing, or moving isMutable(). The
 * concept of mutable vs immutable is not consistently implemented in the
 * various implementations. For example, GLWireframeShape2D uses it for some
 * optimizations, but you can alter an immutable shape. GLShadedShape doesn't
 * seem to use mutable in most scenarios, making the flag fairly irrelevant.
 * Therefore it is ambiguous to developers on whether or not mutable should be
 * true or false. If this todo is worked, update IGraphicsTarget API
 * appropriately.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 07/1/06                 chammack    Initial Creation.
 * Feb 10, 2015   3947     njensen     Improved javadoc
 * 
 * </pre>
 * 
 * @author chammack
 * 
 */
public interface IShape {

    /**
     * Prepares the data for fast access NOTE: this will flush the existing
     * data, and addLineSegment should not be called.
     * 
     * If data is mutable, do not compile.
     * 
     */
    public abstract void compile();

    /**
     * Returns true if the shape is mutable (e.g. will not change after initial
     * creation)
     * 
     * @return if the shape is mutable
     */
    public abstract boolean isMutable();

    /**
     * Returns true if the shape is currently in a drawable state
     * 
     * @return if the shape is in a drawable state
     */
    public abstract boolean isDrawable();

    /**
     * Dispose the resource (if necessary)
     * 
     */
    public abstract void dispose();

    /**
     * Clear the shape's geometry, but leave open for use
     * 
     */
    public abstract void reset();

}
