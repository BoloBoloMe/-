package com.bolo.downloader.sync;

import com.bolo.downloader.factory.ConfFactory;
import com.bolo.downloader.factory.StoneMapFactory;
import com.bolo.downloader.respool.coder.MD5Util;
import com.bolo.downloader.respool.db.StoneMap;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/**
 * 文件信息同步器
 * 1.封装了StoneMap用于保存文件列表，并提供接口将Map中的内容封装为Record对象以便外部访问
 * 2.扫描文件目录，刷新文件列表和服务器版本号
 */
public class Synchronizer {
    final static private String VIDEO_NAME_PATTERN = ".+(\\.mp4|\\.webm|\\.wmv|\\.avi|\\.dat|\\.asf|\\.mpeg|\\.mpg|\\.rm|\\.rmvb|\\.ram|\\.flv|\\.3gp|\\.mov|\\.divx|\\.dv|\\.vob|\\.mkv|\\.qt|\\.cpk|\\.fli|\\.flc|\\.f4v|\\.m4v|\\.mod|\\.m2t|\\.swf|\\.mts|\\.m2ts|\\.3g2|\\.mpe|\\.ts|\\.div|\\.lavf|\\.dirac){1}";
    final static private AtomicReference<String> fileList = new AtomicReference<>("[]");
    static private final String KEY_LAST_VER = "lastVer";
    static private final AtomicInteger VAL_LAST_VER = new AtomicInteger(0);
    static private final ConcurrentHashMap<String, Record> cache = new ConcurrentHashMap<>();
    static private final AtomicReference<StoneMap> stoneMap = new AtomicReference<>(null);
    static private final MyLogger log = LoggerFactory.getLogger(Synchronizer.class);
    static private final ReentrantLock lock = new ReentrantLock();


    /**
     * 缓冲StoneMap中持久化的信息
     */
    public static void cache() {
        if (!stoneMap.compareAndSet(null, StoneMapFactory.getObject())) {
            throw new Error("Synchronizer 并发初始化异常！");
        }
        // 缓存版本号
        if (map().get(KEY_LAST_VER) != null) {
            VAL_LAST_VER.set(Integer.parseInt(map().get(KEY_LAST_VER)));
        }
        // 缓存文件列表
        cache.clear();
        for (Map.Entry<String, String> entry : map().entrySet()) {
            if (!KEY_LAST_VER.equals(entry.getKey())) {
                cache.put(entry.getKey(), new Record(entry.getValue()));
            }
        }
    }

    /**
     * 关闭同步器：最后刷新一次StoneMap
     */
    public static void clean() {
        map().rewriteDbFile();
    }


    /**
     * 获取服务器版本号
     *
     * @return
     */
    public static int getCurrVer() {
        return VAL_LAST_VER.get();
    }

    /**
     * 返回文件列表的JSON字符串
     */
    public static String fileList() {
        return fileList.get();
    }

    /**
     * 文件记录状态更新为已下载
     *
     * @param key
     */
    public static void isDownloaded(String key) {
        Record record = cache.get(key);
        if (record != null && record.getState() != SyncState.LOSE) {
            record.setState(SyncState.DOWNLOADED);
            map().put(key, record.value());
        }
    }

    public static Record getRecord(String key, int version, String url, String fileName, SyncState state) {
        if (key != null) return cache.get(key);
        if (version <= 0 && url == null && fileName == null && state == null) return null;
        for (Map.Entry<String, Record> entry : cache.entrySet()) {
            Record record = entry.getValue();
            if (version > 0 && record.getVersion() == version) return record;
            if (url != null && url.equals(record.getMd5())) return record;
            if (fileName != null && fileName.equals(record.getFileName())) return record;
            if (state != null && state == record.getState()) return record;
        }
        return null;
    }

    /**
     * 同步：1.删除已下载的文件; 2.扫描文件目录,刷新文件列表; 3.持久化StoneMap
     * 同一批
     */
    public static void flush() {
        try {
            if (lock.tryLock()) {
                // 删除已下载的文件
                for (String key : cache.keySet()) {
                    Record record = cache.get(key);
                    if (SyncState.DOWNLOADED == record.getState()) {
                        File target = new File(ConfFactory.get("videoPath"), record.getFileName());
                        if (target.exists()) {
                            log.info("删除已下载的文件：" + record.getFileName());
                            target.delete();
                        }
                        cache.remove(key);
                        map().remove(key);
                    }
                }
                // 扫描文件目录
                scanDisc();
                // 持久化StoneMap
                StoneMap map = map();
                if (map.modify() < 10) {
                    map.flushWriteBuff();
                } else {
                    map.rewriteDbFile();
                }
            }
        } catch (Exception e) {
            log.error("文件信息同步异常!", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


    /**
     * 扫描文件目录，更新文件列表
     */
    private static void scanDisc() {
        final String videoPath = ConfFactory.get("videoPath");
        String[] fileArr = Optional.ofNullable(new File(videoPath).list((dir, name) -> Pattern.matches(VIDEO_NAME_PATTERN, name.toLowerCase()))).orElse(new String[0]);
        StoneMap map = map();
        // 扫描是否有新增的文件
        for (String fileName : fileArr) {
            if (!map.containsKey(fileName)) {
                String md5;
                try (RandomAccessFile target = new RandomAccessFile(new File(videoPath, fileName), "r")) {
                    md5 = MD5Util.md5HashCode32(target);
                } catch (Exception e) {
                    throw new RuntimeException("文件校验码计算失败！", e);
                }
                Record record = new Record(VAL_LAST_VER.incrementAndGet(), SyncState.NEW, fileName, md5);
                cache.put(fileName, record);
                map.put(fileName, record.value());
                map.put(KEY_LAST_VER, Integer.toString(VAL_LAST_VER.get()));
                log.info("新增文件%s,版本号:%d", fileName, record.getVersion());
            }
        }
        // 扫描是否有遗失的文件
        for (String fileName : map.keySet()) {
            if (!KEY_LAST_VER.equals(fileName)) {
                boolean notInclude = true;
                for (String instance : fileArr) {
                    if (instance.equals(fileName)) {
                        notInclude = false;
                        break;
                    }
                }
                if (notInclude) {
                    log.error("文件丢失：" + fileName);
                    Record loser = getRecord(fileName, 0, null, null, null);
                    if (loser == null) {
                        map.remove(fileName);
                    } else {
                        loser.setState(SyncState.LOSE);
                        map.put(fileName, loser.value());
                    }
                }
            }
        }
        // 更新文件列表JSON
        if (fileArr.length == 0) {
            fileList.compareAndSet(fileList.get(), "[]");
        } else {
            StringBuilder json = new StringBuilder().append('[');
            for (String instance : fileArr) {
                json.append('"').append(instance).append('"').append(',');
            }
            json.deleteCharAt(json.length() - 1).append(']');
            fileList.compareAndSet(fileList.get(), json.toString());
        }
    }

    private static StoneMap map() {
        return stoneMap.get();
    }

}
