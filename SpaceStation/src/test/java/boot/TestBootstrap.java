package boot;


import com.bolo.downloader.Bootstrap;
import com.bolo.downloader.factory.ConfFactory;

public class TestBootstrap extends Bootstrap {
    private static final String CONF_FILE_PATH = "/home/bolo/program/VideoDownloader/SpaceStation/conf/SpaceStation.conf";

    public static void main(String[] args) {
        ConfFactory.load(CONF_FILE_PATH);
        Bootstrap.main(args);
    }

}

