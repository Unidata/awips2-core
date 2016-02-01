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
package com.raytheon.uf.common.localization;

import com.raytheon.uf.common.localization.FileUpdatedMessage.FileChangeType;

/**
 * This class serves as an intermediary between the old localization file
 * observing API (ILocalizationFileObserver) and the new localization path
 * observing API (ILocalizationPathObserver). For code that is still registering
 * and using the old observers, this class attempts to bridge the differences
 * between the two APIs and recreate the same behavior as existed before.
 * 
 * TODO: When no code is using the old observers anymore, this class can be
 * deleted.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 28, 2016  4834      njensen     Initial creation
 *
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public final class LocalizationFileIntermediateObserver implements
        ILocalizationPathObserver {

    private final LocalizationFile watchedFile;

    private final ILocalizationFileObserver observer;

    /**
     * Constructor, do not allow instantiation outside of this package
     */
    protected LocalizationFileIntermediateObserver(
            LocalizationFile watchedFile, ILocalizationFileObserver observer) {
        this.watchedFile = watchedFile;
        this.observer = observer;
    }

    @Override
    public void fileChanged(ILocalizationFile file) {
        // this should never be called
        throw new UnsupportedOperationException();
    }

    /**
     * Recreates the behavior of the API when observing was accomplished with
     * ILocalizationFileObservers instead of ILocalizationPathObservers.
     * 
     * This method exists solely to ensure the exact changeType is sent along.
     * FileChangeType DELETED can be detected with the NON_EXISTENT_CHECKSUM on
     * the file, however, the path observer API has no distinction between ADDED
     * and UPDATED. In theory observing code should be written to either not
     * distinguish between ADDED and UPDATED or already have enough knowledge in
     * memory to know whether the file that arrived was brand new or just an
     * update.
     * 
     * @param file
     * @param changeType
     */
    public void fileChanged(ILocalizationFile file, FileChangeType changeType) {
        LocalizationContext listenContext = watchedFile.getContext();
        LocalizationContext notifyContext = file.getContext();
        int compare = notifyContext.getLocalizationLevel().compareTo(
                listenContext.getLocalizationLevel());

        /*
         * Before the API changes, earlier in the notification routing we would
         * discard notifications in specific scenarios. The checks below
         * recreate that behavior by returning early if the notification should
         * be discarded.
         */
        if (compare < 0) {
            /*
             * Notification was for a lower ranking level than the observer was
             * registered; e.g. observer was registered for USER notifications
             * and a SITE notification arrived.
             */
            return;
        } else if (compare == 0) {
            if (!notifyContext.getContextName().equals(
                    listenContext.getContextName())) {
                /*
                 * Notification level matches observer's level, but the name
                 * doesn't match; e.g. observer was registered for site OAX but
                 * a site DMX notification arrived.
                 */
                return;
            }
        } else {
            LocalizationContext activeContext = PathManagerFactory
                    .getPathManager().getContext(
                            notifyContext.getLocalizationType(),
                            notifyContext.getLocalizationLevel());
            if (!notifyContext.getContextName().equals(
                    activeContext.getContextName())) {
                /*
                 * Notification was for a higher ranking level than the observer
                 * was registered, but the name doesn't match the currently
                 * active localization for that level; e.g. observer was
                 * registered for BASE and a site DMX notification arrived but
                 * this JVM is running with localization site=OAX.
                 */
                return;
            }
        }

        /*
         * Notification appears to be relevant, send it along to the observer
         * using the old API.
         */
        FileUpdatedMessage fum = new FileUpdatedMessage(file.getContext(),
                file.getPath(), changeType, file.getTimeStamp().getTime(),
                file.getCheckSum());
        observer.fileUpdated(fum);
    }

}
