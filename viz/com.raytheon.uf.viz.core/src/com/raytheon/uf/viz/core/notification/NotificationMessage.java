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
package com.raytheon.uf.viz.core.notification;

import java.util.List;

import javax.jms.Message;

import com.raytheon.uf.viz.core.exception.VizException;

/**
 * @Deprecated use
 *             {@link com.raytheon.uf.common.jms.notification.NotificationMessage}
 * 
 *             <pre>
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Sep 02, 2008  1448     chammack    Initial creation
 * Oct 04, 2010  7193     cjeanbap    Added a new method, isNotExpired().
 * Feb 01, 2011  7193     cjeanbap    Added a new method, getPublishedTime().
 * Aug 06, 2013  2228     njensen     Use deserialize(byte[])
 * Aug 16, 2013  2169     bkowal      Unzip any gzipped information
 * Jul 21, 2014  3390     bsteffen    Move to common.jms.notification make this a deprecated wrapper
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1.0
 */
@Deprecated
public class NotificationMessage {

    private com.raytheon.uf.common.jms.notification.NotificationMessage delegate;

    /**
     * Construct a notification message from a JMS message
     * 
     * @param message
     *            the jms message
     */
    public NotificationMessage(Message message) {
        this.delegate = new com.raytheon.uf.common.jms.notification.NotificationMessage(
                message);
    }

    public NotificationMessage(
            com.raytheon.uf.common.jms.notification.NotificationMessage delegate) {
        this.delegate = delegate;
    }

    public Object getMessagePayload() throws NotificationException {
        try {
            return delegate.getMessagePayload();
        } catch (com.raytheon.uf.common.jms.notification.NotificationException e) {
            throw new NotificationException(e);
        }
    }

    /**
     * Return a list of properties
     * 
     * @return a list of properties
     * @throws VizException
     */
    public List<String> getProperties() throws NotificationException {
        try {
            return delegate.getProperties();
        } catch (com.raytheon.uf.common.jms.notification.NotificationException e) {
            throw new NotificationException(e);
        }
    }

    /**
     * Get a property by name
     * 
     * @param propertyName
     *            the property name
     * @return the property value
     * @throws VizException
     */
    public Object getProperty(String propertyName) throws NotificationException {
        try {
            return delegate.getProperty(propertyName);
        } catch (com.raytheon.uf.common.jms.notification.NotificationException e) {
            throw new NotificationException(e);
        }
    }

    /**
     * Return the source information (where the message came from)
     * 
     * @return the source information
     * @throws VizException
     */
    public String getSource() throws NotificationException {
        try {
            return delegate.getSource();
        } catch (com.raytheon.uf.common.jms.notification.NotificationException e) {
            throw new NotificationException(e);
        }
    }

    /**
     * Returns true, if the message has not expired set by the time-to-live
     * value on the Message object; otherwise false.
     * 
     * @return true, if the message has not expired.
     * @throws NotificationException
     */
    public boolean isNotExpired() throws NotificationException {
        try {
            return delegate.isNotExpired();
        } catch (com.raytheon.uf.common.jms.notification.NotificationException e) {
            throw new NotificationException(e);
        }
    }

    /**
     * Returns the time as a long when the message was handled off to the
     * provider to be sent.
     * 
     * @return long, the time as in milliseconds.
     * @throws NotificationException
     */
    public long getPublishedTime() throws NotificationException {
        try {
            return delegate.getPublishedTime();
        } catch (com.raytheon.uf.common.jms.notification.NotificationException e) {
            throw new NotificationException(e);
        }
    }
}
