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
package com.raytheon.uf.viz.core.localization;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.jms.notification.INotificationObserver;
import com.raytheon.uf.common.jms.notification.NotificationException;
import com.raytheon.uf.common.jms.notification.NotificationMessage;
import com.raytheon.uf.common.localization.FileUpdatedMessage;
import com.raytheon.uf.common.localization.ILocalizationNotificationObserver;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationNotificationObserver;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.viz.core.notification.jobs.NotificationManagerJob;

/**
 * Provides notification support for Localization
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * May 20, 2008             randerso    Initial creation
 * Sep 3, 2008  1448        chammack    Support refactored interface
 * Oct 07, 2014 2768        bclement    Added white list to filter unwanted localization types
 * Aug 24, 2015 4393        njensen     Updates for observer changes
 * 
 * </pre>
 * 
 * @author randerso
 * @version 1.0
 */

public class CAVELocalizationNotificationObserver implements
        INotificationObserver {

    private static final Logger logger = LoggerFactory
            .getLogger(CAVELocalizationNotificationObserver.class);

    private static final Set<LocalizationType> TYPE_WHITELIST = new HashSet<>(
            Arrays.asList(LocalizationType.COMMON_STATIC,
                    LocalizationType.CAVE_CONFIG, LocalizationType.CAVE_STATIC));

    private static CAVELocalizationNotificationObserver instance = null;

    private ILocalizationNotificationObserver observer;

    public static synchronized void register() {
        if (instance == null) {
            instance = new CAVELocalizationNotificationObserver();
        }
        NotificationManagerJob.addObserver(
                LocalizationNotificationObserver.LOCALIZATION_TOPIC, instance);
    }

    public static synchronized void unregister() {
        if (instance != null) {
            NotificationManagerJob.removeObserver(
                    LocalizationNotificationObserver.LOCALIZATION_TOPIC,
                    instance);
        }
    }

    private CAVELocalizationNotificationObserver() {
        observer = PathManagerFactory.getPathManager().getObserver();
    }

    @Override
    public void notificationArrived(NotificationMessage[] messages) {
        for (NotificationMessage message : messages) {
            try {
                FileUpdatedMessage fum = (FileUpdatedMessage) message
                        .getMessagePayload();
                LocalizationContext context = fum.getContext();
                LocalizationType type = context.getLocalizationType();
                if (TYPE_WHITELIST.contains(type)) {
                    observer.fileUpdateMessageReceived(fum);
                }
            } catch (NotificationException e) {
                logger.error("Error reading incoming notification", e);
            }
        }

    }

}
