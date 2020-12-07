package com.bolo.downloader.sync;

import com.bolo.downloader.factory.ConfFactory;
import com.bolo.downloader.factory.StoneMapFactory;
import com.bolo.downloader.respool.db.StoneMap;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * 文件同步器
 */
public class Synchronizer {
    final static private String FILE_NAME_PATTERN = ".+(\\.mp4|\\.webm|\\.wmv|\\.avi|\\.dat|\\.asf|\\.mpeg|\\.mpg|\\.rm|\\.rmvb|\\.ram|\\.flv|\\.3gp|\\.mov|\\.divx|\\.dv|\\.vob|\\.mkv|\\.qt|\\.cpk|\\.fli|\\.flc|\\.f4v|\\.m4v|\\.mod|\\.m2t|\\.swf|\\.mts|\\.m2ts|\\.3g2|\\.mpe|\\.ts|\\.div|\\.lavf|\\.dirac){1}";
    final static private AtomicReference<List<String>> fileList = new AtomicReference<>(new ArrayList<>());
    static private final String lastVerKey = "lastVer";
    static private final AtomicInteger version = new AtomicInteger(0);
    static private final ConcurrentHashMap<String, Record> cache = new ConcurrentHashMap<>();
    static private volatile boolean needScanDisc = true;

    public static void scanDisc() {
        String[] fileArr = new File(new File(ConfFactory.get("videoPath")).getAbsolutePath()).list((dir, name) -> Pattern.matches(FILE_NAME_PATTERN, name.toLowerCase()));
        List<String> newList = Arrays.asList(fileArr == null ? new String[]{} : fileArr);
        fileList.lazySet(newList);
        for (String file : newList) {
            if (!map().containsKey(file)) {
                commit(file, SyncState.NEW, file);
            }
        }
    }

    public static int getCurrVer() {
        if (version.get() == 0) {
            loadVer();
        }
        return version.get();
    }

    public static List<String> fileList() {
        if (needScanDisc) {
            needScanDisc = false;
            scanDisc();
        }
        return fileList.get();
    }

    public static void clean() {
        for (String key : cache.keySet()) {
            Record record = cache.get(key);
            if (SyncState.DOWNLOADED == record.getState()) {
                File target = new File(ConfFactory.get("videoPath"), record.getFileName());
                if (target.exists()) {
                    target.delete();
                    cache.remove(key);
                    map().remove(key);
                }
            }
        }
    }


    /**
     * 提交新增的文件记录
     */
    private static void commit(String key, SyncState state, String name) {
        Record record;
        if (null == (record = cache.get(key))) {
            commit(key, new Record(version.incrementAndGet(), "", state, name));
        } else {
            record.setState(state);
            commit(key, record);
        }
    }


    /**
     * 提交新增的文件记录
     */
    private static void commit(String key, Record record) {
        StoneMap map = map();
        cache.put(key, record);
        map.put(key, RecordConvert.toValue(record));
        map.put(lastVerKey, version.toString());
    }

    private static void loadVer() {
        StoneMap map = map();
        if (map.get(lastVerKey) != null) {
            version.set(Integer.valueOf(map.get(lastVerKey)));
        }
    }

    private static StoneMap map() {
        return StoneMapFactory.getObject();
    }


}
