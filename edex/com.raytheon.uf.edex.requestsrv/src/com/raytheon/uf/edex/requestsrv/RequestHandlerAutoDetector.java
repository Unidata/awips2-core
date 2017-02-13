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
package com.raytheon.uf.edex.requestsrv;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.common.util.registry.RegistryException;

/**
 * ApplicationListener to automatically detect IRequestHandlers and attempt to
 * register them with the HandlerRegistry IF they have not already been
 * registered explicitly in Spring XML. The IRequestHandler's corresponding
 * IServerRequest class name is determined through
 * IRequestHandler.getRequestType().
 * 
 * Note that this cannot detect all variants of IRequestHandlers. In particular
 * it may fail for IRequestHandlers that declare their IServerRequest as an
 * interface, an abstract class, or have multiple handleRequest(IServerRequest)
 * methods due to inheritance. In those cases it is recommended that you don't
 * do that, or if you must, the IRequestHandler implementation should override
 * getRequestType().
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 2, 2017   6111      njensen     Initial creation
 *
 * </pre>
 *
 * @author njensen
 */

public class RequestHandlerAutoDetector implements ApplicationContextAware,
        ApplicationListener<ContextRefreshedEvent> {

    private final Logger logger = LoggerFactory
            .getLogger(RequestHandlerAutoDetector.class);

    private ConfigurableApplicationContext appContext;

    private final HandlerRegistry registry;

    /**
     * Constructor
     * 
     * @param registry
     */
    public RequestHandlerAutoDetector(HandlerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        if (applicationContext instanceof ConfigurableApplicationContext) {
            this.appContext = (ConfigurableApplicationContext) applicationContext;
            this.appContext.addApplicationListener(this);
        } else {
            throw new ApplicationContextException(
                    "ApplicationContext is not configurable");
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        /*
         * The ContextRefreshedEvent happens after all the beans in the
         * ApplicationContext have been instantiated, triggered by
         * ConfigurableApplicationContext.refresh().
         */
        Collection<IRequestHandler<?>> registryValues = registry
                .getRegisteredValues();
        Collection<IRequestHandler> detected = this.appContext
                .getBeansOfType(IRequestHandler.class).values();
        logger.info(detected.size() + " IRequestHandlers were detected.");
        logger.info(registryValues.size()
                + " IRequestHandlers were explicitly registered through Spring XML.");

        /*
         * If the detected IRequestHandler is not already registered, add it to
         * the map toAdd and attempt to determine its IServerRequest through its
         * getRequestType() method.
         */
        Map<Class<?>, IRequestHandler<?>> toAdd = new HashMap<>();
        for (IRequestHandler<?> handler : detected) {
            if (!registryValues.contains(handler)) {
                Class<?> serverReqClz = handler.getRequestType();

                if (serverReqClz != null && !serverReqClz.isInterface()
                        && !Modifier.isAbstract(serverReqClz.getModifiers())) {
                    String key = serverReqClz.getName();
                    // extra safety check
                    if (registry.getRegisteredObject(key) == null) {
                        toAdd.put(serverReqClz, handler);
                    }
                } else {
                    /*
                     * TODO Update error message. Eventually we want to
                     * discourage anyone registering explicitly through Spring
                     * XML with the factory-bean="handlerRegistry"
                     * factory-method="register" technique, as that is a hack.
                     */
                    StringBuilder sb = new StringBuilder()
                            .append("Cannot automatically register IRequestHandler ")
                            .append(handler.getClass().getName())
                            .append(" with key ").append(serverReqClz)
                            .append(". Either implement ")
                            .append(handler.getClass().getSimpleName())
                            .append(".getRequestType() or ")
                            .append("explicitly register the IServerRequest")
                            .append(" class' concrete classname through Spring XML.");
                    logger.error(sb.toString());
                }
            }
        }

        /*
         * Register the (detected but not registered) IRequestHandlers with the
         * HandlerRegistry.
         */
        int successCount = 0;
        for (Entry<Class<?>, IRequestHandler<?>> entry : toAdd.entrySet()) {
            Class<?> serverReqClz = entry.getKey();
            IRequestHandler<?> handler = entry.getValue();
            try {
                registry.register(serverReqClz.getName(), handler);
                successCount++;
            } catch (RegistryException e) {
                logger.error("Error auto-registering IRequestHandler "
                        + handler.getClass().getName() + " with key "
                        + serverReqClz.getName(), e);
            }
        }
        logger.info("Successfully auto-registered " + successCount
                + " IRequestHandlers.");

        /*
         * Do some safety checks to warn developers of potential errors.
         */
        Collection<IRequestHandler<?>> registeredValues = registry
                .getRegisteredValues();
        checkForMultipleKeys(registeredValues);
        checkForUnregistered(registeredValues, detected);
        if (logger.isDebugEnabled()) {
            checkForUndetectedButRegistered(registeredValues, detected);
        }
    }

    /**
     * Checks if an IRequestHandler instance is registered with multiple
     * IServerRequest keys.
     * 
     * @param registered
     */
    private void checkForMultipleKeys(
            Collection<IRequestHandler<?>> registered) {
        Set<IRequestHandler<?>> check = new HashSet<>();
        for (IRequestHandler<?> handler : registered) {
            if (!check.add(handler)) {
                StringBuilder sb = new StringBuilder();
                sb.append("IRequestHandler ")
                        .append(handler.getClass().getName())
                        .append(" has multiple IServerRequest classname keys.")
                        .append(" This is considered bad practice.");
                logger.warn(sb.toString());
            }
        }
    }

    /**
     * Checks for any IRequestHandlers that were detected but never registered
     * with the HandlerRegistry.
     * 
     * @param registered
     * @param detected
     */
    @SuppressWarnings("rawtypes")
    private void checkForUnregistered(Collection<IRequestHandler<?>> registered,
            Collection<IRequestHandler> detected) {
        List<IRequestHandler<?>> copy = new ArrayList<>(detected);
        copy.removeAll(registered);
        if (!copy.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("The following IRequestHandlers are not registered: ");
            sb.append(copy);
            logger.warn(sb.toString());
        }
    }

    /**
     * Checks for any IRequestHandlers that were registered but were not
     * detected by ApplicationContext.getBeansOfType(IRequestHandler.class).
     * This can happen depending on how the Spring XML is written.
     * 
     * @param registered
     * @param detected
     */
    @SuppressWarnings("rawtypes")
    private void checkForUndetectedButRegistered(
            Collection<IRequestHandler<?>> registered,
            Collection<IRequestHandler> detected) {
        Set<IRequestHandler<?>> regSet = new HashSet<>(registered);
        regSet.removeAll(detected);
        if (!regSet.isEmpty()) {
            logger.info(
                    "The following IRequestHandlers were registered but not detected "
                            + regSet);
        }
    }

}
