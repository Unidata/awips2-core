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

package com.raytheon.uf.common.time.dbtype;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.EnumSet;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

/**
 * Custom Hibernate type for storing and retrieving EnumSets
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 *                         bphillip    Initial Creation
 * 10/16/2014   3454       bphillip    Upgrading to Hibernate 4
 * Oct 14, 2016 5934       njensen     Moved to time plugin
 * Feb 26, 2019 6140       tgurney     Hibernate 5 UserType fix
 *
 * </pre>
 *
 * @author bphillip
 */
public class EnumSetType<E extends Enum<E>> implements UserType {

    /** Stored as a varchar in the databse */
    private static final int[] SQL_TYPES = { Types.VARCHAR };

    /** The EnumSet parameter class */
    private Class<E> clazz;

    /**
     * Creates a new EnumSetType for a specific class
     *
     * @param clazz
     *            The enum type
     */
    protected EnumSetType(Class<E> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Object assemble(Serializable cached, Object owner)
            throws HibernateException {
        return cached;
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        if (x == y) {
            return true;
        }
        if (null == x || null == y) {
            return false;
        }
        return x.equals(y);
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names,
            SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException {
        String name = rs.getString(names[0]);
        EnumSet<E> result = EnumSet.noneOf(clazz);
        if (!rs.wasNull() && !"[]".equals(name)) {
            String[] tokens = name.split(",");
            if (tokens.length > 0) {
                tokens[0] = tokens[0].replace("[", "");
                tokens[tokens.length - 1] = tokens[tokens.length - 1]
                        .replace("]", "");
            }
            for (String token : tokens) {
                result.add(Enum.valueOf(clazz, token.trim()));
            }
        }
        return result;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index,
            SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if (null == value) {
            st.setNull(index, Types.VARCHAR);
        } else {
            st.setString(index, value.toString());
        }

    }

    @Override
    public Object replace(Object original, Object target, Object owner)
            throws HibernateException {
        // TODO Auto-generated method stub
        return original;
    }

    @Override
    public Class<E> returnedClass() {
        return clazz;
    }

    @Override
    public int[] sqlTypes() {
        return SQL_TYPES;
    }
}
