package com.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

//封装的JDBC数据库连接类
public class JDBCUtils {
	private static String driver;
	private static String url;
	private static String username;
	private static String password;
	static {
		//用流的形式读出来，getClassLoader类加载器
		InputStream is = JDBCUtils.class.getClassLoader().getResourceAsStream("db.properties");
		//创建property 的对象
		Properties p = new Properties();
		//加载流文件
		try {
			p.load(is);
			driver = p.getProperty("driver");
			url = p.getProperty("url");
			username = p.getProperty("username");
			password = p.getProperty("password");
			//加载MySql驱动
			Class.forName(driver);
			System.out.println("驱动加载成功");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace() ;
		}
	}
//获得连接对象

	public static Connection getConnection() {
		try {
			System.out.println("数据库连接成功");
			return DriverManager.getConnection(url, username, password);			
		}catch(SQLException e) {
			System.out.println("数据库连接失败");
			e.printStackTrace();
		}
		return null;
		
	}
//释放资源
	public static void close(Connection conn,Statement statement,ResultSet result) {
		try {
			if(result!=null) {
				result.close();
				result=null;
			}
			if(statement!=null) {
				statement.close();
				statement=null;
			}
			if(conn!=null) {
				conn.close();
				conn=null;
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}
		
}

