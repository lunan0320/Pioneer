package com.ABC.pioneer.sensor.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import java.util.Calendar;
import java.util.TimeZone;

public class CustomTimer extends Service {
    public static final String UpdateDatabaseAction = "Pioneer.UpdateDatabase";
    public static final String DownloadDataAction = "Pioneer.DownloadData";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        setUpdateDatabase(this);
        setDownloadData(this);
        return super.onStartCommand(intent,flags,startId);
    }

    private void setUpdateDatabase(Context context)
    {
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+08"));
        // 设置为下一天的零点
        calendar.setTimeInMillis(System.currentTimeMillis()+24*60*60*1000);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Intent intent = new Intent();
        intent.setAction(UpdateDatabaseAction);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,0,intent,PendingIntent.FLAG_CANCEL_CURRENT);
        manager.setExact(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis(),pendingIntent);
    }

    private void setDownloadData(Context context)
    {
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+08"));
        // 设置为下一天的8点
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 8);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Intent intent = new Intent();
        intent.setAction(DownloadDataAction);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,1,intent,PendingIntent.FLAG_CANCEL_CURRENT);
        manager.setExact(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis(),pendingIntent);
    }
}
