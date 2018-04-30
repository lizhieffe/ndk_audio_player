//
// Created by lizhieffe on 4/30/18.
//

#include <android/log.h>
#include <cstddef>
#include <jni.h>
#include <vector>

// Android log function wrappers
static const char* kTAG = "audio-recorder-jni";
#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, kTAG, __VA_ARGS__))
#define LOGW(...) \
  ((void)__android_log_print(ANDROID_LOG_WARN, kTAG, __VA_ARGS__))
#define LOGE(...) \
  ((void)__android_log_print(ANDROID_LOG_ERROR, kTAG, __VA_ARGS__))

std::vector<char> recordedAudio_;

// JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
//   JNIEnv *env;
//   if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
//     return JNI_ERR;   // JNI version not supported.
//   }
// }

extern "C"
JNIEXPORT void JNICALL
Java_com_zl_ndkaudioplayer_AudioController_recordAudioNative(
        JNIEnv *env, jobject instance, jbyteArray byte_array) {
  jsize len = env->GetArrayLength(byte_array);
  jbyte *body = env->GetByteArrayElements(byte_array, 0);
  LOGE("from c recording audio bytes: %d", len);
  int i = 0;
  for (i = 0; i < len; i++) {
    recordedAudio_.push_back(body[i]);
  }
  LOGE("from c total audio bytes: %d", recordedAudio_.size());
  env->ReleaseByteArrayElements(byte_array, body, 0);
}

// extern "C"
// JNIEXPORT jstring JNICALL
// Java_com_zl_ndkaudioplayer_AudioController_getAudioNative(
//         JNIEnv *env, jobject instance) {
//
// }
