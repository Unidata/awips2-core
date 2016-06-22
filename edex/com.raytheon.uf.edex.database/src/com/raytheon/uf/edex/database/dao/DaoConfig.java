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

package com.raytheon.uf.edex.database.dao;

import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate4.HibernateTransactionManager;

import com.raytheon.uf.edex.core.EDEXUtil;

/**
 * Configuration settings for a data access object.<br>
 * This object contains the required information to correctly instantiate a
 * valid data access object.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Dec 11, 0007  600      bphillip  Initial Check in
 * Oct 10, 2012  1261     djohnson  Add ability for test overriding of bean
 *                                  lookups.
 * Oct 16, 2014  3454     bphillip  Upgrading to Hibernate 4
 * Jun 20, 2016  5679     rjpeter   Add admin database account.
 * 
 * </pre>
 * 
 * @author bphillip
 */
public abstract class DaoConfig {

    /** The default database name */
    public static final String DEFAULT_DB_NAME = "metadata";

    /** The admin prefix */
    private static final String ADMIN = "admin_";

    /** The session factory suffix */
    private static final String SESSION_FACTORY = "SessionFactory";

    /** The transaction manager suffix */
    private static final String TX_MANAGER = "TxManager";

    // @VisibleForTesting
    static SpringBeanLocator DEFAULT_LOCATOR = new SpringBeanLocator() {
        @Override
        public <T> T lookupBean(Class<T> resultClass, String beanName) {
            return resultClass.cast(EDEXUtil.getESBComponent(beanName));
        }
    };

    /**
     * Used to locate Spring beans. By default, uses EDEXUtil to look them up.
     * Package-level access for testing purposes.
     */
    // @VisibleForTesting
    static SpringBeanLocator locator = DEFAULT_LOCATOR;

    /**
     * The default data access object configuration. This configuration
     * specifies the metadata database
     */
    public static final DaoConfig DEFAULT = DaoConfig
            .forDatabase(DEFAULT_DB_NAME);

    /**
     * Retrieve the transaction manager.
     * 
     * @return the transaction manager
     */
    public abstract HibernateTransactionManager getTxManager();

    /**
     * Retrieve the session factory.
     * 
     * @return the session factory
     */
    public abstract SessionFactory getSessionFactory();

    /**
     * Retrieve the class type this DAO manages.
     * 
     * @return the class type
     */
    public abstract Class<?> getDaoClass();

    /**
     * Gets a DaoConfig object for the specified class using the default session
     * factory and default transaction manager.
     * 
     * @param className
     *            The class for which to create the DaoConfig object
     * @return A DaoConfig instance using the specified class, default session
     *         factory and default transaction manager.
     */
    public static DaoConfig forClass(Class<?> className) {
        return new SpringLookupDaoConfig(className);
    }

    /**
     * Gets a DaoConfig object for the specified class using the default session
     * factory and default transaction manager. If admin, will login as a super
     * user, otherwise a normal user login.
     * 
     * @param className
     *            The class for which to create the DaoConfig object
     * @param admin
     *            Whether to login as a super user or not
     * @return A DaoConfig instance using the specified class, default session
     *         factory and default transaction manager.
     */
    public static DaoConfig forClass(Class<?> className, boolean admin) {
        return new SpringLookupDaoConfig(className, admin);
    }

    /**
     * Gets a DaoConfig object for the specified class using the default session
     * factory and default transaction manager.
     * 
     * @param className
     *            The class for which to create the DaoConfig object
     * @return A DaoConfig instance using the specified class, default session
     *         factory and default transaction manager.
     * @throws ClassNotFoundException
     *             If the given class name does not exist on the class path
     */
    public static DaoConfig forClass(String className)
            throws ClassNotFoundException {
        return new SpringLookupDaoConfig(DaoConfig.class.getClassLoader()
                .loadClass((className).trim()));
    }

    /**
     * Gets a DaoConfig object for the specified class and database
     * 
     * @param dbName
     *            The database name
     * @param className
     *            The class object
     * @return A DaoConfig instance with the specified database name and class
     *         name
     */
    public static DaoConfig forClass(String dbName, Class<?> className) {
        return new SpringLookupDaoConfig(dbName, className);
    }

    /**
     * Gets a DaoConfig object for the specified class and database
     * 
     * @param dbName
     *            The database name
     * @param className
     *            The class object
     * @param admin
     *            Whether to login as a super user or not
     * @return A DaoConfig instance with the specified database name and class
     *         name
     */
    public static DaoConfig forClass(String dbName, Class<?> className,
            boolean admin) {
        return new SpringLookupDaoConfig(dbName, className, admin);
    }

    /**
     * Gets a DaoConfig object for the specified class and database
     * 
     * @param dbName
     *            The database name
     * @param className
     *            The class name
     * @return A DaoConfig instance with the specified database name and class
     *         name
     * @throws ClassNotFoundException
     *             If the given class name does not exist on the class path
     */
    public static DaoConfig forClass(String dbName, String className)
            throws ClassNotFoundException {
        return new SpringLookupDaoConfig(dbName, DaoConfig.class
                .getClassLoader().loadClass((className).trim()));
    }

    /**
     * Gets a DaoConfig object for the specified database
     * 
     * @param dbName
     *            The database name
     * @return
     */
    public static DaoConfig forDatabase(String dbName) {
        return new SpringLookupDaoConfig(dbName);
    }

    /**
     * Gets a DaoConfig object for the specified database. If admin will login
     * as a super user, otherwise will use a normal user login.
     * 
     * @param dbName
     *            The database name
     * @param admin
     *            Whether to login as a super user or not
     * @return
     */
    public static DaoConfig forDatabase(String dbName, boolean admin) {
        return new SpringLookupDaoConfig(dbName, admin);
    }

    private static class SpringLookupDaoConfig extends DaoConfig {

        /** The class for which the desired data access object is to be used for */
        private final Class<?> daoClass;

        /** The name of the Hibernate session factory to use */
        private final String sessionFactoryName;

        /** The name of the Hibernate transaction manager to use */
        private final String txManagerName;

        /**
         * Default constructor.
         */
        private SpringLookupDaoConfig() {
            this((Class<?>) null);
        }

        /**
         * Constructs a DaoConfig object using the specified class name, default
         * session factory, and the default transaction manager. Database login
         * will not be as a super user.
         * 
         * @param className
         *            The class object
         */
        private SpringLookupDaoConfig(Class<?> className) {
            this(DEFAULT_DB_NAME, className, false);
        }

        /**
         * Constructs a DaoConfig object using the specified class name, default
         * session factory, and the default transaction manager. If admin, the
         * database login will be as a super user, otherwise a normal user login
         * will be used.
         * 
         * @param className
         *            The class object
         * @param admin
         *            Whether to login as a super user or not
         */
        private SpringLookupDaoConfig(Class<?> className, boolean admin) {
            this(DEFAULT_DB_NAME, className, admin);
        }

        /**
         * Constructs a DaoConfig object for the specified database.
         * 
         * @param dbName
         *            The database name
         */
        private SpringLookupDaoConfig(String dbName) {
            this(dbName, null, false);
        }

        /**
         * Constructs a DaoConfig object for the specified database. If admin,
         * the database login will be as a super user, otherwise a normal user
         * login will be used.
         * 
         * @param dbName
         *            The database name
         * @param admin
         *            Whether to login as a super user or not
         */
        private SpringLookupDaoConfig(String dbName, boolean admin) {
            this(dbName, null, admin);
        }

        /**
         * Constructs a DaoConfig object for the specified database using the
         * specified class name. The appropriate session factory and transaction
         * manager will be determined from the database name.
         * 
         * @param dbName
         *            The database name
         * @param daoClass
         *            The class object
         */
        private SpringLookupDaoConfig(String dbName, Class<?> daoClass) {
            this(dbName, daoClass, false);
        }

        /**
         * Constructs a DaoConfig object for the specified database using the
         * specified class name. The appropriate session factory and transaction
         * manager will be determined from the database name. If admin, the
         * database login will be as a super user, otherwise a normal user login
         * will be used.
         * 
         * @param dbName
         *            The database name
         * @param daoClass
         *            The class object
         * @param admin
         *            Whether to login as a super user or not
         */
        private SpringLookupDaoConfig(String dbName, Class<?> daoClass,
                boolean admin) {
            this.daoClass = daoClass;
            String prefix = "";
            if (admin) {
                prefix = ADMIN;
            }
            this.sessionFactoryName = prefix + dbName + SESSION_FACTORY;
            this.txManagerName = prefix + dbName + TX_MANAGER;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public HibernateTransactionManager getTxManager() {
            return locator.lookupBean(HibernateTransactionManager.class,
                    txManagerName);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SessionFactory getSessionFactory() {
            return locator.lookupBean(SessionFactory.class, sessionFactoryName);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<?> getDaoClass() {
            return daoClass;
        }
    }
}
