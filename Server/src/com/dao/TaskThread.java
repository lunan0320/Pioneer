package com.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.jdbc.JDBCUtils;

public class TaskThread extends Timer{
	
	/**
	 * 定时任务对象
	 */
	private  TaskThread timer = null;
	//定义每个Matching_key的长度
	private static final int Matching_key_length=3;
	//时间间隔(一天)  
    private static final long PERIOD_DAY =  24*60*60 * 1000;  
  
    //Update Infected_users
  	private static final String SQL_Select_Infectedusers="select AES_DECRYPT(UNHEX(ID),'123456'),AES_DECRYPT(UNHEX(Matching_keys),'123456') from `infected_users`";
  	private static final String SQL_Update_Infectedusers="update `infected_users` set `Matching_keys`=HEX(AES_ENCRYPT(?,'123456')) where `ID`=HEX(AES_ENCRYPT(?,'123456'))";
  	private static final String SQL_Delete_Infectedusers="DELETE  from `infected_users` where `ID` =HEX(AES_ENCRYPT(?,'123456'))";
  	
  	//Update Contacted_users
  	private static final String SQL_Select_Contactedusers="select AES_DECRYPT(UNHEX(ID),'123456'),AES_DECRYPT(UNHEX(Matching_keys),'123456') from `contacted_users`";
  	private static final String SQL_Update_Contactedusers="update `contacted_users` set `Matching_keys`=HEX(AES_ENCRYPT(?,'123456')) where `ID`=HEX(AES_ENCRYPT(?,'123456'))";
  	private static final String SQL_Delete_Contactedusers="DELETE  from `contacted_users` where `ID` =HEX(AES_ENCRYPT(?,'123456'))";
  	
    public void start() {  
        Calendar calendar = Calendar.getInstance();  
        calendar.set(Calendar.HOUR_OF_DAY, 0); //凌晨0点  
        calendar.set(Calendar.MINUTE, 0);  
        calendar.set(Calendar.SECOND, 0);  
        Date date=calendar.getTime(); //第一次执行定时任务的时间  
        //如果第一次执行定时任务的时间 小于当前的时间  
        //此时要在 第一次执行定时任务的时间加一天，以便此任务在下个时间点执行。如果不加一天，任务会立即执行。  
        if (date.before(new Date())) {        
            date = this.addDay(date, 1);  
        }
        timer = new TaskThread();
        //安排指定的任务在指定的时间开始进行重复的固定延迟执行。  
        timer.schedule(new TimerTask() {
			@Override
			public void run() {
				//执行代码
				System.out.println("定时任务执行！");
				Update_Infectedusers();
				Update_Contactedusers();
			}
		},date,PERIOD_DAY);  
        this.cancel();
    }  
    
    // 增加或减少天数  
    public Date addDay(Date date, int num) {  
        Calendar startDT = Calendar.getInstance();  
        startDT.setTime(date);  
        startDT.add(Calendar.DAY_OF_MONTH, num);  
        return startDT.getTime();  
    }  
    public void Update_Infectedusers() {
		// TODO Auto-generated method stub
		Connection conn=JDBCUtils.getConnection();
		PreparedStatement prepareStatement=null;
		PreparedStatement prepareStatement2=null;
		PreparedStatement prepareStatement3=null;
		try {
			String ID="";
			String Matching_keys="";
			String Update_keys="";
			//prepareStatement = conn.prepareStatement(SQL_Select_Infectedusers);
			//设置sql的参数
			//ResultSet result= prepareStatement.executeQuery();
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
		} catch (SQLException e) {	
			e.printStackTrace();
			
		}finally {
			JDBCUtils.close(conn, prepareStatement, null);
		}
	}
	
	public void Update_Contactedusers() {
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
					Update_keys=Matching_keys.substring(Matching_key_length); 
					System.out.println("Contacted_ID:"+ID);
					System.out.println("Contacted_Matching_key:"+Update_keys);
							
					prepareStatement2=conn.prepareStatement(SQL_Update_Contactedusers);
					prepareStatement2.setString(1, Update_keys);
					prepareStatement2.setString(2, ID);
					prepareStatement2.executeUpdate();
				}
			}
		} catch (SQLException e) {	
			e.printStackTrace();
					
		}finally {
			JDBCUtils.close(conn, prepareStatement, null);
		}
	}
}