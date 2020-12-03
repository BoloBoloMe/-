package com.bolo.downloader;


import com.bolo.downloader.factory.ConfFactory;
import com.bolo.downloader.factory.ReqQueueFactory;
import com.bolo.downloader.factory.StoneMapFactory;
import com.bolo.downloader.nio.HttpServer;
import com.bolo.downloader.nio.ReqRecord;
import com.bolo.downloader.respool.db.StoneMap;
import com.bolo.downloader.utils.PageHelper;
import com.bolo.downloader.utils.RestHelper;
import io.netty.handler.codec.http.HttpMethod;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;


public class Bootstrap {
    public static final String CONF_FILE_PATH = "D:\\MyResource\\Desktop\\conf\\SpaceStation.conf";
    private static int PORT;
    private static final BlockingDeque<ReqRecord> deque = ReqQueueFactory.get();
    private static final ReqRecord LINKED_HEAD = new ReqRecord(null, null, null, null, null);
    private static ReqRecord LINKED_CURR = LINKED_HEAD;

    static {
        LINKED_CURR.putLinked(LINKED_HEAD, LINKED_HEAD);
    }

    public static void main(String[] args) throws CertificateException, InterruptedException, SSLException {
        PORT = Integer.valueOf(ConfFactory.get("port"));
        new HttpServer(PORT).start();

        while (true) {
            // linked loop
            while (LINKED_CURR != LINKED_HEAD) {
                handelReqRecord(LINKED_CURR);
                LINKED_CURR = LINKED_CURR.getNext();
                ReqRecord oldRecord;
                if ((oldRecord = LINKED_CURR.getPrev()).isDone()) {
                    oldRecord.delLinked();
                    closeChannel(oldRecord);
                }
            }

            // request loop
            while (true) {
                ReqRecord reqRecord;
                try {
                    reqRecord = deque.pollLast(1, TimeUnit.SECONDS);
                    if (reqRecord == null) break;
                } catch (InterruptedException e) {
                    break;
                }
                handelReqRecord(reqRecord);
                if (reqRecord.isDone()) {
                    closeChannel(reqRecord);
                } else {
                    reqRecord.putLinked(LINKED_HEAD, LINKED_HEAD.getNext());
                }
            }

            // background loop
            StoneMap stoneMap = StoneMapFactory.getObject();
            try {
                stoneMap.flushWriteBuff();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void handelReqRecord(ReqRecord reqRecord) {
        boolean deon = true;
        if (HttpMethod.GET.equals(reqRecord.getMethod())) {
            PageHelper.toPage(reqRecord.getUri(), reqRecord.getParams(), reqRecord.getCtx(), reqRecord.getRequest());
        } else if (HttpMethod.POST.equals(reqRecord.getMethod())) {
            deon = RestHelper.handle(reqRecord.getUri(), reqRecord.getParams(), reqRecord.getCtx(), reqRecord.getRequest());
        }
        reqRecord.setDone(deon);
    }

    private static void closeChannel(ReqRecord reqRecord) {
        try {
            if (reqRecord.getCtx().channel().isOpen()) {
                reqRecord.getCtx().close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

