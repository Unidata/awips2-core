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
package com.raytheon.uf.edex.database.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import com.raytheon.uf.common.time.CommutativeTimestamp;
import com.raytheon.uf.common.time.FormattedDate;

/**
 * Type override for {@link java.util.Date} and {@link java.sql.Timestamp} so
 * that Hibernate returns a
 * {@link com.raytheon.uf.common.time.CommutativeTimestamp}.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 05, 2015 4486       rjpeter     Initial creation.
 * Sep 14, 2015 4486       rjpeter     Return FormattedDate.
 * Jun 23, 2016 5696       rjpeter     Return CommutativeTimestamp.
 * Feb 26, 2019 6140       tgurney     Hibernate 5 UserType fix
 * </pre>
 *
 * @author rjpeter
 */
public class CommutativeTimestampType implements UserType {
    private static final int[] SQL_TYPES = { Types.TIMESTAMP };

    @Override
    public int[] sqlTypes() {
        return SQL_TYPES;
    }

    @Override
    public Class<?> returnedClass() {
        return Timestamp.class;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        if (x == null) {
            return y == null;
        }

        if (y == null) {
            return false;
        }

        return translate(x).equals(translate(y));
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        if (x == null) {
            return 0;
        }

        return x.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names,
            SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException {
        Timestamp s = rs.getTimestamp(names[0]);
        return translate(s);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index,
            SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if (value == null) {
            st.setTimestamp(index, null);
        } else if (value instanceof Timestamp) {
            st.setTimestamp(index, (Timestamp) value);
        } else if (value instanceof Date) {
            st.setTimestamp(index, new Timestamp(((Date) value).getTime()));
        }
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return translate(value);
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return translate(value);
    }

    @Override
    public Object assemble(Serializable cached, Object owner)
            throws HibernateException {
        return translate(cached);
    }

    @Override
    public Object replace(Object original, Object target, Object owner)
            throws HibernateException {
        return translate(original);
    }

    protected CommutativeTimestamp translate(Object o)
            throws HibernateException {
        if (o == null) {
            return null;
        }

        /*
         * TODO: replace all FormattedDate with CommutativeTimestamp once 16.4.1
         * no longer in field
         */
        CommutativeTimestamp rval = null;
        if (o instanceof Timestamp) {
            rval = new FormattedDate((Timestamp) o);
        } else if (o instanceof Date) {
            rval = new FormattedDate((Date) o);
        } else if (o instanceof Long) {
            rval = new FormattedDate(((Long) o).longValue());
        } else {
            throw new HibernateException("Cannot translate class "
                    + o.getClass() + " to " + CommutativeTimestamp.class);
        }

        return rval;
    }

    public static String[] getRegistryKeys() {
        return new String[] { "timestamp", Timestamp.class.getName(),
                java.util.Date.class.getName() };
    }
}
