package com.dao;
import com.bean.User;
import com.dao.RandomGenerator;
import com.jdbc.JDBCUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

//UserDao方法的继承实现，与数据库的操作
public class UserDao_Imp implements UserDao {
	
	//实现定义Matching_key的长度,以及AES密钥
	private static final int Matching_key_length=3;
	private static final int token_length=6;
	private static final String SQL_USER_INSERT ="INSERT INTO `user` VALUES(id,?,?)";
	private static final String SQL_USER_INSERT_token ="INSERT INTO `tokens`(Username,Token) VALUES(HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')))";
	private static final String SQL_USER_DELETE_token = "DELETE  from `tokens` where `Token` =HEX(AES_ENCRYPT(?,'123456'))";
	private static final String SQL_USER_SELECT_users_key = "SELECT AES_DECRYPT(UNHEX(Secret_key),'123456') FROM `users_key` WHERE `ID`=HEX(AES_ENCRYPT(?,'123456'))";
	private static final String SQL_USER_SELECT_users_ID = "SELECT AES_DECRYPT(UNHEX(ID),'123456') FROM `users_key` WHERE `Secret_key`=HEX(AES_ENCRYPT(?,'123456'))";
	private static final String SQL_USER_SELECT_managers_key = "SELECT AES_DECRYPT(UNHEX(Password),'123456') FROM `managers` WHERE `Username`=HEX(AES_ENCRYPT(?,'123456'))";
	
	private static final String SQL_USER_CHECK_token = "SELECT AES_DECRYPT(UNHEX(Username),'123456') FROM `tokens` WHERE `Token`=HEX(AES_ENCRYPT(?,'123456'))";
	private static final String SQL_USER_LOGIN_User ="INSERT INTO `users_key`(ID,Secret_key) VALUES(HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')))";
	private static final String SQL_USER_LOGIN_Manager ="INSERT INTO `managers`(Username,Password) VALUES(HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')))";
	
	private static final String SQL_USER_AUTOUPLOAD_Matching_key="INSERT INTO `contacters`(ID,Matching_key1,Matching_key2,Matching_key3,Matching_key4,Matching_key5,Matching_key6,Matching_key7,Matching_key8,Matching_key9,Matching_key10,Matching_key11,Matching_key12,Matching_key13,Matching_key14)  VALUES(HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')))";
	private static final String SQL_USER_AUTOUPLOAD_Matching_keys="INSERT INTO `contacted_users`(ID,Matching_keys) VALUE(HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')))";
	private static final String SQL_USER_UPLOAD_Matching_key="INSERT INTO `infected_users1`(ID,Matching_key1,Matching_key2,Matching_key3,Matching_key4,Matching_key5,Matching_key6,Matching_key7,Matching_key8,Matching_key9,Matching_key10,Matching_key11,Matching_key12,Matching_key13,Matching_key14)  VALUES(HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')))";
	private static final String SQL_USER_UPLOAD_Matching_keys="INSERT INTO `infected_users`(ID,Matching_keys) VALUE(HEX(AES_ENCRYPT(?,'123456')),HEX(AES_ENCRYPT(?,'123456')))";
	private static final String SQL_USER_DELETE = null;

	//Download
	private static final String SQL_SELECT_AES_Matching_keys="select AES_DECRYPT(UNHEX(Matching_keys),'123456') from `infected_users`";
	
	//Update Infected_users
	private static final String SQL_Select_Infectedusers="select AES_DECRYPT(UNHEX(ID),'123456'),AES_DECRYPT(UNHEX(Matching_keys),'123456') from `infected_users`";
	private static final String SQL_Update_Infectedusers="update `infected_users` set `Matching_keys`=HEX(AES_ENCRYPT(?,'123456')) where `ID`=HEX(AES_ENCRYPT(?,'123456'))";
	private static final String SQL_Delete_Infectedusers="DELETE  from `infected_users` where `ID` =HEX(AES_ENCRYPT(?,'123456'))";
	
	//Update Contacted_users
	private static final String SQL_Select_Contactedusers="select AES_DECRYPT(UNHEX(ID),'123456'),AES_DECRYPT(UNHEX(Matching_keys),'123456') from `contacted_users`";
	private static final String SQL_Update_Contactedusers="update `contacted_users` set `Matching_keys`=HEX(AES_ENCRYPT(?,'123456')) where `ID`=HEX(AES_ENCRYPT(?,'123456'))";
	private static final String SQL_Delete_Contactedusers="DELETE  from `contacted_users` where `ID` =HEX(AES_ENCRYPT(?,'123456'))";
	
	private String Generate_token() {
		String token=new String(RandomGenerator.GeneratePsuRandomString(token_length));
		return token;
	}

	@Override
	public boolean select_users_ID(User user) {
		// 创建连接
		//创建连接对象
		Connection conn=JDBCUtils.getConnection();
		PreparedStatement prepareStatement=null;
		try {
			prepareStatement = conn.prepareStatement(SQL_USER_SELECT_users_key);
			//设置sql的参数
			prepareStatement.setString(1,user.getId());
			ResultSet result = prepareStatement.executeQuery();
			while(result.next()) {
				String ukey=result.getString(1);
				System.out.println(ukey);
				if(ukey!=null) {return false;}
				else 	{return true;}
			}
		} catch (Exception e) {
			return false;
		}finally {
			JDBCUtils.close(conn, prepareStatement, null);
		}
		return true ;		
	}
	
	@Override
	public boolean insert(User user) {
		// 创建连接 
		//创建连接对象
		Connection conn=JDBCUtils.getConnection();
		PreparedStatement prepareStatement=null;
		try {
			prepareStatement = conn.prepareStatement(SQL_USER_INSERT);
			//设置sql的参数
			prepareStatement.setString(1, user.getName());
			prepareStatement.setString(2, user.getKey());
			int line = prepareStatement.executeUpdate();
			return line>0?true:false;
		} catch (SQLException e) {	
			e.printStackTrace();
		}finally {
			JDBCUtils.close(conn, prepareStatement, null);
		}
		return false;
	}

	@Override
	public boolean delete(User user) {
		// 创建连接
		//创建连接对象
		Connection conn=JDBCUtils.getConnection();
		PreparedStatement prepareStatement=null;
		try {
			prepareStatement = conn.prepareStatement(SQL_USER_DELETE);
			//设置sql的参数
			prepareStatement.setString(1,user.getKey());
			int line = prepareStatement.executeUpdate();
			return line>0?true:false;
		} catch (SQLException e) {	
			e.printStackTrace();
		}finally {
			JDBCUtils.close(conn, prepareStatement, null);
		}
		return false;
	}
	
	@Override
	public boolean select_users_key(User user) {
		// 创建连接
		//创建连接对象
		Connection conn=JDBCUtils.getConnection();
		PreparedStatement prepareStatement=null;
		try {
			prepareStatement = conn.prepareStatement(SQL_USER_SELECT_users_ID);
			//设置sql的参数
			prepareStatement.setString(1,user.getKey());
			ResultSet result = prepareStatement.executeQuery();
			while(result.next()) {
				String uID=result.getString(1);
				if(uID!=null) {return false;}
				else 	{return true;}
			}
		} catch (Exception e) {	
			return false;
		}finally {
			JDBCUtils.close(conn, prepareStatement, null);
		}
		return true ;		
	}
	
	@Override
	public String loginUser(User user) {
		Connection conn=JDBCUtils.getConnection();
		PreparedStatement prepareStatement=null;
		try {
			boolean flag=select_users_ID(new User(user.getId(),user.getKey()));
			if(!flag) {return "1";}
			flag=select_users_key(new User(user.getId(),user.getKey()));
			if(!flag) {return "2";}
			prepareStatement = conn.prepareStatement(SQL_USER_LOGIN_User);
			//设置sql的参数
			prepareStatement.setString(1, user.getId());
			prepareStatement.setString(2, user.getKey());
			prepareStatement.executeUpdate();
			return "0" ;
		} catch (SQLException e) {	
			e.printStackTrace();
		}finally {
			JDBCUtils.close(conn, prepareStatement, null);
		}
		return "2";
	}

	public boolean loginManager(User user) {
		Connection conn=JDBCUtils.getConnection();
		PreparedStatement prepareStatement=null;
		try {
			prepareStatement = conn.prepareStatement(SQL_USER_LOGIN_Manager);
			//设置sql的参数
			prepareStatement.setString(1, user.getUsername());
			prepareStatement.setString(2, user.getPassword());
			prepareStatement.executeUpdate();
			return true ;
		} catch (SQLException e) {	
			e.printStackTrace();
		}finally {
			JDBCUtils.close(conn, prepareStatement, null);
		}
		return true;
	}
	
	public boolean insert_token(User user,String token) {
		// 创建连接
		//创建连接对象
		Connection conn=JDBCUtils.getConnection();
		PreparedStatement prepareStatement=null;
		try {
			prepareStatement = conn.prepareStatement(SQL_USER_INSERT_token);
			prepareStatement.setString(1, user.getUsername());
			prepareStatement.setString(2, token);
			int line = prepareStatement.executeUpdate();
			return line>0?true:false;
		} catch (SQLException e) {	
			e.printStackTrace();
		}finally {
			JDBCUtils.close(conn, prepareStatement, null);
		}
		return false;
	}

	public String ApplyToken(User user) {
		Connection conn=JDBCUtils.getConnection();
		PreparedStatement prepareStatement=null;
		try {
			/* 注册管理者账号
			 User user0=new User(); user0.setUsername(user.getUsername());
			 user0.setPassword(user.getPassword()); loginManager(user0);*/
			prepareStatement = conn.prepareStatement(SQL_USER_SELECT_managers_key);
			//设置sql的参数
			prepareStatement.setString(1, user.getUsername());
			ResultSet result = prepareStatement.executeQuery();
			while(result.next()) {
				String uKey=result.getString(1); 
				if(uKey!=null) {
					System.out.println("uKey"+uKey.length());
					System.out.println("passwd"+user.getPassword().length());
					boolean flag=uKey.equals(user.getPassword());
					if(flag) {
						System.out.println("用户名和密码有效！");
						String token=Generate_token().toString();
						System.out.println("生成Token为:"+token);
						insert_token(user,token);
						return token;
					}
				}
			}
		} catch (SQLException e) {	
			e.printStackTrace();
		}finally {
			JDBCUtils.close(conn, prepareStatement, null);
		}
		return "1";
	}

	public String CheckToken(User user) {
		// 创建连接
		//创建连接对象
		Connection conn=JDBCUtils.getConnection();
		PreparedStatement prepareStatement=null;
		try {
			prepareStatement = conn.prepareStatement(SQL_USER_CHECK_token);
			//设置sql的参数
			prepareStatement.setString(1,user.getToken());
			System.out.println("token_user："+user.getToken());
			System.out.println("token_len："+user.getToken().length());
			ResultSet result = prepareStatement.executeQuery();
			while(result.next()) {
				System.out.println(result.getString(1));
				String uname=result.getString(1);
				System.out.println("uname"+uname);
				if(uname!=null) {return "0";}
				else{return "1";}
			}
		} catch (Exception e) {	
			e.printStackTrace();
		}finally {
			JDBCUtils.close(conn, prepareStatement, null);
		}
		return "1" ;
	}

	public void UploadMatching_key(User user) {
		Connection conn=JDBCUtils.getConnection();
		PreparedStatement prepareStatement=null;
		try {
			prepareStatement = conn.prepareStatement(SQL_USER_UPLOAD_Matching_key);
			//设置sql的参数
			for(int i=1;i<15;i++) {
				prepareStatement.setString(i,user.getMatchingKeys(i-1));
				System.out.println(user.getMatchingKeys(i-1));
			}
			prepareStatement.executeUpdate();
		} catch (SQLException e) {	
			e.printStackTrace();
		}finally {
			JDBCUtils.close(conn, prepareStatement, null);
		}	
	}
	
	public boolean DeleteToken(String token) {
		Connection conn=JDBCUtils.getConnection();
		PreparedStatement prepareStatement=null;
		try {
			prepareStatement=conn.prepareStatement(SQL_USER_DELETE_token);
			System.out.println(token);
			prepareStatement.setString(1, token);
			int line=prepareStatement.executeUpdate();
			return line>0?true:false;
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}finally {
			JDBCUtils.close(conn, prepareStatement, null);
		}
		
	}

	public String Upload(User user,String token) {
		// upload到一个字段中
		Connection conn=JDBCUtils.getConnection();
		PreparedStatement prepareStatement=null;
		try {
			prepareStatement = conn.prepareStatement(SQL_USER_UPLOAD_Matching_keys);
			//设置sql的参数
			prepareStatement.setString(1,user.getId());
			prepareStatement.setString(2,user.getMatching_keys());
			System.out.println("Matching_keys:"+user.getMatching_keys());
			System.out.println("上传长度id："+user.getId().length());
			System.out.println("上传长度key："+user.getMatching_keys().length());
			prepareStatement.executeUpdate();
			if(!token.equals("123456")) {
				//一次性token删除
				if(DeleteToken(token)) { System.out.println("delete token"); }
			}
			return "0" ;
		} catch (SQLException e) {	
			e.printStackTrace();
		}finally {
			JDBCUtils.close(conn, prepareStatement, null);
		}
		return "1";		
	}
	
	public String AutoUpload(User user) {
		Connection conn=JDBCUtils.getConnection();
		PreparedStatement prepareStatement=null;
		try {
			prepareStatement = conn.prepareStatement(SQL_USER_AUTOUPLOAD_Matching_keys);
			//设置sql的参数
			prepareStatement.setString(1,user.getId());
			prepareStatement.setString(2,user.getMatching_keys());
			System.out.println("userID:"+user.getId());
			System.out.println("Matching_keys"+user.getMatching_keys());
			prepareStatement.executeUpdate();
			return "0" ;
		} catch (SQLException e) {	
			e.printStackTrace();
		}finally {
			JDBCUtils.close(conn, prepareStatement, null);
		}
		return "1";		
	}
	
	public String Update_Infectedusers() {
		Connection conn=JDBCUtils.getConnection();
		PreparedStatement prepareStatement=null;
		PreparedStatement prepareStatement2=null;
		PreparedStatement prepareStatement3=null;
		try {
			String ID="";
			String Matching_keys="";
			String Update_keys="";
			//设置sql的参数
			Statement statement=conn.createStatement();
			ResultSet result=statement.executeQuery(SQL_Select_Infectedusers);
			while(result.next()) {
				ID=result.getString(1);					//得到一行的ID
				Matching_keys=result.getString(2);		//得到一行的Matching_keys
	
				if(Matching_keys.length()<=Matching_key_length) {
					System.out.println("Delete_Infected_ID:"+ID);
					System.out.println("Delete_Infected_Matching_key:"+Update_keys);
					prepareStatement3=conn.prepareStatement(SQL_Delete_Infectedusers);
					prepareStatement3.setString(1,ID);
					prepareStatement3.executeUpdate();
					
				}else {
					Update_keys=Matching_keys.substring(Matching_key_length); //每天把前面的3位截取掉
					System.out.println("Infected_ID:"+ID);
					System.out.println("Infected_Matching_key:"+Update_keys);
					
					prepareStatement2=conn.prepareStatement(SQL_Update_Infectedusers);
					prepareStatement2.setString(1, Update_keys);
					prepareStatement2.setString(2, ID);
					prepareStatement2.executeUpdate();
					
				}
			}
			return "0";
		} catch (SQLException e) {	
			e.printStackTrace();
			
		}finally {
			JDBCUtils.close(conn, prepareStatement, null);
		}
		return "1";
	}
	
	public String Update_Contactedusers() {
		Connection conn=JDBCUtils.getConnection();
		PreparedStatement prepareStatement=null;
		PreparedStatement prepareStatement2=null;
		PreparedStatement prepareStatement3=null;
		try {
			String ID="";
			String Matching_keys="";
			String Update_keys="";
			Statement statement=conn.createStatement();
			ResultSet result=statement.executeQuery(SQL_Select_Contactedusers);
			while(result.next()) {
				ID=result.getString(1);					//得到一行的ID
				Matching_keys=result.getString(2);		//得到一行的Matching_keys
				
				if(Matching_keys.length()<=Matching_key_length) {
					System.out.println("Delete_Contacted_ID:"+ID);
					System.out.println("Delete_Contacted_Matching_key:"+Update_keys);
					prepareStatement3=conn.prepareStatement(SQL_Delete_Contactedusers);
					prepareStatement3.setString(1,ID);
					prepareStatement3.executeUpdate();
							
				}else {
					Update_keys=Matching_keys.substring(Matching_key_length); //每天把前面的3位截取掉
					System.out.println("Contacted_ID:"+ID);
					System.out.println("Contacted_Matching_key:"+Update_keys);
							
					prepareStatement2=conn.prepareStatement(SQL_Update_Contactedusers);
					prepareStatement2.setString(1, Update_keys);
					prepareStatement2.setString(2, ID);
					prepareStatement2.executeUpdate();
				}
			}
			return "0";
		} catch (SQLException e) {	
			e.printStackTrace();
					
		}finally {
			JDBCUtils.close(conn, prepareStatement, null);
		}
		return "1";
	}
	
	public String Download() {
		Connection conn=JDBCUtils.getConnection();
		PreparedStatement prepareStatement=null;
		String Matching_keys="",Line_Matching_keys;
		try {
			prepareStatement = conn.prepareStatement(SQL_SELECT_AES_Matching_keys);
			ResultSet result= prepareStatement.executeQuery();
			while(result.next()) {
				Line_Matching_keys=result.getString(1);	//查询到所有人AES_HEX加密后的数据，返回到一个字符串
				Matching_keys +="ABC"+Line_Matching_keys;
			}
			Matching_keys +="ABC";
			return Matching_keys;
		} catch (SQLException e) {	
			e.printStackTrace();
		}finally {
			JDBCUtils.close(conn,prepareStatement,null); 
		}
		return null;
	}

	
}
