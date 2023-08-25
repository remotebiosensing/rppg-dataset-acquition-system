package com.example.bluetoothreceiver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UserSettingActivity extends Activity {
    EditText userNameEditText;
    EditText userAgeEditText;
    TextView userInfoSettingTextView;

    private Map<String, EditText> editTextMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_user_info);

        findView();
        initView();
    }
    private void findView() {
        editTextMap.put("userName", findViewById(R.id.userNameEditText));
        editTextMap.put("userAge", findViewById(R.id.userAgeEditText));

        userInfoSettingTextView = findViewById(R.id.userInfoSettingTextView);
    }

    private void initView() {
        userInfoSettingTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveInfo();
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    private void saveInfo(){
        SharedPreferences sharedPreferences = getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Set<String> keySet = editTextMap.keySet();
        for(String key : keySet) {
            String value = editTextMap.get(key).getText().toString();
            editor.putString(key, value);
        }

        editor.commit();
    }
}
