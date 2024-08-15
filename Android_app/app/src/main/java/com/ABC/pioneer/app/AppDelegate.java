
package com.ABC.pioneer.app;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.ABC.pioneer.sensor.Sensor;
import com.ABC.pioneer.sensor.SensorArray;
import com.ABC.pioneer.sensor.SensorDelegate;
import com.ABC.pioneer.sensor.datatype.Data;
import com.ABC.pioneer.sensor.datatype.ImmediateSendData;
import com.ABC.pioneer.sensor.datatype.PayloadData;
import com.ABC.pioneer.sensor.datatype.Proximity;
import com.ABC.pioneer.sensor.datatype.SensorType;
import com.ABC.pioneer.sensor.datatype.TargetIdentifier;
import com.ABC.pioneer.sensor.payload.Crypto.SpecificUsePayloadSupplier;
import com.ABC.pioneer.sensor.payload.Crypto.GenerateKey;
import com.ABC.pioneer.sensor.payload.Crypto.SecretKey;
import com.ABC.pioneer.sensor.service.NotificationService;

import java.util.List;

public class AppDelegate extends Application implements SensorDelegate  {
    private final static String tag =AppDelegate.class.getName();
    private final static String NOTIFICATION_CHANNEL_ID = "PIONEER_NOTIFICATION_CHANNEL_ID";
    private final static int NOTIFICATION_ID = NOTIFICATION_CHANNEL_ID.hashCode();
    private static AppDelegate appDelegate = null;
    public static SecretKey secretKey;
    private static AppDelegate instance;

    @Override
    public PackageManager getPackageManager() {
        return super.getPackageManager();
    }

    // 接近检测传感器
     static SensorArray sensor = null;


    /// 生成唯一一致的设备标识符以测试检测和跟踪
    private int identifier() {
        final String text = Build.MODEL + ":" + Build.BRAND;
        return text.hashCode();
    }

    public static int getNotificationId() {
        return NOTIFICATION_ID;
    }
    public static String getNotificationChannelId(){
        return NOTIFICATION_CHANNEL_ID;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final SharedPreferences sp_SecretKey = getSharedPreferences("SecretKey",MODE_PRIVATE);

        appDelegate = this;
        // 初始化前台服务以使应用程序在后台运行
        this.createNotificationChannel();
        NotificationService.shared(this).startForegroundService(this.getForegroundNotification(), NOTIFICATION_ID);
        // 初始化传感器序列，用于给定的有效负载数据

        if(sp_SecretKey.getString("SecretKey","").equals("")){
            SharedPreferences.Editor edit_key = sp_SecretKey.edit();
            secretKey = GenerateKey.secretKey();
            edit_key.putString("SecretKey",secretKey.base64EncodedString());
            edit_key.apply();
        }
        else {
            final Data secretkeyData =  new Data(sp_SecretKey.getString("SecretKey",""));
            secretKey = new SecretKey(secretkeyData.value);
            System.out.println(secretKey);
        }
        final SpecificUsePayloadSupplier payloadDataSupplier = new SpecificUsePayloadSupplier(secretKey);
        sensor = new SensorArray(getApplicationContext(), payloadDataSupplier);
        // 将appDelegate添加为侦听器，以记录和启动传感器的检测事件
        sensor.add(this);

        // 效率功能记录
        // 测试
        /*PayloadData payloadData = sensor.payloadData();
        if (BuildConfig.DEBUG) {
            sensor.add(new ContactLog(this, "contacts.csv"));
            sensor.add(new StatisticsLog(this, "statistics.csv",payloadData));
            sensor.add(new DetectionLog(this,"detection.csv", payloadData));
            new BatteryLog(this, "battery.csv");
            if (Configurations.payloadDataUpdateTimeInterval != TimeInterval.never) {
                sensor.add(new EventTimeIntervalLog(this, "statistics_didRead.csv", payloadData, EventTimeIntervalLog.EventType.read));
            }
        }*/
        // 传感器将通过UI开关（默认为ON）和蓝牙状态启动和停止

    }

    public static AppDelegate getInstance(){
        return instance;
    }

    @Override
    public void onTerminate() {
        sensor.stop();
        super.onTerminate();
    }

    /// 获取应用程序委托
    public static AppDelegate getAppDelegate() {
        return appDelegate;
    }

    /// 获取传感器
    public Sensor sensor() {
        return sensor;
    }

    // SensorDelegate用于记录接近检测到的事件

    @Override
    public void sensor(SensorType sensor, TargetIdentifier didDetect) {
    }

    @Override
    public void sensor(SensorType sensor, PayloadData didRead, TargetIdentifier fromTarget) {
    }

    @Override
    public void sensor(SensorType sensor, ImmediateSendData didReceive, TargetIdentifier fromTarget) {
    }

    @Override
    public void sensor(SensorType sensor, List<PayloadData> didShare, TargetIdentifier fromTarget) {

    }

    @Override
    public void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget) {
    }





    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final int importance = NotificationManager.IMPORTANCE_DEFAULT;
            final NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    this.getString(R.string.notification_channel_name), importance);

            channel.setDescription(this.getString(R.string.notification_channel_description));

            final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification getForegroundNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(this.getString(R.string.notification_content_title))
                .setContentText(this.getString(R.string.notification_content_text))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        final Notification notification = builder.build();
        return notification;
    }


}
