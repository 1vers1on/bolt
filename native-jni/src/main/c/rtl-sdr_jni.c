#include <rtl-sdr.h>
#include "include/net_ellie_bolt_jni_rtlsdr_RTLSDR.h"
#include "jni.h"
#include "jni_md.h"

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_getDeviceCount(JNIEnv *env, jclass cls) {
    return rtlsdr_get_device_count();
}

JNIEXPORT jstring JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_getDeviceName(JNIEnv *env, jclass cls, jint index) {
    const char* name = rtlsdr_get_device_name(index);
    return (*env)->NewStringUTF(env, name);
}

JNIEXPORT jlong JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nOpen(JNIEnv *env, jobject obj, jint index) {
    rtlsdr_dev_t *dev = NULL;
    int result = rtlsdr_open(&dev, index);
    if (result < 0) {
        return 0;
    }
    return (jlong)(uintptr_t)dev;
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nClose(JNIEnv *env, jobject obj, jlong device) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    return rtlsdr_close(dev);
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nSetXtalFreq(JNIEnv *env, jobject obj, jlong device, jint rtlFreq, jint tunerFreq) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    return rtlsdr_set_xtal_freq(dev, rtlFreq, tunerFreq);
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nWriteEeprom(JNIEnv *env, jobject obj, jlong device, jbyteArray eeprom, jint offset, jint length) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    jbyte *eepromBytes = (*env)->GetByteArrayElements(env, eeprom, NULL);
    int result = rtlsdr_write_eeprom(dev, (uint8_t *)eepromBytes, offset, length);
    (*env)->ReleaseByteArrayElements(env, eeprom, eepromBytes, 0);
    return result;
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nReadEeprom(JNIEnv *env, jobject obj, jlong device, jbyteArray eeprom, jint offset, jint length) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    jbyte *eepromBytes = (*env)->GetByteArrayElements(env, eeprom, NULL);
    int result = rtlsdr_read_eeprom(dev, (uint8_t *)eepromBytes, offset, length);
    (*env)->ReleaseByteArrayElements(env, eeprom, eepromBytes, 0);
    return result;
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nSetCenterFreq(JNIEnv *env, jobject obj, jlong device, jint freq) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    return rtlsdr_set_center_freq(dev, freq);
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nGetCenterFreq(JNIEnv *env, jobject obj, jlong device) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    return rtlsdr_get_center_freq(dev);
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nSetFreqCorrection(JNIEnv *env, jobject obj, jlong device, jint ppm) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    return rtlsdr_set_freq_correction(dev, ppm);
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nGetFreqCorrection(JNIEnv *env, jobject obj, jlong device) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    return rtlsdr_get_freq_correction(dev);
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nGetTunerType(JNIEnv *env, jobject obj, jlong device) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    return rtlsdr_get_tuner_type(dev);
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nGetTunerGains(JNIEnv *env, jobject obj, jlong device, jintArray gainsArray) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    int num_gains = rtlsdr_get_tuner_gains(dev, NULL);
    jint *gains = (*env)->GetIntArrayElements(env, gainsArray, NULL);
    num_gains = rtlsdr_get_tuner_gains(dev, (int *)gains);
    (*env)->ReleaseIntArrayElements(env, gainsArray, gains, 0);
    return num_gains;
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nSetTunerGain(JNIEnv *env, jobject obj, jlong device, jint gain) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    return rtlsdr_set_tuner_gain(dev, gain);
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nSetTunerBandwidth(JNIEnv *env, jobject obj, jlong device, jint bandwidth) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    return rtlsdr_set_tuner_bandwidth(dev, bandwidth);
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nGetTunerGain(JNIEnv *env, jobject obj, jlong device) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    return rtlsdr_get_tuner_gain(dev);
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nSetTunerIFGain(JNIEnv *env, jobject obj, jlong device, jint stage, jint gain) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    return rtlsdr_set_tuner_if_gain(dev, stage, gain);
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nSetTunerGainMode(JNIEnv *env, jobject obj, jlong device, jboolean manual) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    return rtlsdr_set_tuner_gain_mode(dev, manual);
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nSetSampleRate(JNIEnv *env, jobject obj, jlong device, jint rate) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    return rtlsdr_set_sample_rate(dev, rate);
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nGetSampleRate(JNIEnv *env, jobject obj, jlong device) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    return rtlsdr_get_sample_rate(dev);
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nSetTestmode(JNIEnv *env, jobject obj, jlong device, jboolean on) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    return rtlsdr_set_testmode(dev, on);
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nSetAgcMode(JNIEnv *env, jobject obj, jlong device, jboolean on) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    return rtlsdr_set_agc_mode(dev, on);
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nSetDirectSampling(JNIEnv *env, jobject obj, jlong device, jint mode) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    return rtlsdr_set_direct_sampling(dev, mode);
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nGetDirectSampling(JNIEnv *env, jobject obj, jlong device) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    return rtlsdr_get_direct_sampling(dev);
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nSetOffsetTuning(JNIEnv *env, jobject obj, jlong device, jboolean on) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    return rtlsdr_set_offset_tuning(dev, on);
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nGetOffsetTuning(JNIEnv *env, jobject obj, jlong device) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    return rtlsdr_get_offset_tuning(dev);
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nSetBiasTee(JNIEnv *env, jobject obj, jlong device, jboolean on) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    return rtlsdr_set_bias_tee(dev, on);
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nSetBiasTeeGpio(JNIEnv *env, jobject obj, jlong device, jint gpio, jboolean on) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    return rtlsdr_set_bias_tee_gpio(dev, gpio, on);
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nResetBuffer(JNIEnv *env, jobject obj, jlong device) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    return rtlsdr_reset_buffer(dev);
}

JNIEXPORT jint JNICALL Java_net_ellie_bolt_jni_rtlsdr_RTLSDR_nReadSync(JNIEnv *env, jobject obj, jlong device, jbyteArray buffer, jint length, jintArray n_read) {
    rtlsdr_dev_t *dev = (rtlsdr_dev_t *)(uintptr_t)device;
    jbyte *bufferBytes = (*env)->GetByteArrayElements(env, buffer, NULL);
    int readBytes = 0;
    int result = rtlsdr_read_sync(dev, (void *)bufferBytes, length, &readBytes);
    (*env)->ReleaseByteArrayElements(env, buffer, bufferBytes, 0);
    if (n_read != NULL) {
        jint readBytes_jint = readBytes;
        (*env)->SetIntArrayRegion(env, n_read, 0, 1, &readBytes_jint);
    }
    return result;
}
