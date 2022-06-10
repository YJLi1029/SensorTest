package com.example.sensortest;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static android.Manifest.permission.CHANGE_WIFI_STATE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.SystemClock.elapsedRealtime;

import static java.lang.Thread.sleep;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    TextView tv;
    Button btn, btn2;
    FloatingActionButton fAB, fAB2;
    boolean flag_running = false;

    // File 쓸 수 있도록 fileModule 생성
    FileModule fileModule;
    private long begin_time; // start 할 때 시간을 저장할 변수

    // Modules
    SensorModule SensorModule;
    WiFiModule wifiModule;
    WifiManager wifiManager;
    ImageModule imageModule;


    // permission-related
    private boolean is_permission_granted = false; // permission을 요구하는 상태로 초기화.

    // Threads
    Looper display_update_looper;
    int count = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // permission-request
        ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, ACCESS_FINE_LOCATION, CHANGE_WIFI_STATE, ACCESS_WIFI_STATE, ACCESS_COARSE_LOCATION}, 1);


        tv = (TextView) findViewById(R.id.tv);
        //btn = (Button) findViewById(R.id.btn);
        btn2 = (Button) findViewById(R.id.btn2);
        fAB = (FloatingActionButton) findViewById(R.id.fAB);


        //btn.setText("Start!");
        btn2.setText("WiFi");

        SensorModule = new SensorModule(this);
        wifiModule = new WiFiModule(getApplicationContext());
        imageModule = new ImageModule(this);


        fAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (flag_running)
                    stop();
                else
                    start();
            }
        });
        btn2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                wifiManager.startScan();
            }
        });

        HandlerThread handlerThread = new HandlerThread("DISPLAY_UPDATE_THREAD", Process.THREAD_PRIORITY_DISPLAY);
        handlerThread.start();

        display_update_looper = handlerThread.getLooper();


        Handler handler = new Handler(display_update_looper);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                display_update_thread();
            }
        }, 0); // delay 없이 실행함
        // thread_test();
    }

    private void display_update_thread(){
        count += 1;
        tv.setText(count + "");

        if (flag_running){
            float deg = SensorModule.get_heading();
            //imageModule.plot_arrow(0, 0, deg); // 깜빡거리는 문제가 발생해서 주석처리 해둠.

            String str = "";
            str += "[WiFi]\n" + wifiModule.get_latest_state() + "\n\n";
            str += "[Sensor]\n" + SensorModule.get_latest_state();
            tv.setText(str);

        }

        Handler handler = new Handler(display_update_looper);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                display_update_thread();
            }
        }, 100);
    }


    private void thread_test(){
        Thread thread = Thread.currentThread();
        Log.d("THREAD_TEST", thread.getName() + ", " + thread.getId());
        for(int i=0; i<10; i++){
            Log.d("THREAD_TEST", thread.getName() + ":" + i);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void stop() {
        flag_running = false;
        SensorModule.stop();
        wifiModule.stop();
        // btn.setText("Start");
        fAB.setImageTintList(ColorStateList.valueOf(Color.rgb(0, 0, 0)));


    }

    private void start() {
        if (!is_permission_granted) {
            Toast.makeText(getApplicationContext(), "Permission is not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        begin_time = elapsedRealtime();
        fileModule = new FileModule(this, "test", true, true, ".txt");
        SensorModule.start(begin_time, fileModule);
        wifiModule.start();

        flag_running = true;
        fAB.setImageTintList(ColorStateList.valueOf(Color.rgb(57, 155, 226)));
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResult){
        for (int i=0; i<grantResult.length; i++)
            if (grantResult[i] != PERMISSION_GRANTED) {// PERMISSION_GRANTED 대신에 0으로 해도 됨!
                Toast.makeText(getApplicationContext(), "Warning: " + permissions[i] + " is not granted", Toast.LENGTH_SHORT).show();
                return;
            }
        is_permission_granted = true;
    }


}
