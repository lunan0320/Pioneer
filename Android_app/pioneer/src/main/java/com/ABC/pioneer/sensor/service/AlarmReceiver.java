package com.ABC.pioneer.sensor.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;


import androidx.annotation.RequiresApi;

import com.ABC.pioneer.sensor.datatype.Data;
import com.ABC.pioneer.sensor.payload.Crypto.SpecificUsePayloadSupplier;
import com.ABC.pioneer.sensor.payload.Crypto.GenerateKey;
import com.ABC.pioneer.sensor.payload.Crypto.MatchingKey;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.content.Context.MODE_PRIVATE;

public class AlarmReceiver  {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Context context;
    private String Maching_keys="";
    private final MatchDelegate delegate;
    private final Runnable UpdateDatabase =  new Runnable() {
        @Override
        public void run() {

            Log.d("BackService", new Date().toString());
            PioneerDb db = new PioneerDb(context,"payloads",null,1);
            db.updateTable();
        }
    };
    private final Runnable DownloadData = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {

            // 此处添加下载的代码
            // 下载之后进行匹配
            // 有一个函数，下载数据并进行匹配，返回 true或 flase
            // 根据true或者false，决定是否调用上传的函数
            int date = GenerateKey.day(new Date());
            // 用执行下载匹配算法时的时间减去两个小时的时间再减去开启app当天零点的时间，然后算出距离开启app过去了多少天，
            // 用天数模14再加一找出哪个表是上一天的表，通过这个表向前找就能找将每张表都对应到相应的日期

            try {
                Connection connection = new Connection(context);
                connection.ConnectToServer();
                String str = connection.Download_Message();
                int i = 0, k = 0;
                int index1 = 0;
                int index2 = 0;
                String temp = "";
                MatchingKey K;
                int tag = 0;

                while (true) {
                    if (tag == 1) break;
                    index1 = str.indexOf("ABC", i);
                    if (index1 == -1) break;
                    index1 = i + 3;
                    index2 = str.indexOf("ABC", index1 + 44);
                    if (index2 == -1) break;
                    temp = str.substring(index1, index2);
                    int num_of_Maching_key = temp.length() / 44;
                    int j = 0;
                    String temp_key;
                    for (int m = 0; m < num_of_Maching_key; m++) {
                        //由tmep_key生成那一天的240个contaceed_key
                        //再生成240个identifier
                        //由这240个identifier去匹配那一天的数据表
                        //找到匹配到的，再去判断mac
                        //验证通过就break，上传14天
                        //否则继续匹配
                        temp_key = temp.substring(j, j + 44);
                        Data data = new Data(temp_key);
                        K = new MatchingKey(data);

                        //打开数据库
                        PioneerDb db = new PioneerDb(context, "payloads", null, 1);
                        if (db.matchMatchingKey(K, 13 - m)) {

                            // 通知代理
                            delegate.matchFound();

                            SharedPreferences sp = context.getSharedPreferences("PhoneNumber", MODE_PRIVATE);
                            String phone = sp.getString("PhoneNumber", "默认值");
                            MatchingKey[] MachingKeys = SpecificUsePayloadSupplier.matchingKeys;
                            for (int a = 13; a >= 0; a--) {
                                Maching_keys += MachingKeys[date - a].base64EncodedString();
                            }
                            String result = connection.TransmitMachingKeys(phone, Maching_keys);
                            //Toast.makeText(TokenActivity.this, "成功上传ID和14天Maching_keys", Toast.LENGTH_LONG).show();
                            tag = 1;
                            break;
                        }
                        j += 44;
                    }
                    i = index2;
                }
                } catch(IOException e){
                    e.printStackTrace();
                }


        }
    };
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(CustomTimer.UpdateDatabaseAction.equals(intent.getAction()))
                executorService.execute(UpdateDatabase);
            else if(CustomTimer.DownloadDataAction.equals(intent.getAction()))
                executorService.execute(DownloadData);
            else
                ;
        }
    };

    public AlarmReceiver(Context context,MatchDelegate delegate)
    {
        this.delegate = delegate;
        this.context = context;
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CustomTimer.DownloadDataAction);
        intentFilter.addAction(CustomTimer.UpdateDatabaseAction);
        context.registerReceiver(broadcastReceiver, intentFilter);
    }

}
