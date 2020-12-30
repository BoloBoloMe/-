package com.bolo.downloader.respool.db.buff;

import java.io.IOException;
import java.io.Writer;

public interface CycleWriteBuff {
    /**
     * 重置流水号到当前最新值
     *
     * @param serial
     */
    void resetSerialInit(int serial);

    /**
     * 向缓冲区写入内容
     *
     * @param key
     * @param value
     */
    void put(String key, String value);

    /**
     * 缓冲区内容写出
     * 行格式：key-length+serial+key+value
     * 长度：
     * key-length —— char*2
     * serial     —— char*2
     * keyvalue   —— char*variable
     *
     * @throws IOException
     */
    void write(Writer writer) throws IOException;

    /**
     * 写入恢复行
     */
    void recoverRow(String key, String value, Writer writer) throws IOException;

    int checkpoint();

    /**
     * buffer usage report
     */
    String usageReport(boolean brief);
}
