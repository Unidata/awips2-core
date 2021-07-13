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

import org.apache.ignite.configuration.IgniteConfiguration;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Generates unique ignite config instances using a prototype spring bean.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 1, 2021  8450       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class IgniteConfigSpringGenerator
        implements IIgniteConfigGenerator, ApplicationContextAware {

    private final String configBeanName;

    private final Object lock = new Object();

    private IgniteConfiguration config;

    private ApplicationContext springContext;

    /**
     * Constructor.
     *
     * @param config
     *            the config of the ignite instance to start/manage
     * @param configBeanName
     *            the bean name/ID of the config param. This is needed to get a
     *            fully new config instance when restarting (the bean also must
     *            have scope="prototype" set). The config param is also needed
     *            since the springContext may not be set yet.
     */
    public IgniteConfigSpringGenerator(IgniteConfiguration config,
            String configBeanName) {
        this.config = config;
        this.configBeanName = configBeanName;
    }

    @Override
    public IgniteConfiguration getNewConfig() {
        synchronized (lock) {
            if (config == null && springContext == null) {
                throw new IllegalStateException(
                        "null config and springContext");
            }

            if (springContext == null) {
                IgniteConfiguration tempConfig = config;
                /*
                 * Set to null because we cannot reuse this instance,
                 * springContext must be set before this method can be called
                 * again or we will throw the IllegalStateException above
                 */
                config = null;
                return tempConfig;
            }
        }

        return springContext.getBean(configBeanName, IgniteConfiguration.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        springContext = applicationContext;
    }
}
