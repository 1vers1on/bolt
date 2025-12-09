#include <stdio.h>
#include <string.h>
#include <portaudio.h>
#include "include/net_ellie_bolt_jni_portaudio_PortAudioJNI.h"
#include "jni.h"
#include "jni_md.h"

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

    jclass deviceInfoClass = (*env)->FindClass(env, "net/ellie/bolt/jni/portaudio/PortAudioJNI$DeviceInfo");
    if (deviceInfoClass == NULL) return NULL;

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

    return (jlong)stream;
}

JNIEXPORT void JNICALL Java_net_ellie_bolt_jni_portaudio_PortAudioJNI_nativeStartStream(
    JNIEnv *env, jclass cls, jlong streamPtr) {

    PaStream* stream = (PaStream*)streamPtr;
    PaError err = Pa_StartStream(stream);
    if (err != paNoError) {
        throwPaException(env, &err);
    }
}

JNIEXPORT void JNICALL Java_net_ellie_bolt_jni_portaudio_PortAudioJNI_nativeStopStream(
    JNIEnv *env, jclass cls, jlong streamPtr) {

    PaStream* stream = (PaStream*)streamPtr;
    PaError err = Pa_StopStream(stream);
    if (err != paNoError) {
        throwPaException(env, &err);
    }
}

JNIEXPORT void JNICALL Java_net_ellie_bolt_jni_portaudio_PortAudioJNI_nativeCloseStream(
    JNIEnv *env, jclass cls, jlong streamPtr) {

    PaStream* stream = (PaStream*)streamPtr;
    PaError err = Pa_CloseStream(stream);
    if (err != paNoError) {
        throwPaException(env, &err);
    }
}

JNIEXPORT jlong JNICALL Java_net_ellie_bolt_jni_portaudio_PortAudioJNI_nativeReadStream(
    JNIEnv *env, jclass cls, jlong streamPtr, jbyteArray buffer, jlong framesRequested, jint channels) {

    PaStream* stream = (PaStream*)streamPtr;
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
    
    const PaStreamParameters* inputParams = Pa_GetStreamInfo(stream)->inputLatency == 0 ? NULL : 
        NULL;
        
    jsize bytesPerFrame = (jsize)(2 * channels);
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

    PaStream* stream = (PaStream*)streamPtr;
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

    const PaDeviceInfo* devInfo = Pa_GetDeviceInfo((PaDeviceIndex)inputDeviceIndex);
    if (devInfo != NULL) {
        channels = devInfo->maxInputChannels > 0 ? devInfo->maxInputChannels : 1;
    }
    jsize bytesPerFrame = (jsize)(2 * channels);

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

    jlong framesRequested = bytesToRead / bytesPerFrame;
    if (framesRequested == 0) return 0;

    jboolean isCopy;
    jbyte* bufferPtr = (*env)->GetByteArrayElements(env, buffer, &isCopy);
    if (bufferPtr == NULL) {
        jclass oom = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        (*env)->ThrowNew(env, oom, "Cannot get byte array elements");
        return -3;
    }

    jbyte* writePtr = bufferPtr + offset;

    PaError err = Pa_ReadStream(stream, writePtr, (unsigned long)framesRequested);

    (*env)->ReleaseByteArrayElements(env, buffer, bufferPtr, 0);

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

    return (jlong)stream;
}


JNIEXPORT jlong JNICALL Java_net_ellie_bolt_jni_portaudio_PortAudioJNI_nativeWriteStream(
    JNIEnv *env, jclass cls, jlong streamPtr, jbyteArray buffer, jlong framesToWrite) {

    PaStream* stream = (PaStream*)streamPtr;
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

    const PaDeviceInfo* devInfo = Pa_GetDeviceInfo(Pa_GetStreamInfo(stream)->outputLatency == 0 ? 0 : Pa_GetDefaultOutputDevice());
    int ch = (devInfo && devInfo->maxOutputChannels > 0) ? devInfo->maxOutputChannels : 1;
    return (jlong)(framesToWrite * ch * 2);
}

JNIEXPORT jlong JNICALL Java_net_ellie_bolt_jni_portaudio_PortAudioJNI_nativeWriteStreamOffset(
    JNIEnv *env, jclass cls, jlong streamPtr, jbyteArray buffer, jint offset, jlong framesToWrite) {

    PaStream* stream = (PaStream*)streamPtr;
    if (!stream) {
        jclass ex = (*env)->FindClass(env, "java/lang/IllegalStateException");
        (*env)->ThrowNew(env, ex, "Stream is NULL");
        return -1;
    }

    jsize bufLen = (*env)->GetArrayLength(env, buffer);
    const PaDeviceInfo* devInfo = Pa_GetDeviceInfo(Pa_GetStreamInfo(stream)->outputLatency == 0 ? 0 : Pa_GetDefaultOutputDevice());
    int ch = (devInfo && devInfo->maxOutputChannels > 0) ? devInfo->maxOutputChannels : 1;
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
