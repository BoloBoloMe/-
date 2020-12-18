package com.bolo.downloader.groundcontrol.handler;

import com.bolo.downloader.groundcontrol.dict.StoneMapDict;
import com.bolo.downloader.groundcontrol.factory.StoneMapFactory;
import com.bolo.downloader.respool.db.StoneMap;
import org.apache.http.client.methods.HttpRequestBase;

public class EqualsHandler extends AbstractResponseHandler {
    @Override
    boolean interested(int responseStatus) {
        return responseStatus == 0;
    }

    @Override
    HttpRequestBase handleResponse(Response response) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        StoneMap map = StoneMapFactory.getObject();
        int lastVer = Integer.parseInt(map.get(StoneMapDict.KEY_LAST_VER));
        return post(lastVer, 1);
    }
}
