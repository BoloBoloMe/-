package com.bolo.downloader.sync;

public class RecordConvert {
    public static String toValue(Record record) {
        return record.value();
    }

    public static Record toRecird(String key, String value) {
        return new Record(key, value);
    }
}
