package com.andersontsai.drivesafe;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
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

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener {

    private AppBarConfiguration mAppBarConfiguration;

    private static final String TAG = "MainActivity";

    private LocationManager locationManager;
    private LocationListener locationListener;
    private Context context;
    TextView txtLocation;

    private Vector<Double> accelerationData = new Vector<Double>();
    private Vector<Double> DataHistory = new Vector<Double>();
    private double[] linear_acceleration = new double[3];


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

        //Gets onboard accelerometer and begins listening
        Log.d(TAG, "onCreate: Initializing sensor services");
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        Log.d(TAG, "onCreate: Registered accelerometer listener");

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) this);
    }



    /** SensorEvent Listener Overrides */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /** Calls HomeViewModel to display acceleration, and logs output from accelerometer. */
    @Override
    public void onSensorChanged(SensorEvent event) {
        HomeViewModel.setAcceleration(event.values[0], event.values[1], event.values[2], takeInNewAccelerationData());
        linear_acceleration[0] = event.values[0];
        linear_acceleration[1] = event.values[1];
        linear_acceleration[2] = event.values[2];

        Log.d(TAG, "onSensorChanged: X:" + event.values[0]
                + " Y: " + event.values[1]
                + " Z:" + event.values[2]);
    }


    @Override
    public void onLocationChanged(Location location) {
        txtLocation = (TextView) findViewById(R.id.location_text);
        HomeViewModel.setLocation(location.getLongitude(), location.getLatitude());
        txtLocation.setText("Latitude: " + location.getLatitude() + "Longitude: " + location.getLongitude());
        Log.d(TAG, "Latitude: " + location.getLatitude() + "Longitude: " + location.getLongitude());

    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG,"Location disabled");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG,"Location enabled");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(TAG,"Status changed");
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

        return Math.sqrt(
                Math.pow(linear_acceleration[0],2)
                        + Math.pow(linear_acceleration[1],2)
                        + Math.pow(linear_acceleration[2],2)
        );
    }

    public double takeInNewAccelerationData() {
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
        Log.d(TAG,"standard deviation" + standardDeviation);

        if (Math.abs(mean - newData) < standardDeviation * 10) {
            for (int i = 0; i < accelerationData.size() - 1; i++) {
                accelerationData.set(i, accelerationData.get(i + 1));
                //Log.d(TAG,"doing the deed");
            }
            accelerationData.set(accelerationData.size()-1, newData);
        }
        else {
            Log.d(TAG,"not going through");
        }
        DataHistory.add(mean);
        Log.d(TAG,"mean: " + mean);
        return mean;

    }
}
