package com.bolo.downloader.respool.db;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
    private final ArrayBlockingQueue<String> sequence;
    /**
     * DELETE FLAG
     */
    private final static String KEY_DEL = "@KEY_DEL";
    /**
     * CHECKPOINT FLAG
     */
    private final static String KEY_CHECKPOINT = "@CHECKPOINT";
    /**
     * 修改操作计数器,统计key的修改和删除操作，不包括新增
     */
    private final AtomicLong modifCounter = new AtomicLong(0);

    @Override
    public String put(String key, String value) {
        String oldVal = bufferPool.put(key, value);
        if (null != oldVal) {
            modifCounter.incrementAndGet();
            if (!value.equals(oldVal)) log(key, value);
        } else {
            log(key, value);
        }

        return null;
    }

    @Override
    public String remove(Object key) {
        if (null != bufferPool.remove(key)) {
            log((String) key, KEY_DEL);
            modifCounter.incrementAndGet();
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        for (Entry<? extends String, ? extends String> entry : m.entrySet()) put(entry.getKey(), entry.getValue());
    }

    /**
     * 清空缓冲池，强制触发数据文件重写 非常不建议使用
     */
    @Override
    public void clear() {
        int keyCount = bufferPool.size();
        bufferPool.clear();
        modifCounter.addAndGet(keyCount);
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

    /**
     * 自上次数据文件重写以来，发生修改、删除的key数量
     */
    public long modify() {
        return modifCounter.get();
    }


    public StoneMap(String dbFilePath, int dbFileId, int wrireBuffSize) {
        this.dbFileName = String.format("stonemap.%d.db", dbFileId);
        this.dbFilePath = dbFilePath;
        this.dbFile = new File(dbFilePath, dbFileName);
        this.sequence = new ArrayBlockingQueue<>(wrireBuffSize);
    }


    /**
     * 加载数据文件
     */
    synchronized public void loadDbFile() {
        if (!dbFile.exists()) {
            try {
                dbFile.createNewFile();
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
                    modifCounter.incrementAndGet();
                    if (KEY_DEL.equals(value)) {
                        bufferPool.remove(key);
                    } else {
                        bufferPool.put(key, value);
                    }
                }
            }
        } catch (IOException e) {
            throw new LogWriteException(e);
        }
    }

    /**
     * 同步日志写缓冲到数据文件，并清空日志写缓冲
     */
    synchronized public void flushWriteBuff() {
        if (sequence.isEmpty()) return;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dbFile, true))) {
            String line;
            while (null != (line = sequence.poll(1, TimeUnit.SECONDS))) {
                writer.append(line);
                writer.newLine();
            }
            writer.flush();
        } catch (IOException e) {
            throw new LogWriteException(e);
        } catch (InterruptedException e) {
            // 队列在1s中内没有值，中止方法
        }
    }


    /**
     * 数据文件重写：扫描当前缓冲池，写入数据文件，缩减占用空间
     */
    synchronized public void rewriteDbFile() {
        // create new db file
        modifCounter.lazySet(0);
        sequence.add(KEY_CHECKPOINT);
        Map<String, String> bufferPoolSnap = new HashMap<>(bufferPool);
        File newDbFile = new File(dbFilePath, dbFileName + ".tem." + Thread.currentThread().getId());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(newDbFile))) {
            for (Map.Entry<String, String> entry : bufferPoolSnap.entrySet()) {
                writer.write(getLine(entry.getKey(), entry.getValue()));
                writer.newLine();
            }
            writer.flush();
        } catch (IOException e) {
            throw new LogWriteException(e);
        }
        // merge db file
        flushWriteBuff();
        try (BufferedReader reader = new BufferedReader(new FileReader(dbFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(newDbFile, true))) {
            String line;
            boolean newCont = false;
            while (null != (line = reader.readLine())) {
                if (newCont) {
                    writer.write(line);
                    writer.newLine();
                } else {
                    if (KEY_CHECKPOINT.equals(line)) newCont = true;
                }
            }
            writer.flush();
        } catch (IOException e) {
            throw new LogWriteException(e);
        }
        // modifying dbFile
        if (dbFile.delete()) newDbFile.renameTo(dbFile);
    }

    /**
     * 新增日志
     */
    private void log(String key, String value) {
        try {
            sequence.put(getLine(key, value));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private String getLine(String key, String value) {
        StringBuilder ele = new StringBuilder(10 + key.length() + (null == value ? 0 : value.length()));
        // length
        ele.append(key.length());
        while (ele.length() < 10) ele.insert(0, "0");
        ele.append(key).append(value);
        return ele.toString();
    }

    @Override
    public String toString() {
        return "StoneMap{" +
                "bufferPool=" + bufferPool +
                "dbFile=" + dbFile +
                '}';
    }
}
