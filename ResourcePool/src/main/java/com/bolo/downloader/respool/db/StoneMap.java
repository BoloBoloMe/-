package com.bolo.downloader.respool.db;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 可持久化的Map
 */
public class StoneMap implements Map<String, String> {
    private String dbFileName;
    private String dbFilePath;
    private File dbFile;
    /**
     * 缓冲池
     */
    private final ConcurrentHashMap<String, String> bufferPool = new ConcurrentHashMap<>();
    /**
     * 数据文件写缓冲
     */
    private LogBuff logBuff;
    /**
     * 最近一次写缓冲刷新时间
     */
    private final AtomicLong logFlushTime = new AtomicLong();
    /**
     * 删除标识
     */
    private final static String KEY_DEL = "@KEY_DEL";

    @Override
    public String put(String key, String value) {
        if (!value.equals(bufferPool.put(key, value))) log(key, value);
        return null;
    }

    @Override
    public String remove(Object key) {
        if (null != bufferPool.remove(key)) log((String) key, KEY_DEL);
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        for (Entry<? extends String, ? extends String> entry : m.entrySet()) put(entry.getKey(), entry.getValue());
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("StoneMap unsupported the clear operation");
    }


    @Override
    public int size() {
        return bufferPool.size();
    }

    @Override
    public boolean isEmpty() {
        return bufferPool.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return bufferPool.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return bufferPool.containsValue(value);
    }

    @Override
    public String get(Object key) {
        return bufferPool.get(key);
    }

    @Override
    public Set<String> keySet() {
        return bufferPool.keySet();
    }

    @Override
    public Collection<String> values() {
        return bufferPool.values();
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return bufferPool.entrySet();
    }


    public StoneMap(String dbFilePath, int dbFileId) {
        this.dbFileName = String.format("stonemap.%d.db", dbFileId);
        this.dbFilePath = dbFilePath;
        this.dbFile = new File(dbFilePath, dbFileName);
        this.logBuff = new LogBuff(16);
    }

    public StoneMap(String dbFilePath, int dbFileId, int logBuffSize) {
        this.dbFileName = String.format("stonemap.%d.db", dbFileId);
        this.dbFilePath = dbFilePath;
        this.dbFile = new File(dbFilePath, dbFileName);
        this.logBuff = new LogBuff(logBuffSize);
    }

    /**
     * 获取距上次checkpoint之后，修改的key的数量
     */
    public int getModifCount() {
        return logBuff.getCount();
    }


    /**
     * 加载数据文件
     */
    synchronized public void load() {
        if (!dbFile.exists()) {
            try {
                dbFile.createNewFile();
                logFlushTime.set(System.currentTimeMillis());
            } catch (IOException e) {
                throw new LogWriteException(e);
            }
            return;
        }
        try (LineNumberReader reader = new LineNumberReader(new FileReader(dbFile))) {
            String line;
            while (null != (line = reader.readLine())) {
                if (!"".equals(line)) {
                    Integer keyLen = Integer.valueOf(line.substring(0, 10));
                    String key = line.substring(10, 10 + keyLen);
                    String value = line.substring(keyLen + 10, line.length());
                    logBuff.increaseCount();
                    if (KEY_DEL.equals(value)) {
                        bufferPool.remove(key);
                    } else {
                        bufferPool.put(key, value);
                    }
                }
            }
            logFlushTime.set(System.currentTimeMillis());
        } catch (IOException e) {
            throw new LogWriteException(e);
        }
    }

    /**
     * 数据文件重写
     */
    public void dbFileRewrite() {
        logBuff.checkpoint();
        Map<String, String> bufferPoolSnap = new HashMap<>(bufferPool);
        File newDbFile = new File(dbFilePath, dbFileName + ".tem." + Thread.currentThread().getId());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(newDbFile))) {
            for (Map.Entry<String, String> entry : bufferPoolSnap.entrySet()) {
                writer.write(LogBuff.getLine(entry.getKey(), entry.getValue()));
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            throw new LogWriteException(e);
        }
        mergeDbFile(newDbFile);
    }


    private void log(String key, String value) {
        logBuff.push(key, value);
        if (logBuff.writeFulfil()) flushLogBuff();
    }

    synchronized public void flushLogBuff() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dbFile, true))) {
            String line;
            while (null != (line = logBuff.pop())) {
                writer.write(line);
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            throw new LogWriteException(e);
        }
    }


    synchronized private void mergeDbFile(File newDbFile) {
        flushLogBuff();
        try (LineNumberReader reader = new LineNumberReader(new FileReader(dbFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(newDbFile, true))) {
            String line;
            while (null != (line = reader.readLine())) {
                if (reader.getLineNumber() <= logBuff.getCheckpoint() || "".equals(line)) continue;
                writer.write(line);
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            throw new LogWriteException(e);
        }
        // change dbFile
        dbFile.delete();
        newDbFile.renameTo(dbFile);
    }

    @Override
    public String toString() {
        return "StoneMap{" +
                "dbFileName='" + dbFileName + '\'' +
                ", dbFilePath='" + dbFilePath + '\'' +
                ", dbFile=" + dbFile +
                ", bufferPool=" + bufferPool +
                ", logBuff=" + logBuff +
                ", logFlushTime=" + logFlushTime +
                '}';
    }
}
