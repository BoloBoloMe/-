package com.bolo.downloader.sync;

public class Record {
    private String fileName;
    private int version;
    private String url;
    private SyncState state;
    final private StringBuffer value = new StringBuffer(11);


    public Record(int version, String url, SyncState state, String fileName) {
        this.version = version;
        this.url = url;
        this.state = state;
        this.fileName = fileName;
        value.append(state.getCode());
        // 版本号共10位，不足在前面补0
        value.append(String.format("%010d", version)).append(fileName);
    }

    public Record(String url, String val) {
        this.url = url;
        this.value.append(val);
        this.state = SyncState.formCode(value.substring(0, 1));
        this.version = Integer.valueOf(value.substring(1, 11));
        this.fileName = value.substring(11);
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public SyncState getState() {
        return state;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setState(SyncState state) {
        this.state = state;
        value.replace(0, 1, state.getCode());
    }

    public String value() {
        return value.toString();
    }
}
