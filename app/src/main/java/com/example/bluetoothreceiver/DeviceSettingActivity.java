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

public class DeviceSettingActivity extends Activity {
    EditText participantFullIDEditText;
    EditText serialNumberEditText;
    EditText dataBucketURLEditText;
    EditText accessKeyEditText;
    EditText secretKeyEditText;
    TextView settingTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_device_setting);

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
                    processingAndSave(participantFullID, serialNumber, dataBucketURL, accessKey, secretKey);
                    finish();
                }
            }
        });
    }

    private void processingAndSave(String participantFullID, String serialNumber, String dataBucketURL, String accessKey, String secretKey){
        SharedPreferences sharedPreferences = getSharedPreferences("DeviceSettings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String bucket = dataBucketURL.substring(5).split("/")[0];
        Log.e("TEST", "bucket : " + bucket);
        //String prefix = dataBucketURL[1] + "/" + split + "participant_data/" + date + "/" + serialNumber + "/" + rawdata + "/v6/";
        String[] splitID = participantFullID.split("-");
        String beforeDate = "v2/" + splitID[0] + "/" + splitID[1] + "/" + splitID[2] + "/participant_data/";
        String afterDate = "/" + splitID[3] + "-" + serialNumber + "/raw_data/v6/";

        editor.putString("bucket", bucket);
        editor.putString("beforeDate", beforeDate);
        editor.putString("afterDate", afterDate);
        editor.putString("accessKey", accessKey);
        editor.putString("secretKey", secretKey);

        editor.commit();
        //String[] splitBucketURL = dataBucketURL.split("-");
        //String clientRegion = splitBucketURL[1] + "-" + splitBucketURL[2] + "-" + splitBucketURL[3];
        //String clientRegion = "us-east-1";
        //String preString = "v2/" + splitParticipantID[0] + "/" + splitParticipantID[1] + "/" + splitParticipantID[2] + "/participant_data/";
        //String postString = "/" + splitParticipantID[3] + "-" + serialNumber + "/raw_data/v6/";

        String clientRegion = "us-east-1";
        //String
    }

    private boolean isValid(String participantFullID, String serialNumber, String dataBucketURL, String accessKey, String secretKey) {
        return false;
    }
}
