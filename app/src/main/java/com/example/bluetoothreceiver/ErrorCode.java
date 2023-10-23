package com.example.bluetoothreceiver;

import android.util.Log;

public enum ErrorCode {
    InvalidAccessKeyId,
    SignatureDoesNotMatch,
    NoSuchBucket,
    PermanentRedirect;

    @Override
    public String toString(){
        switch (this) {
            case InvalidAccessKeyId: return "InvalidAccessKeyId";
            case SignatureDoesNotMatch: return "SignatureDoesNotMatch";
            case NoSuchBucket: return "NoSuchBucket";
            case PermanentRedirect: return "PermanentRedirect";
            default: return "NoError";
        }
    }

    public String errString(){
        switch (this) {
            case InvalidAccessKeyId: return "잘못된 액세스 키";
            case SignatureDoesNotMatch: return "잘못된 시크릿 키";
            case NoSuchBucket: return "잘못된 버킷";
            case PermanentRedirect: return "잘못된 지역";
            default: return "NoError";
        }
    }
}
