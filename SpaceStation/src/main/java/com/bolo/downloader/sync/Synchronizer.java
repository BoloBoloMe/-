package com.bolo.downloader.sync;

import com.bolo.downloader.factory.ConfFactory;
import com.bolo.downloader.factory.StoneMapFactory;
import com.bolo.downloader.respool.db.StoneMap;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;

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
    static private final MyLogger log = LoggerFactory.getLogger(Synchronizer.class);

    /**
     * 扫描文件存放目录，缓存文件列表
     */
    public static void scanDisc() {
        String[] fileArr = new File(new File(ConfFactory.get("videoPath")).getAbsolutePath()).list((dir, name) -> Pattern.matches(FILE_NAME_PATTERN, name.toLowerCase()));
        List<String> newList = Arrays.asList(fileArr == null ? new String[]{} : fileArr);
        fileList.lazySet(newList);
        for (String fileName : newList) {
            if (!map().containsKey(fileName)) commit(fileName, SyncState.NEW, fileName, "");
        }
        for (String fileName : map().keySet()) {
            if (!lastVerKey.equals(fileName) && !newList.contains(fileName)) {
                commit(fileName, SyncState.LOSE, fileName, "");
            }
        }
    }

    /**
     * 获取当前版本号
     *
     * @return
     */
    public static int getCurrVer() {
        if (version.get() == 0) {
            loadVer();
        }
        return version.get();
    }

    /**
     * 返回文件列表
     */
    public static List<String> fileList() {
        return fileList.get();
    }

    public static void isDownloaded(String key) {
        Record record = cache.get(key);
        if (record != null && record.getState() != SyncState.LOSE) {
            record.setState(SyncState.DOWNLOADED);
            commit(key, record);
        }
    }

    /**
     * 删除已下载的文件
     */
    public static void clean() {
        for (String key : cache.keySet()) {
            Record record = cache.get(key);
            if (SyncState.DOWNLOADED == record.getState() || SyncState.LOSE == record.getState()) {
                log.info("移除已遗失或已下载的缓存：" + record.getFileName());
                File target = new File(ConfFactory.get("videoPath"), record.getFileName());
                if (target.exists()) {
                    log.info("删除已同步的文件：" + record.getFileName());
                    target.delete();
                }
                cache.remove(key);
                map().remove(key);
            }
        }
    }

    /**
     * 缓存持久化的map
     *
     * @param map
     */
    public static void cache(StoneMap map) {
        if (!cache.isEmpty()) cache.clear();
        for (Map.Entry<String, String> entry : map.entrySet())
            if (!lastVerKey.equals(entry.getKey()))
                cache.put(entry.getKey(), new Record(entry.getValue()));
    }

    public static Record getRecord(String key, int version, String url, String fileName, SyncState state) {
        if (key != null) return cache.get(key);
        if (version <= 0 && url == null && fileName == null && state == null) return null;
        for (Map.Entry<String, Record> entry : cache.entrySet()) {
            Record record = entry.getValue();
            if (version > 0 && record.getVersion() == version) return record;
            if (url != null && url.equals(record.getUrl())) return record;
            if (fileName != null && fileName.equals(record.getFileName())) return record;
            if (state != null && state == record.getState()) return record;
        }
        return null;
    }


    /**
     * 提交新增的文件记录
     */
    private static void commit(String key, SyncState state, String name, String url) {
        Record record;
        if (null == (record = cache.get(key))) {
            commit(key, new Record(version.incrementAndGet(), state, name, url));
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
        String lastVerStr = map.get(lastVerKey);
        Integer lastVer = null != lastVerStr ? Integer.valueOf(lastVerStr) : null;
        cache.put(key, record);
        map.put(key, record.value());
        if (lastVer == null || lastVer < record.getVersion()) {
            String newVer = Integer.toString(record.getVersion());
            map.put(lastVerKey, newVer);
            log.info("服务器版本号更新:%s", newVer);
        }
    }

    /**
     * 从持久化map中加载版本号
     */
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
