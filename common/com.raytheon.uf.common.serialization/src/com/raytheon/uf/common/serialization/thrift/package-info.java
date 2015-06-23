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

/**
 * Binary support for dynamic serialization built on top of the thrift protocol.
 * 
 * The <a href="https://thrift.apache.org/">Thrift</a> project is intended to
 * support sending objects between different languages. It achieves this by
 * having developers write a .thrift file that declares fields of a class and
 * then generating equivalent code for specific languages (Java, Python, C++,
 * etc) based on the .thrift file. Therefore each language-specific version of a
 * class is compatible between languages (MyObj.java <-> MyObj.py <->
 * MyObj.cpp...)
 * 
 * Dynamic serialize's thrift package uses the underlying thrift protocol for
 * byte encoding but otherwise diverges from thrift's intent. Dynamic serialize
 * instead looks at the {code DynamicSerialize} annotations on Java classes and
 * any registered type adapters. It then encodes self-describing metadata about
 * the objects along with the annotated fields using the thrift binary protocol.
 * One advantage this provides over thrift is that objects do not need to be
 * encoded in the same exact order every time (though they generally are).
 * Another advantage is that developers can add/remove/change fields on an
 * object without needing to regenerate code files like when using thrift.
 * 
 * There is one major gotcha though where if the serializer is on a different
 * release than the deserializer, the deserializer may not know how to handle
 * the self-describing data format that it encounters. This occurs when
 * developers add/remove/change fields on a class, or add/remove/rename classes.
 * To attempt to dodge these compatibility issues, the following guidelines
 * should be followed:
 * 
 * (For purposes of improving understanding, the word "server" will replace
 * "serializer" and the word "client" will replace deserializer).
 * 
 * <ul>
 * <li>If you are moving/renaming a class such as com.raytheon.a.X to package
 * com.raytheon.b.Y, you should move the class as desired and make the
 * DynamicSerialize fields protected. Next create a new empty class
 * com.raytheon.a.X extends com.raytheon.b.Y and deprecate the empty class with
 * the old name. The server code should use com.raytheon.a.X until all clients
 * have been updated to a release that contains class com.raytheon.b.Y. At that
 * point, switch the server code to use com.raytheon.b.Y and delete
 * com.raytheon.a.X.</li>
 * <li>If you are removing a field from class com.raytheon.X, you should first
 * deprecate the field. Next you should update the client code to not use that
 * field, ignoring it completely as if it were always null. Meanwhile the server
 * should continue to populate the field. Once all clients have the release
 * where the field is ignored, you should remove the field from the class.</li>
 * <li>If you are adding a field to class com.raytheon.X, you should not have to
 * do anything extra. The client code will ignore the new field as it does know
 * not about the field.</li>
 * </ul>
 * 
 * Note that if a change alters a {@code DynamicSerializeTypeAdapter} or adds a
 * new enum value, compatibility will most likely break.
 * 
 * 
 * 
 * @author njensen
 */
package com.raytheon.uf.common.serialization.thrift;

