#include <jni.h>
#include <stdio.h>
#include "LagmarkerNative.h"

JNIEXPORT jint JNICALL Java_LagmarkerNative_lnativeEcho(JNIEnv *env, jobject thisObj, jint x) {
   fprintf(stdout, "Native echo: %d\n", x);
   return x;
}
