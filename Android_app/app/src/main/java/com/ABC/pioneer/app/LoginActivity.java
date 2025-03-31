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
    // 本地变量
    private SharedPreferences sp;
    private SharedPreferences.Editor edit;
    private Button loginbtn;
    private Button regisbtn_jump;
    private EditText et_phone;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 检查已有登录状态
        checkAutoLogin();
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // 初始化视图
        initViews();
        
        // 设置登录按钮事件
        setupLoginButton();
        
        // 设置注册跳转按钮事件
        setupRegisterButton();
    }
    
    /**
     * 检查是否已登录，如果是则自动跳转
     */
    private void checkAutoLogin() {
        sp = getSharedPreferences("PhoneNumber", MODE_PRIVATE);
        String savedPhone = sp.getString("PhoneNumber", "");
        
        if (!savedPhone.isEmpty()) {
            jumpToMainActivity(getApplication());
            finish();
        }
    }
    
    /**
     * 初始化所有视图组件
     */
    private void initViews() {
        loginbtn = (Button) findViewById(R.id.btn_login);
        regisbtn_jump = (Button) findViewById(R.id.btn_register_jump);
        et_phone = (EditText) findViewById(R.id.et_phone);
    }
    
    /**
     * 设置登录按钮点击事件
     */
    private void setupLoginButton() {
        loginbtn.setOnClickListener(v -> {
            String phone = et_phone.getText().toString().trim();
            
            if (!isValidPhone(phone)) {
                showToast("请输入正确的手机号");
                return;
            }
            
            handleLoginLogic(phone);
        });
    }
    
    /**
     * 设置注册跳转按钮点击事件
     */
    private void setupRegisterButton() {
        regisbtn_jump.setOnClickListener(v -> {
            Intent it = new Intent(getApplicationContext(), RegisterActivity.class);
            startActivity(it);
        });
    }
    
    /**
     * 验证手机号格式
     */
    private boolean isValidPhone(String phone) {
        return phone != null && phone.length() == 11;
    }
    
    /**
     * 处理登录逻辑
     */
    private void handleLoginLogic(String phone) {
        // 测试账号直接登录
        if ("12345678901".equals(phone)) {
            jumpToMainActivity(LoginActivity.this);
            finish();
            return;
        }
        
        String savedPhone = sp.getString("PhoneNumber", "");
        
        if (savedPhone.isEmpty()) {
            showToast("此电话号码未在本机注册过，请先注册");
        } else if (!savedPhone.equals(phone)) {
            showToast("此电话号码不是本机注册号码");
        } else {
            jumpToMainActivity(getApplicationContext());
            finish();
        }
    }
    
    /**
     * 跳转到主界面
     */
    private void jumpToMainActivity(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        startActivity(intent);
    }
    
    /**
     * 显示Toast提示
     */
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}