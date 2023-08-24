# rPPG-Dataset-Acquition-System
----------------------
## Program Overview
This program is designed for Empatica EmbracePlus.

To be added...

----------------------
## What data can you get
If you have an EmbracePlus, you will be able to get the following data through this program.
1. Recorded mp4 video named "startTimestamp.mp4" and image file for each frame named "timestamp.jpg"
2. Light absorption raw data measured by EmbracePlus
3. Accelormeter and light sensor value measured by mobile device.

----------------------
## How To Use Program

It would be nice to run Empatica's CareLab before using this app.

If the app is not turned on, it may take a long time to synchronize data from EmbracePlus as it cannot be uploaded to the server.

### 1. Device Settings

To use this program, you must first set the information of your device.

When you Click "기기설정", the following information input box appears. this information is required to receive raw data from the AWS S3 server.

 - Participant Full ID
 - Serial Number
 - data bucket URL
 - Access Key
 - Secret Key

#### * Get Participant Full ID, Serial Number
   
   Participant full ID and serial number can be easily checked at Empatica carelab on mobile.
   
<img src="https://github.com/gemutsruhe/rPPG-Dataset-Acquition-System/assets/44457864/743bd1dd-e445-4e64-8f16-685b976f5deb.png" width="200" height="400"/>

   When you turn on carelab, there are vertical ellipsis on the upper right corner. and you can enter Settings by clicking on them.
   
<img src="https://github.com/gemutsruhe/rPPG-Dataset-Acquition-System/assets/44457864/45764406-0b9c-46e5-ab3a-9a302dad283c.png" width="200" height="400"/>

 Find and enter Participant full ID and Serial number.


#### * Get data bucket URL, Access Key, Secret Key

##### Here, it is explained based on the PC, but it is recommended to proceed on the mobile to copy the key. <br> In addition, since both mobile and PC proceed in a similar way, they are not explained separately.

You need to Login to [carelab.](https://carelab.empatica.com/) 

<img src="https://github.com/gemutsruhe/rPPG-Dataset-Acquition-System/assets/44457864/b4ccff67-b610-4462-8e39-4dd46a525ea2.png" width="300" height="400"/>

If you look at the left side, you will find the following menu. Select DATA ACCESS KEYS.

<img src="https://github.com/gemutsruhe/rPPG-Dataset-Acquition-System/assets/44457864/a2cf87e7-22b5-42c4-b5e5-21265ffd5c72.png" width="250" height="400"/>

Then you will see the GENERATE ACCESS KEY. When you click GENERATE ACCESS KEY, the ollowing screen appears.

<img src="https://github.com/gemutsruhe/rPPG-Dataset-Acquition-System/assets/44457864/6c3cda7a-3b35-4f4b-9007-c167a68dedc3.png" width="700" height="400"/>


Enter the Data Bucket URL, Access Key ID, and Scret Access Key on your device settings. 


If the data Bucket URL, Access Key and Secret Key are invalid, the error can be determined and feedback can be given, but it cannot be determined if the participant full ID and Serial Number are incorrect.



**Instead of using these device settings as they are, process them once and connect to the AWS server. There may be errors because the pattern was analyzed and processed using the developer's device. If you find any errors, please file a bug report.**



### 2. Recording

Press "동영상촬영" to move to VideoRecordingActivity and press "시작" button to start recording.
After that, if you press "중지", video recording stops and images are extracted frame by frame from the video.
Since this work is carried out by Worker, images are continuously extracted from the bakground unless it is completely closed.

**Since extracting an image from a video takes a long time, it is best not to close the app completely.**

If you return from VideoRecordingActivity to RecordingListActivity or close the app and turn it on again, synchronization starts.
Synchronization continues until alll recordings are syncheronized.
Synchronization requires an Network connection, so if the Network is not connected, the message that there is no network connection appears at the top of the recordingList until it is connected to the network.

**Since raw data measured by EmbracePlus is stored on the server in units of up to 15 minutes, it may take time to receive the data.**



---------------------
## Data Acquisition
Currently, the program takes the gyro sensor and light sensor values of the mobile phone and saves them in the sensor.json, but if you need other snesor values, you can modify the code as follows.

### - Default Data Acquisition

Recording Video(1280*720, 30fps), Image(480*320)

Light Absorption(timestamp, value)

Mobille Device Light Sensor value(timestamp, value), Accelerometer(timestamp, x, y, z)

### - EmbracePlus Device Data Acquisition Customize
To be added...

### - Mobile Device Data Acquisition Customize
1. In SensorReceiver.java's SensorReceiver Constructor, you can add a sensor to get a value throght addSensor(SensorType).
![image](https://github.com/gemutsruhe/rPPG-Dataset-Acquition-System/assets/44457864/13b4af84-2571-435a-a234-e46fd90ec9cd)

2. You must designate the name of the sensor and sensor value added to sensorInfoMap in VideoRecordingActivity.
![image](https://github.com/gemutsruhe/rPPG-Dataset-Acquition-System/assets/44457864/38781917-5ab9-4a00-8047-163aed849f1b)

You can find information about the sensor you want on this site. Also, you can name the valule of No.2 by referring to this site.<br>
[SensorEvent.](https://developer.android.com/reference/android/hardware/SensorEvent)

--------------------



