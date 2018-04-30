package com.zl.ndkaudioplayer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * Created by lizhieffe on 4/29/18.
 */

public class AudioController {
    private static final String TAG = "AudioController";

    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    // When changing this format, also change the first param for the read() method of AudioRecord.
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord mRecorder = null;
    private Thread mRecordingThread = null;
    private boolean mIsRecording = false;
    private int mBufferSize;

    public AudioController() {
        mBufferSize = AudioRecord
                .getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING) * 2;
    }

    public void startRecording() {
        Log.i(TAG, "startRecording");
        if (!mIsRecording) {
            try {
                mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE,
                        RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, mBufferSize);
                if (mRecorder.getState() == AudioRecord.STATE_UNINITIALIZED) {
                    Log.e(TAG, "AudioController.startRecording: cannot init audio record");
                }
                Log.d(TAG, "MainActivity.onCreate: starting audio record ...");
                mRecorder.startRecording();
                Log.d(TAG, "MainActivity.onCreate: audio record started ...");
            } catch (UnsupportedOperationException e) {
                Log.e(TAG, "MainActivity.onCreate: ", e);
            }

            mIsRecording = true;
            mRecordingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    fetchAudioData();
                }
            }, "AudioRecorder Thread");
            mRecordingThread.start();
        }
    }

    public void stopRecording() {
        Log.i(TAG, "stopRecording");

    }

    public void play() {
        Log.i(TAG, "play");
    }

    private void fetchAudioData() {
        int shortArraySize = mBufferSize / 2;
        short sData[] = new short[shortArraySize];
        while (mIsRecording) {
            mRecorder.read(sData, 0, shortArraySize);
            byte bData[] = short2byte(sData);
            Log.d(TAG, "fetchAudioData: fetched audio data");
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
