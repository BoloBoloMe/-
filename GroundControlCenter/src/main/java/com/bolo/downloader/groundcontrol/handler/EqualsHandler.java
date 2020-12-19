package com.bolo.downloader.groundcontrol.handler;

import com.bolo.downloader.groundcontrol.dict.StoneMapDict;
import com.bolo.downloader.groundcontrol.factory.StoneMapFactory;
import com.bolo.downloader.respool.db.StoneMap;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import org.apache.http.client.methods.HttpRequestBase;

public class EqualsHandler extends AbstractResponseHandler {
    private MyLogger log = LoggerFactory.getLogger(EqualsHandler.class);

    @Override
    boolean interested(int responseStatus) {
        return responseStatus == 0;
    }

    @Override
    HttpRequestBase handleResponse(Response response) {
        StoneMap map = StoneMapFactory.getObject();
        int lastVer = Integer.parseInt(map.get(StoneMapDict.KEY_LAST_VER));
        log.info("[equals] version=%s", lastVer);
        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
        }
        return post(lastVer, 1);
    }
}