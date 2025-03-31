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
    private Button registerButton;
    private EditText phoneEditText;
    private String serverResponse = "";
    private final Context appContext = AppDelegate.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        
        initializeViews();
        setupRegisterButton();
    }

    private void initializeViews() {
        registerButton = findViewById(R.id.btn_register);
        phoneEditText = findViewById(R.id.et_phone_1);
    }

    private void setupRegisterButton() {
        registerButton.setOnClickListener(v -> handleRegistration());
    }

    private void handleRegistration() {
        String phoneNumber = phoneEditText.getText().toString().trim();
        
        if (!isValidPhoneNumber(phoneNumber)) {
            showToast("请输入正确的手机号");
            return;
        }

        performRegistration(phoneNumber);
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber.length() == 11;
    }

    private void performRegistration(String phoneNumber) {
        SharedPreferences phonePrefs = getSharedPreferences("PhoneNumber", MODE_PRIVATE);
        SharedPreferences secretKeyPrefs = getSharedPreferences("SecretKey", MODE_PRIVATE);
        SecretKey currentSecretKey = AppDelegate.secretKey;

        new Thread(() -> {
            try {
                Connection connection = new Connection(RegisterActivity.this);
                connection.connectToServer();
                serverResponse = connection.register(phoneNumber, currentSecretKey.base64EncodedString());
            } catch (IOException e) {
                // 处理连接异常
            }
        }).start().joinWithUiThread(this::processRegistrationResult);
    }

    private void processRegistrationResult() {
        switch (serverResponse) {
            case "0":
                handleSuccessfulRegistration();
                break;
            case "1":
                showToast("该手机号已被注册");
                break;
            case "2":
                handleSecretKeyConflict();
                break;
        }
    }

    private void handleSuccessfulRegistration() {
        SharedPreferences.Editor editor = getSharedPreferences("PhoneNumber", MODE_PRIVATE).edit();
        editor.putString("PhoneNumber", phoneEditText.getText().toString().trim());
        editor.apply();
        showToast("注册成功");
    }

    private void handleSecretKeyConflict() {
        showToast("SecretKey重复，已重新生成，请再次点击注册");
        
        SharedPreferences.Editor editor = getSharedPreferences("SecretKey", MODE_PRIVATE).edit();
        SecretKey newSecretKey = GenerateKey.secretKey();
        editor.putString("SecretKey", newSecretKey.base64EncodedString());
        editor.apply();

        SpecificUsePayloadSupplier payloadSupplier = new SpecificUsePayloadSupplier(newSecretKey);
        AppDelegate.sensor = new SensorArray(getApplicationContext(), payloadSupplier);
        AppDelegate.sensor.add(AppDelegate.getAppDelegate());
    }

    private void showToast(String message) {
        runOnUiThread(() -> 
            Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show()
        );
    }

    // 辅助方法：在UI线程执行回调
    private interface UiThreadCallback {
        void execute();
    }

    private static void joinWithUiThread(Thread thread, Activity activity, UiThreadCallback callback) {
        try {
            thread.join();
            activity.runOnUiThread(callback::execute);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
