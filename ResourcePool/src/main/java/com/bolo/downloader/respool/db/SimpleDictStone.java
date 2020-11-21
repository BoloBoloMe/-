package com.bolo.downloader.respool.db;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class SimpleDictStone implements DictStone<String, String> {
    /**
     * 缓冲池
     */
    private final ConcurrentHashMap<String, String> bufferPool = new ConcurrentHashMap<>();
    /**
     * 数据版本号，每修改一次键值对版本号+1
     */
    private AtomicLong ver = new AtomicLong(0);
    /**
     * 磁盘数据版本号
     */
    private long discVer;
    /**
     * 敏感度，当版本号与磁盘版本号的差异达到该值时需要触发数据文件刷新
     */
    private long sens;
    private long minusSens;
    private static final String dbFileNameTmpl = "simple.%d.db";
    private static final String dbFileNameRegex = "simple.[0-9]+.db";
    private final File dbFilePath;
    private File lruDbFile;

    public String put(String key, String value) {
        String oldVal = bufferPool.put(key, value);
        if (!value.equals(oldVal)) {
            ver.incrementAndGet();
            checkDiff();
        }
        return oldVal;
    }

    public void putAll(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!entry.getValue().equals(bufferPool.put(entry.getKey(), entry.getValue()))) ver.incrementAndGet();
        }
        checkDiff();
    }

    public String remove(String key) {
        String oldVal = bufferPool.remove(key);
        if (oldVal != null) {
            ver.incrementAndGet();
            checkDiff();
        }
        return oldVal;
    }

    public void removeAll() {
        if (bufferPool.size() > 0) {
            ver.addAndGet(bufferPool.size());
            bufferPool.clear();
            checkDiff();
        }
    }

    public String get(String key) {
        return bufferPool.get(key);
    }

    @Override
    public int size() {
        return bufferPool.size();
    }

    public synchronized void save() {
        long verSnap = ver.get();
        if (verSnap == discVer) return;
        File discSpace = new File(dbFilePath, String.format(dbFileNameTmpl, verSnap));
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(discSpace))) {
            for (Map.Entry<String, String> entry : bufferPool.entrySet()) {
                writer.write(entry.getKey() + ',' + entry.getValue());
                writer.newLine();
                writer.flush();
            }
            if (lruDbFile != null && lruDbFile.exists()) lruDbFile.delete();
            lruDbFile = discSpace;
            discVer = verSnap;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将磁盘内容加载到缓冲区
     */
    synchronized void buff() throws IOException {
        File disabledDbFile = null;
        String[] dbFils = dbFilePath.list(((dir, name) -> name.matches(dbFileNameRegex)));
        if (dbFils == null || dbFils.length <= 0) {
            return;
        } else if (dbFils.length == 1) {
            int max = -1;
            int curr;
            for (String name : dbFils) {
                curr = Integer.valueOf(name.substring(7, name.lastIndexOf('.')));
                if (curr > max) max = curr;
            }
            ver.set(max);
            discVer = max;
        } else {
            // 如果db文件数量大于1,则说明上次刷新数据到磁盘时，可能中断了，导致旧的db文件没有被清理，最新的db文件可能数据并不完整，所以加载第二新的数据文件，保证数据的完整性
            int[] history = new int[dbFils.length];
            int max = -1;
            int curr;
            for (int i = 0; i < dbFils.length; i++) {
                String name = dbFils[i];
                curr = Integer.valueOf(name.substring(7, name.lastIndexOf('.')));
                history[i] = curr;
                if (curr > max) max = curr;
            }
            int second = -1;
            for (int h : history) {
                if (second < h && h < max) {
                    second = h;
                }
            }
            ver.set(second);
            discVer = second;
            disabledDbFile = new File(String.format(dbFileNameTmpl, max));
        }

        lruDbFile = new File(dbFilePath, String.format(dbFileNameTmpl, ver.get()));
        try (BufferedReader reader = new BufferedReader(new FileReader(lruDbFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if ("".equals(line)) continue;
                String[] entryArry = line.split(",");
                if (entryArry.length == 2) {
                    bufferPool.put(entryArry[0], entryArry[1]);
                }
            }
            if (disabledDbFile != null && disabledDbFile.exists()) disabledDbFile.delete();
        }
    }

    /**
     * 检查版本修改程度，如果达到敏感度将触发数据文件刷新
     */
    private void checkDiff() {
        long diff = ver.get() - discVer;
        if (diff <= minusSens || diff >= sens) {
            save();
        }
    }

    /**
     * @param dbFilePath 数据文件路径
     * @param sens       敏感度。当被修改的key数量达到该值时触发数据文件刷新
     */
    SimpleDictStone(String dbFilePath, int sens) {
        this.dbFilePath = new File(dbFilePath);
        this.sens = sens;
        this.minusSens = sens * -1;
    }
}
