package com.bolo.downloader.respool.db;

import java.io.*;
import java.util.*;
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
     * 写缓冲
     */
    private final CycleWriteBuff<String, String> writeBuff;

    /**
     * DELETE FLAG
     */
    private final static String KEY_DEL = "@KEY_DEL";
    /**
     * 修改操作计数器,统计key的修改和删除操作，不包括新增
     */
    private final AtomicLong modCounter = new AtomicLong(0);

    @Override
    public String put(String key, String value) {
        String oldVal = bufferPool.put(key, value);
        if (null != oldVal) {
            modCounter.incrementAndGet();
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
            modCounter.incrementAndGet();
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
        modCounter.addAndGet(keyCount);
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
        return modCounter.get();
    }


    public StoneMap(String dbFilePath, int dbFileId, int wrireBuffSize) {
        this.dbFileName = String.format("stonemap.%d.db", dbFileId);
        this.dbFilePath = dbFilePath;
        writeBuff = new CycleWriteBuff<>(wrireBuffSize, 500, 1000);
        this.dbFile = new File(dbFilePath, dbFileName);
    }

    public StoneMap(String dbFilePath, int dbFileId, int wrireBuffSize, int putSpedMax, int writeLoopMax) {
        this.dbFileName = String.format("stonemap.%d.db", dbFileId);
        this.dbFilePath = dbFilePath;
        writeBuff = new CycleWriteBuff<>(wrireBuffSize, putSpedMax, writeLoopMax);
        this.dbFile = new File(dbFilePath, dbFileName);
    }

    /**
     * 加载数据文件
     */
    synchronized public void loadDbFile() {
        if (!dbFile.exists()) {
            try {
                dbFile.createNewFile();
            } catch (IOException e) {
                throw new LogReadException(e);
            }
            return;
        }
        try (LineNumberReader reader = new LineNumberReader(new FileReader(dbFile))) {
            TreeMap<Integer, Node> sort = new TreeMap<>();
            String line;
            char[] depictArea = new char[4];
            while (true) {
                int len = reader.read(depictArea);
                if (len <= 0) break;
                else if (len != 4) throw new LogReadException("数据文件已损坏！");
                line = reader.readLine();
                if (null == line) break;
                if (!"".equals(line)) {
                    Node node = resolve(line, depictArea);
                    if (node.serial == 0) {
                        bufferPool.put(node.key, node.value);
                    } else {
                        sort.put(node.serial, node);
                    }
                    modCounter.incrementAndGet();
                }
            }
            for (Integer key : sort.keySet()) {
                Node node = sort.get(key);
                if (KEY_DEL.equals(node.value)) {
                    bufferPool.remove(node.key);
                } else {
                    bufferPool.put(node.key, node.value);
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
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dbFile, true))) {
            writeBuff.write(writer);
        } catch (IOException e) {
            throw new LogWriteException(e);
        }
    }


    /**
     * 数据文件重写：扫描当前缓冲池，写入数据文件，缩减占用空间
     */
    synchronized public void rewriteDbFile() {
        // create new db file
        modCounter.set(0);
        writeBuff.checkpoint();
        Map<String, String> bufferPoolSnap = new HashMap<>(bufferPool);
        File newDbFile = new File(dbFilePath, dbFileName + ".tem." + Thread.currentThread().getId());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(newDbFile))) {
            for (Map.Entry<String, String> entry : bufferPoolSnap.entrySet()) {
                writeBuff.recoverRow(entry.getKey(), entry.getValue(), writer);
            }
        } catch (IOException e) {
            throw new LogWriteException(e);
        }
        // merge db file
        flushWriteBuff();
        try (BufferedReader reader = new BufferedReader(new FileReader(dbFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(newDbFile, true))) {
            boolean checkpointLater = false;
            char[] depictArea = new char[4];
            String line;
            while (true) {
                int len = reader.read(depictArea);
                if (len <= 0) break;
                else if (len != 4) throw new LogReadException("数据文件已损坏！");
                line = reader.readLine();
                if (null == line) break;
                Node node = resolve(line, depictArea);
                if (checkpointLater) {
                    writer.write(line);
                    writer.newLine();
                } else if (node.serial == 1) {
                    writer.write(line);
                    writer.newLine();
                    checkpointLater = true;
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
        writeBuff.put(key, value);
    }

    private Node resolve(String row, char[] depictArea) {
        int serial = depictArea[0] + depictArea[1] * 65535;
        int keyLen = depictArea[2] + depictArea[3] * 65535;
        Node node = new Node();
        node.serial = serial;
        node.key = row.substring(0, keyLen);
        node.value = row.substring(keyLen, row.length());
        return node;
    }

    private class Node {
        private int serial;
        private String key;
        private String value;
    }

    @Override
    public String toString() {
        return "StoneMap{" +
                "size=" + bufferPool.size() +
                "dbFile=" + dbFile +
                "writeBuffUsageReport=" + writeBuff.usageReport(true) +
                '}';
    }
}
