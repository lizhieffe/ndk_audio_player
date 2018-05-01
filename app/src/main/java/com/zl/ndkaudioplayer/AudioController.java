package com.zl.ndkaudioplayer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by lizhieffe on 4/29/18.
 */

public class AudioController {
    private static final String TAG = "AudioController";

    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    // When changing this format, also change the first param for the read() method of AudioRecord.
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord mRecorder = null;
    private boolean mIsRecording = false;
    private int mBufferSize;

    private Thread mRecordingThread = null;
    private Thread mPlaybackThread = null;

    // private List<Byte> mAudioStore;
    private AudioTrack mAudioTrack;

    public AudioController() {
        mBufferSize = AudioRecord
                .getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING) * 2;
        // mAudioStore = new ArrayList<>();
    }

    synchronized public void startRecording() {
        Log.i(TAG, "startRecording");
        // mAudioStore.clear();

        if (!mIsRecording) {
            try {
                mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE,
                        RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, mBufferSize);
                if (mRecorder.getState() == AudioRecord.STATE_UNINITIALIZED) {
                    Log.e(TAG, "AudioController.startRecording: cannot init audio record");
                }
                Log.d(TAG, "Starting audio record ...");
                mRecorder.startRecording();
                Log.d(TAG, "Audio record started ...");
            } catch (UnsupportedOperationException e) {
                Log.e(TAG, "Error on recording: ", e);
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
        synchronized (this) {
            if (mIsRecording) {
                mIsRecording = false;
            }
        }
    }

    public void play() {
        Log.i(TAG, "playing recorded audio...");
        mPlaybackThread = new Thread(new Runnable() {
            @Override
            public void run() {
                doPlay();
            }
        }, "AudioRecorder Playback Thread");
        mPlaybackThread.start();

    }

    private void doPlay() {
        Log.i(TAG, "Start playing...");
        int bufferSize = AudioTrack.getMinBufferSize(RECORDER_SAMPLERATE,
                AudioFormat.CHANNEL_OUT_MONO, RECORDER_AUDIO_ENCODING);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO,
                RECORDER_AUDIO_ENCODING, bufferSize, AudioTrack.MODE_STREAM);
        mAudioTrack.play();

        while (true) {
            byte[] buffer = getAudioNative(bufferSize);
            if (buffer == null) {
                Log.i(TAG, "Finished playing...");
                return;
            }
            mAudioTrack.write(buffer, 0, buffer.length);
        }
    }

    private void fetchAudioData() {
        int shortArraySize = mBufferSize / 2;
        short sData[] = new short[shortArraySize];
        while (true) {
            boolean isRecording;
            synchronized (this) {
                isRecording = mIsRecording;
            }
            if (isRecording) {
                mRecorder.read(sData, 0, shortArraySize);
                byte bData[] = short2byte(sData);
                recordAudioNative(bData);
                Log.d(TAG, "fetchAudioData: fetched audio data");
            } else {
                Log.i(TAG, "fetchAudioData: Stopping...");
                mRecorder.stop();
                mRecorder = null;
                return;
            }
        }
    }

    // Convert short to byte
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

    static {
        System.loadLibrary("audio-recorder-jni");
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native void recordAudioNative(byte[] bytes);
    public native byte[] getAudioNative(int size);
}
