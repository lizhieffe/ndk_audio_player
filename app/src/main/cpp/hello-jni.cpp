#include <inttypes.h>
#include <pthread.h>
#include <jni.h>
#include <android/log.h>
#include <assert.h>
#include <stdio.h>
#include <string>
#include <cstring>

// Android log function wrappers
static const char* kTAG = "hello-jni";
#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, kTAG, __VA_ARGS__))
#define LOGW(...) \
  ((void)__android_log_print(ANDROID_LOG_WARN, kTAG, __VA_ARGS__))
#define LOGE(...) \
  ((void)__android_log_print(ANDROID_LOG_ERROR, kTAG, __VA_ARGS__))

// processing callback to handler class
typedef struct tick_context {
    JavaVM  *javaVM;
    jclass   jniHelperClz;
    jobject  jniHelperObj;
    jclass   mainActivityClz;
    jobject  mainActivityObj;
    pthread_mutex_t  lock;
    int      done;
} TickContext;
TickContext g_ctx;

extern "C"
JNIEXPORT jstring
JNICALL
Java_com_zl_ndkaudioplayer_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

/*
 *  A helper function to show how to call
 *     java static functions JniHelper::getBuildVersion()
 *     java non-static function JniHelper::getRuntimeMemorySize()
 *  The trivial implementation for these functions are inside file
 *     JniHelper.java
 */
void queryRuntimeInfo(JNIEnv *env, jobject instance) {
    // Find out which OS we are running on. It does not matter for this app
    // just to demo how to call static functions.
    // Our java JniHelper class id and instance are initialized when this
    // shared lib got loaded, we just directly use them
    //    static function does not need instance, so we just need to feed
    //    class and method id to JNI
    jmethodID versionFunc = (*env).GetStaticMethodID(
            g_ctx.jniHelperClz,
            "getBuildVersion", "()Ljava/lang/String;");
    if (!versionFunc) {
        LOGE("Failed to retrieve getBuildVersion() methodID @ line %d",
             __LINE__);
        return;
    }
    jstring buildVersion = (jstring)env->CallStaticObjectMethod(g_ctx.jniHelperClz, versionFunc);
    const char *version = (*env).GetStringUTFChars(buildVersion, NULL);
    if (!version) {
        LOGE("Unable to get version string @ line %d", __LINE__);
        return;
    }
    LOGI("Android Version - %s", version);
    (*env).ReleaseStringUTFChars(buildVersion, version);

    // we are called from JNI_OnLoad, so got to release LocalRef to avoid leaking
    (*env).DeleteLocalRef(buildVersion);

    // Query available memory size from a non-static public function
    // we need use an instance of JniHelper class to call JNI
    jmethodID memFunc = (*env).GetMethodID(g_ctx.jniHelperClz,
                                            "getRuntimeMemorySize", "()J");
    if (!memFunc) {
        LOGE("Failed to retrieve getRuntimeMemorySize() methodID @ line %d",
             __LINE__);
        return;
    }
    jlong result = (*env).CallLongMethod(instance, memFunc);
    LOGI("Runtime free memory size: %" PRId64, result);
    (void)result;  // silence the compiler warning
}

/*
 * processing one time initialization:
 *     Cache the javaVM into our context
 *     Find class ID for JniHelper
 *     Create an instance of JniHelper
 *     Make global reference since we are using them from a native thread
 * Note:
 *     All resources allocated here are never released by application
 *     we rely on system to free all global refs when it goes away;
 *     the pairing function JNI_OnUnload() never gets called at all.
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    printf("===lizhi %s\n", __FUNCTION__);
    JNIEnv* env;
    memset(&g_ctx, 0, sizeof(g_ctx));

    g_ctx.javaVM = vm;
    if ((*vm).GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR; // JNI version not supported.
    }

    jclass  clz = (*env).FindClass("com/zl/ndkaudioplayer/JniHandler");
    g_ctx.jniHelperClz = (jclass)env->NewGlobalRef(clz);

    jmethodID  jniHelperCtor = (*env).GetMethodID(g_ctx.jniHelperClz,
                                                   "<init>", "()V");
    jobject    handler = (*env).NewObject(g_ctx.jniHelperClz,
                                           jniHelperCtor);
    g_ctx.jniHelperObj = (*env).NewGlobalRef(handler);
    queryRuntimeInfo(env, g_ctx.jniHelperObj);

    g_ctx.done = 0;
    g_ctx.mainActivityObj = NULL;
    return  JNI_VERSION_1_6;
}

/*
 * A helper function to wrap java JniHelper::updateStatus(String msg)
 * JNI allow us to call this function via an instance even it is
 * private function.
 */
void   sendJavaMsg(JNIEnv *env, jobject instance,
                   jmethodID func,const char* msg) {
    jstring javaMsg = (*env).NewStringUTF(msg);
    (*env).CallVoidMethod(instance, func, javaMsg);
    (*env).DeleteLocalRef(javaMsg);
}

/*
 * Main working thread function. From a pthread,
 *     calling back to MainActivity::updateTimer() to display ticks on UI
 *     calling back to JniHelper::updateStatus(String msg) for msg
 */
void*  UpdateTicks(void* context) {
    TickContext *pctx = (TickContext*) context;
    JavaVM *javaVM = pctx->javaVM;
    JNIEnv *env;
    jint res = (*javaVM).GetEnv((void**)&env, JNI_VERSION_1_6);
    if (res != JNI_OK) {
        res = (*javaVM).AttachCurrentThread(&env, NULL);
        if (JNI_OK != res) {
            LOGE("Failed to AttachCurrentThread, ErrorCode = %d", res);
            return NULL;
        }
    }

    jmethodID statusId = (*env).GetMethodID(pctx->jniHelperClz,
                                             "updateStatus",
                                             "(Ljava/lang/String;)V");
    sendJavaMsg(env, pctx->jniHelperObj, statusId,
                "TickerThread status: initializing...");

    // get mainActivity updateTimer function
    jmethodID timerId = (*env).GetMethodID(pctx->mainActivityClz,
                                            "updateTimer", "()V");

    struct timeval beginTime, curTime, usedTime, leftTime;
    const struct timeval kOneSecond = {
            (__kernel_time_t)1,
            (__kernel_suseconds_t) 0
    };

    sendJavaMsg(env, pctx->jniHelperObj, statusId,
                "TickerThread status: start ticking ...");
    while(1) {
        gettimeofday(&beginTime, NULL);
        pthread_mutex_lock(&pctx->lock);
        int done = pctx->done;
        if (pctx->done) {
            pctx->done = 0;
        }
        pthread_mutex_unlock(&pctx->lock);
        if (done) {
            break;
        }
        (*env).CallVoidMethod(pctx->mainActivityObj, timerId);

        gettimeofday(&curTime, NULL);
        timersub(&curTime, &beginTime, &usedTime);
        timersub(&kOneSecond, &usedTime, &leftTime);
        struct timespec sleepTime;
        sleepTime.tv_sec = leftTime.tv_sec;
        sleepTime.tv_nsec = leftTime.tv_usec * 1000;

        if (sleepTime.tv_sec <= 1) {
            nanosleep(&sleepTime, NULL);
        } else {
            sendJavaMsg(env, pctx->jniHelperObj, statusId,
                        "TickerThread error: processing too long!");
        }
    }

    sendJavaMsg(env, pctx->jniHelperObj, statusId,
                "TickerThread status: ticking stopped");
    (*javaVM).DetachCurrentThread();
    return context;
}

/*
 * Interface to Java side to start ticks, caller is from onResume()
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_zl_ndkaudioplayer_MainActivity_startTicks(JNIEnv *env, jobject instance) {
    printf("===lizhi in c: %s\n", __FUNCTION__);
    pthread_t       threadInfo_;
    pthread_attr_t  threadAttr_;

    pthread_attr_init(&threadAttr_);
    pthread_attr_setdetachstate(&threadAttr_, PTHREAD_CREATE_DETACHED);

    pthread_mutex_init(&g_ctx.lock, NULL);

    jclass clz = (*env).GetObjectClass(instance);
    g_ctx.mainActivityClz = (jclass)env->NewGlobalRef(clz);
    g_ctx.mainActivityObj = env->NewGlobalRef(instance);

    int result  = pthread_create( &threadInfo_, &threadAttr_, UpdateTicks, &g_ctx);
    assert(result == 0);

    pthread_attr_destroy(&threadAttr_);

    (void)result;
}

/*
 * Interface to Java side to stop ticks:
 *    we need to hold and make sure our native thread has finished before return
 *    for a clean shutdown. The caller is from onPause
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_zl_ndkaudioplayer_MainActivity_StopTicks(JNIEnv *env, jobject instance) {
    pthread_mutex_lock(&g_ctx.lock);
    g_ctx.done = 1;
    pthread_mutex_unlock(&g_ctx.lock);

    // waiting for ticking thread to flip the done flag
    struct timespec sleepTime;
    memset(&sleepTime, 0, sizeof(sleepTime));
    sleepTime.tv_nsec = 100000000;
    while (g_ctx.done) {
        nanosleep(&sleepTime, NULL);
    }

    // release object we allocated from StartTicks() function
    (*env).DeleteGlobalRef(g_ctx.mainActivityClz);
    (*env).DeleteGlobalRef(g_ctx.mainActivityObj);
    g_ctx.mainActivityObj = NULL;
    g_ctx.mainActivityClz = NULL;

    pthread_mutex_destroy(&g_ctx.lock);
}