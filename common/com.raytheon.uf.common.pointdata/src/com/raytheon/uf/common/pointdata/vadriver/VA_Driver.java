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
package com.raytheon.uf.common.pointdata.vadriver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import org.locationtech.jts.geom.Coordinate;

/**
 * Driver for using the VA_Advanced progressive disclosure to generate
 * localizaed spi files.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Feb 24, 2011           bfarmer     Initial creation
 * Dec 02, 2013  2537     bsteffen    Ensure streams are closed.
 * Jan 10, 2018  6713     dgilling    Cleanup file I/O error handling in
 *                                    vaStationsFile.
 * Feb 12, 2019 DCS20569 MPorricelli  Tweaked output for ldad spi files
 *
 * </pre>
 *
 * @author bfarmer
 */

public class VA_Driver {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(VA_Driver.class);

    private static final int MNS = 150_000;

    private final Map<String, Integer> nameIndexMap = new HashMap<>();

    private int ns = 0;

    private final Coordinate[] latLon = new Coordinate[MNS];

    private final int[] elevs = new int[MNS];

    private final int[] nums = new int[MNS];

    private final String[] nams = new String[MNS];

    private final String[] ids = new String[MNS];

    private final Integer[] goodness = new Integer[MNS];

    private final Double[] dist = new Double[MNS];

    private int max_name = 0;

    private int stn_fmt = 0;

    private int loc_fmt = 0;

    private int pass = 0;

    private int cities_file = 0;

    private int verify = 0;

    private int just_check = 0;

    private int advanced = 0;

    private int recomp = 0;

    private float weight = 0;

    private int uniqueFlag = 1;

    private int use_aspect = 0;

    private int dist_test = 0;

    private int accum_good = 0;

    /*
     * defaults to 100 km
     */
    private float dist_thresh = (float) (100. * 100. / (111. * 111.));

    public void vaStationsFile(File goodnessFile, File primary, File output) {
        try (BufferedReader fis = Files
                .newBufferedReader(goodnessFile.toPath())) {
            for (String line = fis.readLine(); line != null; line = fis.readLine()) {
                if (line.startsWith("#")) {
                    continue;
                }
                int comment = line.indexOf("//", 0);
                if (comment != -1) {
                    line = line.substring(0, comment);
                }
                String[] splitLine = line.trim().split("\\s+");
                if (splitLine.length == 2) {
                    try {
                        double minDist = Double.valueOf(splitLine[0]);
                        double maxDist = Double.valueOf(splitLine[1]);
                        // Lat = Y
                        latLon[ns].y = minDist;
                        // Lon = X
                        latLon[ns].x = maxDist;
                    } catch (NumberFormatException e) {
                        String errorMsg = String.format(
                                "Invalid number format in file [%s]: invalid line [%s]",
                                goodnessFile, line);
                        statusHandler.warn(errorMsg, e);
                    }
                    continue;
                } else if (splitLine.length >= 6) {
                    try {
                        nums[ns] = Integer.valueOf(splitLine[0]);
                        nams[ns] = splitLine[1];
                        latLon[ns] = new Coordinate(Float.valueOf(splitLine[3]),
                                Float.valueOf(splitLine[2]));
                        elevs[ns] = Integer.valueOf(splitLine[4]);
                        goodness[ns] = Float.valueOf(splitLine[5]).intValue();
                        if (splitLine.length >= 7) {
                            ids[ns] = splitLine[6];
                        } else {
                            ids[ns] = "";
                        }
                    } catch (NumberFormatException e) {
                        String errorMsg = String.format(
                                "Invalid number format in file [%s]: invalid line [%s]",
                                goodnessFile, line);
                        statusHandler.warn(errorMsg, e);
                        continue;
                    }
                } else {
                    if (splitLine.length > 0) {
                        String errorMsg = String.format(
                                "Invalid number of data tokens [%d] found in line in file [%s]. Expected 2 or 6 or more. Invalid line [%s]",
                                goodnessFile, splitLine.length, line);
                        statusHandler.warn(errorMsg);
                    }
                    continue;
                }
                if ((latLon[ns].y == 0.0 && latLon[ns].x == 0.0)
                        || latLon[ns].y > 90.0 || latLon[ns].y < -90.0
                        || latLon[ns].x > 180.0 || latLon[ns].x < -180.0) {
                    String errorMsg = String.format(
                            "Invalid lat/lon combination (%.1f, %.1f) found in file [%s]. Invalid line [%s]",
                            latLon[ns].y, latLon[ns].x, goodnessFile, line);
                    statusHandler.warn(errorMsg);
                    continue;
                }
                if (splitLine.length <= 6 || ids[ns].isEmpty()) {
                    ids[ns] = nams[ns];
                }
                if (splitLine.length < 6) {
                    goodness[ns] = -1000 * (nums[ns] / 1000);
                } else {
                    dist[ns] = -1.0;
                    // = goodness[ns].doubleValue();
                }
                nameIndexMap.put(ids[ns], ns);
                if (nams[ns].length() > max_name) {
                    max_name = nams[ns].length();
                }
                ++ns;
            }
        } catch (IOException e) {
            statusHandler
                    .error("Error reading from file [" + goodnessFile + "]", e);
            return;
        }

        if (primary != null) {
            try (BufferedReader fis = Files
                    .newBufferedReader(primary.toPath())) {
                int g = 0x7FFFFFFF;
                for (String pline = fis.readLine(); pline != null; pline = fis
                        .readLine()) {
                    pline = pline.trim();
                    Integer index = nameIndexMap.get(pline);
                    if (index != null) {
                        goodness[index] = g;
                        g--;
                    }
                }
            } catch (IOException e) {
                statusHandler.error("Error reading from file [" + primary + "]",
                        e);
                return;
            }
        }

        Coordinate[] latLonInput = new Coordinate[ns];
        Integer[] goodnessInput = new Integer[ns];
        Double[] distInput = new Double[ns];
        System.arraycopy(latLon, 0, latLonInput, 0, ns);
        System.arraycopy(goodness, 0, goodnessInput, 0, ns);
        System.arraycopy(dist, 0, distInput, 0, ns);
        VA_Advanced vaa = new VA_Advanced();
        vaa.setVaDistPass(true);
        vaa.setVaRecomp(false);
        vaa.setVaWeighting(weight);
        distInput = vaa.getVaAdvanced(latLonInput, goodnessInput, distInput);

        // Write the output.
        try (BufferedWriter fos = Files.newBufferedWriter(output.toPath())) {
            String nameFormat = String.format("%s%ds", "%", max_name);
            for (int i = 0; i < ns; ++i) {
                fos.write(String.format("%5d ", nums[i]));
                fos.write(String.format(nameFormat, nams[i]));
                if (output.getName().contains("ldad")) {
                    fos.write(String.format(" %8.4f %9.4f %5d %9.3f %s",
                            latLonInput[i].y, latLonInput[i].x, elevs[i],
                            distInput[i], ids[i]));
                } else {
                    fos.write(String.format(" %8.4f %9.4f %5d %9.3f",
                            latLonInput[i].y, latLonInput[i].x, elevs[i],
                            distInput[i]));
                }
                fos.newLine();
            }
        } catch (IOException e) {
            statusHandler.error("Error writing to file [" + output + "]", e);
        }
    }

    /**
     * @return the stn_fmt
     */
    public int getStn_fmt() {
        return stn_fmt;
    }

    /**
     * @param stn_fmt
     *            the stn_fmt to set
     */
    public void setStn_fmt(int stn_fmt) {
        this.stn_fmt = stn_fmt;
    }

    /**
     * @return the loc_fmt
     */
    public int getLoc_fmt() {
        return loc_fmt;
    }

    /**
     * @param loc_fmt
     *            the loc_fmt to set
     */
    public void setLoc_fmt(int loc_fmt) {
        this.loc_fmt = loc_fmt;
    }

    /**
     * @return the pass
     */
    public int getPass() {
        return pass;
    }

    /**
     * @param pass
     *            the pass to set
     */
    public void setPass(int pass) {
        this.pass = pass;
    }

    /**
     * @return the cities_file
     */
    public int getCities_file() {
        return cities_file;
    }

    /**
     * @param cities_file
     *            the cities_file to set
     */
    public void setCities_file(int cities_file) {
        this.cities_file = cities_file;
    }

    /**
     * @return the verify
     */
    public int getVerify() {
        return verify;
    }

    /**
     * @param verify
     *            the verify to set
     */
    public void setVerify(int verify) {
        this.verify = verify;
    }

    /**
     * @return the just_check
     */
    public int getJust_check() {
        return just_check;
    }

    /**
     * @param just_check
     *            the just_check to set
     */
    public void setJust_check(int just_check) {
        this.just_check = just_check;
    }

    /**
     * @return the advanced
     */
    public int getAdvanced() {
        return advanced;
    }

    /**
     * @param advanced
     *            the advanced to set
     */
    public void setAdvanced(int advanced) {
        this.advanced = advanced;
    }

    /**
     * @return the recomp
     */
    public int getRecomp() {
        return recomp;
    }

    /**
     * @param recomp
     *            the recomp to set
     */
    public void setRecomp(int recomp) {
        this.recomp = recomp;
    }

    /**
     * @return the weight
     */
    public float getWeight() {
        return weight;
    }

    /**
     * @param weight
     *            the weight to set
     */
    public void setWeight(float weight) {
        this.weight = weight;
    }

    /**
     * @return the uniqueFlag
     */
    public int getUniqueFlag() {
        return uniqueFlag;
    }

    /**
     * @param uniqueFlag
     *            the uniqueFlag to set
     */
    public void setUniqueFlag(int uniqueFlag) {
        this.uniqueFlag = uniqueFlag;
    }

    /**
     * @return the use_aspect
     */
    public int getUse_aspect() {
        return use_aspect;
    }

    /**
     * @param use_aspect
     *            the use_aspect to set
     */
    public void setUse_aspect(int use_aspect) {
        this.use_aspect = use_aspect;
    }

    /**
     * @return the dist_test
     */
    public int getDist_test() {
        return dist_test;
    }

    /**
     * @param dist_test
     *            the dist_test to set
     */
    public void setDist_test(int dist_test) {
        this.dist_test = dist_test;
    }

    /**
     * @return the accum_good
     */
    public int getAccum_good() {
        return accum_good;
    }

    /**
     * @param accum_good
     *            the accum_good to set
     */
    public void setAccum_good(int accum_good) {
        this.accum_good = accum_good;
    }

    /**
     * @return the dist_thresh
     */
    public float getDist_thresh() {
        return dist_thresh;
    }

    /**
     * @param dist_thresh
     *            the dist_thresh to set
     */
    public void setDist_thresh(float dist_thresh) {
        this.dist_thresh = dist_thresh;
    }

}
