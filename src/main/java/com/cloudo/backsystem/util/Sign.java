package com.cloudo.backsystem.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Sign {
    private static final Logger logger = LoggerFactory.getLogger(Sign.class);

    //签名
    public static final String SIGN = "sign";
    //算法
    public static final String MD5 = "md5";
    //字符编码
    public static final String CHARSET_UTF8 = "utf-8";


    /**
     * @param json 请求报文
     * @param appSecret 密钥
     */
    public static String sign(String json, String appSecret) {
        TreeMap<String, Object> map = new TreeMap<>();
        map.putAll(JSON.parseObject(json, new TypeReference<LinkedHashMap<String, Object>>() {
        }, Feature.OrderedField));

        //获取明文
        String plain = getPlain(map, appSecret);
        logger.info("签名明文：" + plain);
        //进行MD5加密
        String sign = md5(plain);
        logger.info("签名密文：" + sign);
        //转换大写
        sign = StringUtils.upperCase(sign);
        logger.info("签名密文转换大写：" + sign);
        return sign;
    }


    /**
     * 获取签名明文
     *
     * @param treeMap
     */
    @SuppressWarnings("rawtypes")
    private static String getPlain(Map<String, Object> map, String appSecret) {
        StringBuilder sb = new StringBuilder(appSecret);
        //遍历map，拼接报文明文
        Object obj = null;
        for (String key : map.keySet()) {
            if (SIGN.equals(key)) {
                continue;
            }
            obj = map.get(key);
            if (null != obj) {
                if (obj instanceof List) {
                    if (((List) obj).size() > 0) {
                        sb.append(key).append(obj.toString());
                    }
                } else if (obj instanceof String) {
                    if (StringUtils.isNotBlank((CharSequence) obj)) {
                        sb.append(key).append(obj.toString());
                    }
                } else {
                    sb.append(key).append(obj.toString());
                }
            }
        }
        sb.append(appSecret);
        return sb.toString();
    }


    /**
     * MD5加密
     *
     */
    public static String md5(String str) {
        return encrypt(str, MD5);
    }

    /**
     * md5或者sha-1加密
     */
    private static String encrypt(String inputText, String algorithmName) {
        if (inputText == null || "".equals(inputText.trim())) {
            return null;
        }
        if (algorithmName == null || "".equals(algorithmName.trim())) {
            algorithmName = MD5;
        }
        String encryptText = null;
        try {
            MessageDigest m = MessageDigest.getInstance(algorithmName);
            m.update(inputText.getBytes(CHARSET_UTF8));
            byte s[] = m.digest();
            return hex(s);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encryptText;
    }


    /**
     * 返回十六进制字符串

     */
    private static String hex(byte[] arr) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < arr.length; ++i) {
            sb.append(Integer.toHexString((arr[i] & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }
}
