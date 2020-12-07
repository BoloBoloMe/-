package com.bolo.downloader.sync;

/**
 * 文件同步状态
 */
public enum SyncState {
    NEW("0", "待下载"),
    DOWNLOADING("1", "下载中"),
    DOWNLOADED("2", "已下载");
    private String code;
    private String desc;


    SyncState(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static SyncState formCode(String code) {
        for (SyncState state : values())
            if (state.code.equals(code)) return state;
        return null;
    }
}
