package com.example.bluetoothreceiver;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class UserSettingActivity extends Activity {
    EditText participantFullIDEditText;
    EditText serialNumberEditText;
    EditText dataBucketURLEditText;
    EditText accessKeyEditText;
    EditText secretKeyEditText;
    TextView settingTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_user_setting);

        findView();
        initView();
    }
    private void findView() {
        participantFullIDEditText = findViewById(R.id.participantFullIDEditText);
        serialNumberEditText = findViewById(R.id.serialNumberEditText);
        dataBucketURLEditText = findViewById(R.id.dataBucketURLEditText);
        accessKeyEditText = findViewById(R.id.accessKeyEditText);
        secretKeyEditText = findViewById(R.id.secretKeyEditText);
        settingTextView = findViewById(R.id.settingTextView);
    }

    private void initView() {
        settingTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String participantFullID = participantFullIDEditText.getText().toString();
                String serialNumber = serialNumberEditText.getText().toString();
                String dataBucketURL = dataBucketURLEditText.getText().toString();
                String accessKey = accessKeyEditText.getText().toString();
                String secretKey = secretKeyEditText.getText().toString();

                //SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
                //SharedPreferences.Editor editor = sharedPreferences.edit();

                //String[] splitParticipantID = participantFullID.split("-");
                //dataBucketURL.substring(5);
                //String bucket = dataBucketURL.substring(5).split("/")[0];
                //String prefix = dataBucketURL[1] + "/" + split + "participant_data/" + date + "/" + serialNumber + "/" + rawdata + "/v6/";
                //editor.putString("accessKey", accessKey);
                //editor.putString("secretKey", secretKey);


                //String[] splitBucketURL = dataBucketURL.split("-");
                //String clientRegion = splitBucketURL[1] + "-" + splitBucketURL[2] + "-" + splitBucketURL[3];
                //String clientRegion = "us-east-1";
                //String preString = "v2/" + splitParticipantID[0] + "/" + splitParticipantID[1] + "/" + splitParticipantID[2] + "/participant_data/";
                //String postString = "/" + splitParticipantID[3] + "-" + serialNumber + "/raw_data/v6/";
                String errorCode;
                if((errorCode = AwsS3.isInvalidSetting(participantFullID, serialNumber, dataBucketURL, accessKey, secretKey)) != null) {
                    Animation animShake = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shake_animation);
                    if(errorCode.compareTo("InvalidAccessKeyId") == 0) {
                        accessKeyEditText.startAnimation(animShake);
                            Log.e("TEST", "accessKey shake");
                    } else if(errorCode.compareTo("SignatureDoesNotMatch") == 0) {
                        secretKeyEditText.startAnimation(animShake);
                        Log.e("TEST", "secretKey shake");
                    } else if(errorCode.compareTo("NoSuchBucket") == 0 || errorCode.compareTo("PermanentRedirect") == 0) {
                        dataBucketURLEditText.startAnimation(animShake);
                        Log.e("TEST", "dataBucketURL shake");
                    }
                } else {
                    accessKeyEditText.getText().toString();
                    secretKeyEditText.getText().toString();

                }
            }
        });
    }

    private void processing(String participantFullID, String serialNumber, String dataBucketURL, String accessKey, String SecretKey){
        String clientRegion = "us-east-1";
        String bucket = dataBucketURL.substring(5).split("/")[0];
        //String
    }

    private boolean isValid(String participantFullID, String serialNumber, String dataBucketURL, String accessKey, String secretKey) {
        return false;
    }
}
