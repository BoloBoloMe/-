package com.bolo.downloader.groundcontrol.handler;

import com.bolo.downloader.groundcontrol.dict.StoneMapDict;
import com.bolo.downloader.groundcontrol.factory.StoneMapFactory;
import com.bolo.downloader.respool.db.StoneMap;
import org.apache.http.client.methods.HttpRequestBase;

public class NewFileHandler extends AbstractResponseHandler {
    @Override
    boolean interested(int responseStatus) {
        return responseStatus == 1 || responseStatus == 3;
    }

    @Override
    HttpRequestBase handleResponse(Response response) {
        StoneMap map = StoneMapFactory.getObject();
        map.put(StoneMapDict.KEY_LAST_VER, Integer.toString(response.getVersion()));
        map.put(StoneMapDict.KEY_LAST_FILE, response.getFileNane());
        map.put(StoneMapDict.KEY_FILE_STATE, StoneMapDict.VAL_FILE_STATE_NEW);
        if (map.modify() > 10) {
            map.rewriteDbFile();
        } else {
            map.flushWriteBuff();
        }
        return post(response.getVersion(), 1);
    }
}
