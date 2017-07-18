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
package com.raytheon.uf.common.inventory.tree;

import com.raytheon.uf.common.dataplugin.level.Level;

/**
 * 
 * The base level node for derived parameters, each Level Node should be able to
 * handle time queries and metadata requests.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -------------------
 * Jan 19, 2010           bsteffen  Initial creation
 * Jul 18, 2017  6345     bsteffen  Javadoc isConstant
 * 
 * </pre>
 * 
 * @author bsteffen
 */
public abstract class AbstractRequestableNode extends LevelNode {

    public static class Dependency {

        public Dependency(AbstractRequestableNode node, int timeOffset) {
            super();
            this.node = node;
            this.timeOffset = timeOffset;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((node == null) ? 0 : node.hashCode());
            result = prime * result + timeOffset;
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
            Dependency other = (Dependency) obj;
            if (node == null) {
                if (other.node != null) {
                    return false;
                }
            } else if (!node.equals(other.node)) {
                return false;
            }
            if (timeOffset != other.timeOffset) {
                return false;
            }
            return true;
        }

        public AbstractRequestableNode node;

        public int timeOffset;
    }

    public AbstractRequestableNode() {
    }

    public AbstractRequestableNode(Level level) {
        super(level);
    }

    public AbstractRequestableNode(LevelNode that) {
        super(that);
    }

    /**
     * False indicates that this is an interesting parameter. True indicates it
     * is a fundamental attribute of the derivation and likely not interesting
     * outside of the math. Constant parameters are generally invisible outside
     * of the inventory. Constant parameters are not necessarily unchanging
     * across time and space although most constant parameters are unchanging in
     * at least one dimension.
     * 
     * For example a node representing pressure at 500MB level is really boring
     * so isConstant returns true but a node representing Pressure at surface is
     * much more interesting and would return false.
     * 
     * Another way to look at it is constant things are generally created from
     * the PluginDataObject metadata while non-constant things come from the
     * actual data itself.
     */
    public abstract boolean isConstant();

}
