package com.zl.ndkaudioplayer;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1232;

    TextView tickView;

    Button mRecordButton;
    Button mStopRecordButton;
    Button mPlayButton;

    ClockController mTimerController;
    AudioController mAudioController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        initClockController();
        initAudioController();
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView tv = findViewById(R.id.sample_text);
        tv.setText(mTimerController.stringFromJNI());
        mTimerController.startTicks();
    }

    @Override
    public void onPause () {
        super.onPause();
        mTimerController.stopTicks();
    }

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

    private void initClockController() {
        tickView = findViewById(R.id.tick_view);

        mTimerController = new ClockController();
        mTimerController.addListener(new ClockController.Listener() {
            @Override
            public void onTimeUpdate(final String time) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.tickView.setText(time);
                    }
                });
            }
        });
    }

    private void initAudioController() {
        mRecordButton = findViewById(R.id.record_button);
        mStopRecordButton = findViewById(R.id.stop_record_button);
        mPlayButton = findViewById(R.id.play_button);
        mAudioController = new AudioController();
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAudioController.startRecording();
            }
        });
        mStopRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAudioController.stopRecording();
            }
        });
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAudioController.play();
            }
        });
    }
}
