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

    public static void setAcceleration(float x, float y, float z) {
        mText.postValue("X: " + x + " Y: " + y + "Z: " + z);
    }

    public LiveData<String> getText() {
        return mText;
    }
}