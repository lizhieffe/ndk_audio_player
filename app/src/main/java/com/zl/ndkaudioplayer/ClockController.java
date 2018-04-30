package com.zl.ndkaudioplayer;

import android.support.annotation.Keep;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lizhieffe on 4/30/18.
 */

public class ClockController {
    private List<Listener> mListeners;

    private int mHour = 0;
    private int mMinute = 0;
    private int mSecond = 0;

    public interface Listener {
        void onTimeUpdate(String time);
    }

    public ClockController() {
        mListeners = new ArrayList<>();
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public String stringFromJNI() {
        return stringFromJNINative();
    }

    public void startTicks() {
        mHour = mMinute = mSecond = 0;
        startTicksNative();
    }

    public void stopTicks() {
        stopTicksNative();
    }

    /*
     * A function calling from JNI to update current timer
     */
    @Keep
    private void updateTimer() {
        ++mSecond;
        if(mSecond >= 60) {
            ++mMinute;
            mSecond -= 60;
            if(mMinute >= 60) {
                ++mHour;
                mMinute -= 60;
            }
        }
        String ticks = "" + mHour + ":" + mMinute + ":" + mSecond;
        for (Listener listener : mListeners) {
            listener.onTimeUpdate(ticks);
        }
    }

    // Used to load the 'hello-jni' library on application startup.
    static {
        System.loadLibrary("hello-jni");
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNINative();
    public native void startTicksNative();
    public native void stopTicksNative();
}
