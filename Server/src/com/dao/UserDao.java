package com.dao;

import com.bean.User;	

//数据库访问接口
public interface UserDao {
//添加用户信息
boolean insert(User user);
//删除用户信息
boolean delete(User user);

//查询用户信息
boolean select_users_ID(User user);
boolean select_users_key(User user);

//注册用户信息
String loginUser(User user);

//医务人员申请token
String ApplyToken(User user);

//检测token是否正确
String CheckToken(User user);

//token验证
String Upload(User user,String token);

//上传感染者信息
String Update_Infectedusers() ;

//上传密切接触者信息
String Update_Contactedusers() ;

//下载感染者信息
String Download();
}
