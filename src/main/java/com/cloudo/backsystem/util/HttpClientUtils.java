package com.cloudo.backsystem.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class HttpClientUtils {
    static Logger logger = LoggerFactory.getLogger(HttpClientUtils.class);

    public static String createHttpGet(String requestAddr,Map<String,String> head, Map<String,String> map) {
        String content = null;
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            URIBuilder uriBuilder = new URIBuilder(requestAddr);
            for(Map.Entry<String,String> entry: map.entrySet()){
                uriBuilder.setParameter(entry.getKey(),entry.getValue());
            }
            HttpGet httpGet = new HttpGet(uriBuilder.build());
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(500000)
                    .setConnectionRequestTimeout(100000)
                    .setSocketTimeout(500000)
                    .build();
            httpGet.setConfig(requestConfig);
            if(head != null){
                head.forEach((e1,e2)->{
                    httpGet.setHeader(e1,e2);
                });
            }
            HttpResponse httpResponse = httpClient.execute(httpGet);
            HttpEntity httpEntity = httpResponse.getEntity();
            content = EntityUtils.toString(httpEntity);
            httpClient.close();
        } catch (Exception e) {
            logger.error("获取html错误：{}", e.getMessage());
        }
        return content;
    }

    public static String createHttpPost(String requestAddr, Map<String,String> map) {
        String content = null;
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            URIBuilder uriBuilder = new URIBuilder(requestAddr);
            HttpPost httpPost = new HttpPost(uriBuilder.build());
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(500000)
                    .setConnectionRequestTimeout(100000)
                    .setSocketTimeout(500000)
                    .build();
            httpPost.setConfig(requestConfig);
            //设置请求格式为json
            httpPost.setHeader("Content-Type","application/json;charset=UTF-8");
            String jsonParam = JSONObject.toJSONString(map);
            StringEntity stringEntity = new StringEntity(jsonParam,"UTF-8");
            httpPost.setEntity(stringEntity);

            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            content = EntityUtils.toString(httpEntity);
            httpClient.close();
        } catch (Exception e) {
            logger.error("获取html错误：{}", e.getMessage());
        }
        return content;
    }
}
