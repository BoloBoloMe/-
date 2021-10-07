package boot;


import com.bolo.downloader.ServerBootstrap;
import com.bolo.downloader.factory.ConfFactory;

public class TestServerBootstrap extends ServerBootstrap {
    private static final String CONF_FILE_PATH = "/home/a_bolo/program/VideoDownloader/SpaceStation/conf/SpaceStation.propertes";
//    private static final String CONF_FILE_PATH = "/home/bolo/program/VideoDownloader/SpaceStation/conf/SpaceStation.propertes";

    public static void main(String[] args) {
        ConfFactory.load(CONF_FILE_PATH);
        ServerBootstrap.main(args);
    }

}

