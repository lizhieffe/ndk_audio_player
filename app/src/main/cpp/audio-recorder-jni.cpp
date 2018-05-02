//
// Created by lizhieffe on 4/30/18.
// The native functions for audio recording and playback.
// This is thread compatible.
//

#include <aaudio/AAudio.h>
#include <algorithm>
#include <android/log.h>
#include <cstddef>
#include <jni.h>
#include <vector>
#include <assert.h>


// Android log function wrappers
static const char* kTAG = "audio-recorder-jni";
#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, kTAG, __VA_ARGS__))
#define LOGW(...) \
  ((void)__android_log_print(ANDROID_LOG_WARN, kTAG, __VA_ARGS__))
#define LOGE(...) \
  ((void)__android_log_print(ANDROID_LOG_ERROR, kTAG, __VA_ARGS__))

std::vector<jbyte> recordedAudio_;
int index_;

// JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
//   JNIEnv *env;
//   if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
//     return JNI_ERR;   // JNI version not supported.
//   }
// }

extern "C"
JNIEXPORT void JNICALL
Java_com_zl_ndkaudioplayer_AudioController_clearNative(
        JNIEnv *env, jobject instance) {
  index_ = 0;
  recordedAudio_.clear();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_zl_ndkaudioplayer_AudioController_recordAudioNative(
        JNIEnv *env, jobject instance, jbyteArray byte_array) {
  jsize len = env->GetArrayLength(byte_array);
  jbyte *body = env->GetByteArrayElements(byte_array, 0);
  int i = 0;
  for (i = 0; i < len; i++) {
    recordedAudio_.push_back(body[i]);
  }
  env->ReleaseByteArrayElements(byte_array, body, 0);
}

// Returns the audio data of at most |size| bytes. A pin is used internally
// to remember how much data has been returned to the caller by calling
// this method before, and the returned data starts from the pin. The pin
// is reset iff the data is cleared or there is a new recording.
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_zl_ndkaudioplayer_AudioController_getAudioNative(
        JNIEnv *env, jobject instance, jint size) {
  if (size <= 0) {
    return NULL;
  }

  jbyteArray byteJavaArray = env->NewByteArray(size);
  if (byteJavaArray == NULL) {
    return NULL;
  }
  if (index_ >= recordedAudio_.size()) {
    return NULL;
  }

  int size_to_copy = std::min(size, (int)recordedAudio_.size() - index_);

  env->SetByteArrayRegion(byteJavaArray, 0, size_to_copy,
                          recordedAudio_.data() + index_ * sizeof(jbyte));
  index_ += size_to_copy;
  return byteJavaArray;
}

class AudioRecorder {
public:
  bool Init() {
    aaudio_result_t result = AAudio_createStreamBuilder(&builder);
    // TODO: create the result using better value.
    if (result != 0) {
      return false;
    }
    AAudioStreamBuilder_setDirection(builder, AAUDIO_DIRECTION_INPUT);
    AAudioStreamBuilder_setSampleRate(builder, 44100);
    AAudioStreamBuilder_setChannelCount(builder, 1);
    AAudioStreamBuilder_setFormat(builder, AAUDIO_FORMAT_PCM_I16);
    AAudioStreamBuilder_setBufferCapacityInFrames(builder, 3000);
    return true;
  }

  bool Record() {
    AAudioStream *stream;
    aaudio_result_t result = AAudioStreamBuilder_openStream(builder, &stream);
    // TODO: create the result using better value.
    if (result != 0) {
      return false;
    }

    assert(AAUDIO_DIRECTION_INPUT == AAudioStream_getDirection(stream));
    assert(44100 == AAudioStream_getSampleRate(stream));
    assert(1 == AAudioStream_getChannelCount(stream));
    assert(AAUDIO_FORMAT_PCM_I16 == AAudioStream_getFormat(stream));

    // TODO: if the builder is to be reused in the future, don't delete.
    AAudioStreamBuilder_delete(builder);

    result = AAudioStream_requestStart(stream);
    // TODO: create the result using better value.
    if (result != 0) {
      return false;
    }

    return true;
  }
 private:
  AAudioStreamBuilder *builder;
};
