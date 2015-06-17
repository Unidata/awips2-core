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
package com.raytheon.uf.common.serialization.thrift.test;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Tests that a field of type Date or Calendar can still be deserialized if
 * the field is switched to the other type.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 17, 2015 4564       njensen     Initial creation
 *
 * </pre>
 *
 * @author njensen
 * @version 1.0	
 */
public class DateTester {

    @DynamicSerialize
    public static class DateTestV1 {

        @DynamicSerializeElement
        private Date date;

        @DynamicSerializeElement
        private Calendar cal;

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public Calendar getCal() {
            return cal;
        }

        public void setCal(Calendar cal) {
            this.cal = cal;
        }
    }

    @DynamicSerialize
    public static class DateTestV2 {
        @DynamicSerializeElement
        private Calendar date;

        @DynamicSerializeElement
        private Date cal;

        public Calendar getDate() {
            return date;
        }

        public void setDate(Calendar date) {
            this.date = date;
        }

        public Date getCal() {
            return cal;
        }

        public void setCal(Date cal) {
            this.cal = cal;
        }

    }

    public static void main(String[] args) throws Exception {
        DateTestV1 testIn = new DateTestV1();
        testIn.cal = Calendar.getInstance();
        testIn.cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        testIn.cal.set(1985, 03, 15);
        testIn.date = new Date(0);

        byte[] b = SerializationUtil.transformToThrift(testIn);
        for (int i = 0; i < b.length; i++) {
            if (b[i] == 'V' && b[i + 1] == '1') {
                b[i + 1] = (byte) '2';
            }
        }

        DateTestV2 testOut = SerializationUtil.transformFromThrift(
                DateTestV2.class, b);
        System.out.println(testIn.cal.getTimeInMillis() == testOut.cal
                .getTime());
        System.out.println(testIn.date.getTime() == testOut.date
                .getTimeInMillis());
    }

}
