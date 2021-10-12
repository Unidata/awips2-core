/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     2120 South 72nd Street, Suite 900
 *                         Omaha, NE 68124
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.datastore.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datastore.ignite.AbstractIgniteManager;

/**
 * Manager for an ignite server instance.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 25, 2021 8450       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class IgniteServerManager extends AbstractIgniteManager {

    private static final long serialVersionUID = 1L;

    private final Ignite ignite;

    public IgniteServerManager(Ignite ignite) {
        this.ignite = ignite;
        setLogger(LoggerFactory.getLogger(getClass()));
    }

    @Override
    public void initialize() {
        // Nothing to do
    }

    @Override
    protected Ignite getIgnite() {
        return ignite;
    }

    @Override
    protected <K, V> IgniteCache<K, V> getCache(String cacheName) {
        IgniteCache<K, V> cache = ignite.cache(cacheName);
        if (cache == null) {
            throw new IllegalStateException("Unknown cache: " + cacheName);
        }
        return cache;
    }
}
