package com.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.Scanner;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.view.View;

// HTTPS的Client实现
public class PioneerClient {
	static int item;
	//private String host = "172.25.226.143";
   //private String host = "192.168.43.19";
    private String host = "95.179.230.181";
    private int port = 446;
    
    public static void main(String[] args) throws UnknownHostException, IOException{
        PioneerClient client = new PioneerClient();
        client.run();
    }
       
    PioneerClient(){      
    }
     
    PioneerClient(String host, int port){
        this.host = host;
        this.port = port;
    }
     
    // Create the and initialize the SSLContext
    private SSLContext createSSLContext(){
   	 try{
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
           // keyStore.load(new FileInputStream("D:\\Java\\cert\\Pioneer.keystore"),"sducst".toCharArray());
             
//            // Create key manager
//            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
//            keyManagerFactory.init(keyStore, "sducst".toCharArray());
//            KeyManager[] km = keyManagerFactory.getKeyManagers();
//            keyStore.load(new FileInputStream("D:\\Java\\cert\\server.p12"), "010320".toCharArray());

            //keyStore.load(new FileInputStream("D:\\Java\\cert\\server.p12"), "010320".toCharArray());

			// Create key manager
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
			keyManagerFactory.init(keyStore, "010320".toCharArray());
			KeyManager[] km = keyManagerFactory.getKeyManagers();
			
            // Create trust manager
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            trustManagerFactory.init(keyStore);
            TrustManager[] tm = trustManagerFactory.getTrustManagers();
             
            // Initialize SSLContext 
            SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(km,  tm, null);
             
            return sslContext;
        } catch (Exception ex){
            ex.printStackTrace();
        }
         
        return null;
   }
     
    // Start to run the server

    
    public void run() throws IOException{
    	final int connectTimeout=5*1000;
		//SSLContext
		SSLContext sslContext = this.createSSLContext();
		 // Create socket factory
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        //创建Socket类型的对象，并提供服务器的主机名和端口号
        //SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(this.host, this.port);
        //SSLSocket socket = (SSLSocket) sslSocketFactory.getDefault().createSocket();
        SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket();
        socket.connect(new InetSocketAddress(host,port), connectTimeout);//设置连接超时5s
        System.out.println("This is SDU Pioneer Client!");
		System.out.println("Successfully connected to the server"); 
        System.out.println("SSL client started");
     
        new ClientThread(socket).start();
    }
     
    static class ClientThread extends Thread{
    	private SSLSocket socket = null;
    	final int handshakeTimeout=3*1000;
    	final int sessionTimeout=7*1000;
     
        ClientThread(SSLSocket sslSocket){
            this.socket = sslSocket;
        }
    	public void run() {	
        	Scanner sc=null;
        	DataInputStream in=null;
			DataOutputStream out=null;
    		try {
    		
    			socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
    			socket.setSoTimeout(handshakeTimeout);
    			// Start handshake
    			socket.startHandshake();
    			socket.setSoTimeout(sessionTimeout);
    			 // Get session after the connection is established
                SSLSession sslSession = socket.getSession();
                System.out.println("SSLSession :");
                System.out.println("\tProtocol : "+sslSession.getProtocol());
                System.out.println("\tCipher suite : "+sslSession.getCipherSuite());
               
    			//初始化输入输出流
    			sc=new Scanner(System.in);
    			in=new DataInputStream(socket.getInputStream());
    			out = new DataOutputStream(socket.getOutputStream()); 

    			//通过菜单导引，从控制台获得用户输入的信息并将返回值作为新建的对象
    			String input=null;
    			String output="";
    			String str=null;
    			
    			//while(true) {
    			String type=null;
				//tag=0，表示注册用户：0+11位手机号+2048位Secret_key
				//tag=1，表示上传前验证token
		        //tag=2，表示申请token
		        //tag=3.表示自动上传14天Matching_key
		        //tag=4。表示通过token验证后上传14天Matching_key
    			//tag=5。表示下载服务器存储的Infected_users的14天的Matching_keys
    			//tag=6。表示每天定时的更新的过程
    			item = View.MenuView();
    			switch(item) {
    				//根据菜单选择，初始化类型
    				case 0://退出
    					type="Exit";
    					break;
    				case 1://手机号注册
    					type="LoginUser";//015536471788123
    					output="0";
    					break;
    				case 2://服务器检测token是否正确并后续上传
    					type="CheckToken";
    					output="1";
    					break;
    				case 3://医务人员管理员申请Token
    					type="ApplyToken";//2doctor1doctor1
    					output="2";
    					break;
    				case 4://自动上传
    					type="AutoUpload";
    					output="4";
    					break;
    				case 5://客户端下载感染者信息
    					type="Download";
    					break;
    				case 6://更新感染者以及接触者信息
    					type="Update";
    					break;
    			 	}
    			//输出过程
    			output+=MenuView();//要输出的字符串
    			System.out.println("发送到服务器长度为:"+output.length());
    			//根据输入的长度动态的分配
    			byte[] out_bytes=new byte[output.length()];
    			out_bytes=output.getBytes();
    			
    			//bytes=input;
    			//发送用户信息到服务器
    			out.write(out_bytes);
    			out.flush();
    			System.out.println("已发送信息到服务端~:"+output);
    			
    			//读入过程
    			//读取服务端回发的验证结果
    			//if(!type.equals("AutoUpload")) {
    				try { 
        				str=in.readUTF();
        				System.out.println("从服务器收到验证结果："+str);
        				if(type.equals("Download")) {
        					int i =0;
        					int index1=0;
        					int index2=0;
        					String temp="";
        					while(true) {
        						index1=str.indexOf("SDU",i);
        						if(index1==-1) break;
        						index2=str.indexOf("SDU",index1+3);
        						if(index2==-1) break;
        						temp=str.substring(index1+3, index2);
        						System.out.println("temp:"+temp);
        						i=index2;
        					}
        				}
            		    if(str.equals("0")&&type.equals("CheckToken")) {
            		        output ="3";
            		        output += View.UploadMenuView();
            		        out_bytes=new byte[10000];
            		        out_bytes=output.getBytes();
            		        out.write(out_bytes);
            		    	out.flush();
            		    	System.out.println("已发送信息到服务端~:"+output);
            		    	//System.out.println("已发送信息到服务端~:"+out_bytes);
            		    	
            		    	str=in.readUTF();
            				System.out.println("从服务器收到验证结果："+str);
            		    }
        		        //0表示注册成功,1表示手机号重复，2表示密钥重复
        			}catch(Exception e) {
        				e.printStackTrace();
        			}
    			//}
    			}catch(IOException e) {
    				e.printStackTrace();
    		}finally {
    			System.out.println("客户端下线，释放资源");
    			//释放资源
    			if(null!=out) {
    				try {
    					out.close();
    				}catch(IOException e) {
    					e.printStackTrace();
    				}
    			}
    			if(null!=in) {
    				try {
    					in.close();
    				}catch(IOException e) {
    					e.printStackTrace();
    				}
    			}
    			if(null!=socket) {
    				try {
    					socket.close();
    				}catch(IOException e) {
    					e.printStackTrace();
    				}
    			}
    			if(null!=sc) {
    				sc.close();
    			}
    		}
    	}
    }
    
    
    private static String MenuView() {
		while(true) {
			String input=null;
			switch(item) {
			case 0://退出
				System.exit(-1);
				break;
			case 1://注册用户
				input= View.loginUserMenuView();
				return input;
			case 2://用户上传token
				input=View.CheckTokenMenuView();
				return input;
			case 3://医务人员token申请
				input= View.ApplyTokenMenuView();
				return input;
			case 4://自动上传Matching_key
				input=View.AutoUploadView();
				return input;
			case 5:
				input="5";
				return input;
			case 6:
				input="6";
				return input;
			default:
				break;
			}
		}
    }
    
}