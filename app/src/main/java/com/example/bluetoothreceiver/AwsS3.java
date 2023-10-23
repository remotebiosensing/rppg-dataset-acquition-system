package com.example.bluetoothreceiver;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.http.AmazonHttpClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import org.apache.avro.specific.AvroGenerated;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AwsS3 {
    private static final String TAG = AwsS3.class.getSimpleName();
    AWSCredentials awsCredentials;
    private AmazonS3Client s3Client;
    /*private String accessKey = "AKIAWWZYTIF57XRMCFPO";
    private String secretKey = "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";
    private Region clientRegion = Region.getRegion(Regions.US_EAST_1);//Regions.US_EAST_1;
    private String bucket = "empatica-us-east-1-prod-data";
    private String beforeDate;
    private String afterDate;
    private String prefix = "v2/394/1/1/participant_data/2023-07-18/001-3YK3L151NT/raw_data/v6/";*/

    private String accessKey;
    private String secretKey;
    private Region clientRegion;
    private String bucket;
    private String beforeDate;
    private String afterDate;
    private String prefix;



    String[] fileKeyList;

    private static final Map<String, Region> stringRegionMap = new HashMap<String, Region>(){{
        put("us-east-1", Region.getRegion(Regions.US_EAST_1));
        /*put("us-east-2", Regions.US_EAST_2);
        put("us-gov-east-1", Regions.US_GOV_EAST_1);
        put("us-west-1, Regions.US_WEST_1);
        put("us-west-2", Regions.US_WEST_2);*/
    }};

    /*public AwsS3() {
        awsCredentials = new BasicAWSCredentials(
                accessKey, secretKey
        );

        s3Client = new AmazonS3Client(
                awsCredentials, clientRegion
        );
    }*/

    public AwsS3(String accessKey, String secretKey, String regionName, String bucket, String beforeDate, String afterDate) {

        this.clientRegion = stringRegionMap.get("us-east-1");
        this.beforeDate = beforeDate;
        this.afterDate = afterDate;
        this.bucket = bucket;
        this.accessKey = accessKey;
        this.secretKey = secretKey;

        awsCredentials = new BasicAWSCredentials(
                accessKey, secretKey
        );

        s3Client = new AmazonS3Client(
                awsCredentials, this.clientRegion
        );

    }

    public static String isInvalidSetting(String participantFullID, String serialNumber, String dataBucketURL, String accessKey, String secretKey) {


        String[] splitID = participantFullID.split("-");
        if(splitID.length != 4) return "participantFullID";
        String beforeDate = "v2/" + splitID[0] + "/" + splitID[1] + "/" + splitID[2] + "/participant_data/";
        String afterDate = "/" + splitID[3] + "-" + serialNumber + "/raw_data/v6/";
        //String bucket = dataBucketURL.split("/")[2];
        Pattern regex = Pattern.compile("empatica.*prod-data");
        Matcher matcher = regex.matcher(dataBucketURL);

        String bucket = null;
        if(matcher.find()) {
            bucket = matcher.group();
        }

        if(bucket == null) {
            return "bucket";
        }

        Region region = null;
        Set<String> keySet = stringRegionMap.keySet();
        for(String key : keySet) {
            if(bucket.contains(key)) {
                region = stringRegionMap.get(key);
                break;
            }
        }
        if(region == null) return "region";

        if(dataBucketURL.split("/").length != 6) {

        }
        Region clientRegion = stringRegionMap.get(bucket.substring(9, bucket.length() - 10));
        return isConnected(accessKey, secretKey, clientRegion, bucket, beforeDate);
    }

    public static String isConnected(String accessKey, String secretKey, Region clientRegion, String bucket, String prefix) {
        final String[] errorCode = new String[1];
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                /*String accessKey = "AKIAWWZYTIF57XRMCFPO";
                String secretKey = "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";
                Region clientRegion = Region.getRegion(Regions.US_EAST_1);
                String bucket = "empatica-us-east-1-prod-data";
                String prefix = "v2/394/1/1/participant_data/";*/
                AmazonS3Client s3Client = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey), clientRegion);
                try {
                    ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request().withBucketName(bucket).withPrefix(prefix);
                    ListObjectsV2Result listObjectsV2Result = s3Client.listObjectsV2(listObjectsV2Request);

                    s3Client.shutdown();
                    errorCode[0] = null;
                } catch (AmazonS3Exception e) {
                    errorCode[0] = e.getErrorCode();
                    Log.d(TAG, ErrorCode.valueOf(errorCode[0]).errString());
                }
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return errorCode[0];
    }

    public ArrayList<String> isDownloadable(String date, Long firstTimestamp, Long lastTimestamp) {
        prefix = beforeDate + date + afterDate;

        ArrayList<String> fileNameList = new ArrayList<>();
        try {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request();
                        listObjectsV2Request.withBucketName(bucket).withPrefix(prefix);
                        ListObjectsV2Result listObjectsV2Result = s3Client.listObjectsV2(listObjectsV2Request);
                        List<S3ObjectSummary> objects = listObjectsV2Result.getObjectSummaries();

                        for (int i = 0; i < objects.size(); i++) {
                            //Log.e("TEST", objects.get(i).getKey());
                            String fileAddress = objects.get(i).getKey();
                            Long fileTimestamp = Long.parseLong(fileAddress.substring(fileAddress.length() - 15, fileAddress.length() - 5));
                            if(fileTimestamp * 1000 < firstTimestamp && firstTimestamp < fileTimestamp * 1000 + 15 * 60000
                                    || firstTimestamp <= fileTimestamp * 1000 && fileTimestamp * 1000 + 15 * 60000 <= lastTimestamp
                                    || fileTimestamp * 1000 <= lastTimestamp && lastTimestamp <= fileTimestamp * 1000 + 15 * 60000) { // 파일의 시작이 비디오를 찍기 시작한지 15분 이내인지
                                fileNameList.add(objects.get(i).getKey());
                            /*if(lastTimestamp <= fileTimestamp * 1000 + 15 * 60000) {
                                break;
                            }*/
                            }
                        }
                    } catch (AmazonClientException e) {
                        throw new RuntimeException(e);
                    } catch (NumberFormatException e) {
                        throw new RuntimeException(e);
                    }

                }
            });

            thread.start();
            thread.join();
        } catch (Exception e) {
            Log.e("TEST", e.toString());
        }
        s3Client.shutdown();
        return fileNameList;
    }

    public void downloadWithTransferUtility(Context context, Long firstTimestamp, Long lastTimestamp, String date, ArrayList<String> fileNameList) {

        TransferNetworkLossHandler.getInstance(context);
        TransferUtility transferUtility = TransferUtility.builder()
                .context(context)
                .defaultBucket(bucket)
                .s3Client(s3Client)
                .build();

        //File file = new File(context.getFilesDir(), "1-1-001_1689647974.avro");
        for(int i = 0; i < fileNameList.size(); i++) {
            String fileName = fileNameList.get(i);
            String[] splitStr = fileName.split("/");
            File file = new File(context.getFilesDir().getPath() + "/record/" + firstTimestamp, "temp" + i + ".avro");
            TransferObserver transferObserver;
            try {
                transferObserver = transferUtility.download(bucket, fileName, file);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            transferObserver.setTransferListener(new TransferListener() {
                @Override
                public void onStateChanged(int id, TransferState state) {
                    if(state == TransferState.COMPLETED) {
                        file.renameTo(new File(context.getFilesDir().getPath() + "/record/" + firstTimestamp, splitStr[splitStr.length - 1]));
                        ArrayList<File> fileList = new ArrayList<>();
                        for(int i = 0; i < fileNameList.size(); i++) {
                            File file = new File(context.getFilesDir().getPath()+ "/record/" + firstTimestamp);
                            String[] splitStr = fileNameList.get(i).split("/");
                            String fileName = splitStr[splitStr.length - 1];
                            fileList.add((new File(file, fileName)));
                            if(Arrays.asList(file.list()).contains(fileName) == false) {
                                return ;
                            }
                        }
                        Log.e("TEST","download all complete");
                        s3Client.shutdown();
                        AvroExtractor.extractDataFromAvroFile(context.getFilesDir().getPath() + "/record/" + firstTimestamp, fileList, firstTimestamp, lastTimestamp);
                        BvpSync.synchronize(context.getFilesDir().getPath() + "/record/" + firstTimestamp);
                    }
                }

                @Override
                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {

                }

                @Override
                public void onError(int id, Exception ex) {

                }
            });
        }

    }
}
