package com.cloudo.backsystem.util;

import com.google.gson.Gson;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpsUtil {
    public static final int CONNECT_TIME_OUT  =35*1000;//从连接池获取连接的timeout
    public static final int CONNECT_REQUEST_TIME_OUT  =35*1000;//客户端和服务器建立连接的timeout
    public static final int SOCKET_TIME_OUT  =60*1000;

    public static String doGet(String url, Map<String,String> headers,Map<String,String> params){
        String result = "";
        url +="?";
        for (Map.Entry<String,String> entry: params.entrySet()) {
            url += entry.getKey()+"="+entry.getValue();
        }
        //获取Get对象
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        //设置request header
        if (headers != null) {
            for (String key: headers.keySet()) {
                httpGet.setHeader(key,headers.get(key));
            }
        }
        //设置请求超时时间
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIME_OUT)
                .setConnectionRequestTimeout(CONNECT_REQUEST_TIME_OUT)
                .setSocketTimeout(SOCKET_TIME_OUT)
                .build();
        httpGet.setConfig(requestConfig);
        //执行请求
        try {
            result = client.execute(httpGet,
                    response -> EntityUtils.toString(response.getEntity()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //返回response
        return result;
    }

    public static String doPost(String url, Map<String,String> headers, Map<String,Object> params){

        CloseableHttpClient client = HttpClients.createDefault();
        String result = "";
        HttpPost httpPost = new HttpPost(url);
        //设置request header
        if (headers != null) {
            for (String key: headers.keySet()) {
                httpPost.setHeader(key,headers.get(key));
            }
        }
        //设置请求超时时间
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIME_OUT)
                .setConnectionRequestTimeout(CONNECT_REQUEST_TIME_OUT)
                .setSocketTimeout(SOCKET_TIME_OUT)
                .build();
        httpPost.setConfig(requestConfig);
        //将参数转化为json
        Gson gson = new Gson();
        String jsonParam = gson.toJson(params);
        StringEntity stringEntity = new StringEntity(jsonParam,"UTF-8");
        httpPost.setEntity(stringEntity);
        //执行请求
        try {
            result = client.execute(httpPost, response -> EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //返回结果
        return result;
    }
}