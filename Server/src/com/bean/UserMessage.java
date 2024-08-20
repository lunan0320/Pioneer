package com.bean;

import java.io.Serializable;
import java.util.Arrays;

//实例化用户信息类
public class UserMessage implements Serializable{
	private static final long serialVersionUID=-5059525171312166179L;
	private String type;	//类型
	private User user;		//用户对象
	private String str;     //返回值为字符串
	private int log;
	private String[] MatchingKeys=new String[14];
	private String token;
	//methods
	public UserMessage() {	}
	public UserMessage(int log) {
		this.log=log;
	}
	public UserMessage(String str) {
		this.str=str;
	}
	public UserMessage(String type,User user) {
		this.type=type;
		this.user=user;
	}
	public UserMessage(String type,User user,String token) {
		this.type=type;
		this.user=user;
		this.token=token;
	}
	public UserMessage(String type,String str) {
		this.type=type;
		this.str=str;
	}
	//getters and setters
	public String getType() {
		return type;
	}
	public User getUser() {
		return user;
	}
	public String getStr() {
		return str;
	}
	public int getLog() {
		return log;
	}
	public void setStr(String str) {
		this.str=str;
	}
	public void setUser(User user) {
		this.user = user;
	}
	public void setType(String type) {
		this.type = type;
	}
	public void setLog(int log) {
		this.log = log;
	}
	public String[] getMatchingKeys() {
		return MatchingKeys;
	}
	public void setMatchingKeys(int i,String MatchingKey) {
		MatchingKeys[i] = MatchingKey;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	@Override
	public String toString() {
		return "UserMessage [type=" + type + ", user=" + user + ", str=" + str + ", log=" + log + ", MatchingKeys="
				+ Arrays.toString(MatchingKeys) + ", token=" + token + "]";
	}
	
}
