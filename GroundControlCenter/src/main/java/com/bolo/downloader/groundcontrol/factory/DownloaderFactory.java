package com.bolo.downloader.groundcontrol.factory;

import com.bolo.downloader.groundcontrol.file.download.Downloader;

public class DownloaderFactory {
    private static Downloader downloader = null;

    public static Downloader getObject() {
        return null != downloader ? downloader : createSingleton();
    }

    private static synchronized Downloader createSingleton() {
        if (null != downloader) return downloader;
        return downloader = new Downloader(ConfFactory.get("videoPath"), ConfFactory.get("youtubeDLPath"),
                Integer.parseInt(ConfFactory.get("concurrenceTaskNum")));
    }
}
