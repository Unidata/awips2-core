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

package com.raytheon.uf.common.style;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.JAXBException;

import com.raytheon.uf.common.localization.FileUpdatedMessage;
import com.raytheon.uf.common.localization.ILocalizationFileObserver;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.jaxb.JaxbDummyObject;
import com.raytheon.uf.common.serialization.reflect.ISubClassLocator;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.style.image.SampleFormat;
import com.raytheon.uf.common.style.level.Level;

/**
 * Manages the visualization styles
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Sep 24, 2007           njensen   Initial creation
 * May 21, 2012  14833    gzhang    Adding a getter for StyleRuleset
 * Sep 06, 2013  2251     mnash     Add ability to plug in new style types
 * Sep 24, 2013  2404     bclement  changed to look in common for files
 * Nov 13, 2013  2361     njensen   Use ISubClassLocator instead of
 *                                  SerializationUtil
 * Mar 10, 2015  4231     nabowle   Watch for changes to loaded style rules and
 *                                  reload them.
 * Apr 16, 2020  8145     randerso  Updated to allow new sample formatting
 *
 * </pre>
 *
 * @author njensen
 */
public class StyleManager implements ILocalizationFileObserver {
    private static final String CONFIG_DIR = "styleRules";

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(StyleManager.class);

    public enum StyleType implements IStyleType {
        IMAGERY("ImageryStyleRules.xml"),
        CONTOUR("ContourStyleRules.xml"),
        ARROW("ArrowStyleRules.xml"),
        GEOMETRY("GeometryStyleRules.xml");

        private String[] extensions;

        private StyleType(String extension) {
            this.extensions = new String[] { extension };
        }

        @Override
        public String[] getExtensions() {
            return extensions;
        }
    }

    private static final StyleManager instance = new StyleManager();

    private Map<IStyleType, StyleRuleset> rules = new ConcurrentHashMap<>();

    private JAXBManager jaxbMgr;

    private ISubClassLocator subClassLocator;

    private boolean setupObservation = true;

    private StyleManager() {
    }

    public static StyleManager getInstance() {
        return instance;
    }

    /**
     * Loads the style rules for the given IStyleType.
     *
     * Callers should synchronize on the IStyleType when calling this to prevent
     * concurrent loads for the same type.
     *
     * @param aType
     *            The IStyleType to load the rules for.
     */
    private void loadRules(IStyleType aType) {
        synchronized (this) {
            if (this.setupObservation) {
                /*
                 * We can't setup observation when constructed due to
                 * uninitialized dependencies, but since we don't need
                 * notifications until rules have been loaded, we can wait until
                 * now.
                 */
                setupObservation();
            }
        }

        try {
            IPathManager pathMgr = PathManagerFactory.getPathManager();
            LocalizationFile[] commonFiles = pathMgr.listFiles(
                    pathMgr.getLocalSearchHierarchy(
                            LocalizationType.COMMON_STATIC),
                    CONFIG_DIR, aType.getExtensions(), true, true);
            StyleRuleset ruleset = createRuleset(commonFiles);
            this.rules.put(aType, ruleset);
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM, "Error loading style rules",
                    e);
        }
    }

    /**
     * Create a StyleRuleSet of the style rules from jaxb files.
     *
     * @param files
     * @return A StyleRuleSet containing the style rules from the files.
     * @throws SerializationException
     */
    private StyleRuleset createRuleset(LocalizationFile[] files)
            throws SerializationException {
        StyleRuleset ruleset = new StyleRuleset();
        if (files == null) {
            return ruleset;
        }

        synchronized (this) {
            if (jaxbMgr == null) {
                jaxbMgr = buildJaxbManager();
            }
        }

        for (LocalizationFile lf : files) {
            ruleset.addStyleRules(jaxbMgr.unmarshalFromXmlFile(
                    StyleRuleset.class, lf.getFile().getPath()));
        }

        return ruleset;
    }

    /**
     * Uses the subClassLocator to build a JAXBManager with classes related to
     * unmarshalling style rules.
     *
     * @return a new JAXBManager for style rules
     * @throws SerializationException
     */
    private JAXBManager buildJaxbManager() throws SerializationException {
        if (subClassLocator == null) {
            throw new IllegalStateException(
                    "StyleManager must have an ISubClassLocator set on it, cannot detect and process style rules");
        }
        Collection<Class<?>> clz = new ArrayList<>(20);
        clz.add(JaxbDummyObject.class);
        clz.add(StyleRuleset.class);
        clz.addAll(subClassLocator
                .locateSubClasses(AbstractStylePreferences.class));
        clz.addAll(subClassLocator.locateSubClasses(MatchCriteria.class));
        clz.addAll(subClassLocator.locateSubClasses(Level.class));
        clz.addAll(subClassLocator.locateSubClasses(SampleFormat.class));
        subClassLocator.save();
        this.subClassLocator = null;
        try {
            return new JAXBManager(clz.toArray(new Class[0]));
        } catch (JAXBException e) {
            throw new SerializationException(
                    "Error initializing StyleManager's JAXB Context", e);
        }
    }

    /**
     * Set this StyleManager as a file updated observer for the base styleRules
     * localization directory.
     *
     * We only register on the base level to prevent multiple notifications for
     * the same file update, while still being notified to changes at any level.
     */
    private void setupObservation() {
        try {
            IPathManager pathMgr = PathManagerFactory.getPathManager();
            LocalizationContext context = pathMgr.getContext(
                    LocalizationType.COMMON_STATIC, LocalizationLevel.BASE);
            LocalizationFile dir = pathMgr.getLocalizationFile(context,
                    CONFIG_DIR);
            dir.addFileUpdatedObserver(this);
            this.setupObservation = false;
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error setting up observation of changes on style rule files",
                    e);
        }
    }

    /**
     * Gets the best matching style rule for a particular match criteria
     *
     * @param aStyleType
     *            the type of style
     * @param aCriteria
     *            the match criteria to find the best match for
     * @return the best matching style rule, or null if no matches are found
     * @throws StyleException
     */
    public StyleRule getStyleRule(IStyleType aStyleType,
            MatchCriteria aCriteria) throws StyleException {
        synchronized (aStyleType) {
            if (!this.rules.containsKey(aStyleType)) {
                loadRules(aStyleType);
            }
        }
        StyleRuleset set = this.rules.get(aStyleType);
        StyleRule bestMatch = null;
        if (set != null) {
            List<StyleRule> rules = set.getStyleRules();
            int matchRank = 0;
            try {
                for (StyleRule rule : rules) {
                    int value = aCriteria.matches(rule.getMatchCriteria());
                    if (value > matchRank) {
                        matchRank = value;
                        bestMatch = rule;
                    }
                }
            } catch (Exception e) {
                throw new StyleException("Error determining matching rules.",
                        e);
            }
        }
        return bestMatch;
    }

    public static double[] calculateMinMax(double level, double minLevel,
            double maxLevel, double minLogValue1, double minLogValue2,
            double maxLogValue1, double maxLogValue2, boolean logarithmic) {
        if (logarithmic) {
            level = Math.log(level);
            minLevel = Math.log(minLevel);
            maxLevel = Math.log(maxLevel);
        }
        // Calculate the percentage of each that is applicable
        double weight = (level - minLevel) / (maxLevel - minLevel);

        // Calculate new weighted mins and maxes
        double vmin = (minLogValue1 * weight) + (minLogValue2 * (1.0 - weight));
        double vmax = (maxLogValue1 * weight) + (maxLogValue2 * (1.0 - weight));

        return new double[] { vmin, vmax };

    }

    /**
     * 2012-05-21 DR 14833: FFMP uses this getter to find the color map if a
     * user modified ffmpImageryStlyeRules.xml incorrectly.
     *
     * @param st
     *            : StyleType
     * @return: StyleRuleset related to the StyleType
     */
    public StyleRuleset getStyleRuleSet(IStyleType st) {

        synchronized (st) {

            if (!rules.containsKey(st)) {
                loadRules(st);
            }
        }

        return rules.get(st);
    }

    /**
     * Sets the sub class locator to detect style rules. Also clears out any
     * rules already loaded, though this should really only be called at
     * startup.
     *
     * @param locator
     */
    public void setSubClassLocator(ISubClassLocator locator) {
        this.subClassLocator = locator;
        jaxbMgr = null;
        rules.clear();
    }

    /**
     * Handles FileUpdateMessages by reloading a style ruleset if the filename
     * matches an extension of a loaded style type.
     */
    @Override
    public void fileUpdated(FileUpdatedMessage message) {
        String fileName = message.getFileName();
        for (IStyleType st : rules.keySet()) {
            for (String ext : st.getExtensions()) {
                if (fileName.endsWith(ext)) {
                    synchronized (st) {
                        loadRules(st);
                    }
                    statusHandler.handle(Priority.DEBUG,
                            "Reloaded style rules due to "
                                    + message.getContext().toString() + " "
                                    + message.getFileName() + " being "
                                    + message.getChangeType() + ".");
                    return;
                }
            }
        }
    }
}
