package com.bolo.downloader.factory;

import com.bolo.downloader.nio.ReqRecord;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class ReqQueueFactory {

    private static final BlockingDeque<ReqRecord> deque = new LinkedBlockingDeque<>();

    public static BlockingDeque<ReqRecord> get() {
        return deque;
    }
}
