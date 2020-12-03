package com.bolo.downloader;


import com.bolo.downloader.factory.ConfFactory;
import com.bolo.downloader.factory.ReqQueueFactory;
import com.bolo.downloader.factory.StoneMapFactory;
import com.bolo.downloader.nio.HttpServer;
import com.bolo.downloader.nio.ReqRecord;
import com.bolo.downloader.respool.db.StoneMap;
import com.bolo.downloader.utils.FileDownloadHelper;
import com.bolo.downloader.utils.PageHelper;
import com.bolo.downloader.utils.ResponseHelper;
import com.bolo.downloader.utils.RestHelper;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;


public class Bootstrap {
    public static final boolean debug = true;
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
        System.out.println("服务启动成功,地址：127.0.0.1:" + PORT);

        while (true) {
            // linked loop
            while (LINKED_CURR != LINKED_HEAD) {
                handelReqRecord(LINKED_CURR);
                LINKED_CURR = LINKED_CURR.getNext();
                if (LINKED_CURR.getPrev().isDone()) {
                    LINKED_CURR.getPrev().delLinked();
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
                if (!reqRecord.isDone()) {
                    reqRecord.putLinked(LINKED_HEAD, LINKED_HEAD.getNext());
                }
            }

            // background loop
            // flush write buffer
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
            // access page apply GET method
            deon = PageHelper.toPage(reqRecord.getUri(), reqRecord.getParams(), reqRecord.getCtx(), reqRecord.getRequest());
        } else if (HttpMethod.POST.equals(reqRecord.getMethod())) {
            // restful & download file apply POST method
            if (reqRecord.getUri().endsWith("/df")) {
                deon = FileDownloadHelper.download(reqRecord.getUri(), reqRecord.getParams(), reqRecord.getCtx(), reqRecord.getRequest());
            } else {
                deon = RestHelper.handle(reqRecord.getUri(), reqRecord.getParams(), reqRecord.getCtx(), reqRecord.getRequest());
            }
        } else {
            ResponseHelper.sendError(reqRecord.getCtx(), HttpResponseStatus.METHOD_NOT_ALLOWED, reqRecord.getRequest());
        }
        reqRecord.setDone(deon);
    }

}
