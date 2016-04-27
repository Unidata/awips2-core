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
package com.raytheon.viz.ui.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.commands.IElementUpdater;

/**
 * 
 * When an {@link AbstractHandler} is implementing {@link IElementUpdater} to
 * change the text visible on a toolbar, then problems can arise when the
 * toolbar changes size. These problems may be toolbar items being moved to a
 * drop down menu or the entire toolbar shifting locations which can cause tool
 * item to jump to a new line. This class is intended to assist such handlers by
 * modifying the text so it is a constant width. Each time the text is changed a
 * sizer should be created and configured the same then
 * {@link #createAdjustedText(String)} can be called to get a String that will
 * be as close as possible to the configured width.
 * 
 * Note: This class contains GC resources to measure the width of various
 * Strings and it must be disposed when you are done with it.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * Mar 31, 2016  5519     bsteffen  Initial creation
 *
 * </pre>
 *
 * @author bsteffen
 */
public class HandlerTextSizer {

    /**
     * Appended to a truncated string when the original string is longer than
     * the maximum desired width.
     */
    private static final char ELLIPSIS = '\u2026';

    /**
     * The smallest space in unicode, the exact size depends on the font, font
     * size, and screen resolution. This is often 1 pixel wide which makes exact
     * padding easy to achieve.
     */
    private static final char HAIR_SPACE = '\u200A';

    /**
     * A space slightly larger than a hair space. Combining different sized
     * spaces can help achieve perfect padding.
     * 
     * @see HandlerTextSizer#pad(String)
     */
    private static final char THIN_SPACE = '\u2009';

    /**
     * Exact size depends on the font, but this space usually falls between a
     * hair space and a thin space in width. Combining different sized spaces
     * can help achieve perfect padding.
     * 
     * @see HandlerTextSizer#pad(String)
     */
    private static final char SIX_PER_EM_SPACE = '\u2006';

    /**
     * This is the biggest space used in this class, it is only needed when the
     * font size or DPI are very large. Combining different sized spaces can
     * help achieve perfect padding.
     * 
     * @see HandlerTextSizer#pad(String)
     */
    private static final char FOUR_PER_EM_SPACE = '\u2005';

    /**
     * The character that is used for determining a pixel width for a specific
     * number of characters. Currently this is 'M'.
     * 
     * @see #setMinCharacters(int)
     * @see #setMaxCharacters(int)
     */
    public final char SAMPLE_CHAR = 'M';

    private final GC gc;

    private int minWidth = Integer.MIN_VALUE;

    private int maxWidth = Integer.MAX_VALUE;

    public HandlerTextSizer(Display display) {
        gc = new GC(display);
    }

    /**
     * Set the minimum width to an exact size in pixels.
     */
    public void setMinWidth(int minWidth) {
        this.minWidth = minWidth;
    }

    /**
     * Set the minimum text width to a specific number of characters. The
     * minimum width will be set to the width of this many {@link #SAMPLE_CHAR}
     * s, so for variable spaced fonts the exact number of characters is not
     * guaranteed.
     */
    public void setMinCharacters(int count) {
        this.minWidth = gc.textExtent(Character.toString(SAMPLE_CHAR)).x
                * count;
    }

    /**
     * Set the minimum text width to the width of a specific String.
     */
    public void setMinFromSample(String sample) {
        this.minWidth = gc.textExtent(sample).x;
    }

    /**
     * Set the minimum text width to the width of a specific String, only if
     * that string is wider than the current min. This is useful for tool items
     * which have a specific set of Possibilities, passing each possibility to
     * this method will result in the min width being set to accommodate the
     * longest option.
     */
    public void setMinIfWider(String sample) {
        minWidth = Math.max(gc.textExtent(sample).x, minWidth);
    }

    /**
     * Set the maximum width to an exact size in pixels, any String which is
     * passed to {@link #createAdjustedText(String)} which is wider than this
     * will be truncated and the truncated characters will be replaced with
     * ellipsis.
     */
    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    /**
     * Set the maximum text width to a specific number of characters. The
     * maximum width will be set to the width of this many {@link #SAMPLE_CHAR}
     * s, so for variable spaced fonts the exact number of characters is not
     * guaranteed.
     */
    public void setMaxCharacters(int count) {
        this.maxWidth = gc.textExtent(Character.toString(SAMPLE_CHAR)).x
                * count;
    }

    /**
     * Set the maximum text width to the width of a specific String.
     */
    public void setMaxFromSample(String sample) {
        maxWidth = gc.textExtent(sample).x;
    }

    public String createAdjustedText(String text) {
        text = truncate(text);
        text = pad(text);
        return text;
    }

    /**
     * If the text is wider than the maximum width then this method will
     * truncate the text and replace the truncated protion with Ellipses.
     */
    protected String truncate(String text) {
        if (gc.textExtent(text).x > maxWidth) {
            for (int i = text.length() - 1; i > 0; i -= 1) {
                String test = text.substring(0, i) + ELLIPSIS;
                if (gc.textExtent(test).x < maxWidth) {
                    text = test;
                    break;
                }
            }
        }
        return text;
    }

    /**
     * Add spacing before and after the text to ensure it is at least as wide as
     * the minimum specified.
     */
    protected String pad(String text) {
        int textWidth = gc.textExtent(text).x;
        int remainder = minWidth - textWidth;
        if (remainder < 0) {
            return text;
        }
        /*
         * For small fonts at low resolution(For example size 12 at 85 DPI) this
         * will be 1, and perfect spacing is easy. As font size and/or
         * resolution increase this will become 2 which will require more
         * finesse
         */
        int hairWidth = gc.getCharWidth(HAIR_SPACE);
        int after = remainder / 2 / hairWidth;
        int before = remainder / hairWidth - after;
        int padWidth = (before + after) * hairWidth;
        if (padWidth < remainder && hairWidth == 2) {
            /*
             * When the hair width is 2 and the remainder is an odd number, then
             * it is necessary to find another form of space that has an odd
             * width to replace one or two hair spaces and add the one extra
             * pixel needed for perfect spacing.
             */
            if (gc.getCharWidth(THIN_SPACE) == 3) {
                before -= 1;
                text = THIN_SPACE + text;
            } else if (gc.getCharWidth(SIX_PER_EM_SPACE) == 3) {
                before -= 1;
                text = SIX_PER_EM_SPACE + text;
            } else if (gc.getCharWidth(FOUR_PER_EM_SPACE) == 5) {
                before -= 2;
                text = FOUR_PER_EM_SPACE + text;
            } else if (gc.getCharWidth(THIN_SPACE) == 5) {
                before -= 2;
                text = THIN_SPACE + text;
            }
        }
        StringBuilder newText = new StringBuilder();
        for (int i = 0; i < before; i += 1) {
            newText.append(HAIR_SPACE);
        }
        newText.append(text);
        for (int i = 0; i < after; i += 1) {
            newText.append(HAIR_SPACE);
        }
        return newText.toString();
    }

    /**
     * To avoid leaking SWT resources this object must be disposed.
     */
    public void dispose() {
        gc.dispose();
    }

}
