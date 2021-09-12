package com.bolo.downloader.groundcontrol.factory;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * 用于进行Https请求的HttpClient
 *
 * @author Devin <xxx>
 * @ClassName: SSLClient
 * @Description: TODO
 * @date 2017年2月7日 下午1:42:07
 */
public class HttpClientFactory {
    public static CloseableHttpClient http() {
        return HttpClientBuilder.create().build();
    }

}
