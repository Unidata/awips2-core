/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
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
package com.raytheon.uf.common.colormap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;

/**
 * 
 * This class represents a condensed byte mask, used for serializing the
 * {@link ColorMapParameters#getAlphaMask()}. The uncondensed mask should be a
 * byte array where all bytes are either 0 or 1. This class will create a
 * minimal amount of xml by storing only the ranges of indexes where the mask is
 * supposed to be 1.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Jun 11, 2018  7316     bsteffen  Initial creation
 * 
 * </pre>
 *
 * @author bsteffen
 */
@XmlAccessorType(XmlAccessType.NONE)
public class CondensedByteMask {

    @XmlAttribute
    private int size;

    @XmlElements({ @XmlElement(name = "range", type = MaskRange.class),
            @XmlElement(name = "index", type = MaskIndex.class) })
    private List<MaskElement> elements;

    public CondensedByteMask() {
    }

    /**
     * Create a condensed version of the given mask.
     * 
     * @param mask
     *            a byte array where each byte is either 0 or 1.
     */
    public CondensedByteMask(byte[] mask) {
        int start = -1;
        this.size = mask.length;
        for (int i = 0; i < size; i += 1) {
            if (mask[i] == 0 && start >= 0) {
                addElement(start, i - 1);
                start = -1;
            } else if (mask[i] != 0 && start < 0) {
                start = i;
            }
        }
        if (start >= 0) {
            addElement(start, size - 1);
        }
    }

    /**
     * 
     * @return true if this contains no masked values.
     */
    public boolean isUnmasked() {
        return elements == null || elements.isEmpty();
    }

    private void addElement(int start, int end) {
        if (elements == null) {
            elements = new ArrayList<>();
        }
        if (start == end) {
            elements.add(new MaskIndex(start));
        } else {
            elements.add(new MaskRange(start, end));
        }
    }

    /**
     * Create a uncondensed map from the elements in this object.
     * 
     * @return an uncondensed mask array.
     */
    public byte[] expand() {
        byte[] mask = new byte[size];
        if (elements != null) {
            for (MaskElement element : elements) {
                element.fill(mask);
            }
        }
        return mask;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public List<MaskElement> getElements() {
        return elements;
    }

    public void setElements(List<MaskElement> elements) {
        this.elements = elements;
    }

    public static interface MaskElement {

        public void fill(byte[] mask);

    }

    /**
     * Xml element to represent a single masked index.
     */
    @XmlAccessorType(XmlAccessType.NONE)
    public static class MaskIndex implements MaskElement {

        @XmlAttribute
        private int value;

        public MaskIndex() {
            value = -1;
        }

        public MaskIndex(int value) {
            super();
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        @Override
        public void fill(byte[] mask) {
            mask[value] = 1;
        }
    }

    /**
     * Xml element to represent a range of mask indexes. The provided start and
     * end are both inclusive.
     */
    @XmlAccessorType(XmlAccessType.NONE)
    public static class MaskRange implements MaskElement {

        @XmlAttribute
        private int start;

        @XmlAttribute
        private int end;

        public MaskRange() {
            start = end = -1;
        }

        public MaskRange(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public void setStart(int start) {
            this.start = start;
        }

        public int getEnd() {
            return end;
        }

        public void setEnd(int end) {
            this.end = end;
        }

        @Override
        public void fill(byte[] mask) {
            Arrays.fill(mask, start, end + 1, (byte) 1);
        }

    }
}
