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
 */
package com.raytheon.uf.edex.database.purge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tree representation of the purge rules. Each Node can contain a List of
 * PurgeRule as well as a collection of other Nodes. Each Node should be a
 * specific purge key value based on the PurgeRuleSet keys. A given set of
 * key/value pairs will return the most significant purge key that matches.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 29, 2012            rjpeter     Initial creation
 * Jan 20, 2016 5262       bkowal      Updated to use {@link PurgeKeyValue}.
 * Feb 10, 2016 5307       bkowal      Ensure that all key values match the purge keys
 *                                     before selecting a rule.
 * 
 * </pre>
 * 
 * @author rjpeter
 * @version 1.0
 */
public class PurgeRuleTree {
    private final PurgeNode root;

    public PurgeRuleTree(PurgeRuleSet ruleSet) {
        PurgeNode currentParent = null;
        root = new PurgeNode(currentParent, null);
        root.setRules(ruleSet.getDefaultRules());
        List<PurgeRule> rules = ruleSet.getRules();
        if (rules != null) {
            for (PurgeRule rule : rules) {
                PurgeNode curNode = root;
                List<PurgeKeyValue> values = rule.getKeyValues();
                if ((values != null) && !values.isEmpty()) {
                    // descend purge tree
                    for (PurgeKeyValue pkv : values) {
                        Map<PurgeKeyValue, PurgeNode> childNodes = curNode
                                .getChildNodes();
                        currentParent = curNode;
                        curNode = childNodes.get(pkv);
                        if (curNode == null) {
                            curNode = new PurgeNode(currentParent, pkv);
                            childNodes.put(pkv, curNode);
                        }
                    }

                    // set the rule on the leaf node defined by key values
                    curNode.addRule(rule);
                }
            }
        }
    }

    /**
     * Returns the purge rules associated with the given key value list.
     * 
     * @param keyValues
     * @return
     */
    public List<PurgeRule> getRulesForKeys(String[] keyValues) {
        // default rule is initial closest rule
        List<PurgeRule> defaultRules = root.getRules();
        PurgeNode currentNode = root;

        if ((keyValues != null) && (keyValues.length > 0)) {
            List<PurgeNode> matchingNodes = currentNode
                    .getChildNode(keyValues[0]);
            if (matchingNodes.isEmpty()) {
                // nothing matched the initial key value.
                return defaultRules;
            }
            // iterate over key values, descending tree as far as possible.
            for (int i = 1; i < keyValues.length; i++) {
                List<PurgeNode> nodesToCompare = new ArrayList<>(matchingNodes);
                List<PurgeNode> endNodes = new ArrayList<>(
                        nodesToCompare.size());
                matchingNodes.clear();
                String value = keyValues[i];
                for (PurgeNode compareNode : nodesToCompare) {
                    if (compareNode.getChildNodes().isEmpty()) {
                        /*
                         * we have reached the end of this tree. Save the node
                         * just in case no other matching nodes are found
                         * because this may be the closest match. Ex: in the
                         * case that only a subset of keys are specified
                         * allowing any values in other keys to match (an
                         * indirect include everything regex).
                         */
                        endNodes.add(compareNode);
                        continue;
                    }
                    matchingNodes.addAll(compareNode.getChildNode(value));
                }
                if (matchingNodes.isEmpty()) {
                    matchingNodes.addAll(endNodes);
                    break;
                }
            }
            /*
             * find the final matching node indicating that all rules up to and
             * including that point have matched.
             */
            PurgeNode matchingNode = null;
            /*
             * Find the first matching node without any children indicating that
             * all rules match. Note: there may be more than one match even at
             * this level if the direct match everything regex is used too
             * carelessly. In the case of multiple matches, the nodes are
             * pre-sorted so that the nodes with the most exact text matches
             * and/or greatest length to the root take precedence over other
             * nodes.
             */
            Collections.sort(matchingNodes, new PurgeNodeComparator());
            for (PurgeNode closeNode : matchingNodes) {
                if (closeNode.childNodes.isEmpty()) {
                    matchingNode = closeNode;
                    break;
                }
            }

            return (matchingNode == null) ? defaultRules : matchingNode
                    .getRules();
        }

        return defaultRules;
    }

    private class PurgeNode {
        private final PurgeNode parent;

        private final boolean directMatch;

        // most nodes only have 1 rule
        private List<PurgeRule> rules = null;

        private final Map<PurgeKeyValue, PurgeNode> childNodes = new HashMap<>();

        public PurgeNode(PurgeNode parent, PurgeKeyValue purgeKeyValue) {
            if (parent != null && purgeKeyValue == null) {
                throw new IllegalArgumentException(
                        "Parameter purgeKeyValue cannot be null for a non-root node.");
            }
            this.parent = parent;
            directMatch = (isRoot()) ? false : purgeKeyValue
                    .getKeyValuePattern() != null;
        }

        public boolean isRoot() {
            return this.parent == null;
        }

        public boolean isDirectMatch() {
            return this.directMatch;
        }

        public PurgeNodeRankingKey getRanking() {
            /*
             * count the current node - length from root.
             */
            int length = 1;
            int exactMatches = isDirectMatch() ? 1 : 0;
            /*
             * start at the current node. count the number of parents and exact
             * matches until the root of the tree is reached.
             */
            PurgeNode current = this;
            while (current.isRoot() == false) {
                ++length;
                current = current.getParent();
                if (current.isDirectMatch()) {
                    ++exactMatches;
                }
            }

            return new PurgeNodeRankingKey(length, exactMatches);
        }

        public PurgeNode getParent() {
            return this.parent;
        }

        public void addRule(PurgeRule rule) {
            if (rules == null) {
                rules = new ArrayList<PurgeRule>(1);
            }

            rules.add(rule);
        }

        public void setRules(List<PurgeRule> rules) {
            this.rules = rules;
        }

        public List<PurgeRule> getRules() {
            return rules;
        }

        public Map<PurgeKeyValue, PurgeNode> getChildNodes() {
            return childNodes;
        }

        public List<PurgeNode> getChildNode(String keyValue) {
            List<PurgeNode> matchingChildren = new ArrayList<>();
            for (PurgeKeyValue pkv : childNodes.keySet()) {
                if ((pkv.getKeyValuePattern() != null && pkv
                        .getKeyValuePattern().matcher(keyValue).matches())
                        || pkv.getKeyValue().equals(keyValue)) {
                    matchingChildren.add(childNodes.get(pkv));
                }
            }

            return matchingChildren;
        }
    }

    private class PurgeNodeComparator implements Comparator<PurgeNode> {
        @Override
        public int compare(PurgeNode o1, PurgeNode o2) {
            return Integer.compare(o1.getRanking().getRankingValue(), o2
                    .getRanking().getRankingValue());
        }
    }
}