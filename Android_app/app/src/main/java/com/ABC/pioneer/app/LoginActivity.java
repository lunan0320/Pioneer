package com.ABC.pioneer.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


//登陆界面
public class LoginActivity extends AppCompatActivity {
    private SharedPreferences sp;
    private SharedPreferences.Editor edit;
    private Button loginbtn;
    private Button regisbtn_jump;
    private EditText et_phone;
    // 保存所有用户的数据


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //在加载布局文件前判断是否登陆过
        sp = getSharedPreferences("PhoneNumber",MODE_PRIVATE);
        //.getBoolean("PhoneNumber",false)；当找不到"PhoneNumber"所对应的键值时默认返回false
        if(!sp.getString("PhoneNumber","").equals("")){
            Intent intent=new Intent(getApplication(),MainActivity.class);
            startActivity(intent);
            LoginActivity.this.finish();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        loginbtn = (Button)findViewById(R.id.btn_login);
        regisbtn_jump = (Button)findViewById(R.id.btn_register_jump);
        et_phone = (EditText)findViewById(R.id.et_phone);


        //给loginbtn设置点击登录事件
        loginbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String phone= et_phone.getText().toString();//输入的电话号码

                //判断输入的手机号是否合法
                //phone.trim()表示去除左右空格
                if(phone.trim().length()!= 11||phone.trim() == null)
                {
                    Toast.makeText(LoginActivity.this,"请输入正确的手机号",Toast.LENGTH_SHORT).show();
                    return;
                }

                if(phone.equals("12345678901")){
                    Intent it=new Intent(LoginActivity.this,MainActivity.class);//启动MainActivity
                    startActivity(it);
                    finish();//关闭当前活动
                }
                //第一次登陆要先注册手机号
                else if(sp.getString("PhoneNumber","").equals("")){
                    Toast.makeText(getApplicationContext(),"此电话号码未在本机注册过，请先注册",Toast.LENGTH_SHORT).show();
                    return;
                }

                //本地没有存过电话号码
                else if(!sp.getString("PhoneNumber","默认值").equals(phone)){
                    Toast.makeText(getApplicationContext(),"此电话号码不是本机注册号码",Toast.LENGTH_SHORT).show();
                    return;
                }

                //本地有此电话号码记录，登录到主界面
                else{
                    Intent it=new Intent(getApplicationContext(),MainActivity.class);//启动MainActivity
                    startActivity(it);
                    finish();//关闭当前活动
                }


            }
        });

        //给regisbtn设置进入注册界面事件
        regisbtn_jump.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent it=new Intent(getApplicationContext(),RegisterActivity.class);//启动RegisterActivity
                startActivity(it);
            }
        });
    }



}