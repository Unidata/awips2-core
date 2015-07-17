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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.raytheon.uf.common.serialization.DynamicSerializationManager;
import com.raytheon.uf.common.serialization.DynamicSerializationManager.SerializationType;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Testing ability to skip fields during deserialization.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 11, 2015  4561      njensen     Initial creation
 * Jul 17, 2015  4561      njensen     Added collection types to ObjectV1
 *
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class SerializationCompatibilityTest {

    /**
     * A POJO emulating the first version of an object to be
     * serialized/deserialized.
     */
    @DynamicSerialize
    public static class ObjectV1 {

        @DynamicSerializeElement
        public int x;

        @DynamicSerializeElement
        public Integer y;

        @DynamicSerializeElement
        public String name;

        @DynamicSerializeElement
        public Map<String, Object> rcMap;

        @DynamicSerializeElement
        public List<Object> rcList;

        @DynamicSerializeElement
        public Set<Object> rcSet;

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public Integer getY() {
            return y;
        }

        public void setY(Integer y) {
            this.y = y;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, Object> getRcMap() {
            return rcMap;
        }

        public void setRcMap(Map<String, Object> rcMap) {
            this.rcMap = rcMap;
        }

        public List<Object> getRcList() {
            return rcList;
        }

        public void setRcList(List<Object> rcList) {
            this.rcList = rcList;
        }

        public Set<Object> getRcSet() {
            return rcSet;
        }

        public void setRcSet(Set<Object> rcSet) {
            this.rcSet = rcSet;
        }

    }

    /**
     * Emulates the same Object as ObjectV1 but adds an extra field as if a new
     * release added an extra field.
     */
    @DynamicSerialize
    public static class ObjectV2 {

        @DynamicSerializeElement
        public int x;

        @DynamicSerializeElement
        public Integer y;

        @DynamicSerializeElement
        public String name;

        @DynamicSerializeElement
        public String extraneous;

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public Integer getY() {
            return y;
        }

        public void setY(Integer y) {
            this.y = y;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getExtraneous() {
            return extraneous;
        }

        public void setExtraneous(String extraneous) {
            this.extraneous = extraneous;
        }

    }

    /**
     * A class that is supposed to only exist in a release with ObjectV3.
     */
    @DynamicSerialize
    public static class InnerObjectY1 {

        @DynamicSerializeElement
        public String something;

        @DynamicSerializeElement
        public List<String> list;

        @DynamicSerializeElement
        public Set<Integer> set;

        @DynamicSerializeElement
        public Map<String, String> map;

        public String getSomething() {
            return something;
        }

        public void setSomething(String something) {
            this.something = something;
        }

        public List<String> getList() {
            return list;
        }

        public void setList(List<String> list) {
            this.list = list;
        }

        public Set<Integer> getSet() {
            return set;
        }

        public void setSet(Set<Integer> set) {
            this.set = set;
        }

        public Map<String, String> getMap() {
            return map;
        }

        public void setMap(Map<String, String> map) {
            this.map = map;
        }
    }

    /**
     * Emulates the same Object as ObjectV1 and ObjectV2 but adds an extra field
     * with a class type that V1 and V2 are not supposed to know about.
     */
    @DynamicSerialize
    public static class ObjectV3 {

        @DynamicSerializeElement
        public int x;

        @DynamicSerializeElement
        public Float y;

        @DynamicSerializeElement
        public String name;

        @DynamicSerializeElement
        public String extraneous;

        @DynamicSerializeElement
        public InnerObjectY1 inner;

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public Float getY() {
            return y;
        }

        public void setY(Float y) {
            this.y = y;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getExtraneous() {
            return extraneous;
        }

        public void setExtraneous(String extraneous) {
            this.extraneous = extraneous;
        }

        public InnerObjectY1 getInner() {
            return inner;
        }

        public void setInner(InnerObjectY1 inner) {
            this.inner = inner;
        }

    }

    protected static void print(int encoding, int decoding, Object encoded,
            Object decoded) throws IllegalArgumentException,
            IllegalAccessException {
        System.out.println("Encoded version " + encoding);
        Field[] fields = encoded.getClass().getFields();
        for (Field f : fields) {
            System.out.println(f.getName() + " = " + f.get(encoded));
        }
        System.out.println("--------");
        System.out.println("Decoded version " + decoding);
        fields = decoded.getClass().getFields();
        for (Field f : fields) {
            System.out.println(f.getName() + " = " + f.get(decoded));
        }
        System.out.println();
        System.out.println();
        System.out.println();
    }

    protected static void replaceByte(byte[] b, char search, char former,
            char newer) {
        for (int i = 0; i < b.length; i++) {
            if (b[i] == search && b[i + 1] == former) {
                b[i + 1] = (byte) newer;
            }
        }
    }

    /**
     * @param args
     * @throws SerializationException
     */
    public static void main(String[] args) throws Exception {
        DynamicSerializationManager dsm = DynamicSerializationManager
                .getManager(SerializationType.Thrift);
        byte[] b = null;

        ObjectV1 v1 = new ObjectV1();
        v1.name = "njensen";
        v1.x = 5;
        v1.y = new Integer(11);
        v1.rcMap = new HashMap<>();
        v1.rcList = new ArrayList<>();
        v1.rcSet = new HashSet<>();

        /*
         * ObjectV1, ObjectV2, and ObjectV3 are all supposed to represent the
         * same object, just three different releases of it. We will slightly
         * manipulate the classname in the encoded bytes to test compatibility
         * when the different fields don't
         */

        // version 1 reading version 1
        b = dsm.serialize(v1);
        print(1, 1, v1, dsm.deserialize(b));

        // version 2 reading version 1
        replaceByte(b, 'V', '1', '2');
        print(1, 2, v1, dsm.deserialize(b));

        // version 3 reading version 1
        replaceByte(b, 'V', '2', '3');
        print(1, 3, v1, dsm.deserialize(b));

        ObjectV2 v2 = new ObjectV2();
        v2.name = "njensen";
        v2.x = 5;
        v2.y = new Integer(11);
        v2.extraneous = "awesome";

        // version 2 reading version 2
        b = dsm.serialize(v2);
        print(2, 2, v2, dsm.deserialize(b));

        // version 1 reading version 2
        replaceByte(b, 'V', '2', '1');
        print(2, 1, v2, dsm.deserialize(b));

        // version 3 reading version 2
        replaceByte(b, 'V', '1', '3');
        print(2, 3, v2, dsm.deserialize(b));

        ObjectV3 v3 = new ObjectV3();
        v3.name = "njensen";
        v3.x = 5;
        v3.y = new Float(11.0f);
        v3.extraneous = "awesome";
        v3.inner = new InnerObjectY1();
        v3.inner.something = "random";
        v3.inner.list = new ArrayList<>();
        v3.inner.list.add("magic");
        v3.inner.list.add("no problem");
        v3.inner.set = new HashSet<>();
        v3.inner.set.add(new Integer(5));
        v3.inner.set.add(new Integer(12));
        v3.inner.map = new HashMap<>();
        v3.inner.map.put("key", "value");
        v3.inner.map.put("pair", "stuff");

        /*
         * since the test is running in the same JVM, InnerObjectY1 will
         * actually be available to the deserializer when encountered. Therefore
         * we will slightly alter the classname of InnerObjectY1 to simulate a
         * class that V1 and V2 of the software don't know about.
         */

        // version 3 reading version 3
        b = dsm.serialize(v3);
        print(3, 3, v3, dsm.deserialize(b));

        // version 1 reading version 3
        replaceByte(b, 'V', '3', '1');
        replaceByte(b, 'Y', '1', '4');
        print(3, 1, v3, dsm.deserialize(b));

        // version 2 reading version 3
        replaceByte(b, 'V', '1', '2');
        print(3, 2, v3, dsm.deserialize(b));

    }

}
