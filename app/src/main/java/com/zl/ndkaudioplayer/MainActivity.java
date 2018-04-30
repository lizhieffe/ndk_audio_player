package com.zl.ndkaudioplayer;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1232;

    int hour = 0;
    int minute = 0;
    int second = 0;
    TextView tickView;

    Button recordButton;
    Button playButton;

    AudioController audioController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        tickView = findViewById(R.id.tick_view);

        recordButton = findViewById(R.id.record_button);
        playButton = findViewById(R.id.play_button);
        audioController = new AudioController();
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioController.startRecording();
            }
        });
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioController.stopRecording();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        hour = minute = second = 0;
        TextView tv = findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());
        startTicks();
    }

    @Override
    public void onPause () {
        super.onPause();
        StopTicks();
    }

    /*
     * A function calling from JNI to update current timer
     */
    @Keep
    private void updateTimer() {
        ++second;
        if(second >= 60) {
            ++minute;
            second -= 60;
            if(minute >= 60) {
                ++hour;
                minute -= 60;
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String ticks = "" + MainActivity.this.hour + ":" +
                        MainActivity.this.minute + ":" +
                        MainActivity.this.second;
                MainActivity.this.tickView.setText(ticks);
            }
        });
    }

    // Used to load the 'hello-jni' library on application startup.
    static {
        System.loadLibrary("hello-jni");
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    public native void startTicks();
    public native void StopTicks();

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "MainActivity.onRequestPermissionsResult: permission for "
                            + Manifest.permission.RECORD_AUDIO + " is granted");
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    // TODO: disable audio recording
                    Log.e(TAG, "MainActivity.onRequestPermissionsResult: permission for "
                            + Manifest.permission.RECORD_AUDIO + " is not granted");
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    // Checks if necessary permissions are granted. The permissions need to be added in the
    // manifest file as 'uses-permission' items. Note that on AndroidThings Apr 4 2018 platform,
    // only adding permission in manifest file doesn't get some permissions granted after reboot,
    // e.g., RECORD_AUDIO. This method will help to pop up an UI to give permissions.
    private void checkPermissions() {
        final String permission = Manifest.permission.RECORD_AUDIO;
        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "MainActivity.onCreate: no permission for "
                    + permission + ", going to request from user.");

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{permission}, PERMISSIONS_REQUEST_RECORD_AUDIO);

                // PERMISSIONS_REQUEST_RECORD_AUDIO is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }
}
