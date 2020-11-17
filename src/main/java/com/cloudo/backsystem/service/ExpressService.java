package com.cloudo.backsystem.service;

import com.cloudo.backsystem.dao.ExpressMappingDao;
import com.cloudo.backsystem.dao.ExpressValueDao;
import com.cloudo.backsystem.dao.TaskDao;
import com.cloudo.backsystem.entity.ExpressContent;
import com.cloudo.backsystem.entity.ExpressMappingEntity;
import com.cloudo.backsystem.entity.ExpressValueEntity;
import com.cloudo.backsystem.entity.TaskEntity;
import com.cloudo.backsystem.util.HttpClientUtils;
import com.cloudo.backsystem.util.HttpUtil;
import com.cloudo.backsystem.util.HttpsUtil;
import com.cloudo.backsystem.util.SFUtil;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.dom4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

@Service
public class ExpressService {
    Logger logger = LoggerFactory.getLogger("ExpressService");
    //通过aftership api获取 欧洲单物流
    private static final String TRACKING_URL = "https://api.aftership.com/v4/trackings/";
    private static final String API_KEY = "";
    private static final String CONTENT_TYPE = "application/json";

    public static final String ZHONGTONG_URL = "http://japi.zto.cn/traceInterfaceNewTraces";
    public static final String ZHONGTONG_COMPANY_ID = " ";
    public static final String ZHONGTONG_SIGNATURE = " ";

    @Autowired
    private ExpressMappingDao expressMappingDao;

    @Autowired
    private ExpressValueDao expressValueDao;

    @Autowired
    private TaskDao taskDao;


    /**
     * 获取欧洲段物流信息
     * @param expressSn
     */
    public void getEuExpressInfo(String expressSn, String parcelSn) {
        Gson gson = new Gson();
        String result = getTracking(expressSn);
        if ("".equals(result)){
            ExpressMappingEntity expressMappingEntity = expressMappingDao.findByParcelSnAndSign(parcelSn,1);
            if(expressMappingEntity != null){
                expressMappingEntity.setStatus("1");
                expressMappingDao.save(expressMappingEntity);
            }
            return;
        }
        JsonParser parser = new JsonParser();
        JsonObject object = parser.parse(result).getAsJsonObject();
        String code = object.get("meta").getAsJsonObject().get("code").toString();
        if(!code.equals("200")){
            return;
        }
        JsonArray data = object.get("data").getAsJsonObject()
                .get("tracking").getAsJsonObject()
                .get("checkpoints").getAsJsonArray();
        List resultList = new ArrayList();
        data.forEach(e->{
            Map<String,String> info = new HashMap<>();
            String msg = e.getAsJsonObject().get("message").isJsonNull() ? "":e.getAsJsonObject().get("message").getAsString();
            String location = e.getAsJsonObject().get("city").isJsonNull()
                    ?(e.getAsJsonObject().get("location").isJsonNull() ? "":e.getAsJsonObject().get("location").getAsString())
                    : e.getAsJsonObject().get("city").getAsString();
            String time = e.getAsJsonObject().get("checkpoint_time").isJsonNull() ?"":e.getAsJsonObject().get("checkpoint_time").getAsString();
            String sign = "1";
            info.put("time",time);
            info.put("location",location);
            info.put("msg",msg);
            info.put("sign",sign);
            resultList.add(info);
            });
        String info = gson.toJson(resultList);

        ExpressValueEntity expressValueEntity = expressValueDao.findByExpressSn(expressSn);
        if(expressValueEntity == null){
            expressValueEntity = new ExpressValueEntity();
            expressValueEntity.setExpressSn(expressSn);
        }
        expressValueEntity.setInfo(info);
        expressValueDao.save(expressValueEntity);

        //是否签收
        List<Map<String,String>> list = gson.fromJson(data,List.class);
        Boolean flag = list.stream().anyMatch(s -> "Delivered".equals(s.get("tag")));
        if(flag){
            ExpressMappingEntity expressMappingEntity = expressMappingDao.findByParcelSnAndSign(parcelSn,1);
            if(expressMappingEntity != null){
                expressMappingEntity.setStatus("1");
                expressMappingDao.save(expressMappingEntity);
            }
        }
    }

    public String getTracking(String expressSn){
        String trackingId = getTrackingId(expressSn);
        if ("".equals(trackingId)){
            return "";
        }
        String url = TRACKING_URL+trackingId;
        Map<String,String> head = new HashMap<>();
        head.put("aftership-api-key",API_KEY);
        head.put("Content-Type",CONTENT_TYPE);

        Map<String,String> param = new HashMap<>();
        return HttpClientUtils.createHttpGet(url,head,param);
    }

    public String getTrackingId(String expressSn){
        String trackingId = "";
        try {
            String url = "http://pfst.cloudokids.com:8880/API/GetAfterShippingTrackingId?trackingNumber="+expressSn;
            String result = HttpClientUtils.createHttpGet(url,new HashMap<>(),new HashMap<>());

            JsonParser parser = new JsonParser();
            JsonObject object = parser.parse(result).getAsJsonObject();
            trackingId = object.get("TrackingId").getAsString();
            String slug = object.get("Slug").getAsString();
            logger.info("===开始获取trackingId信息 slug:"+slug+" expressSn:"+expressSn+" trackingId:"+trackingId);
        }catch (Exception e){
            logger.error("获取trackingId错误，expressSn:" +expressSn);
        }
        return trackingId;
    }

    public String getExpressInfo(String parcelSn,int type) {
        String expressNumber = "";
        String expressCode = "";
        String status = "";
        Gson gson = new Gson();
        Object data;
        List<ExpressContent> result = new ArrayList<>();

        List<ExpressMappingEntity> expressMappingEntities = expressMappingDao.findByParcelSn(parcelSn);
        if (expressMappingEntities == null || expressMappingEntities.size() ==0){
            Map<String,Object> map = new HashMap();
            map.put("express",expressCode);
            map.put("status",status);
            map.put("expressNumber",expressNumber);
            map.put("expressInfo","");
            return gson.toJson(map);
        }

        expressMappingEntities = expressMappingEntities.stream()
                .sorted(Comparator.comparing(ExpressMappingEntity::getSign).reversed())
                .collect(Collectors.toList());
        expressNumber = expressMappingEntities.get(0).getExpressSn();
        expressCode = expressMappingEntities.get(0).getExpressCode();

        if (expressMappingEntities.get(0).getStatus().equals("1")){
            status = "已签收";
        }else {
            status = "运输中";
        }

        List<ExpressContent> finalResult = result;
        expressMappingEntities.forEach(e->{
            String expressSn = e.getExpressSn();
            ExpressValueEntity expressValueEntity = expressValueDao.findByExpressSn(expressSn);
            if (expressValueEntity != null){
                String info = expressValueEntity.getInfo();
                List<ExpressContent> contents =  gson.fromJson(info, new TypeToken<List<ExpressContent>>() {}.getType());
                finalResult.addAll(contents);
            }
        });
        result = result.stream().sorted((o1, o2) -> {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                Date dt1 = format.parse(o1.getTime());
                Date dt2 = format.parse(o2.getTime());
                if (dt1.getTime() > dt2.getTime()) {
                    return -1;
                } else if (dt1.getTime() < dt2.getTime()) {
                    return 1;
                } else {
                    return 0;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return 0;
        }).collect(Collectors.toList());

        if (type == 0){
            data = result.get(0);
        }else {
            data = result;
        }

        Map<String,Object> map = new HashMap();
        map.put("express",expressCode);
        map.put("status",status);
        map.put("expressNumber",expressNumber);
        map.put("expressInfo",data);

        return gson.toJson(map);
    }

    /**
     * 查询顺丰快递信息
     * @param expressSn
     * @return
     * @throws IOException
     * @throws DocumentException
     */
    public String querySHUNFENG(String expressSn,String checkPhoneNo) throws IOException, DocumentException {
        String url = "http://bsp-oisp.sf-express.com/bsp-oisp/sfexpressService"; //外网地址
        int port = 11080;
        String checkWord = "8u1i9rkq13yzg5W8C3WOxWxbFcGzE9MX";
        // 组装查询的xml数据，使用物流单号+用户手机后四位方式查询
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Request service=\"RouteService\" lang=\"zh-CN\"><Head>kdss</Head><Body>" +
                "<RouteRequest tracking_type=\"3\" method_type=\"1\" tracking_number=\"" + expressSn + "\" check_phoneNo = \""+ checkPhoneNo +"\"/></Body></Request>";

        String verifyCode = SFUtil.md5EncryptAndBase64(xml + checkWord);
        List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("xml", xml));
        nameValuePairs.add(new BasicNameValuePair("verifyCode", verifyCode));

        HttpClient httpclient = SFUtil.getHttpClient(port);
        HttpPost httpPost = new HttpPost(url);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(2000)  // 设置连接到目标URL的等待时长
                .setSocketTimeout(3000)   // 是从connect Manager（连接池）获取连接的等待时长
                .setConnectionRequestTimeout(2000) // 连接到目标URL之后等待返回响应的时长
                .build();
        httpPost.setConfig(requestConfig);
        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, Consts.UTF_8));
        HttpResponse httpResponse = httpclient.execute(httpPost);
        String responseXML = EntityUtils.toString(httpResponse.getEntity());
        List<Element> remarks;
        List<Map<String,String>> result = new ArrayList<>();
        if (httpResponse.getStatusLine().getStatusCode() == 200) {
            // 顺丰返回的xml报文信息
            Document responseInfo = DocumentHelper.parseText(responseXML);
            Element body = responseInfo.getRootElement().element("Body");
            if (body.content().size() > 0) {
                remarks = body.element("RouteResponse").elements();
                remarks.forEach(e->{
                    Map<String,String> info = new HashMap<>();
                    List<Attribute> s = e.attributes();
                    String msg = s.get(0).getValue();
                    info.put("time",s.get(1).getValue());
                    info.put("location",s.get(2).getValue());
                    info.put("msg",msg);
                    info.put("sign","3");
                    result.add(info);
                    if (msg.contains("已签收") || msg.contains("代签收")){
                        ExpressMappingEntity expressMappingEntity = expressMappingDao.findByExpressSn(expressSn);
                        if (expressMappingEntity != null){
                            expressMappingEntity.setStatus("1");
                            expressMappingDao.save(expressMappingEntity);
                        }
                    }
                });
            }
        }
        Gson g =new Gson();
        httpPost.abort();
        return g.toJson(result);
    }

    public String queryZHONGTONG(String expressSn) throws IOException{
        Gson g =new Gson();
        List<Map<String,String>> resultList = new ArrayList<>();
        String param = "company_id=" + ZHONGTONG_COMPANY_ID + "&msg_type=NEW_TRACES&data=[" + "'"+ expressSn +"'" + "]";
        Map<String, String> header = new HashMap<>();
        header.put("x-companyId", ZHONGTONG_COMPANY_ID);
        header.put("x-dataDigest", Base64.encodeBase64String(DigestUtils.md5(param + ZHONGTONG_SIGNATURE)));
        String result = HttpUtil.post(ZHONGTONG_URL, header, param);
        if(result == null){
            return null;
        }
        JsonParser jsonParser = new JsonParser();
        JsonArray array = jsonParser.parse(result).getAsJsonObject().get("data").getAsJsonArray().get(0).getAsJsonObject().get("traces").getAsJsonArray();
        array.forEach(e->{
            Map<String,String> info = new HashMap<>();
            String msg = e.getAsJsonObject().get("desc").getAsString();
            String location = e.getAsJsonObject().get("preOrNextProv").getAsString()
                    +e.getAsJsonObject().get("scanSite").getAsString();
            String time = e.getAsJsonObject().get("scanDate").getAsString();
            String sign = "3";
            info.put("time",time);
            info.put("location",location);
            info.put("msg",msg);
            info.put("sign",sign);
            resultList.add(info);
        });
        return g.toJson(result);
    }

    public String queryYUANTONG(String expressSn) throws IOException{

        return "";
    }

    public String queryEpassExpress(String bizContent) {
        Map<String,Object> retMap = new HashMap<>();
        Gson gson = new Gson();
        try{
            logger.error("开始处理卓志快递信息");
            List<Map<String,String>> resultList = new ArrayList<>();
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = jsonParser.parse(bizContent).getAsJsonObject();

            String code = jsonObject.get("cp_code").getAsString();
            logger.error("国内快递公司编码："+code);

            String esdNo = jsonObject.get("esdno").getAsString();
            logger.error("卓志快递信息快递单号："+esdNo);

            String mailNo = jsonObject.get("mail_no").getAsString();
            logger.error("国内快递信息快递单号："+mailNo);

            jsonObject.get("traces").getAsJsonArray().forEach(e->{
                Map<String,String> info = new HashMap<>();
                JsonObject  trace = e.getAsJsonObject();
                info.put("time",trace.get("accept_time").getAsString());
                info.put("location",trace.get("accept_station").getAsString());
                info.put("msg",trace.get("accept_station").getAsString());
                info.put("sign","2");
                resultList.add(info);
            });

            String info = gson.toJson(resultList);
            ExpressValueEntity expressValueEntity = expressValueDao.findByExpressSn(esdNo);
            if (expressValueEntity == null){
                expressValueEntity = new ExpressValueEntity();
                expressValueEntity.setExpressSn(esdNo);
                expressValueEntity.setInfo(info);
            }else {
                List<Map<String,String>> date = gson.fromJson(expressValueEntity.getInfo(),List.class);
                if(date.containsAll(resultList)){
                    return "此信息已存在";
                }
                date.addAll(resultList);
                date = date.stream().distinct().collect(Collectors.toList());
                expressValueEntity.setInfo(gson.toJson(date));
            }
            expressValueDao.save(expressValueEntity);
            retMap.put("status", 1);
            retMap.put("notes", "成功");
        }catch (Exception e){
            logger.error("接受卓志物流回推失败:   "+e.getMessage());
            retMap.put("status", -1);
            retMap.put("notes", "失败");
        }
        return gson.toJson(retMap);
    }

    public Object saveMerchantsExpressInfo(String parcelNumber, String dateTime, String msg, String location) {
        Gson gson = new Gson();
        int sign = 1;
        ExpressMappingEntity expressMappingEntity = expressMappingDao.findByParcelSnAndSign(parcelNumber,1);
        if (expressMappingEntity ==null){
            expressMappingEntity = new ExpressMappingEntity();
            expressMappingEntity.setParcelSn(parcelNumber);
            expressMappingEntity.setStatus("0");
            expressMappingEntity.setExpressSn(parcelNumber);
            expressMappingEntity.setSign(1);
            expressMappingEntity.setExpressCode("CLOUDO");

            expressMappingDao.save(expressMappingEntity);
        }

        ExpressContent expressContent = new ExpressContent();
        expressContent.setLocation(location);
        expressContent.setMsg(msg);
        expressContent.setTime(dateTime);
        expressContent.setSign(sign+"");

        ExpressValueEntity expressValueEntity = expressValueDao.findByExpressSn(parcelNumber);
        if (expressValueEntity == null){
            List<ExpressContent> expressContents = new ArrayList<>();
            expressContents.add(expressContent);
            expressValueEntity = new ExpressValueEntity();
            expressValueEntity.setExpressSn(parcelNumber);

            expressValueEntity.setInfo(gson.toJson(expressContents));

            expressValueDao.save(expressValueEntity);
        }else {
            List<ExpressContent> contents =  gson.fromJson(expressValueEntity.getInfo(), new TypeToken<List<ExpressContent>>() {}.getType());
            if (contents.contains(expressContent)){
                return "该信息已存在";
            }

            contents.add(expressContent);

            contents = contents.stream().sorted((o1, o2) -> {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                try {
                    Date dt1 = format.parse(o1.getTime());
                    Date dt2 = format.parse(o2.getTime());
                    if (dt1.getTime() > dt2.getTime()) {
                        return 1;
                    } else if (dt1.getTime() < dt2.getTime()) {
                        return -1;
                    } else {
                        return 0;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return 0;
            }).collect(Collectors.toList());
            expressValueEntity.setInfo(gson.toJson(contents));
            expressValueDao.save(expressValueEntity);
        }
        return "success";
    }


    public Object saveParcelNumber(String parcelNumber,String checkPhoneNo) {
        try{
            TaskEntity taskEntity = taskDao.findByParcelSn(parcelNumber);
            if (taskEntity == null){
                taskEntity = new TaskEntity();
                taskEntity.setParcelSn(parcelNumber);
                taskEntity.setStatus("0");
            }
            taskEntity.setCheckPhoneNo(checkPhoneNo);

            taskDao.save(taskEntity);

            return "success";
        }catch (Exception e){
            return "error";
        }
    }

    public Object queryExpress(String code, String expressNo,String checkPhoneNo) {
        try {
            ExpressValueEntity expressValueEntity = expressValueDao.findByExpressSn(expressNo);
            if(expressValueEntity != null){
                return expressValueEntity.getInfo();
            }

            String info = doExpressQuery(code,expressNo,checkPhoneNo);
            expressValueEntity = new ExpressValueEntity();
            expressValueEntity.setInfo(info);
            expressValueEntity.setExpressSn(expressNo);
            expressValueEntity.setCode(code);
            expressValueDao.save(expressValueEntity);

            return info;
        }catch (Exception e){
            return "";
        }
    }

    private String doExpressQuery(String code, String expressNo,String checkPhoneNo) throws IOException, DocumentException {
        String info = "";
        switch (code){
            case "SF":
                info = querySHUNFENG(expressNo,"");
                break;
            case "YT":
                info = queryYUANTONG(expressNo);
                break;
            case "ZT":
                info = queryZHONGTONG(expressNo);
                break;
            case "ZZ":
                info ="";
                break;
            default :
                info =queryInternationalLogistics(code,expressNo);
                break;
        }
        return info;
    }

    private String queryInternationalLogistics(String slug,String trackingNumber){

        Gson gson = new Gson();
        JsonParser parser = new JsonParser();
        String url = TRACKING_URL+slug+"/"+trackingNumber;
        System.out.println(url);
        Map<String,String> head = new HashMap<>();
        head.put("aftership-api-key",API_KEY);
        head.put("Content-Type",CONTENT_TYPE);

        Map<String,String> param = new HashMap<>();
        String result =  HttpClientUtils.createHttpGet(url,head,param);
        if (result.contains("Tracking does not exist")){
            String tracking = createTracking(trackingNumber,slug);
            result =  HttpClientUtils.createHttpGet(url,head,param);
        }

        JsonObject object = parser.parse(result).getAsJsonObject();

        String code = object.get("meta").getAsJsonObject().get("code").toString();
        if(!code.equals("200")){
            return "";
        }
        JsonArray data = object.get("data").getAsJsonObject()
                .get("tracking").getAsJsonObject()
                .get("checkpoints").getAsJsonArray();
        List resultList = new ArrayList();
        data.forEach(e->{
            Map<String,String> info = new HashMap<>();
            String msg = e.getAsJsonObject().get("message").isJsonNull() ? "":e.getAsJsonObject().get("message").getAsString();
            String location = e.getAsJsonObject().get("city").isJsonNull()
                    ?(e.getAsJsonObject().get("location").isJsonNull() ? "":e.getAsJsonObject().get("location").getAsString())
                    : e.getAsJsonObject().get("city").getAsString();
            String time = e.getAsJsonObject().get("checkpoint_time").isJsonNull() ?"":e.getAsJsonObject().get("checkpoint_time").getAsString();
            String sign = "1";
            info.put("time",time);
            info.put("location",location);
            info.put("msg",msg);
            info.put("sign",sign);
            resultList.add(info);
        });
        String info = gson.toJson(resultList);
        return info;
    }

    private String createTracking(String trackingNumber,String slug){
        String url = TRACKING_URL;

        Map<String,String> head = new HashMap<>();
        head.put("aftership-api-key",API_KEY);
        head.put("Content-Type",CONTENT_TYPE);

        Map<String,Object> tracking = new HashMap<>();
        tracking.put("tracking_number",trackingNumber);
        tracking.put("slug",slug);

        Map<String,Object> param = new HashMap<>();
        param.put("tracking",tracking);
        String result =  HttpsUtil.doPost(url,head,param);
        return result;
    }
}
