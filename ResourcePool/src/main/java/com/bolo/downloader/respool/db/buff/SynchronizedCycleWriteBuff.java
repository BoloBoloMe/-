package com.bolo.downloader.respool.db.buff;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 环形链表写缓冲
 * 线程安全
 */
public class SynchronizedCycleWriteBuff implements CycleWriteBuff {
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private final int size;
    private final Note entry;
    private final AtomicReference<Note> writeNote = new AtomicReference<>();
    private final AtomicReference<Note> readNote = new AtomicReference<>();
    /**
     * 流水号
     */
    private final AtomicInteger serial = new AtomicInteger(0);

    public SynchronizedCycleWriteBuff(int size) {
        this.size = size;
        Note curr = entry = new Note();
        while (size-- >= 1) {
            curr.next = new Note();
            curr = curr.next;
            if (size == 0) {
                curr.next = entry;
            }
        }
        writeNote.set(entry);
        readNote.set(entry);
    }

    /**
     * 重置流水号到当前最新值
     *
     * @param serial
     */
    public void resetSerialInit(int serial) {
        this.serial.set(serial);
    }

    public void put(String key, String value) {
        Note curr = writeNote.get();
        for (int i = 0; ; i++) {
            if (curr.readable) {
                curr = curr.next;
                if (i >= size) {
                    synchronized (entry) {
                        try {
                            entry.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            } else {
                synchronized (curr) {
                    if (!curr.readable) {
                        curr.key = key;
                        curr.value = value;
                        curr.serial = serial.incrementAndGet();
                        curr.readable = true;
                        curr.count += 1;
                        writeNote.compareAndSet(writeNote.get(), curr);
                        break;
                    }
                }
            }
        }
    }


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
    public void write(Writer writer) throws IOException {
        Note curr = readNote.get();
        for (int i = 0; i < size; i++) {
            if (curr.readable) {
                synchronized (curr) {
                    if (curr.readable) {
                        newRow(curr.key, curr.value, curr.serial, writer);
                        // reset this note
                        curr.readable = false;
                        curr.key = null;
                        curr.value = null;
                        curr.serial = null;
                        curr.count += 1;
                        readNote.compareAndSet(readNote.get(), curr);
                    }
                }
            }
            curr = curr.next;
        }
        writer.flush();
        synchronized (entry) {
            entry.notifyAll();
        }
    }

    private static void newRow(String key, String value, int serial, Writer writer) throws IOException {
        int keyLen = key.length();
        // serial
        writer.append((char) (serial % 65535)).append((char) (serial / 65535))
                // key length
                .append((char) (keyLen % 65535)).append((char) (keyLen / 65535))
                // key & value
                .append(key).append(value).append(LINE_SEPARATOR);
    }

    /**
     * 写入恢复行
     */
    public void recoverRow(String key, String value, Writer writer) throws IOException {
        newRow(key, value, 0, writer);
    }

    private static class Note {
        private Note next;
        private boolean readable = false;
        private String key;
        private String value;
        private Integer serial;
        private int count = 0;
    }

}
