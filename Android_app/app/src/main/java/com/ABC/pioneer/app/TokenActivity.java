package com.ABC.pioneer.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.ABC.pioneer.sensor.payload.Crypto.SpecificUsePayloadSupplier;
import com.ABC.pioneer.sensor.payload.Crypto.GenerateKey;
import com.ABC.pioneer.sensor.payload.Crypto.MatchingKey;
import com.ABC.pioneer.sensor.service.Connection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;


public class TokenActivity extends AppCompatActivity {
    private Button tokenbtn;
    private EditText tokenet;
    DataInputStream in=null;
    DataOutputStream out=null;
    private String Maching_keys="";
    private SharedPreferences sp;
    String result = "";
    String result1 = "";
    Connection connection;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token);
        sp = getSharedPreferences("PhoneNumber",MODE_PRIVATE);
        tokenbtn = (Button)findViewById(R.id.btn_token);
        tokenet = (EditText)findViewById(R.id.et_token);
        MatchingKey[] MachingKeys = SpecificUsePayloadSupplier.matchingKeys;

        tokenbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 /*根据当前时间和app开启时间的时间差计算距开启已经过多少天，用天数i定位到MachingKeys[i],
                这个即为当天的Machingkey，然后再向前找14天的Machingkey*/
                final int date = GenerateKey.day(new Date());
                String token = tokenet.getText().toString();
                if (token.trim().length() != 6) {
                    Toast.makeText(TokenActivity.this, "请输入正确的Token号码", Toast.LENGTH_LONG).show();
                    return;
                }

                Thread thread = new Thread(new Runnable(){
                    @Override
                    public void run(){
                        try{
                            connection = new Connection(TokenActivity.this);
                            connection.ConnectToServer();
                            result = connection.Token(token);
                            if(result.equals("0")) {
                                String phone = sp.getString("PhoneNumber","默认值");
                                for(int i = 13; i >= 0; i--){
                                    Maching_keys += MachingKeys[date - i].base64EncodedString();
                                }
                                result1 = connection.Token_TransmitMachingKeys(phone,Maching_keys);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(result.equals("0")) {

                    Toast.makeText(TokenActivity.this, "验证通过", Toast.LENGTH_LONG).show();
                    Toast.makeText(TokenActivity.this, "成功上传ID和14天Maching_keys", Toast.LENGTH_LONG).show();
                }
                else if(result.equals("1")){
                    Toast.makeText(TokenActivity.this, "Token输入有误，请重新输入！", Toast.LENGTH_LONG).show();
                }

            }
        });
    }




}
