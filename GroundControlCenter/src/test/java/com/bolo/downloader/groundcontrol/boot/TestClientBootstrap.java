package com.bolo.downloader.groundcontrol.boot;

import com.bolo.downloader.groundcontrol.ClientBootstrap;
import com.bolo.downloader.groundcontrol.factory.ConfFactory;

public class TestClientBootstrap {
    public static final String CONF_FILE_PATH = "C:\\ProgramFiles_green\\VideoDownloader_client\\conf\\GroundControlCenter.conf";

    public static void main(String[] args) {
        ConfFactory.load(CONF_FILE_PATH);
        ClientBootstrap.main(args);
    }
}
