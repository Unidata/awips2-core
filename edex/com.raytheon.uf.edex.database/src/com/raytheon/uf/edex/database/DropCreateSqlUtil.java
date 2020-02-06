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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaExport.Action;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.TargetDescriptor;

/**
 * Methods to generate drop/create sql scripts from Hibernate annotated classes
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 26, 2019 6140       tgurney     Initial creation
 * May 31, 2019 6140       tgurney     Specify the legacy naming strategy
 * Aug 12, 2019 6140       tgurney     No longer specify legacy naming strategy
 *
 * </pre>
 *
 */

public class DropCreateSqlUtil {

    /** Captures hibernate generated DDL commands in a list */
    private static class ListTargetDescriptor
            implements TargetDescriptor, ScriptTargetOutput {

        protected final List<String> commands = new ArrayList<>();

        @Override
        public EnumSet<TargetType> getTargetTypes() {
            return EnumSet.of(TargetType.SCRIPT);
        }

        @Override
        public ScriptTargetOutput getScriptTargetOutput() {
            return this;
        }

        @Override
        public void prepare() {
        }

        @Override
        public void accept(String command) {
            commands.add(command);
        }

        @Override
        public void release() {
        }

        public List<String> getCommands() {
            return commands;
        }

    }

    /**
     * Generates the create ddl for the passed in hibernate annotated pojos. All
     * relationships need to be available in the passed in array of classes.
     * i.e. if a class has a OneToMany the subclass will also need to be in the
     * passed in array.
     *
     * @param classes
     *            Classes to generate the ddl for.
     * @param serviceRegistry
     * @return List of create sql.
     * @throws org.hibernate.AnnotationException
     */
    public static List<String> getCreateSql(Collection<Class<?>> classes,
            ServiceRegistry serviceRegistry)
            throws org.hibernate.AnnotationException {
        MetadataSources metadataSources = getMetadataSources(classes,
                serviceRegistry);
        SchemaExport export = new SchemaExport();
        ListTargetDescriptor targetDescriptor = new ListTargetDescriptor();
        Metadata m = metadataSources.getMetadataBuilder().build();
        export.doExecution(Action.CREATE, false, m,
                metadataSources.getServiceRegistry(), targetDescriptor);
        return targetDescriptor.getCommands();
    }

    /**
     * Generates the drop ddl for the passed in hibernate annotated pojos. All
     * relationships need to be available in the passed in array of classes.
     * i.e. if a class has a OneToMany the subclass will also need to be in the
     * passed in array.
     *
     * @param classes
     *            Classes to generate the ddl for.
     * @param serviceRegistry
     *
     * @return List of drop sql.
     * @throws org.hibernate.AnnotationException
     */
    public static List<String> getDropSql(Collection<Class<?>> classes,
            ServiceRegistry serviceRegistry)
            throws org.hibernate.AnnotationException {
        MetadataSources metadataSources = getMetadataSources(classes,
                serviceRegistry);
        SchemaExport export = new SchemaExport();
        ListTargetDescriptor targetDescriptor = new ListTargetDescriptor();
        Metadata m = metadataSources.getMetadataBuilder().build();
        export.doExecution(Action.DROP, false, m,
                metadataSources.getServiceRegistry(), targetDescriptor);
        return targetDescriptor.getCommands();
    }

    private static MetadataSources getMetadataSources(
            Collection<Class<?>> classes, ServiceRegistry serviceRegistry) {
        MetadataSources metadata = new MetadataSources(serviceRegistry);
        for (Class<?> c : classes) {
            metadata.addAnnotatedClass(c);
        }

        return metadata;
    }
}
