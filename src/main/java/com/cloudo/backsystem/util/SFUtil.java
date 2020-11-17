package com.cloudo.backsystem.util;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class SFUtil {
    public static HttpClient getHttpClient(int port) {
        PoolingClientConnectionManager pcm = new PoolingClientConnectionManager();
        SSLContext ctx = null;
        try {
            ctx = SSLContext.getInstance("TLS");
            X509TrustManager x509 = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            ctx.init(null, new TrustManager[]{x509}, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        SSLSocketFactory ssf = new SSLSocketFactory(ctx, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        Scheme sch = new Scheme("https", port, ssf);
        pcm.getSchemeRegistry().register(sch);
        return new DefaultHttpClient(pcm);
    }


    public static String md5EncryptAndBase64(String str) {
        return Base64.encode(md5Encrypt(str));
    }

    private static byte[] md5Encrypt(String encryptStr) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(encryptStr.getBytes(StandardCharsets.UTF_8));
            return md5.digest();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
