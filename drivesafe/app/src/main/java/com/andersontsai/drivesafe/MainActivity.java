package com.andersontsai.drivesafe;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.andersontsai.drivesafe.ui.home.HomeViewModel;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener {

    private AppBarConfiguration mAppBarConfiguration;

    private static final String TAG = "MainActivity";

    private LocationManager locationManager;
    private LocationListener locationListener;
    private Context context;

    TextView txtLocation;
    TextView username;
    TextView userEmail;

    private Vector<Double> accelerationData = new Vector<Double>();
    private Vector<Double> DataHistory = new Vector<Double>();
    private double[] linear_acceleration = new double[3];

    private long prevTime = 0;
    private double prevLat = 0;
    private double prevLong = 0;

    private final int REQUEST_CODE = 69;


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
        startLogin();
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
        double speed= 0;
        txtLocation = (TextView) findViewById(R.id.location_text);


        Log.d(TAG, "Latitude: " + location.getLatitude() + "Longitude: " + location.getLongitude());
        if (prevTime != 0) { // if second time through iteration or more
            speed = computeSpeed(measure(prevLat,prevLong,location.getLatitude(),location.getLongitude()));
        }
        txtLocation.setText("Latitude: " + location.getLatitude() + "Longitude: " + location.getLongitude()
                + " Speed: " + speed + "m/s");
        HomeViewModel.setLocation(location.getLongitude(), location.getLatitude(), speed);
        prevLat = location.getLatitude();
        prevLong = location.getLongitude();
        prevTime = System.currentTimeMillis();


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

    public void startLogin() {
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build());

        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                final FirebaseFirestore db = FirebaseFirestore.getInstance();
                final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                DocumentReference docRef = db.collection("users").document(firebaseUser.getEmail());
                docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                            } else {
                                CollectionReference users = db.collection("users");
                                Map<String, Object> user = new HashMap<>();
                                user.put("Name", firebaseUser.getDisplayName());
                                user.put("Score", 0);
                                users.document(firebaseUser.getEmail()).set(user);
                                db.collection("users")
                                        .add(user)
                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                            @Override
                                            public void onSuccess(DocumentReference documentReference) {
                                                Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.w(TAG, "Error adding document", e);
                                            }
                                        });
                            }
                        } else {
                            Log.d(TAG, "get failed with ", task.getException());
                        }
                    }
                });

                username = (TextView) findViewById(R.id.username);
                username.setText(firebaseUser.getDisplayName());
                userEmail = (TextView) findViewById(R.id.userEmail);
                userEmail.setText(firebaseUser.getEmail());
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                if (response == null) {
                    startLogin();
                }
            }
        }
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

        if (accelerationData.size() < 20) {
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

        if (Math.abs(mean - newData) < standardDeviation * 20) {
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

//    public double measure(double lat1, double lon1, double lat2, double lon2){  // geo measurement function
//        double R = 6378.137; // Radius of earth in KM
//        double dLat = lat2 * Math.PI / 180 - lat1 * Math.PI / 180;
//        double dLon = lon2 * Math.PI / 180 - lon1 * Math.PI / 180;
//        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
//                Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
//                        Math.sin(dLon/2) * Math.sin(dLon/2);
//        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
//        double d = R * c;
//        return d * 1000; // meters
//    }

    public static double measure(double lat1,
                                  double lon1, double lat2,
                                  double lon2)
    {

        // The math module contains a function
        // named toRadians which converts from
        // degrees to radians.
        lon1 = Math.toRadians(lon1);
        lon2 = Math.toRadians(lon2);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        // Haversine formula
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dlon / 2),2);

        double c = 2 * Math.asin(Math.sqrt(a));

        // Radius of earth in kilometers. Use 3956
        // for miles
        double r = 6371;

        // calculate the result
        return(c * r) * 1000;
    }

    public double computeSpeed(double meters) {
          double timeSpent = System.currentTimeMillis() - prevTime;
          return meters / timeSpent;
    }
}
