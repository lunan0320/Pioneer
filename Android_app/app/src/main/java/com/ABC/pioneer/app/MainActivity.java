
package com.ABC.pioneer.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.ABC.pioneer.app.fragment.FragmentActivity1;
import com.ABC.pioneer.app.fragment.FragmentActivity2;
import com.ABC.pioneer.app.fragment.FragmentActivity3;
import com.ABC.pioneer.sensor.Sensor;
import com.ABC.pioneer.sensor.datatype.PayloadData;
import com.ABC.pioneer.sensor.datatype.TargetIdentifier;
import com.ABC.pioneer.sensor.service.AlarmReceiver;
import com.ABC.pioneer.sensor.service.CustomTimer;
import com.ABC.pioneer.sensor.service.MatchDelegate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity implements  View.OnClickListener,MatchDelegate {
    private final static int permissionRequestCode = 1249951875;
    private long didDetect = 0, didRead = 0, didMeasure = 0, didShare = 0, didReceive = 0;
    private final static int WARNING_NOTIFICATION_ID = "WARNING".hashCode();
    private final Map<TargetIdentifier,PayloadData> targetIdentifiers = new ConcurrentHashMap<>();
    private final Map<PayloadData,Target> payloads = new ConcurrentHashMap<>();
    private final List<Target> targets = new ArrayList<>();
    private TargetListAdapter targetListAdapter = null;
    private AlertDialog dialog;
    private RelativeLayout bottom_bar_bluetooth_btn;
    private RelativeLayout bottom_bar_token_btn;
    private RelativeLayout bottom_bar_user_btn;
    private TextView bottom_bar_text_bluetooth;
    private TextView bottom_bar_text_token;
    private TextView bottom_bar_text_user;
    private ImageView bottom_bar_image_bluetooth;
    private ImageView bottom_bar_image_token;
    private ImageView bottom_bar_image_user;
    private LinearLayout main_bottom_bar;
    private LinearLayout main_body;
    private int TAG = 0;
    private FragmentActivity1 fragmentActivity1;
    private FragmentActivity2 fragmentActivity2;
    private FragmentActivity3 fragmentActivity3;
    public final static Sensor sensor = AppDelegate.getAppDelegate().sensor();
    private static MainActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = MainActivity.this;

        // 开启接收器
        final AlarmReceiver alarmReceiver = new AlarmReceiver(getApplicationContext(),this);
        // 开启定时服务
        Intent i = new Intent(this, CustomTimer.class);
        startService(i);


        setContentView(R.layout.activity_bottom_bar);
        initView();
        fragmentActivity1 = new FragmentActivity1();
        fragmentActivity2 = new FragmentActivity2();
        fragmentActivity3 = new FragmentActivity3();
        setMain();
        // 确保应用具有所有必需的权限
        requestPermissions();
        ignoreBatteryOpt.ignoreBatteryOptimization(this);

    }

    public static MainActivity getInstance(){
        // 因为我们程序运行后，Application是首先初始化的，如果在这里不用判断instance是否为空
        return instance;
    }

    /// 请求传感器操作的应用程序权限。
    private void requestPermissions() {
        // Check and request permissions
        final List<String> requiredPermissions = new ArrayList<>();
        requiredPermissions.add(Manifest.permission.BLUETOOTH);
        requiredPermissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        requiredPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            requiredPermissions.add(Manifest.permission.FOREGROUND_SERVICE);
        }
        requiredPermissions.add(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        requiredPermissions.add(Manifest.permission.WAKE_LOCK);
        final String[] requiredPermissionsArray = requiredPermissions.toArray(new String[0]);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(requiredPermissionsArray, permissionRequestCode);
        } else {
            ActivityCompat.requestPermissions(this, requiredPermissionsArray, permissionRequestCode);
        }
    }

    /// 处理权限结果
    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == permissionRequestCode) {
            boolean permissionsGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                final String permission = permissions[i];
                if (grantResults[i] != PERMISSION_GRANTED) {
                    permissionsGranted = false;
                } else {
                }
            }
        }
    }


    @Override
    public void matchFound()
    {
        if(FragmentActivity1.foreground)
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // 如果前台则弹出警告框，警告用户可能被感染
                    AlertDialog.Builder alertdialogbuilder = new AlertDialog.Builder(MainActivity.this);
                    alertdialogbuilder.setMessage("传感器检测到您与感染者有接触过,请及时到医院进行核酸检测");
                    alertdialogbuilder.setPositiveButton("确定", null);
                    alertdialogbuilder.setCancelable(true);
                    alertdialogbuilder.setIcon(R.drawable.ic_warning);
                    alertdialogbuilder.setTitle("接触警告");
                    final AlertDialog alertdialog1 = alertdialogbuilder.create();
                    alertdialog1.show();
                    try {
                        Field mAlert = AlertDialog.class.getDeclaredField("mAlert");
                        mAlert.setAccessible(true);
                        Object mAlertController = mAlert.get(alertdialog1);
                        Field mMessage = mAlertController.getClass().getDeclaredField("mMessageView");
                        mMessage.setAccessible(true);
                        TextView mMessageView = (TextView) mMessage.get(mAlertController);
                        mMessageView.setTextColor(Color.RED);
                        Field mTitleView = mAlertController.getClass().getDeclaredField("mTitleView");
                        mTitleView.setAccessible(true);
                        TextView title = (TextView) mTitleView.get(mAlertController);
                        title.setTextColor(Color.RED);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                }
            });

        }
        else
        {
            // 如果在后台运行，则弹出一个notification告知用户可能被感染
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(WARNING_NOTIFICATION_ID,getWarningNotification());
        }
    }

    private Notification getWarningNotification() {
        /*final Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);*/
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, AppDelegate.getNotificationChannelId())
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(Color.RED)
                .setContentTitle(this.getString(R.string.warning_notification_content_title))
                .setContentText(this.getString(R.string.warning_notification_content_text))
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        final Notification notification = builder.build();
        return notification;
    }



    // 电量优化
    private void showActivity(@NonNull String packageName) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        startActivity(intent);
    }

    private void showActivity(@NonNull String packageName, @NonNull String activityDir) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(packageName, activityDir));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void initView(){
        //底部导航栏
        bottom_bar_bluetooth_btn = findViewById(R.id.bottom_bar_bluetooth);
        bottom_bar_token_btn = findViewById(R.id.bottom_bar_token);
        bottom_bar_user_btn = findViewById(R.id.bottom_bar_user);
        bottom_bar_text_bluetooth = findViewById(R.id.bottom_bar_text_bluetooth);
        bottom_bar_text_token = findViewById(R.id.bottom_bar_text_token);
        bottom_bar_text_user = findViewById(R.id.bottom_bar_text_user);
        bottom_bar_image_bluetooth = findViewById(R.id.bottom_bar_image_bluetooth);
        bottom_bar_image_token = findViewById(R.id.bottom_bar_image_token);
        bottom_bar_image_user = findViewById(R.id.bottom_bar_image_user);
        main_bottom_bar  = findViewById(R.id.main_body_bar);
        main_body = findViewById(R.id.main_body);
        //设置点击事件
        bottom_bar_bluetooth_btn.setOnClickListener(this);
        bottom_bar_token_btn.setOnClickListener(this);
        bottom_bar_user_btn.setOnClickListener(this);

    }

    private void setSelectStatus(int index) {
        switch (index){
            case 0:
                //图片点击选择变换图片，颜色的改变，其他变为原来的颜色，并保持原有的图片
                bottom_bar_image_bluetooth.setImageResource(R.drawable.ic_discover_choose);
                bottom_bar_text_bluetooth.setTextColor(Color.parseColor("#0097F7"));
                //其他的文本颜色不变
                bottom_bar_text_token.setTextColor(Color.parseColor("#666666"));
                bottom_bar_text_user.setTextColor(Color.parseColor("#666666"));
                //图片也不变
                bottom_bar_image_token.setImageResource(R.drawable.ic_token_icon);
                bottom_bar_image_user.setImageResource(R.drawable.ic_user);
                break;
            case 1:
                bottom_bar_image_token.setImageResource(R.drawable.ic_token_choose);
                bottom_bar_text_token.setTextColor(Color.parseColor("#0097F7"));
                bottom_bar_text_bluetooth.setTextColor(Color.parseColor("#666666"));
                bottom_bar_text_user.setTextColor(Color.parseColor("#666666"));
                bottom_bar_image_bluetooth.setImageResource(R.drawable.ic_discover_icon);
                bottom_bar_image_user.setImageResource(R.drawable.ic_user);
                break;
            case 2:
                bottom_bar_image_user.setImageResource(R.drawable.ic_user_choose);
                bottom_bar_text_user.setTextColor(Color.parseColor("#0097F7"));
                bottom_bar_text_token.setTextColor(Color.parseColor("#666666"));
                bottom_bar_text_bluetooth.setTextColor(Color.parseColor("#666666"));
                bottom_bar_image_bluetooth.setImageResource(R.drawable.ic_discover_icon);
                bottom_bar_image_token.setImageResource(R.drawable.ic_token_icon);
                break;
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v){
        if(TAG == 0){
            TAG = 1;
            switch(v.getId()){
                case R.id.bottom_bar_bluetooth:
                    getSupportFragmentManager().beginTransaction().add(R.id.fl_container,fragmentActivity1).commitAllowingStateLoss();
                    setSelectStatus(0);
                    break;
                case R.id.bottom_bar_token:
                    getSupportFragmentManager().beginTransaction().add(R.id.fl_container,fragmentActivity2).commitAllowingStateLoss();
                    setSelectStatus(1);
                    break;
                case R.id.bottom_bar_user:
                    getSupportFragmentManager().beginTransaction().add(R.id.fl_container,fragmentActivity3).commitAllowingStateLoss();
                    setSelectStatus(2);
                    break;
            }
        }
        else{
            switch(v.getId()){
                case R.id.bottom_bar_bluetooth:
                    getSupportFragmentManager().beginTransaction().replace(R.id.fl_container,fragmentActivity1).commitAllowingStateLoss();
                    setSelectStatus(0);
                    break;
                case R.id.bottom_bar_token:
                    getSupportFragmentManager().beginTransaction().replace(R.id.fl_container,fragmentActivity2).commitAllowingStateLoss();
                    setSelectStatus(1);
                    break;
                case R.id.bottom_bar_user:
                    getSupportFragmentManager().beginTransaction().replace(R.id.fl_container,fragmentActivity3).commitAllowingStateLoss();
                    setSelectStatus(2);
                    break;
            }

        }
    }
    private void setMain() {
        //打开初始界面
        TAG = 1;
        this.getSupportFragmentManager().beginTransaction().add(R.id.fl_container,fragmentActivity1).commit();
    }


}