#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>

#include <errno.h>
#include <string.h>
#include <stdint.h>

#include "LagmarkerNative.h"

JNIEXPORT jint JNICALL Java_LagmarkerNative_lnativeEcho
  (JNIEnv *env, jobject thisObj, jint x) {
   fprintf(stdout, "Native echo: %d\n", x);
   return x;
}

JNIEXPORT jboolean JNICALL Java_LagmarkerNative_lnativeLoadRGBFrameBuffer
  (JNIEnv *env, jobject thisObj, jstring filename, jobject targetBuff) {
	if(filename == NULL) {
		fprintf(stderr, "Given filename must not be null!\n");
		return false;
	}

	if(targetBuff == NULL) {
		fprintf(stderr, "Given target buffer must not be null!\n");
		return false;
	}

	const char *cfilename = (*env)->GetStringUTFChars(env, filename, NULL);
	FILE *file = fopen(cfilename, "r");
	if (file == NULL) {
		fprintf(stderr, "Failed to open rgb frame file [%s]: %s\n", cfilename, strerror(errno));
		return false;
	}

	// reading frame header (pixel width and height)
	unsigned int i = 0;
	ssize_t read = -1;
	bool headerSkipped = false;
	char width[20];
	char height[20];
	char c;
	int count = 0;
	while(!headerSkipped) {
		read = fread(&c, sizeof(char), 1, file);
		if (read == -1) {
			fprintf(stderr, "Error reading from file, %s\n", strerror(errno));
			fclose(file);
			return false;
		} else if (read == 0) {
			fprintf(stderr, "Reading terminated before header was through.\n");
			fclose(file);
			return false;
		}

		switch(count) {
			case 1:
				if(c == ' ') {
					width[i] = '\0';
					i = 0;
					count++;
					break;
				}
				width[i++] = c;
				break;
			case 2:
				if(c == '\n') {
					height[i] = '\0';
					i = 0;
					count++;
					break;
				}
				height[i++] = c;
				break;
			default:
				if(c == '\n') count++;
				break;
		}

		if(count == 4) {
			headerSkipped = true;
		}
	}

	// setting width and height in java buffer object

	jclass rgbBuffClass = (*env)->GetObjectClass(env, targetBuff);
	jfieldID fidWidth = (*env)->GetFieldID(env, rgbBuffClass, "width", "I");
	jfieldID fidHeight = (*env)->GetFieldID(env, rgbBuffClass, "height", "I");
	jfieldID fidBuffSize = (*env)->GetFieldID(env, rgbBuffClass, "buffSize", "I");
	jfieldID fidbuffer = (*env)->GetFieldID(env, rgbBuffClass, "buffer", "[B");
	if (!fidWidth || !fidHeight || !fidBuffSize || !fidbuffer) {
		fprintf(stderr, "Given target buffer has unexpected format!\n");
		return false;
	}

	jint buffWidth = atoi(&width[0]);
	jint buffHeight = atoi(&height[0]);
	jsize buffSize = buffWidth * buffHeight * 3; // assume 3 colour channels per pixel
	(*env)->SetIntField(env, targetBuff, fidWidth, buffWidth);
	(*env)->SetIntField(env, targetBuff, fidHeight, buffHeight);
	(*env)->SetIntField(env, targetBuff, fidBuffSize, buffSize);

	jbyteArray jbuffer = (*env)->NewByteArray(env, buffSize);

	jbyte* nativeBuffer = (jbyte*) calloc(buffSize, sizeof(jbyte));
	for(i = 0; i < buffSize; i++) {
		read = fread(&(nativeBuffer[i]), sizeof(uint8_t), 1, file);

		if (read == -1) {
			fprintf(stderr, "Error reading from file, %s\n", strerror(errno));
			fclose(file);
			return false;
		} else if (read == 0) {
			fprintf(stderr, "Reading terminated. Read %u events out of %d.\n", i, buffSize);
			fclose(file);
			return false;
		}
	}

	fclose(file);

	// put native buffer into java object
	(*env)->SetByteArrayRegion(env, jbuffer, 0, buffSize, nativeBuffer);
	(*env)->SetObjectField(env, targetBuff, fidbuffer, jbuffer);

	free(nativeBuffer);
	return true;
}

JNIEXPORT jboolean JNICALL Java_LagmarkerNative_lnativeSaveRGBFrameBuffer
	(JNIEnv *env, jobject thisObj, jstring filename, jobject srcBuffer) {

	const char *cfilename = (*env)->GetStringUTFChars(env, filename, NULL);
	FILE *pFile = fopen(cfilename, "wb");
	if (pFile == NULL) return false;

	jclass rgbBuffClass = (*env)->GetObjectClass(env, srcBuffer);
	jfieldID fidWidth = (*env)->GetFieldID(env, rgbBuffClass, "width", "I");
	jfieldID fidHeight = (*env)->GetFieldID(env, rgbBuffClass, "height", "I");
	jfieldID fidbuffer = (*env)->GetFieldID(env, rgbBuffClass, "buffer", "[B");
	if (!fidWidth || !fidHeight || !fidbuffer) {
		fprintf(stderr, "Given source buffer has unexpected format!\n");
		return false;
	}

	// Write header
	jint buffWidth = (*env)->GetIntField(env, srcBuffer, fidWidth);
	jint buffHeight = (*env)->GetIntField(env, srcBuffer, fidHeight);
	fprintf(pFile, "P6\n%d %d\n255\n", buffWidth, buffHeight);


	// Write pixel data
	jbyteArray jbytebuffer = (jbyteArray) (*env)->GetObjectField(env, srcBuffer, fidbuffer);
	jbyte *data = (*env)->GetByteArrayElements(env, jbytebuffer, NULL);
	uint y;
	for (y = 0; y < buffHeight; y++)
		fwrite(data + y * buffWidth * 3, 1, buffWidth * 3, pFile);

	(*env)->ReleaseByteArrayElements(env, jbytebuffer, data, 0);

	fclose(pFile);
	return true;
}

