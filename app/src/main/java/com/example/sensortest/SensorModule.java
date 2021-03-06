package com.example.sensortest;

import static android.os.SystemClock.elapsedRealtime;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Matrix;

// MainActivity 에서 센서에 관련된 코드를 전부 여기로 넣어줌
public class SensorModule implements SensorEventListener {
    private boolean flag_is_sensor_running = false;

    private Activity mActivity;
    private FileModule file; // MainActivity에서 넘어온 파일을 저장

    // Sensors
    SensorManager sm;
    Sensor s1, s2, s3, s4, s5, s6, s7, s8;

    // counter
    private int[] count = new int[9]; // 센서 측정 개수를 저장

    private int sensor_sampling_interval_ms = 10; // 측정 간격
    private long sensor_measurement_start_time; // 센서 측정 시간

    // 센서 데이터 저장 공간
    float[] accL = new float[4]; // acc(디바이스 좌표계에서 측정)
    float[] gyroL = new float[4]; // gyro
    float[] magL = new float[4]; // magnetic
    float prx;                 // proximity
    float press;                // air pressure
    float light;                // light

    float[] quat = new float[4]; //quaternion
    float[] game_quat = new float[4]; //game quaternion

    float[] rot_mat = new float[16];
    // float[][] rMat = new float[3][3]; -> 2차원배열로 해도 됨!
    float[] rot_mat_opengl = new float[16]; // 단말의 orientation 에 상관없이 무조건 9.8이 나오도록
    float[] game_rot_mat = new float[16];
    float[] orientation_angle = new float[4];
    float[] accW = new float[4]; // 기준좌표계(World 좌표계)
    float[] gyroW = new float[4];

    // 현재까지의 측정결과
    String current_state = "";


    SensorModule(Activity activity){
        mActivity = activity;

        sm = (SensorManager) activity.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        //실제 센서들
        s1 = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        s2 = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        s3 = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        s4 = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        s5 = sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
        s6 = sm.getDefaultSensor(Sensor.TYPE_LIGHT);
        //가상의 센서들
        s7 = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        s8 = sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);

    }

    public float get_heading(){
        return orientation_angle[0] * 180f / 3.141592f; // rad 단위를 deg로 바꿔줘야 함.

    }

    public void start(long start_time, FileModule file_in){
        flag_is_sensor_running = true;
        sensor_measurement_start_time = start_time;

        file = file_in;

        sm.registerListener((SensorEventListener) this, s1, sensor_sampling_interval_ms * 1000);
        //SamplingPeriodUs 자리에 10000 혹은 SensorManager.SENSOR_DELAY_FASTEST, SENSOR_DELAY_NORMAL 를 사용한다.
        sm.registerListener((SensorEventListener) this, s2, sensor_sampling_interval_ms * 1000);
        sm.registerListener((SensorEventListener) this, s3, sensor_sampling_interval_ms * 1000);
        sm.registerListener((SensorEventListener) this, s4, sensor_sampling_interval_ms * 1000);
        sm.registerListener((SensorEventListener) this, s5, sensor_sampling_interval_ms * 1000);
        sm.registerListener((SensorEventListener) this, s6, sensor_sampling_interval_ms * 1000);
        sm.registerListener((SensorEventListener) this, s7, sensor_sampling_interval_ms * 1000);
        sm.registerListener((SensorEventListener) this, s8, sensor_sampling_interval_ms * 1000);

        for(int i=0; i<9; i++){
            count[i] = 0;
        }

    }

    public void stop(){
        flag_is_sensor_running = false;

        sm.unregisterListener((SensorEventListener) this, s1);
        sm.unregisterListener((SensorEventListener) this, s2);
        sm.unregisterListener((SensorEventListener) this, s3);
        sm.unregisterListener((SensorEventListener) this, s4);
        sm.unregisterListener((SensorEventListener) this, s5);
        sm.unregisterListener((SensorEventListener) this, s6);
        sm.unregisterListener((SensorEventListener) this, s7);
        sm.unregisterListener((SensorEventListener) this, s8);
    }



    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float elapsed_time_s = (float)(elapsedRealtime()/1e3 - sensor_measurement_start_time/1e3);
        float elapsed_fw_time_s = (float)(sensorEvent.timestamp / 1e9 - sensor_measurement_start_time / 1e3);

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(sensorEvent.values, 0, accL, 0, 3);
            file.save_str_to_file(String.format("ACC, %f, %f, %f, %f, %f\n",
                    elapsed_time_s, elapsed_fw_time_s, accL[0], accL[1], accL[2]));
            count[1] += 1;
        }
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            System.arraycopy(sensorEvent.values, 0, gyroL, 0, 3);
            file.save_str_to_file(String.format("GYRO, %f, %f, %f, %f, %f\n",
                    elapsed_time_s, elapsed_fw_time_s, gyroL[0], gyroL[1], gyroL[2]));
            count[2] += 1;

        }
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(sensorEvent.values, 0, magL, 0, 3);
            file.save_str_to_file(String.format("MAG, %f, %f, %f, %f, %f\n",
                    elapsed_time_s, elapsed_fw_time_s, magL[0], magL[1], magL[2]));
            count[3] += 1;
        }
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            prx = sensorEvent.values[0];
            file.save_str_to_file(String.format("PRX, %f, %f, %f\n",
                    elapsed_time_s, elapsed_fw_time_s, prx));
            count[4] += 1;
        }
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_PRESSURE) {
            press = sensorEvent.values[0];
            file.save_str_to_file(String.format("PRESS, %f, %f, %f\n",
                    elapsed_time_s, elapsed_fw_time_s, press));
            count[5] += 1;
        }
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_LIGHT) {
            light = sensorEvent.values[0];
            file.save_str_to_file(String.format("LIGHT, %f, %f, %f\n",
                    elapsed_time_s, elapsed_fw_time_s, light));
            count[6] += 1;
        }
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
            System.arraycopy(sensorEvent.values, 0, quat, 0, 4);
            SensorManager.getRotationMatrixFromVector(rot_mat, quat);
            SensorManager.getOrientation(rot_mat, orientation_angle);
            Matrix.transposeM(rot_mat_opengl, 0, rot_mat, 0);
            Matrix.multiplyMV(accW, 0, rot_mat_opengl, 0, accL, 0);
            file.save_str_to_file(String.format("ROT_VEC, %f, %f, %f, %f, %f\n",
                    elapsed_time_s, elapsed_fw_time_s, quat[0], quat[1], quat[2], quat[3]));
            count[7] += 1;
        }
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) {
            System.arraycopy(sensorEvent.values, 0, game_quat, 0, 4); // values 왜 오류?
            file.save_str_to_file(String.format("GAME_ROT_VEC, %f, %f, %f, %f, %f, %f\n",
                    elapsed_time_s, elapsed_fw_time_s, quat[0], quat[1], quat[2], quat[3]));
            count[8] += 1;
        }


        String str = "";
        str += String.format("Acc x: %2.3f, y: %2.3f, z: %2.3f\n", accL[0], accL[1], accL[2]);
        str += String.format("Gyro x: %2.3f, y: %2.3f, z: %2.3f\n", gyroL[0], gyroL[1], gyroL[2]);
        str += String.format("Mag x: %2.3f, y: %2.3f, z: %2.3f\n", magL[0], magL[1], magL[2]);
        str += String.format("Prx: %2.3f cm\n", prx);
        str += String.format("Press: %2.3f hPa\n", press);
        str += String.format("Light: %2.3f\n", light);
        str += String.format("Heading: %f, Pitch: %f, Roll: %f\n", orientation_angle[0]*180/3.1415, orientation_angle[1]*180/3.1415, orientation_angle[2]*180/3.1415);
        str += String.format("AccW x: %2.3f, y: %2.3f, z: %2.3f", accW[0], accW[1], accW[2]);

        current_state = str;
        // tv.setText(str);

    }

    public String get_latest_state(){
        return current_state;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i){
    }

}
