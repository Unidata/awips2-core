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

package com.raytheon.uf.edex.database;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.orm.hibernate5.LocalSessionFactoryBuilder;

import com.raytheon.uf.edex.database.type.CommutativeTimestampType;

/**
 * Extension of the AnnotationSessionFactoryBean provided by Spring.
 * <p>
 * This class utilizes the SerializableManager to dynamically discover which
 * classes are mapped using annotations. The existing
 * AnnotationSessionFactoryBean requires a list of annotated classes be provided
 * in the Hibernate configuration. This approach is inadequate for the software
 * architecture.
 *
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------------------------------------------
 * Oct 08, 2008  1532     bphillip  Initial checkin
 * Jun 18, 2013  2117     djohnson  Remove use of config.buildSettings().
 * Oct 14, 2013  2361     njensen   Changes to support new technique for finding classes
 * Oct 16, 2014  3454     bphillip  Upgrading to Hibernate 4
 * Aug 05, 2015  4486     rjpeter   Ensure hibernate returns java.util.Date.
 * Sep 01, 2016  5846     rjpeter   Fix infinite database session creation issue.
 * Feb 26, 2017  6140     tgurney   Move drop/create sql scripts to new class
 *                                  (Hibernate 5 upgrade)
 * </pre>
 */
public class DatabaseSessionFactoryBean extends LocalSessionFactoryBean {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected Class<?>[] accessibleClasses = null;

    /**
     * Creates a new MetadataSessionFactoryBean.
     * <p>
     * This constructor uses the SerializableManager to dynamically discover and
     * provide the underlying Hibernate SessionFactory with a list of mapped
     * classes
     */
    public DatabaseSessionFactoryBean() {
        super();
    }

    public void setDatabaseSessionConfiguration(
            DatabaseSessionConfiguration databaseSessionConfiguration) {
        // make own copy so can modify it
        List<Class<?>> annotatedClasses = new LinkedList<>(
                databaseSessionConfiguration.getAnnotatedClasses());

        if (databaseSessionConfiguration != null) {
            Iterator<Class<?>> iter = annotatedClasses.iterator();
            while (iter.hasNext()) {
                Class<?> clazz = iter.next();
                if (!databaseSessionConfiguration.matches(clazz.getName())) {
                    iter.remove();
                }
            }
        }

        // Set the annotated classes
        this.setAnnotatedClasses(annotatedClasses.toArray(new Class[] {}));

    }

    @Override
    public void setAnnotatedClasses(Class<?>... annotatedClasses) {
        super.setAnnotatedClasses(annotatedClasses);
        // overrode setter because we need access to the classes
        // for determining dependent classes for create/drop SQL
        this.accessibleClasses = annotatedClasses;
    }

    /**
     * Get the annotated classes associated with the database session
     *
     * @return
     */
    public Class<?>[] getAnnotatedClasses() {
        return accessibleClasses;
    }

    @Override
    protected SessionFactory buildSessionFactory(
            LocalSessionFactoryBuilder sfb) {
        try {
            sfb.registerTypeOverride(new CommutativeTimestampType(),
                    CommutativeTimestampType.getRegistryKeys());
            return super.buildSessionFactory(sfb);
        } catch (Throwable e) {
            logger.error("Failed to build database session factory", e);
            return null;
        }
    }
}
