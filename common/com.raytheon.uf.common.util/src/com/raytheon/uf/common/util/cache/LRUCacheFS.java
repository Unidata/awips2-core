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
package com.raytheon.uf.common.util.cache;

import java.io.File;

/**
 * 
 * Disk-backed LRU cache.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 8, 2009             mschenke    Initial creation
 * Mar 9, 2016       5461  tgurney     Add loadFrom() method and clean up
 *                                     dead code
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

/*
 * This class will manage files on the file system including total size and
 * creation and destruction. The users of this class do not have to use it to
 * create cache files, they can create and name their own files and have them
 * cached. The main thing to remember is to use this class to maintain files and
 * delete files when they are no longer needed. When wanting to update a file be
 * sure to lock then file then release the lock when done so your file doesn't
 * get deleted while writing. This may already be covered by the File class but
 * I wanted to be sure. Remember, this class should only be used if you WANT to
 * manage the caching of files, there is no requirement on it and you can create
 * and write to files all you want. There may be a time where we allow multiple
 * file cache objects but for now it is one
 */
public class LRUCacheFS {

    private static class CachedFile implements ICacheObject {

        private File file;

        public CachedFile(File file) {
            this.file = file;
        }

        @Override
        public int getSize() {
            return (int) file.length();
        }

        public void destroy() {
            this.file.delete();
        }

    }

    private static class LRUCacheInternal extends LRUCache<String, CachedFile> {

        public LRUCacheInternal(long maxSize) {
            super(maxSize);
        }

        @Override
        public void removeItem(Item item) {
            super.removeItem(item);
            item.value.destroy();
        }

    }

    /** Instance for file system cache */
    private static LRUCacheInternal fsCache;

    /** Maximum cache size in MB */
    private static final int FILESYSTEM_CACHE_SIZE = 512;

    /** Get preferences for directory and size of cache */
    static {
        fsCache = new LRUCacheInternal(FILESYSTEM_CACHE_SIZE * 1024 * 1024);
    }

    private LRUCacheFS() {

    }

    /**
     * Let the cache know the file has been read. If the file is not in the
     * cache, it will be placed in the cache
     * 
     * @param file
     */
    public static void poll(File file) {
        CachedFile cf = fsCache.get(file.getAbsolutePath());
        if (cf == null) {
            cf = new CachedFile(file);
            fsCache.put(file.getAbsolutePath(), cf);
        }
    }

    /**
     * Register existing cache files with the cache
     * 
     * @param directory
     *            directory containing cache files
     */
    public static void loadFrom(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    poll(file);
                }
            }
        }
    }

}
