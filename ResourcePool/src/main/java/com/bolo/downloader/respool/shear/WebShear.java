package com.bolo.downloader.respool.shear;

import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 共享接口-向全网提供数据暂存，数据查询的能力
 */
public class WebShear {
    private static final MyLogger log = LoggerFactory.getLogger(WebShear.class);
    private static final String ITEM_TYPE_TEXT = "text";
    private static final String ITEM_TYPE_CACHE_FILE = "cacheFile";
    private static final String ITEM_TYPE_LOCAL_FILE = "localFile";
    private static final LinkedList<ShearItem> list = new LinkedList<>();
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();
    private static final Lock readLock = lock.readLock();
    private static final Lock writeLock = lock.writeLock();


    /**
     * 添加共享的文本
     *
     * @param text
     * @return
     */
    public static boolean addText(String text, long currentTime) {
        try {
            writeLock.lock();
            return list.add(new ShearItem(ITEM_TYPE_TEXT, text, null, currentTime));
        } catch (Exception e) {
            log.error("添加共享文本时发生异常!", e);
        } finally {
            writeLock.unlock();
        }
        return false;
    }

    /**
     * 添加共享的文件
     *
     * @return
     */
    public static boolean addFile(File file, long currentTime, boolean local) {
        try {
            writeLock.lock();
            if (file.exists() && file.isFile()) {
                return list.add(new ShearItem(local ? ITEM_TYPE_LOCAL_FILE : ITEM_TYPE_CACHE_FILE, file.getName(), file, currentTime));
            } else {
                return false;
            }
        } catch (Exception e) {
            log.error("添加共享文件时发生异常!", e);
        } finally {
            writeLock.unlock();
        }
        return false;
    }

    /**
     * 获取共享列表
     *
     * @return
     */
    public static List<ShearItem> list() {
        try {
            readLock.lock();
            return list;
        } finally {
            readLock.lock();
        }
    }


    /**
     * 清除列表中的过时记录
     */
    public static void clearList(long currentTime) {
        try {
            writeLock.lock();
            for (ShearItem item : list) {
                // 3分钟超时
                if (currentTime - item.getCreateTime() >= 180000) {
                    list.remove(item);
                    // 删除临时文件
                    if (item.getType() == ITEM_TYPE_CACHE_FILE) {
                        File target = item.getFile();
                        if (target.exists() && target.isFile()) {
                            target.delete();
                        }
                    }
                }
            }
        } finally {
            writeLock.lock();
        }
    }
}
