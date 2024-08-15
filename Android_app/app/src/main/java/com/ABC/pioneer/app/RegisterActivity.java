package com.ABC.pioneer.app;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ABC.pioneer.sensor.SensorArray;
import com.ABC.pioneer.sensor.payload.Crypto.GenerateKey;
import com.ABC.pioneer.sensor.payload.Crypto.SecretKey;
import com.ABC.pioneer.sensor.payload.Crypto.SpecificUsePayloadSupplier;
import com.ABC.pioneer.sensor.service.Connection;

import java.io.IOException;

public class RegisterActivity extends AppCompatActivity {
    private Button regisbtn;
    private EditText et_phone;
    private String result=new String();
    private final Context context = AppDelegate.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        final SharedPreferences sp_PhoneNumber = getSharedPreferences("PhoneNumber",MODE_PRIVATE);
        regisbtn = (Button)findViewById(R.id.btn_register);
        et_phone = (EditText)findViewById(R.id.et_phone_1);


        regisbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final SharedPreferences sp_SecretKey = getSharedPreferences("SecretKey",MODE_PRIVATE);
                final SecretKey secretKey = AppDelegate.secretKey;
                String phone = et_phone.getText().toString();
                if (phone.trim().length() != 11) {
                    Toast.makeText(RegisterActivity.this, "请输入正确的手机号", Toast.LENGTH_LONG).show();
                    return;
                }


                Thread thread = new Thread(new Runnable(){
                    @Override
                    public void run(){
                        try {
                            Connection connection = null;
                            connection = new Connection(RegisterActivity.this);
                            connection.ConnectToServer();
                            result= connection.Register(phone, secretKey.base64EncodedString());

                        }
                        catch (IOException e) {
                        }
                    }
                });
                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(result.equals("0")){
                    Toast.makeText(RegisterActivity.this, "注册成功", Toast.LENGTH_LONG).show();
                    SharedPreferences.Editor edit=sp_PhoneNumber.edit();
                    edit.putString("PhoneNumber",phone);
                    edit.apply();
                }
                else if(result.equals("1")) {
                    Toast.makeText(RegisterActivity.this, "该手机号已被注册", Toast.LENGTH_LONG).show();
                }
                else if(result.equals("2")){
                    Toast.makeText(RegisterActivity.this, "SecretKey重复，已重新生成，请再次点击注册", Toast.LENGTH_LONG).show();
                    SharedPreferences.Editor edit=sp_SecretKey.edit();
                    SecretKey SecretKey = GenerateKey.secretKey();
                    edit.putString("SecretKey", SecretKey.base64EncodedString());
                    edit.apply();
                    final SpecificUsePayloadSupplier payloadDataSupplier = new SpecificUsePayloadSupplier(SecretKey);
                    AppDelegate.sensor = new SensorArray(getApplicationContext(), payloadDataSupplier);
                    // 将appDelegate添加为侦听器，以记录和启动传感器的检测事件
                    AppDelegate.sensor.add(AppDelegate.getAppDelegate());

                }
            }
        });


    }




}
