package com.ABC.pioneer.sensor.client.controller;

import android.content.Context;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

// HTTPS的Client实现
public class PioneerClient {
    static int item;
    private String host ;
    private int port;
    private Context context;


    public PioneerClient(){
    }

    public PioneerClient(String host, int port, Context context){
        this.host = host;
        this.port = port;
        this.context = context;
    }

    // 创建并初始化SSLContext
    public SSLContext createSSLContext(){
        try{
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            InputStream certificates = this.context.getAssets().open("server.p12");
            keyStore.load(certificates,"010320".toCharArray());

            // 创建密钥管理器
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, "010320".toCharArray());
            KeyManager[] km = keyManagerFactory.getKeyManagers();

            // 创建信任管理器
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            TrustManager[] tm = trustManagerFactory.getTrustManagers();

            // 初始化SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(km,  tm, null);

            return sslContext;
        } catch (Exception ex){
            ex.printStackTrace();
        }

        return null;
    }

    // 开始运行服务器


    public SSLSocket run() throws IOException{

        //SSLContext
        SSLContext sslContext = this.createSSLContext();
        // Create socket factory
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        //创建Socket类型的对象，并提供服务器的主机名和端口号
        SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(this.host, this.port);
        return socket;
    }






}