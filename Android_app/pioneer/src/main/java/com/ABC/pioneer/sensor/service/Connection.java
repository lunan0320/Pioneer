package com.ABC.pioneer.sensor.service;

import android.content.Context;

import com.ABC.pioneer.sensor.client.controller.PioneerClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.net.ssl.SSLSocket;

public class Connection {
    private Context context;
    DataInputStream in = null;
    DataOutputStream out = null;

    public Connection() throws IOException {
    }

    public Connection(Context context) throws IOException {
        this.context = context;
    }

    //连接至服务器
    public void ConnectToServer() {
        PioneerClient connect = new PioneerClient("95.179.230.181", 446, context);
        final int handshakeTimeout=3*1000;
        final int sessionTimeout=7*1000;
        try {
            SSLSocket socket = connect.run();
            socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
            // 开始握手
            socket.setSoTimeout(handshakeTimeout);
            socket.startHandshake();
            socket.setSoTimeout(sessionTimeout);
            // 建立连接后获取会话
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
        }
    }

    public String Register(String phone, String secretKey) {
        String str = "0";
        try {

                str += phone;
                str += secretKey.toString();
                byte[] out_bytes = new byte[str.length()];
                out_bytes = str.getBytes();
                //发送用户信息到服务器
                out.write(out_bytes);
                out.flush();
                try {
                    //0表示注册成功,1表示手机号重复，2表示密钥重复
                    str = in.readUTF();
                } catch (Exception e) {
                }
        } catch (IOException e) {
        }
        return str;

    }

    public String Token(String token) {
        String str = "1";
        try {
                str += token;
                byte[] out_bytes = new byte[str.length()];
                out_bytes = str.getBytes();
                //发送用户信息到服务器
                out.write(out_bytes);
                out.flush();
                try {
                    str = in.readUTF();
                } catch (Exception e) {
                }
        } catch (IOException e) {
        }
        return str;
    }

    public String Token_TransmitMachingKeys(String phone,String Maching_keys) throws IOException {
        String str ="3";
        str += phone;
        str+=Maching_keys;
        byte[] out_bytes=new byte[10000];
        out_bytes=str.getBytes();
        out.write(out_bytes);
        out.flush();
        str=in.readUTF();
        return str;
    }
    public String TransmitMachingKeys(String phone,String Maching_keys) throws IOException {
        String str ="4";
        str += phone;
        str+=Maching_keys;
        byte[] out_bytes=new byte[10000];
        out_bytes=str.getBytes();
        out.write(out_bytes);
        out.flush();
        str=in.readUTF();
        return str;
    }

    public String Download_Message() throws IOException {
        String str = "5";
        //根据输入的长度动态的分配
        byte[] out_bytes = new byte[str.length()];
        out_bytes = str.getBytes();
        //发送用户信息到服务器
        out.write(out_bytes);
        out.flush();
        str = in.readUTF();
        return str;
    }



}
