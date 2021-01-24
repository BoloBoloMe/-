package com.bolo.downloader.groundcontrol.transfer;

import java.util.concurrent.ConcurrentSkipListSet;

public class TransferService {
    private static final String RECORD_TYPE_FILE = "FILE";
    private static final String RECORD_TYPE_Text = "TEXT";

    private static final ConcurrentSkipListSet<Record> records = new ConcurrentSkipListSet<>();

    public void add(String type, String content) {
        records.add(new Record(type, content));
    }

    public Record findById(int id) {
        for (Record record : records) if (record.getId() == id) return record;
        return null;
    }

    public void remove(int id) {
        for (Record record : records) if (record.getId() == id) records.remove(record);
    }

    static class Record {
        private String type;
        private int id;
        private String content;

        public Record(String type, String content) {
            this.type = type;
            this.id = hashCode();
            this.content = content;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
