package com.andersontsai.drivesafe.ui.home;

import android.hardware.Sensor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.andersontsai.drivesafe.MainActivity;

import java.util.List;

public class HomeViewModel extends ViewModel {
    private static MutableLiveData<String> acceleration = new MutableLiveData<>();
    private static MutableLiveData<String> location = new MutableLiveData<>();

//    public HomeViewModel() {
//        mText = new MutableLiveData<>();
//        mText.setValue("This is a home fragment.");
//    }

    /** Sets acceleration to display X. Y, and Z acceleration. */
    public static void setAcceleration(float x, float y, float z, double accelerate) {
        acceleration.postValue("acceleration: " + accelerate + " m/s^2");
    }

    /** Returns acceleration. */
    public LiveData<String> getAcceleration() {
        return acceleration;
    }

    /** Sets location to display LAT and LON. */
    public static void setLocation(double lat, double lon, double speed) {
        location.postValue("Latitude: " + lat + " Longitude: " + lon + " Speed: " + speed);
    }

    /** Returns location */
    public LiveData<String> getLocation() {
        return location;
    }

}