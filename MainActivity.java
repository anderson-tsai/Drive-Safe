package com.andersontsai.drivesafe;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.andersontsai.drivesafe.ui.home.HomeViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.Vector;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private AppBarConfiguration mAppBarConfiguration;

    private static final String TAG = "MainActivity";

    Vector<Double> accelerationData = new Vector<Double>();
    Vector<Double> DataHistory = new Vector<Double>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
/*        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        Log.d(TAG, "onCreate: Initializing sensor services");
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        Log.d(TAG, "onCreate: Registered accelerometer listener");

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        HomeViewModel.setAcceleration(event.values[2]);
        Log.d(TAG, "onSensorChanged: X:" + event.values[0]
                + " Y:" + event.values[1]
                + " Z:" + event.values[2]);

//        double alpha = 0.8;

//        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
//        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
//        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
//
//        linear_acceleration[0] = event.values[0] - gravity[0];
//        linear_acceleration[1] = event.values[1] - gravity[1];
//        linear_acceleration[2] = event.values[2] - gravity[2];
        takeInNewAccelerationData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }


    public double getAccelerationData() {
        double[] linear_acceleration = new double[] {1,2,3};

        return Math.sqrt(
                Math.pow(linear_acceleration[0],2)
                + Math.pow(linear_acceleration[1],2)
                + Math.pow(linear_acceleration[2],2)
        );
    }

    public void takeInNewAccelerationData() {
        double newData = getAccelerationData();
        double mean = 0;
        double runningSum = 0;
        double standardDeviation = 0;
        if (accelerationData.size() < 100) {
            accelerationData.add(newData);
        }
        for (int i = 0; i < accelerationData.size(); i++) { // mean
            runningSum += accelerationData.get(i);
        }
        mean = runningSum / accelerationData.size();

        runningSum = 0;
        for (int i = 0; i < accelerationData.size(); i++) { // standardDeviation
            runningSum += Math.pow(accelerationData.get(i) - mean,2);
        }
        standardDeviation = Math.sqrt(runningSum/accelerationData.size());

        if (Math.abs(mean - newData) < standardDeviation * 2) {
            for (int i = 0; i < accelerationData.size() - 1; i++) {
                accelerationData.set(i, accelerationData.get(i + 1));
            }
            accelerationData.set(accelerationData.size()-1, newData);
        }
        DataHistory.add(mean);

        Log.d(TAG, "ontakeinNewData: mean: " + mean);
        Log.d(TAG, "ontakeinNewData: standard deviation: " + standardDeviation);

//        for (int i = 0; i < accelerationData.size(); i++) {
//            Log.d(TAG, "ontakeinNewData: element" + i + ": " + accelerationData.get(i));
//        }


    }




}
