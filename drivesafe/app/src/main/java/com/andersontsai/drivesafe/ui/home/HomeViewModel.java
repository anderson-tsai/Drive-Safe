package com.andersontsai.drivesafe.ui.home;

import android.hardware.Sensor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.andersontsai.drivesafe.MainActivity;

import java.util.List;

public class HomeViewModel extends ViewModel {
    private static MutableLiveData<String> mText = new MutableLiveData<>();

//    public HomeViewModel() {
//        mText = new MutableLiveData<>();
//        mText.setValue("This is a home fragment.");
//    }

    public static void setAcceleration(float a) {
        if (a > -0.3 && a < 0.3) {
            mText.setValue("Linear Acceleration: Not Moving");
        } else {
            mText.setValue("Linear Acceleration: " + a + " m/s²");
        }
    }

    public LiveData<String> getText() {
        return mText;
    }
}