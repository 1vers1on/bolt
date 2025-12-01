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
    jmethodID arrayListInit = (*env)->GetMethodID(env, arrayListClass, "<init>", "()V");
    jobject deviceList = (*env)->NewObject(env, arrayListClass, arrayListInit);
    jmethodID arrayListAdd = (*env)->GetMethodID(env, arrayListClass, "add", "(Ljava/lang/Object;)Z");
    jclass deviceInfoClass = (*env)->FindClass(env, "net/ellie/portaudiojni/PortAudioJNI$DeviceInfo");
    jmethodID deviceInfoInit = (*env)->GetMethodID(env, deviceInfoClass, "<init>", "(ILjava/lang/String;Ljava/lang/String;IID)V");

    PaError err;
    int numDevices = Pa_GetDeviceCount();
    if (numDevices < 0) {
        throwPaException(env, (PaError*)&numDevices);
        return NULL;
    }

    for (int i = 0; i < numDevices; i++) {
        const PaDeviceInfo* deviceInfo = Pa_GetDeviceInfo(i);
        const PaHostApiInfo* hostApiInfo = Pa_GetHostApiInfo(deviceInfo->hostApi);
        jstring name = (*env)->NewStringUTF(env, deviceInfo->name);
        jstring hostApiName = (*env)->NewStringUTF(env, hostApiInfo->name);
        jobject deviceInfoObj = (*env)->NewObject(env, deviceInfoClass, deviceInfoInit,
                                                 (jint)i, name, hostApiName,
                                                 (jint)deviceInfo->maxInputChannels,
                                                 (jint)deviceInfo->maxOutputChannels,
                                                 (jdouble)deviceInfo->defaultSampleRate);
        (*env)->CallBooleanMethod(env, deviceList, arrayListAdd, deviceInfoObj);
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
    JNIEnv *env, jclass cls, jlong streamPtr, jbyteArray buffer, jlong framesRequested) {

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

    int channels = 1;
    const PaDeviceInfo* devInfo = Pa_GetDeviceInfo(Pa_GetDefaultInputDevice());
    if (devInfo && devInfo->maxInputChannels > 0) {
        channels = devInfo->maxInputChannels;
    }
    jsize bytesPerFrame = (jsize)(2 * channels);

    jlong requiredBytes = framesRequested * bytesPerFrame;
    if (requiredBytes > bufferLength) {
        jclass ex = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        (*env)->ThrowNew(env, ex, "Requested frames exceed buffer capacity");
        return -3;
    }

    if (framesRequested == 0) return 0;

    jboolean isCopy;
    jbyte* bufferPtr = (*env)->GetByteArrayElements(env, buffer, &isCopy);
    if (bufferPtr == NULL) {
        jclass oom = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        (*env)->ThrowNew(env, oom, "Cannot get byte array elements");
        return -4;
    }

    PaError err = Pa_ReadStream(stream, bufferPtr, (unsigned long)framesRequested);

    (*env)->ReleaseByteArrayElements(env, buffer, bufferPtr, 0);

    if (err == paInputOverflowed) {
        return 0;
    } else if (err != paNoError) {
        throwPaException(env, &err);
        return -5;
    }

    return (jlong)(framesRequested * bytesPerFrame);
}

JNIEXPORT jlong JNICALL Java_net_ellie_bolt_jni_portaudio_PortAudioJNI_nativeReadStreamOffset
  (JNIEnv *env, jclass cls, jlong streamPtr, jbyteArray buffer, jint offset, jlong bytesToRead) {

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

    int channels = 1;
    const PaDeviceInfo* devInfo = Pa_GetDeviceInfo(Pa_GetStreamInfo(stream)->inputLatency == 0 ? 0 : Pa_GetDefaultInputDevice());
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

    return (jlong)(framesRequested * bytesPerFrame);
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

    return (jlong)(framesToWrite * bytesPerFrame);
}
