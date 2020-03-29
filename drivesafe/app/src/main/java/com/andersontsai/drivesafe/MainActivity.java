package com.andersontsai.drivesafe;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
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
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.service.voice.VoiceInteractionService;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import java.net.*;
import java.io.*;
import java.util.regex.*;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener {

    private AppBarConfiguration mAppBarConfiguration;

    private static final String TAG = "MainActivity";

    private LocationManager locationManager;
    private LocationListener locationListener;
    private Context context;

    MediaRecorder recorder;
    private long recordingTime = 0;
    private static String fileName = null;
    private StorageReference storage;

    TextView username;
    TextView userEmail;

    private ArrayList<Double> accelerationData = new ArrayList<>();
    private ArrayList<Double> AccelDataHistory = new ArrayList<>();
    private double[] linear_acceleration = new double[3];

    private long prevTime = System.currentTimeMillis();
    private double prevLat = 0;
    private double prevLong = 0;

    private final int REQUEST_CODE = 69;

    private int speedLimit = 20;


    private static long absoluteStartTime = System.currentTimeMillis();
    private Vector<Integer> SpeedLimitOffenses = new Vector<Integer>();
    private Vector<Integer> AccelerationOffenses = new Vector<Integer>();
    private long lastSpeedOffenseTime = 0;
    private double maxAcceleration = 0;

    private TextView theAccel;
    private TextView theScore;
    private TextView theSpeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        theAccel = (TextView) findViewById(R.id.accelNumDisplay);
        theScore = (TextView) findViewById(R.id.scoreNumDisplay);
        theSpeed = (TextView) findViewById(R.id.speedNumDisplay);
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

        askPermissions();

        //Gets onboard accelerometer and begins listening for accelerometer output
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

        fileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        fileName += "/recorded_audio.3gp";
        storage = FirebaseStorage.getInstance().getReference("users");
        startRecording();
    }

    /** Request permissions needed to run app. */
    public boolean askPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

            // Request location and microphone access
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE);
            return false;
        }
        return true;
    }

    private void startRecording() {
//        if ((System.currentTimeMillis() - recordingTime) % 15000 == 0) {
//            stopRecording();
//        }
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile(fileName);

        try {
            recorder.prepare();
            recorder.start();
            Log.d(TAG, "Started recording");
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
    }

    public void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
        uploadAudio();
        startRecording();
//        createSoundFile();
    }

    private void uploadAudio() {
        StorageReference filepath = storage.child(userEmail.getText().toString()).child("driving_audio.3gp");
        Uri uri = Uri.fromFile(new File(fileName));
        filepath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG, "Uploaded audio file.");
            }
        });
    }

/*    private void createSoundFile() {
        //creating content values of size 4
        ContentValues values = new ContentValues(4);
        long current = System.currentTimeMillis();
        values.put(MediaStore.Audio.Media.TITLE, "audio" + audio.getName());
        values.put(MediaStore.Audio.Media.DATE_ADDED, (int) (current / 1000));
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp3");
        values.put(MediaStore.Audio.Media.DATA, audio.getAbsolutePath());

        //creating content resolver and storing it in the external content uri
        ContentResolver contentResolver = getContentResolver();
        Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Uri newUri = contentResolver.insert(base, values);

        //sending broadcast message to scan the media file so that it can be available
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, newUri));
    }*/

    /** SensorEvent Listener Overrides */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /** Calls HomeViewModel to display acceleration, and logs output from accelerometer. */
    @Override
    public void onSensorChanged(SensorEvent event) {
        HomeViewModel.setAcceleration(takeInNewAccelerationData());
        linear_acceleration[0] = event.values[0];
        linear_acceleration[1] = event.values[1];
        linear_acceleration[2] = event.values[2];
        double accelerationData = takeInNewAccelerationData();
        HomeViewModel.setAcceleration(accelerationData);
        //HomeViewModel.setAcceleration(event.values[0], event.values[1], event.values[2], accelerationData);
        BigDecimal bd = new BigDecimal(accelerationData);
        bd = bd.round(new MathContext(2));
        double roundedAccel = bd.doubleValue();
        theAccel.setText(roundedAccel + " m/s^2");
        theScore.setText(String.valueOf(calculateScore()));
        checkAcceleration(accelerationData);

//        Log.d(TAG, "onSensorChanged: X:" + event.values[0]
//                + " Y: " + event.values[1]
//                + " Z:" + event.values[2]);
    }


    @Override
    public void onLocationChanged(Location location) {
        double speed = 0;
        if (prevTime != 0) { // if second time through iteration or more
            speed = computeSpeed(measure(prevLat, prevLong, location.getLatitude(), location.getLongitude()));
            try {
                if (prevLong <= location.getLongitude()) {
                    speedLimit = speedLimit("Golden State Freeway", location.getLatitude(), location.getLongitude(), prevLat, prevLong);
                } else {
                    speedLimit = speedLimit("Golden State Freeway", prevLat, prevLong, location.getLatitude(), location.getLongitude());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        HomeViewModel.setLocation(location.getLongitude(), location.getLatitude(), speed);
        Log.d(TAG, "Latitude: " + location.getLatitude() + "Longitude: " + location.getLongitude()
                + " Speed: " + speed + "m/s");

        //        HomeViewModel.setLocation(location.getLongitude(), location.getLatitude(), speed);
//        Log.d(TAG, "Latitude: " + location.getLatitude() + "Longitude: " + location.getLongitude()
//                + " Speed: " + speed + "m/s");
//        TextView theLat = (TextView) findViewById(R.id.tempLat);
//        theLat.setText("Latitude: " + location.getLatitude());
//        TextView theLong = (TextView) findViewById(R.id.tempLong);
//        theLong.setText("Longitude: " + location.getLongitude());
        BigDecimal bd = new BigDecimal(speed);
        bd = bd.round(new MathContext(2));
        double roundedSpeed = bd.doubleValue();
        theSpeed.setText(roundedSpeed + " m/s");
        Log.d(TAG, "Latitude: " + location.getLatitude() + "Longitude: " + location.getLongitude()
                + " Speed: " + speed + "m/s");
        //TextView theLat = (TextView) findViewById(R.id.tempLat);
        //theLat.setText("Latitude: " + location.getLatitude());
        //TextView theLong = (TextView) findViewById(R.id.tempLong);
        //theLong.setText("Longitude: " + location.getLongitude());
        //hello
        theSpeed.setText(speed + "m/s");
        checkSpeed(speed);
        // checkSpeed(25);

        prevLat = location.getLatitude();
        prevLong = location.getLongitude();
        prevTime = System.currentTimeMillis();
        recordingTime = System.currentTimeMillis();
    }
    
    public int speedLimit(String name, Double x1, Double y1, Double x2, Double y2) throws IOException{
        // West to East ONLY
        assert y1 <= y2 : "speedLimit: y1 must be less than y2.";
        String replace = name.replaceAll(" ", "%20");
        URL oracle = new URL("https://overpass-api.de/api/interpreter?data=way[name=\"" + replace + "\"](" + x1 + ',' + y1 + ',' + x2 + ',' + y2 + ")[maxspeed];out;");
        BufferedReader in = new BufferedReader(new InputStreamReader(oracle.openStream()));
        String inputLine, substr = "-1";
        while ((inputLine = in.readLine()) != null) {
            String regex = "<tag k=\"maxspeed\" v=\".* ";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(inputLine);
            if (matcher.find()) {
                substr = inputLine.substring(25, inputLine.lastIndexOf(' '));
                break;
            }
        }
        in.close();
        return Integer.parseInt(substr);
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
        //Log.d(TAG,"standard deviation" + standardDeviation);

        if (Math.abs(mean - newData) < standardDeviation * 20) {
            for (int i = 0; i < accelerationData.size() - 1; i++) {
                accelerationData.set(i, accelerationData.get(i + 1));
                //Log.d(TAG,"doing the deed");
            }
            accelerationData.set(accelerationData.size()-1, newData);
        }
        AccelDataHistory.add(mean);
        //Log.d(TAG,"mean: " + mean);
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

    //finds the distance between latitude and longitude points in meters
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
        Log.d(TAG, "timeTaken: " + timeSpent);
        return meters / (timeSpent/1000);
    }

    public void checkSpeed(double speed) {
        if ((speed > (speedLimit + 2.2352)) //  2.2352 = 5mph over the speed limit in m/s
                && (speed < 54) && (speed > 0)
                && ((System.currentTimeMillis() - lastSpeedOffenseTime) > 5000))
        {
            double speedDifference = speed - speedLimit;
            lastSpeedOffenseTime = System.currentTimeMillis();
            if (speedDifference < 4.4704) {
                SpeedLimitOffenses.add(3600);
            }
            if(speedDifference < 5.36448) {
                SpeedLimitOffenses.add(4000);
            }
            else if (speedDifference < 6.7056) {
                SpeedLimitOffenses.add(5000);
            }
            else if (speedDifference < 7.59968) {
                SpeedLimitOffenses.add(6500);
            }
            else if (speedDifference < 8.9408) {
                SpeedLimitOffenses.add(7500);
            }
            else {
                SpeedLimitOffenses.add(8800);
            }

        }
        calculateScore();
    }

    public void checkAcceleration(double acceleration) {
        if (acceleration > 2.950464 || maxAcceleration != 0) { //in m/s^2
            if (maxAcceleration < acceleration) {
                maxAcceleration = acceleration;
                return;
            }
            if (maxAcceleration < 3.442208) {
                AccelerationOffenses.add(1);
            }
            else if (maxAcceleration < 4) {
                AccelerationOffenses.add(2);
            }
            else if (maxAcceleration < 4.57200) {
                AccelerationOffenses.add(3);
            }
            else {
                AccelerationOffenses.add(4);
            }
            maxAcceleration = 0;
        }
        calculateScore();
    }

    public int calculateScore() {
        long drivenTime = (System.currentTimeMillis() - absoluteStartTime);
        int runningSum = 0;


        for (int i = 0; i < SpeedLimitOffenses.size(); i++ ) {
            runningSum += SpeedLimitOffenses.get(i);
        }
        double SpeedOffensePercentage = 1- ( ((double)runningSum) / drivenTime );


        runningSum = 0;
        for (int i = 0; i < AccelerationOffenses.size(); i++ ) {
            runningSum += AccelerationOffenses.get(i);
        }
        double AccelOffensePercentage = 1 - ( ((double)runningSum) / drivenTime);

        Log.d(TAG, "Speed Offense size: " + SpeedLimitOffenses.size());
//        Log.d(TAG, "Acceleration Offense size: " + AccelerationOffenses.size());
        int score = (int)(SpeedOffensePercentage * 75) + (int)(AccelOffensePercentage * 25);
        Log.d(TAG, "score: " + score);
        if (score < 0) { // an extremely bad driver minimizes their score at 0
            score = 0;
        }
        if (score > 100) { //not needed, but good for full proofing
            score = 100;
        }
        return score;
    }
}
