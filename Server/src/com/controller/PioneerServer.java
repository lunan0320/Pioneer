package com.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;


import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.bean.User;
import com.bean.UserMessage;
import com.dao.TaskThread;
import com.dao.UserDao_Imp;

//HTTPS的Server实现
public class PioneerServer {
	private int port = 446;
	private boolean isServerDone = false;
	public static void main(String[] args) throws IOException {
		PioneerServer server = new PioneerServer();
		server.run();
	}

	public PioneerServer() {
	}

	PioneerServer(int port) {
		this.port = port;
	}

	// 创建并初始化SSLContext
	public SSLContext createSSLContext() {
		try {
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			keyStore.load(new FileInputStream("./server.p12"), "010320".toCharArray());
			// 自己实现信任管理器类，让它信任我们指定的证书
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
			keyManagerFactory.init(keyStore, "010320".toCharArray());
			KeyManager[] km = keyManagerFactory.getKeyManagers();
			
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
			trustManagerFactory.init(keyStore);
			TrustManager[] tm = trustManagerFactory.getTrustManagers();

			// Initialize SSLContext
			SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
			sslContext.init(km, tm, null);

			return sslContext;
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return null;
	}

	// 服务器开始运行
	@SuppressWarnings({ "resource", "null" })
	public void run() throws IOException {
		try {
			SSLContext sslContext = this.createSSLContext();

			// 创建服务器socket factory
			SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();

			// 创建ServerSocket类型的对象并提供端口号
			SSLServerSocket serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(this.port,3);
			
			System.out.println("This is  Pioneer Server!");
			System.out.println("Waiting for the client to connect......");
			System.out.println("SSL server started");
		
			// 当没用客户端连接时，则服务器阻塞在accept方法这里
			new TaskThread().start();
			ExecutorService fixedThreadPool=Executors.newFixedThreadPool(3);
			
			while(!isServerDone){
				SSLSocket socket = (SSLSocket) serverSocket.accept();
                // 线程池的实现
                fixedThreadPool.execute(new Runnable() {
					@Override
					public void run() {
						 System.out.println("线程 " + Thread.currentThread().getName() + " 工作");
						 try {
							 new ServerThread(socket).run();
						 }catch(Exception e) {
						 }finally {
							 if(Thread.currentThread().isInterrupted()) {
								 Thread.currentThread().stop();
							 }
							 System.out.println("finally:"+Thread.currentThread().getName() + "stop");
						 }
					}
            		
            	});
            }
		}catch (Exception ex){
            ex.printStackTrace();
        }
	}
	
	// 线程处理
    static class ServerThread {
    	
    	private SSLSocket serverSocket = null;
    	final int readTimeout=60*1000;
    	final int connectTimeout=10*1000;
		UserMessage userMessage = null;

        ServerThread(SSLSocket sslSocket){
            this.serverSocket = sslSocket;
        }
		@SuppressWarnings("null")
		public void run()  {
			DataInputStream in=null;
			DataOutputStream out=null;
        	try {
        		serverSocket.setSoTimeout(connectTimeout);//服务端设置连接超时
            	serverSocket.setEnabledCipherSuites(serverSocket.getSupportedCipherSuites());
    			// 握手过程
            	serverSocket.startHandshake();
            	serverSocket.setSoTimeout(readTimeout);
    			// 会话获取
    			SSLSession sslSession = serverSocket.getSession();
    			System.out.println("SSLSession :");
    			System.out.println("\tProtocol : " + sslSession.getProtocol());
    			System.out.println("\tCipher suite : " + sslSession.getCipherSuite());
    			// 输出成功连接的客户端地址
    			System.out.println("客户端" + serverSocket.getInetAddress() + "连接成功！");
    			in=new DataInputStream(serverSocket.getInputStream());
    			out = new DataOutputStream(serverSocket.getOutputStream()); 
    		    out.flush();
    		    String token=null;
    		    while (true) {
    		         //执行过程
    		    	 byte[] bytes=new byte[10000];
    		         User user = new User();
    	    		 String tag=null;
    	    		 String str="";
    		         UserDao_Imp userDao_Imp = new UserDao_Imp();
    		         int input=0;
    			     //读取客户端发送的请求
    		         do {
    		        	 input = in.read(bytes);
    		        	 str=new String(bytes);
    		        	 if(str.substring(0,1).equals("5")||str.substring(0,1).equals("6")) {break;}
    		         }while(input<=1);
    		         if(input!=0) {
    		        	String ini_str="";
    		        	str="";
    		        	ini_str=new String(bytes);
    		        	for(int i=0;i<ini_str.length();i++) {
    	    				char ch=ini_str.charAt(i);
    	    				if(ch!='\0'&&ch!=' ') {
    	    					str+=ch;
    	    				}
    	    			}
    		        	System.out.println("客户端发送长度："+str.length());
    		        	
    		          }else {break;}
    		          //根据type类型的不同，执行不同的与数据库交互的操作
   				      //根据tag的不同，来判断 
   				      //tag=0，表示注册用户：0+11位手机号+2048位Secret_key
   				      //tag=1，表示上传前验证token
    		          //tag=2，表示申请token
   		              //tag=3.表示自动上传14天Matching_key
    		          //tag=4。表示通过token验证后上传14天Matching_key
    		          //tag=5。表示客户端向服务器发起请求，下载infected_users的内容信息
    		          String log=null;
    		          tag=str.substring(0,1);
    		          System.out.println("tag:"+tag);
    				  switch(tag) {  
    				  case("0")://"LoginUser"  
    					  user.setId(str.substring(1,12));//截取手机号
    				  	  user.setKey(str.substring(12)); //截取SecretKey
    				  	  System.out.println(user.getId());
    				  	  System.out.println("key:"+user.getKey().length());
    					  log=userDao_Imp.loginUser(user); 
    					  System.out.println("返回值："+log);
    					  break;
    				  case("1")://"CheckToken"  token设置为了6位
    					  user.setToken(str.substring(1,7));
    				  	  token=user.getToken();
    				  	  System.out.println("token"+user.getToken());
    				  	  System.out.println("length"+user.getToken().length());
    					  log=userDao_Imp.CheckToken(user); 
    				  	  System.out.println("返回值"+log); 
    				  	  break;
    				  case("2")://"ApplyToken" 
    					  //用户名和密码都设置为了7位
    					  user.setName(str.substring(1,8));
    				      user.setPassword(str.substring(8,15));
    				      System.out.println(user.getName());
    				      System.out.println(user.getPassword());
    	 				  token=userDao_Imp.ApplyToken(user); 
    				  	  break; 
    				  case("3")://一个字段的upload14天Matchingkeys
    					  user.setId(str.substring(1,12));
    				  	  System.out.println("id"+user.getId());
    				  	  user.setMatching_keys(str.substring(12));
    				  	  System.out.println("MatchingKeys"+user.getMatching_keys());
    				  	  log=userDao_Imp.Upload(user, token);
  				  	      System.out.println("log"+log); 
    				  	  break; 
    				  case("4"): //auto upload\
    					  user.setId(str.substring(1,12));
    				  	  System.out.println("自动上传接触者信息~");
    				  	  System.out.println("id"+user.getId());
    				  	  user.setMatching_keys(str.substring(12));
    				  	  System.out.println(user.getMatching_keys());
    				  	  log=userDao_Imp.AutoUpload(user);
    					  System.out.println("log:"+log); 
    				  	  break; 
    				  case("5"):
    					  System.out.println("客户端从服务器下载感染者信息~");
    					  log=userDao_Imp.Download();
    				  	  System.out.println("log:"+log); 
    				  	  System.out.println("长度:"+log.length()); 
    				  	  break;
    				  case("6"):
    					  log=userDao_Imp.Update_Infectedusers();
    				  	  log+=userDao_Imp.Update_Contactedusers();
    				  	  break;
    				  default:
    					  break;
    				  } 	
    				  if(tag.equals("0")) { 
    					  if(log.equals("0")){out.writeUTF("0");System.out.println("发送success信息给客户端~");break;} 
    					  if(log.equals("1")){out.writeUTF("1");break;} 
    					  if(log.equals("2")){out.writeUTF("2");break;}
    					  out.flush();
    				  } 
    				  if(tag.equals("1")) { 
    					  out.writeUTF(log);
    					  out.flush();
    					  if(!log.equals("0")) {
    						  break;
    					  }
    					  System.out.println("发送success信息给客户端~"); 
    				  }
    				  if(tag.equals("2")) { 
    					  System.out.println("返回值："+token);
						  out.writeUTF(token);
    					  out.flush();
    					  break;
    				  }
    				  if(tag.equals("3")) {    
    					  System.out.println("log:"+log);
    					  out.writeUTF(log);
						  out.flush();
						  if(log.equals("0")) {
	    					  System.out.println("发送success信息给客户端~"); 
	    					  break;
	    				  }
    				  }
    				  if(tag.equals("4")) { 
    					  out.writeUTF(log);
						  out.flush();
						  if(log.equals("0")) {
	    					  System.out.println("自动上传接触者信息成功~"); 
	    					  break;
	    				  }
    				  }
    				  if(tag.equals("5")) {
    					  if(log.equals(null)) {
    						  log="1";
    					  }
    					  out.writeUTF(log);
						  out.flush();
						  System.out.println("感染者信息下载成功~");
						  break;
    				  }
    				  if(tag.equals("6")) {
    					  out.writeUTF(log);
						  out.flush();
						  break;
    				  }
    			} 
            }catch(IOException e) {
			e.printStackTrace();
		} finally {
			System.out.println("finally:"+Thread.currentThread().getName() + " interrupt");
			System.out.println("该线程服务器下线");
			// 释放资源
			if (null != out) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (null != in) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (null != serverSocket) {
				try {
					serverSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			//中断线程
			Thread.currentThread().interrupt();
		}
    }
  }
}