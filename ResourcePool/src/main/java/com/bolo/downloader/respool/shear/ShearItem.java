package com.bolo.downloader.respool.shear;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 共享的记录
 */
public class ShearItem {
    // 共享项目的id生成器
    private static final AtomicLong idGenerator = new AtomicLong(0);
    private long id = 0;
    private String type;
    private String text;
    private File file;
    private long createTime;

    public ShearItem(String type, String text, File file, long createTime) {
        this.id = idGenerator.incrementAndGet();
        this.type = type;
        this.text = text;
        this.file = file;
        this.createTime = createTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
}
