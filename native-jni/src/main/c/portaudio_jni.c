#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <portaudio.h>
#include "include/net_ellie_bolt_jni_portaudio_PortAudioJNI.h"
#include "jni.h"
#include "jni_md.h"

typedef struct StreamHandle {
    PaStream* stream;
    int channels; // number of channels configured for the stream
} StreamHandle;

void throwPaException(JNIEnv* env, PaError* err) {
    if (err == NULL || *err == paNoError) {
        return;
    }

    jclass paExceptionClass = (*env)->FindClass(env, "java/lang/RuntimeException");
    if (paExceptionClass != NULL) {
        char errorMessage[256];
        snprintf(errorMessage, sizeof(errorMessage), "PortAudio error: %s", Pa_GetErrorText(*err));
        (*env)->ThrowNew(env, paExceptionClass, errorMessage);
    }
}

JNIEXPORT jboolean JNICALL Java_net_ellie_bolt_jni_portaudio_PortAudioJNI_isFormatSupported(JNIEnv *env, jobject obj, jint deviceIndex, jint channels, jdouble sampleRate) {
    PaError err;
    if (deviceIndex < 0 || channels <= 0 || sampleRate <= 0) {
        return JNI_FALSE;
    }

    PaStreamParameters parameters;
    parameters.device = (PaDeviceIndex)deviceIndex;
    parameters.channelCount = (int)channels;
    parameters.sampleFormat = paInt16;
    parameters.suggestedLatency = Pa_GetDeviceInfo(parameters.device)->defaultLowOutputLatency;
    parameters.hostApiSpecificStreamInfo = NULL;
    err = Pa_IsFormatSupported(&parameters, NULL, (double)sampleRate);
    if (err != paFormatIsSupported) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_portaudio_PortAudioJNI_initialize(JNIEnv *env, jobject obj) {
    PaError err = Pa_Initialize();
    if (err != paNoError) {
        throwPaException(env, &err);
        return (jint)err;
    }
    return (jint)err;
}

JNIEXPORT void JNICALL Java_net_ellie_bolt_jni_portaudio_PortAudioJNI_terminate(JNIEnv *env, jobject obj) {
    PaError err = Pa_Terminate();
    if (err != paNoError) {
        throwPaException(env, &err);
    }
}

JNIEXPORT jobject JNICALL Java_net_ellie_bolt_jni_portaudio_PortAudioJNI_enumerateDevices(JNIEnv *env, jobject obj) {
    jclass arrayListClass = (*env)->FindClass(env, "java/util/ArrayList");
    if (arrayListClass == NULL) return NULL;
    jmethodID arrayListInit = (*env)->GetMethodID(env, arrayListClass, "<init>", "()V");
    if (arrayListInit == NULL) return NULL;
    jobject deviceList = (*env)->NewObject(env, arrayListClass, arrayListInit);
    if (deviceList == NULL) return NULL;
    jmethodID arrayListAdd = (*env)->GetMethodID(env, arrayListClass, "add", "(Ljava/lang/Object;)Z");
    if (arrayListAdd == NULL) return NULL;

    jclass portAudioJNIClass = (*env)->GetObjectClass(env, obj);
    if (portAudioJNIClass == NULL) return NULL;
    jmethodID getClassLoader = (*env)->GetMethodID(env, portAudioJNIClass, "getClass", "()Ljava/lang/Class;");
    if (getClassLoader == NULL) return NULL;
    jobject classObj = (*env)->CallObjectMethod(env, obj, getClassLoader);
    if ((*env)->ExceptionCheck(env) || classObj == NULL) return NULL;

    jclass classClass = (*env)->FindClass(env, "java/lang/Class");
    if (classClass == NULL) return NULL;
    jmethodID getLoaderMethod = (*env)->GetMethodID(env, classClass, "getClassLoader", "()Ljava/lang/ClassLoader;");
    if (getLoaderMethod == NULL) return NULL;
    jobject classLoader = (*env)->CallObjectMethod(env, classObj, getLoaderMethod);
    if ((*env)->ExceptionCheck(env) || classLoader == NULL) return NULL;

    jclass classLoaderClass = (*env)->FindClass(env, "java/lang/ClassLoader");
    if (classLoaderClass == NULL) return NULL;
    jmethodID loadClassMethod = (*env)->GetMethodID(env, classLoaderClass, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
    if (loadClassMethod == NULL) return NULL;

    jstring deviceInfoName = (*env)->NewStringUTF(env, "net.ellie.bolt.jni.portaudio.PortAudioJNI$DeviceInfo");
    if (deviceInfoName == NULL) return NULL;
    jobject deviceInfoClassObj = (*env)->CallObjectMethod(env, classLoader, loadClassMethod, deviceInfoName);
    (*env)->DeleteLocalRef(env, deviceInfoName);
    if ((*env)->ExceptionCheck(env) || deviceInfoClassObj == NULL) return NULL;
    jclass deviceInfoClass = (jclass)deviceInfoClassObj;

    jmethodID deviceInfoInit = (*env)->GetMethodID(env, deviceInfoClass, "<init>", "(ILjava/lang/String;Ljava/lang/String;IID)V");
    if (deviceInfoInit == NULL) return NULL;

    PaError err;
    int numDevices = Pa_GetDeviceCount();
    if (numDevices < 0) {
        throwPaException(env, (PaError*)&numDevices);
        return NULL;
    }

    for (int i = 0; i < numDevices; i++) {
    const PaDeviceInfo* deviceInfo = Pa_GetDeviceInfo(i);
        if (deviceInfo == NULL) continue;
        const PaHostApiInfo* hostApiInfo = Pa_GetHostApiInfo(deviceInfo->hostApi);
        const char* hostName = hostApiInfo ? hostApiInfo->name : "Unknown";
        jstring name = (*env)->NewStringUTF(env, deviceInfo->name ? deviceInfo->name : "Unknown");
        jstring hostApiName = (*env)->NewStringUTF(env, hostName);
        if (name == NULL || hostApiName == NULL) {
            if (name) (*env)->DeleteLocalRef(env, name);
            if (hostApiName) (*env)->DeleteLocalRef(env, hostApiName);
            return NULL;
        }

    jobject deviceInfoObj = (*env)->NewObject(env, deviceInfoClass, deviceInfoInit,
                                                 (jint)i, name, hostApiName,
                                                 (jint)deviceInfo->maxInputChannels,
                                                 (jint)deviceInfo->maxOutputChannels,
                                                 (jdouble)deviceInfo->defaultSampleRate);
    if (deviceInfoObj == NULL) {
        (*env)->DeleteLocalRef(env, name);
        (*env)->DeleteLocalRef(env, hostApiName);
        return NULL;
    }

    (*env)->CallBooleanMethod(env, deviceList, arrayListAdd, deviceInfoObj);
    if ((*env)->ExceptionCheck(env)) {
            (*env)->DeleteLocalRef(env, name);
            (*env)->DeleteLocalRef(env, hostApiName);
            (*env)->DeleteLocalRef(env, deviceInfoObj);
            return NULL;
        }

        (*env)->DeleteLocalRef(env, name);
        (*env)->DeleteLocalRef(env, hostApiName);
        (*env)->DeleteLocalRef(env, deviceInfoObj);
    }

    return deviceList;
}

JNIEXPORT jlong JNICALL Java_net_ellie_bolt_jni_portaudio_PortAudioJNI_nativeOpenInputStream(
    JNIEnv *env, jclass cls, jint deviceIndex, jint channels, jdouble sampleRate, jlong framesPerBuffer) {

    PaStream* stream;
    PaStreamParameters inputParameters;
    memset(&inputParameters, 0, sizeof(PaStreamParameters));
    inputParameters.device = (PaDeviceIndex)deviceIndex;
    inputParameters.channelCount = (int)channels;
    inputParameters.sampleFormat = paInt16;
    inputParameters.suggestedLatency = Pa_GetDeviceInfo(inputParameters.device)->defaultLowInputLatency;
    inputParameters.hostApiSpecificStreamInfo = NULL;

    PaError err = Pa_OpenStream(&stream, &inputParameters, NULL, (double)sampleRate,
                                (unsigned long)framesPerBuffer, paNoFlag, NULL, NULL);
    if (err != paNoError) {
        throwPaException(env, &err);
        return 0;
    }

    StreamHandle* handle = (StreamHandle*)malloc(sizeof(StreamHandle));
    if (handle == NULL) {
        Pa_CloseStream(stream);
        jclass oom = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        (*env)->ThrowNew(env, oom, "Failed to allocate StreamHandle");
        return 0;
    }
    handle->stream = stream;
    handle->channels = (int)channels;

    return (jlong)handle;
}

JNIEXPORT void JNICALL Java_net_ellie_bolt_jni_portaudio_PortAudioJNI_nativeStartStream(
    JNIEnv *env, jclass cls, jlong streamPtr) {

    StreamHandle* handle = (StreamHandle*)streamPtr;
    PaStream* stream = handle ? handle->stream : NULL;
    PaError err = Pa_StartStream(stream);
    if (err != paNoError) {
        throwPaException(env, &err);
    }
}

JNIEXPORT void JNICALL Java_net_ellie_bolt_jni_portaudio_PortAudioJNI_nativeStopStream(
    JNIEnv *env, jclass cls, jlong streamPtr) {

    StreamHandle* handle = (StreamHandle*)streamPtr;
    PaStream* stream = handle ? handle->stream : NULL;
    PaError err = Pa_StopStream(stream);
    if (err != paNoError) {
        throwPaException(env, &err);
    }
}

JNIEXPORT void JNICALL Java_net_ellie_bolt_jni_portaudio_PortAudioJNI_nativeCloseStream(
    JNIEnv *env, jclass cls, jlong streamPtr) {

    StreamHandle* handle = (StreamHandle*)streamPtr;
    PaStream* stream = handle ? handle->stream : NULL;
    PaError err = Pa_CloseStream(stream);
    if (err != paNoError) {
        throwPaException(env, &err);
    }
    if (handle) {
        handle->stream = NULL;
        free(handle);
    }
}

JNIEXPORT jlong JNICALL Java_net_ellie_bolt_jni_portaudio_PortAudioJNI_nativeReadStream(
    JNIEnv *env, jclass cls, jlong streamPtr, jbyteArray buffer, jlong framesRequested, jint channels) {

    StreamHandle* handle = (StreamHandle*)streamPtr;
    PaStream* stream = handle ? handle->stream : NULL;
    if (stream == NULL) {
        jclass ex = (*env)->FindClass(env, "java/lang/IllegalStateException");
        (*env)->ThrowNew(env, ex, "Stream is NULL");
        return -1;
    }

    if (framesRequested < 0) {
        jclass ex = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        (*env)->ThrowNew(env, ex, "framesRequested < 0");
        return -2;
    }

    jsize bufferLength = (*env)->GetArrayLength(env, buffer);

    const PaStreamInfo* streamInfo = Pa_GetStreamInfo(stream);
    if (streamInfo == NULL) {
        jclass ex = (*env)->FindClass(env, "java/lang/IllegalStateException");
        (*env)->ThrowNew(env, ex, "Cannot get stream info");
        return -3;
    }
    
    int ch = handle ? handle->channels : (int)channels;
    jsize bytesPerFrame = (jsize)(2 * ch);
    jlong requiredBytes = framesRequested * bytesPerFrame;
    
    if (requiredBytes > bufferLength) {
        jclass ex = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        (*env)->ThrowNew(env, ex, "Requested frames exceed buffer capacity");
        return -4;
    }

    if (framesRequested == 0) return 0;

    jboolean isCopy;
    jbyte* bufferPtr = (*env)->GetByteArrayElements(env, buffer, &isCopy);
    if (bufferPtr == NULL) {
        jclass oom = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        (*env)->ThrowNew(env, oom, "Cannot get byte array elements");
        return -5;
    }

    PaError err = Pa_ReadStream(stream, bufferPtr, (unsigned long)framesRequested);

    (*env)->ReleaseByteArrayElements(env, buffer, bufferPtr, 0);

    if (err == paInputOverflowed) {
        return 0;
    } else if (err != paNoError) {
        throwPaException(env, &err);
        return -6;
    }

    return framesRequested;
}

JNIEXPORT jlong JNICALL Java_net_ellie_bolt_jni_portaudio_PortAudioJNI_nativeReadStreamOffset
  (JNIEnv *env, jclass cls, jlong streamPtr, jbyteArray buffer, jint offset, jlong bytesToRead, jint channels, jint inputDeviceIndex) {

    StreamHandle* handle = (StreamHandle*)streamPtr;
    PaStream* stream = handle ? handle->stream : NULL;
    if (stream == NULL) {
        jclass illegalStateExceptionClass = (*env)->FindClass(env, "java/lang/IllegalStateException");
        (*env)->ThrowNew(env, illegalStateExceptionClass, "Stream is NULL");
        return -1;
    }

    jsize bufferLength = (*env)->GetArrayLength(env, buffer);

    const PaStreamInfo* streamInfo = Pa_GetStreamInfo(stream);
    if (streamInfo == NULL) {
        jclass illegalStateExceptionClass = (*env)->FindClass(env, "java/lang/IllegalStateException");
        (*env)->ThrowNew(env, illegalStateExceptionClass, "Cannot get stream info");
        return -1;
    }

    int ch = handle ? handle->channels : (int)channels;
    jsize bytesPerFrame = (jsize)(2 * ch);

    if (bytesToRead < 0) {
        jclass illegalArgument = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        (*env)->ThrowNew(env, illegalArgument, "bytesToRead < 0");
        return -2;
    }

    jlong end = (jlong)offset + bytesToRead;
    if (offset < 0 || end > bufferLength) {
        jclass illegalArgument = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        (*env)->ThrowNew(env, illegalArgument, "Read would exceed buffer length");
        return -2;
    }

    if ((bytesToRead % bytesPerFrame) != 0) {
        jclass illegalArgument = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        (*env)->ThrowNew(env, illegalArgument, "bytesToRead must be a multiple of bytesPerFrame");
        return -2;
    }

    jlong framesRequested = bytesToRead / bytesPerFrame;
    if (framesRequested == 0) return 0;

    jboolean isCopy;
    jbyte* bufferPtr = (*env)->GetPrimitiveArrayCritical(env, buffer, &isCopy);
    if (bufferPtr == NULL) {
        jclass oom = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        (*env)->ThrowNew(env, oom, "Cannot get byte array elements");
        return -3;
    }

    jbyte* writePtr = bufferPtr + offset;

    PaError err = Pa_ReadStream(stream, writePtr, (unsigned long)framesRequested);

    (*env)->ReleasePrimitiveArrayCritical(env, buffer, bufferPtr, 0);

    if (err == paInputOverflowed) {
        return 0;
    } else if (err != paNoError) {
        throwPaException(env, &err);
        return -4;
    }

    return (jlong)(framesRequested);
}

JNIEXPORT jlong JNICALL Java_net_ellie_bolt_jni_portaudio_PortAudioJNI_nativeOpenOutputStream(
    JNIEnv *env, jclass cls, jint deviceIndex, jint channels, jdouble sampleRate, jlong framesPerBuffer) {

    PaStream* stream;
    PaStreamParameters outputParameters;
    memset(&outputParameters, 0, sizeof(PaStreamParameters));
    outputParameters.device = (PaDeviceIndex)deviceIndex;
    outputParameters.channelCount = (int)channels;
    outputParameters.sampleFormat = paInt16;
    outputParameters.suggestedLatency = Pa_GetDeviceInfo(outputParameters.device)->defaultLowOutputLatency;
    outputParameters.hostApiSpecificStreamInfo = NULL;

    PaError err = Pa_OpenStream(&stream, NULL, &outputParameters, (double)sampleRate,
                                (unsigned long)framesPerBuffer, paNoFlag, NULL, NULL);
    if (err != paNoError) {
        throwPaException(env, &err);
        return 0;
    }

    StreamHandle* handle = (StreamHandle*)malloc(sizeof(StreamHandle));
    if (handle == NULL) {
        Pa_CloseStream(stream);
        jclass oom = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        (*env)->ThrowNew(env, oom, "Failed to allocate StreamHandle");
        return 0;
    }
    handle->stream = stream;
    handle->channels = (int)channels;

    return (jlong)handle;
}


JNIEXPORT jlong JNICALL Java_net_ellie_bolt_jni_portaudio_PortAudioJNI_nativeWriteStream(
    JNIEnv *env, jclass cls, jlong streamPtr, jbyteArray buffer, jlong framesToWrite) {

    StreamHandle* handle = (StreamHandle*)streamPtr;
    PaStream* stream = handle ? handle->stream : NULL;
    if (!stream) {
        jclass ex = (*env)->FindClass(env, "java/lang/IllegalStateException");
        (*env)->ThrowNew(env, ex, "Stream is NULL");
        return -1;
    }

    jboolean isCopy;
    jbyte* bufferPtr = (*env)->GetByteArrayElements(env, buffer, &isCopy);
    if (!bufferPtr) {
        jclass oom = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        (*env)->ThrowNew(env, oom, "Cannot get byte array elements");
        return -2;
    }

    PaError err = Pa_WriteStream(stream, bufferPtr, (unsigned long)framesToWrite);

    (*env)->ReleaseByteArrayElements(env, buffer, bufferPtr, 0);

    if (err == paOutputUnderflowed) {
        return 0;
    } else if (err != paNoError) {
        throwPaException(env, &err);
        return -3;
    }

    int ch = handle ? handle->channels : 1;
    return (jlong)(framesToWrite * ch * 2);
}

JNIEXPORT jlong JNICALL Java_net_ellie_bolt_jni_portaudio_PortAudioJNI_nativeWriteStreamOffset(
    JNIEnv *env, jclass cls, jlong streamPtr, jbyteArray buffer, jint offset, jlong framesToWrite) {

    StreamHandle* handle = (StreamHandle*)streamPtr;
    PaStream* stream = handle ? handle->stream : NULL;
    if (!stream) {
        jclass ex = (*env)->FindClass(env, "java/lang/IllegalStateException");
        (*env)->ThrowNew(env, ex, "Stream is NULL");
        return -1;
    }

    jsize bufLen = (*env)->GetArrayLength(env, buffer);
    int ch = handle ? handle->channels : 1;
    jsize bytesPerFrame = 2 * ch;

    jlong requiredBytes = framesToWrite * bytesPerFrame;
    if (offset < 0 || (offset + requiredBytes) > bufLen) {
        jclass ex = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        (*env)->ThrowNew(env, ex, "Write would exceed buffer length");
        return -2;
    }

    jboolean isCopy;
    jbyte* bufferPtr = (*env)->GetByteArrayElements(env, buffer, &isCopy);
    if (!bufferPtr) {
        jclass oom = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        (*env)->ThrowNew(env, oom, "Cannot get byte array elements");
        return -3;
    }

    PaError err = Pa_WriteStream(stream, bufferPtr + offset, (unsigned long)framesToWrite);

    (*env)->ReleaseByteArrayElements(env, buffer, bufferPtr, 0);

    if (err == paOutputUnderflowed) {
        return 0;
    } else if (err != paNoError) {
        throwPaException(env, &err);
        return -4;
    }

    return (jlong)(framesToWrite);
}
