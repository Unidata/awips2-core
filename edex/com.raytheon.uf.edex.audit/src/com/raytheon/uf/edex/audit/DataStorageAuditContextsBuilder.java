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
package com.raytheon.uf.edex.audit;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.raytheon.uf.common.datastorage.audit.DataStorageAuditUtils;
import com.raytheon.uf.edex.core.IMessageProducer;
import com.raytheon.uf.edex.esb.camel.EDEXRouteContext;
import com.raytheon.uf.edex.esb.camel.EDEXRouteContextFactory;
import com.raytheon.uf.edex.esb.camel.context.ContextManager;
import com.raytheon.uf.edex.routes.EDEXRouteBuilder;

/**
 * Class that dynamically initializes clustered contexts for auditor routes.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 03, 2023 9019       mapeters    Initial creation
 * Feb 10, 2023 9019       smoorthy    Migrate to separate plugin
 * Jul 24, 2024 2037700    tgurney     Remove clustering and state processor
 *                                     registration (temporary, Camel 4)
 * Aug  2, 2024 2037700    tgurney     Change to use EDEXRouteContexts
 * Sep  5, 2024 2037700    tgurney     Set id on routes without an id. Accept
 *                                     ContextManager constructor arg for
 *                                     dependency tracking purposes.
 *
 * </pre>
 */

public class DataStorageAuditContextsBuilder
        implements ApplicationContextAware, BeanFactoryPostProcessor {

    private final IMessageProducer messageProducer;

    private ApplicationContext applicationContext;

    private ContextManager contextManager;

    /*
     * ContextManager is a static singleton, but still have to let Spring inject
     * it in this case so that it will recognize the dependency relationship
     * from this class -> ContextManager -> the CamelContext. Otherwise the
     * CamelContext might not exist when this class is instantiated which will
     * cause it to throw an exception in the postProcessBeanFactory method.
     */
    public DataStorageAuditContextsBuilder(IMessageProducer messageProducer,
            ContextManager contextManager) {
        this.messageProducer = messageProducer;
        this.contextManager = contextManager;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void postProcessBeanFactory(
            ConfigurableListableBeanFactory beanFactory) throws BeansException {
        for (int i = 1; i <= DataStorageAuditUtils.NUM_QUEUES; ++i) {
            DataStorageAuditer auditor = new DataStorageAuditer(messageProducer,
                    i);
            EDEXRouteContextFactory ctxFactory = applicationContext
                    .getBean(EDEXRouteContextFactory.class);
            DataStorageAuditRouteBuilder routeBuilder = new DataStorageAuditRouteBuilder(
                    auditor);
            EDEXRouteContext routeCtx = ctxFactory
                    .createClustered(routeBuilder);
            String beanName = "clusteredDataStorageAuditContext" + i;
            beanFactory.initializeBean(routeCtx, beanName);
            beanFactory.registerSingleton(beanName, routeCtx);
            contextManager.registerContextStateProcessor(routeCtx, auditor);
        }
    }

    private static class DataStorageAuditRouteBuilder extends EDEXRouteBuilder {

        private final DataStorageAuditer auditor;

        public DataStorageAuditRouteBuilder(DataStorageAuditer auditor) {
            this.auditor = auditor;
        }

        @Override
        public void configure() throws Exception {
            int id = auditor.getId();
            String auditorBeanId = "dataStorageAuditorImpl" + id;
            bindToRegistry(auditorBeanId, auditor);

            String auditEventQueueUri = DataStorageAuditUtils.QUEUE_JMS_PREFIX
                    + DataStorageAuditUtils.QUEUE_ROOT_NAME + id
                    + "?threadName=DataStorageAudit" + id;
            from(auditEventQueueUri)
                    .bean("serializationUtil", "transformFromThrift")
                    .bean(auditorBeanId, "processEvent")
                    .setId("processAuditEvent-" + id);

            String auditerCron = System
                    .getProperty("data.storage.auditer.cleanup.cron");
            String auditCleanupQuartzUri = "quartz://DataStorageAuditCleanup"
                    + id + "/?cron=" + auditerCron;
            from(auditCleanupQuartzUri).bean(auditorBeanId, "cleanup")
                    .setId(auditorBeanId);
        }
    }
}
