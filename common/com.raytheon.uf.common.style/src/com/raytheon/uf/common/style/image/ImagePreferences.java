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

package com.raytheon.uf.common.style.image;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.raytheon.uf.common.colormap.prefs.DataMappingPreferences;
import com.raytheon.uf.common.style.AbstractStylePreferences;
import com.raytheon.uf.common.style.FillLabelingPreferences;
import com.raytheon.uf.common.style.ImageryLabelingPreferences;
import com.raytheon.uf.common.style.StyleException;

/**
 *
 * Contains the imagery preferences
 *
 * <pre>
 *
 *    SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ---------------------------
 * Jul 27, 2007           chammack  Initial Creation.
 * Nov 25, 2013  2492     bsteffen  Add colorMapUnits
 * Apr 26, 2017  6247     bsteffen  Implement clone
 * Aug 25, 2017  6403     bsteffen  Use CollapsedStringAdapter
 * Apr 04, 2018  6889     njensen   Added brightness
 * Jun 27, 2019  65510    ksunil    support color fill through XML entries
 *
 * </pre>
 *
 * @author chammack
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "imageStyle")
public class ImagePreferences extends AbstractStylePreferences {

    @XmlElement
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    private String defaultColormap;

    @XmlElement(name = "range")
    private DataScale dataScale;

    @XmlElement(name = "displayLegend")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    private String legend;

    @XmlElement
    private SamplePreferences samplePrefs;

    @XmlElement
    private DataMappingPreferences dataMapping;

    @XmlElement
    private ImageryLabelingPreferences colorbarLabeling;

    @XmlElement
    private boolean interpolate = true;

    @XmlElement
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    private String colorMapUnits;

    @XmlElement
    private Float brightness;

    @XmlElement
    private Double smoothingDistance;

    @XmlElement(name = "fill")
    private List<FillLabelingPreferences> fill = new ArrayList<>();

    public ImagePreferences() {

    }

    public ImagePreferences(ImagePreferences prefs) {
        super(prefs);
        this.defaultColormap = prefs.getDefaultColormap();
        this.dataScale = prefs.getDataScale();
        this.legend = prefs.getLegend();
        this.samplePrefs = prefs.getSamplePrefs().clone();
        this.dataMapping = prefs.getDataMapping().clone();
        this.colorbarLabeling = prefs.getColorbarLabeling().clone();
        this.interpolate = prefs.isInterpolate();
        this.colorMapUnits = prefs.getColorMapUnits();
        this.smoothingDistance = prefs.getSmoothingDistance();

        Iterator<FillLabelingPreferences> iterator = prefs.getFill().iterator();
        while (iterator.hasNext()) {
            this.fill.add((FillLabelingPreferences) iterator.next().clone());
        }
    }

    public boolean isInterpolate() {
        return interpolate;
    }

    public void setInterpolate(boolean interpolate) {
        this.interpolate = interpolate;
    }

    public String getDefaultColormap() {
        return defaultColormap;
    }

    public void setDefaultColormap(String defaultColormap) {
        this.defaultColormap = defaultColormap;
    }

    public DataScale getDataScale() {
        return dataScale;
    }

    public void setDataScale(DataScale dataScale) {
        this.dataScale = dataScale;
    }

    public Double getSmoothingDistance() {
        return smoothingDistance;
    }

    public void setSmoothingDistance(Double smoothingDistance) {
        this.smoothingDistance = smoothingDistance;
    }

    public ImageryLabelingPreferences getColorbarLabeling() {
        return colorbarLabeling;
    }

    public void setColorbarLabeling(
            ImageryLabelingPreferences colorbarLabeling) {
        this.colorbarLabeling = colorbarLabeling;
    }

    public DataMappingPreferences getDataMapping() {
        return dataMapping;
    }

    public void setDataMapping(DataMappingPreferences dataMapping) {
        this.dataMapping = dataMapping;
    }

    public SamplePreferences getSamplePrefs() {
        return samplePrefs;
    }

    public void setSamplePrefs(SamplePreferences samplePrefs) {
        this.samplePrefs = samplePrefs;
    }

    public String getLegend() {
        return legend;
    }

    /**
     * 
     * @return String representation of colorMapUnits for serialization.
     * @see ImagePreferences#getColorMapUnitsObject()
     */
    public String getColorMapUnits() {
        return colorMapUnits;
    }

    /**
     * @param colorMapUnits
     *            String representation of colorMapUnits for serialization.
     * @see ImagePreferences#getColorMapUnitsObject()
     */
    public void setColorMapUnits(String colorMapUnits) {
        this.colorMapUnits = colorMapUnits;
    }

    /**
     * Units that should be used when color mapping. Mostly useful for cases
     * where the colormap should be applied non linearly.
     */
    public Unit<?> getColorMapUnitsObject() throws StyleException {
        if (colorMapUnits != null && !colorMapUnits.isEmpty()) {
            try {
                return UnitFormat.getUCUMInstance()
                        .parseProductUnit(colorMapUnits, new ParsePosition(0));
            } catch (ParseException e) {
                throw new StyleException("Unable to parse colorMap Units.");
            }
        }
        return null;
    }

    @Override
    public ImagePreferences clone() {
        return new ImagePreferences(this);
    }

    public Float getBrightness() {
        return brightness;
    }

    public void setBrightness(Float brightness) {
        this.brightness = brightness;
    }

    public List<FillLabelingPreferences> getFill() {
        return fill;
    }

    public void setFill(List<FillLabelingPreferences> fill) {
        this.fill = fill;
    }

}
