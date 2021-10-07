package com.bolo.downloader.sync;

public class Record {
    private final String md5;
    private final String fileName;
    private final int version;
    private SyncState state;
    final private static String NAME_AND_URL_CUT = "|*|";
    // state(1*char)+version(10*char)+fileName|*|url(variable*char)
    final private StringBuffer value = new StringBuffer(64);


    Record(int version, SyncState state, String fileName, String md5) {
        this.md5 = md5;
        this.version = version;
        this.state = state;
        this.fileName = fileName;
        value.append(state.getCode());
        // 版本号共10位，不足在前面补0
        value.append(String.format("%010d", version)).append(fileName).append(NAME_AND_URL_CUT).append(md5);
    }

    Record(String value) {
        this.value.append(value);
        this.state = SyncState.formCode(value.substring(0, 1));
        this.version = Integer.valueOf(value.substring(1, 11));
        int urlStartIndex;
        this.fileName = value.substring(11, urlStartIndex = value.indexOf(NAME_AND_URL_CUT));
        this.md5 = value.substring(urlStartIndex + NAME_AND_URL_CUT.length());
    }

    public void setState(SyncState state) {
        this.state = state;
        value.replace(0, 1, state.getCode());
    }

    public int getVersion() {
        return version;
    }


    public SyncState getState() {
        return state;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMd5() {
        return md5;
    }

    public String value() {
        return value.toString();
    }
}
