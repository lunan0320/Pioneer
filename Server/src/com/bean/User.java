package com.bean;

import java.io.Serializable;

//实例化用户类
public class User implements Serializable{
	private static final long serialVersionUID=-989796422860230162L;
	private String ID;		//主键
	private String key;	    //公钥
	private String name;
	private String Username;//管理者用户名
	private String Password;//管理者密码
	private String token;
	private String[] MatchingKeys=new String[14];
	private String Matching_keys;

	public User() {
		super();
		// TODO Auto-generated constructor stub
	}
	public User(String ID) {
		super();
		this.ID=ID;
	}
	public User(String ID,String key) {
		this.ID=ID;
		this.key=key;
	}

	public User(String id, String name, String key) {
		this.ID = id;
		this.key = key;
		this.name = name;
	}


	public String getId() {
		return ID;
	}

	public void setId(String ID) {
		this.ID= ID;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getName() {
		return Username;
	}

	public void setName(String Username) {
		this.Username = Username;
	}
	
	public String getPassword() {
		return Password;
	}

	public void setPassword(String Password) {
		this.Password= Password;
	}
	public String getUsername() {
		return Username;
	}
	public void setUsername(String Username) {
		this.Username = Username;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token=token;
	}	
	public String getMatchingKeys(int i) {
		return MatchingKeys[i];
	}
	public void setMatchingKeys(int i,String str) {
		this.MatchingKeys[i]=str;
	}
	public String getMatching_keys() {
		return Matching_keys;
	}
	public void setMatching_keys(String Matching_keys) {
		this.Matching_keys = Matching_keys;
	}

	@Override
	public String toString() {
		return "User [ID=" + ID + ", key=" + key + ", name=" + name + ", Username=" + Username + ", Password="
				+ Password + "]";
	}


}
