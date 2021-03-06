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

package com.raytheon.viz.core.preferences;

import java.util.Arrays;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.raytheon.uf.common.util.SizeUtil;
import com.raytheon.uf.viz.core.Activator;
import com.raytheon.uf.viz.core.preferences.PreferenceConstants;

/**
 * Specifies performance preferences
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------
 * Jul 01, 2006           chammack  Initial Creation.
 * Mar 29, 2016  5523     bsteffen  Add Larger texture cache sizes.
 * Mar 29, 2017  6202     bsteffen  Add pixel density preference.
 * May 25, 2017  6202     bsteffen  Remove text limit on pixel density.
 * 
 * </pre>
 * 
 * @author chammack
 * 
 */
public class PerformancePreferences extends FieldEditorPreferencePage
        implements IWorkbenchPreferencePage {

    /** Must be sorted, these values are in MB. */
    private static final int[] TEXTURE_SIZE_CHOICES = { 128, 256, 512, 1024,
            2048, 4096, 8192 };

    public PerformancePreferences() {
        super(GRID);
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription(
                "Specify performance settings (Requires restart of Viz)");
    }

    /**
     * Get the possible values for the texture cache. The result is paired
     * key/values for use with a {@link ComboFieldEditor}.
     */
    private String[][] getTextureSizeEntries() {
        int[] textureChoices = TEXTURE_SIZE_CHOICES;

        int currentTextureValue = getPreferenceStore().getInt(
                com.raytheon.uf.viz.core.preferences.PreferenceConstants.P_TEXTURES_CARD);
        if (currentTextureValue > 0) {
            if (Arrays.binarySearch(textureChoices, currentTextureValue) < 0) {
                textureChoices = Arrays.copyOf(textureChoices,
                        textureChoices.length + 1);
                textureChoices[textureChoices.length - 1] = currentTextureValue;
                Arrays.sort(textureChoices);
            }
        }

        String[][] values = new String[textureChoices.length][2];
        for (int i = 0; i < textureChoices.length; i += 1) {
            values[i][0] = SizeUtil
                    .prettyByteSize(textureChoices[i] * SizeUtil.BYTES_PER_MB);
            values[i][1] = Integer.toString(textureChoices[i]);
        }
        return values;
    }

    @Override
    public void createFieldEditors() {
        ComboFieldEditor gpuEditor = new ComboFieldEditor(
                PreferenceConstants.P_TEXTURES_CARD,
                "&Video Card Texture Cache Size:", getTextureSizeEntries(),
                getFieldEditorParent());

        addField(gpuEditor);
        addField(new IntegerFieldEditor(PreferenceConstants.P_FPS,
                "&Frames Per Second:", getFieldEditorParent()));

        addField(new BooleanFieldEditor(PreferenceConstants.P_LOG_PERF,
                "&Log CAVE performance", getFieldEditorParent()));

        SpinnerFieldEditor pixelDensity = new SpinnerFieldEditor(
                PreferenceConstants.P_PIXEL_DENSITY, "&Tileset Pixel Density",
                getFieldEditorParent());

        pixelDensity.setDigits(2);
        pixelDensity.setMinimum(75);
        pixelDensity.setMaximum(800);
        pixelDensity.setIncrement(5);
        pixelDensity.setPageIncrement(25);
        pixelDensity.setToolTipText(
                "Smaller numbers improve image quality.\nLarger numbers improve performance.");

        addField(pixelDensity);

    }

    @Override
    public void init(IWorkbench workbench) {

    }

}
