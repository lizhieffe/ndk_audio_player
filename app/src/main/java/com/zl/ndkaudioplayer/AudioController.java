package com.zl.ndkaudioplayer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;

/**
 * Created by lizhieffe on 4/29/18.
 */

public class AudioController {
    private static final String TAG = "AudioController";

    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    private int BytesPerElement = 2; // 2 bytes in 16bit format

    private int bufferSize;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private int mBufferSize;

    public AudioController() {
        mBufferSize = AudioRecord
                .getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
    }

    public void startRecording() {
        Log.i(TAG, "startRecording");
        if (!isRecording) {
            // try {
            //     recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE,
            //             RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, 2 * mBufferSize);
            //     if (recorder.getState() == AudioRecord.STATE_UNINITIALIZED) {
            //         // Oops looks like it was not initalized correctly
            //         Log.e(TAG, "AudioController.startRecording: cannot init audio record");
            //     }
            //     Log.d(TAG, "MainActivity.onCreate: starting audio record ...");
            //     recorder.startRecording();
            //     Log.d(TAG, "MainActivity.onCreate: audio record started ...");
            // } catch (UnsupportedOperationException e) {
            //     Log.e(TAG, "MainActivity.onCreate: ", e);
            // }

            // // recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
            // //         RECORDER_SAMPLERATE, RECORDER_CHANNELS,
            // //         RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

            // // recorder.startRecording();
            // isRecording = true;
            // recordingThread = new Thread(new Runnable() {
            //     @Override
            //     public void run() {
            //         fetchAudioData();
            //     }
            // }, "AudioRecorder Thread");
            // recordingThread.start();



            try {
                Log.e(TAG, "MainActivity.maybeStartAudioRecord: 222222");
                    MediaRecorder mMediaRecorder = new MediaRecorder();
                    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    mMediaRecorder.setOutputFile("/dev/null");
                    mMediaRecorder.prepare();
                    mMediaRecorder.start();
                    Log.e(TAG, "MainActivity.maybeStartAudioRecord: 333333");
            } catch (IOException e) {
                Log.e(TAG, "MainActivity.maybeStartAudioRecord: ", e);
            }
        }
    }

    public void stopRecording() {
        Log.i(TAG, "stopRecording");

    }

    public void play() {
        Log.i(TAG, "play");
    }

    private void fetchAudioData() {
        short sData[] = new short[BufferElements2Rec];
        while (isRecording) {
            recorder.read(sData, 0, BufferElements2Rec);
            byte bData[] = short2byte(sData);
            Log.i(TAG, "fetchAudioData: fetched audio data");
        }
    }

    //convert short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }
}
