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
package com.raytheon.edex.db.dao;

import java.util.List;

import org.hibernate.Criteria;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.raytheon.edex.subscription.Subscription;
import com.raytheon.uf.common.dataplugin.persist.PersistableDataObject;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.database.dao.CoreDao;
import com.raytheon.uf.edex.database.dao.DaoConfig;

/**
 * EDEX Data Access Layer (DAL) Data Access Object (DAO) for interactions with
 * the subscription tables (awips.subscription and awips.scripts).
 * <P>
 * Operations provided are: <br>
 * {@link #persistSubscription(AbstractDataRecord) persist a subscription},
 * {@link #getSubscriptions() get list of available subscriptions},
 * {@link #removeSubscription(AbstractDataRecord) delete a subscription}, and
 * {@link #updateSubscription(AbstractDataRecord) update an existing
 * subscription}.
 * <p>
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date       	Ticket#		Engineer	Description
 * ------------	----------	-----------	--------------------------
 * 27Apr2007    208         MW Fegan    Initial creation.
 * 10/16/2014   3454       bphillip    Upgrading to Hibernate 4
 * 10/28/2014   3454        bphillip    Fix usage of getSession()
 * 
 * </pre>
 * 
 * @author mfegan
 * @version 1
 */

public class SubscribeDAO extends CoreDao {

    /**
     * 
     */
    public SubscribeDAO() {
        super(DaoConfig.forClass(Subscription.class));
    }

    /**
     * Updates (replaces) the specified data record in the database.
     * 
     * @param val
     *            the data record to update
     * 
     * @throws DataAccessLayerException
     *             if the update operation fails
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void updateSubscription(PersistableDataObject val)
            throws DataAccessLayerException {
        super.update(val);
    }

    /**
     * Gets the list of currently available subscriptions from the database.
     * 
     * @return the list of currently available subscriptions
     * 
     * @throws DataAccessLayerException
     *             if the query fails.
     */
    public List<Subscription> getSubscriptions()
            throws DataAccessLayerException {
        return (List<Subscription>)loadAll();
    }
    
    @SuppressWarnings("unchecked")
    private List<Subscription> loadAll() {
        return txTemplate
                .execute(new TransactionCallback<List<Subscription>>() {
                    @Override
                    public List<Subscription> doInTransaction(
                            TransactionStatus status) {
                        Criteria criteria = getCurrentSession().createCriteria(
                                Subscription.class);
                        return criteria.list();
                    }
                });
    }

    /**
     * Retrieves the Subscription object matching the specified data URI.
     * 
     * @param dataURI
     *            the data URI to match
     * 
     * @return the Subscription object
     * 
     * @throws DataAccessLayerException
     *             in the event of an error retrieving the Subscription object
     */
    public Object getSubscription(String dataURI)
            throws DataAccessLayerException {

        return this.queryBySingleCriteria("identifier", dataURI);
    }
}
