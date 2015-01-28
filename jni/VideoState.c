#include <jni.h>
#include <stdio.h>
#include <stdbool.h>

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/avutil.h>
#include <libavutil/mathematics.h>

#include <libswscale/swscale.h>

#include "VideoState.h"
#include "NativeVideoState.h"

#ifdef __cplusplus
extern "C" {
#endif

static AVCodecContext* find_video_stream(AVFormatContext* pFormatCtx, int* videoStream) {
	int i;
	AVCodecContext *pCodecCtx;

	// Find the first video stream
	*videoStream = -1;
	for (i = 0; i < pFormatCtx->nb_streams; i++)
		if (pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
			*videoStream = i;
			break;
		}
	if (*videoStream == -1)
		return NULL; // Didn't find a video stream

	// Get a pointer to the codec context for the video stream
	pCodecCtx = pFormatCtx->streams[*videoStream]->codec;

	return pCodecCtx;
}

static AVCodec* find_codec(AVCodecContext *pCodecCtx) {
	AVCodec *pCodec;

	// Find the decoder for the video stream
	pCodec = avcodec_find_decoder(pCodecCtx->codec_id);
	if (pCodec == NULL) {
		fprintf(stderr, "Unsupported codec!\n");
		return NULL;
	}
	// Open codec
	if (avcodec_open2(pCodecCtx, pCodec, NULL) < 0) {
		fprintf(stderr, "Could not open codec!\n");
		return NULL;
	}

	return pCodec;
}

JNIEXPORT jboolean JNICALL Java_VideoState_lnativeAllocVideoState(JNIEnv *env, jobject videoState) {
	jclass videoStateClass = (*env)->GetObjectClass(env, videoState);
	jfieldID fidNativeVideoState = (*env)->GetFieldID(env, videoStateClass, "pNativeVideoState", "J");
	jfieldID fidVideoFileName = (*env)->GetFieldID(env, videoStateClass, "videoFileName", "Ljava/lang/String;");
	jfieldID fidFrameRate = (*env)->GetFieldID(env, videoStateClass, "frameRate", "F");
	jfieldID fidTimePerFrameS = (*env)->GetFieldID(env, videoStateClass, "timePerFrameS", "F");
	jfieldID fidTimePerFrameNS = (*env)->GetFieldID(env, videoStateClass, "timePerFrameNS", "J");
	if (!fidFrameRate || !fidTimePerFrameS || !fidTimePerFrameNS || !fidVideoFileName || !fidNativeVideoState) {
		fprintf(stderr, "Given video state has unexpected format!\n");
		return false;
	}

	NativeVideoState* nVideoState = (NativeVideoState*) malloc(sizeof(NativeVideoState));
	(*env)->SetLongField(env, videoState, fidNativeVideoState, (long)nVideoState);

	jstring videoFileName = (*env)->GetObjectField(env, videoState, fidVideoFileName);
	const char *cVideoFileName = (*env)->GetStringUTFChars(env, videoFileName, NULL);

	av_register_all();

	// Open video file
	nVideoState->pFormatCtx = avformat_alloc_context();
	if (avformat_open_input(&nVideoState->pFormatCtx, cVideoFileName, NULL, NULL ) != 0) {
		fprintf(stderr, "Could not open video file: %s\n", cVideoFileName);
		return false;
	}

	// Retrieve stream information
	if (avformat_find_stream_info(nVideoState->pFormatCtx, NULL ) < 0) {
		fprintf(stderr, "Couldn't find stream information!\n");
		return false;
	}

	nVideoState->videoStreamIdx = -1;
	nVideoState->pCodecCtx = find_video_stream(nVideoState->pFormatCtx, &nVideoState->videoStreamIdx);
	if (nVideoState->pCodecCtx == NULL ) {
		fprintf(stderr, "Didn't find a video stream!\n");
		return false;
	}

	nVideoState->pCodec = find_codec(nVideoState->pCodecCtx);
	if (nVideoState->pCodec == NULL ) {
		fprintf(stderr, "Didn't find a codec!\n");
		return false;
	}

	double frameRate = av_q2d(nVideoState->pFormatCtx->streams[nVideoState->videoStreamIdx]->avg_frame_rate);
	double timePerFrameS = 1 / frameRate;
	(*env)->SetFloatField(env, videoState, fidFrameRate, frameRate);
	(*env)->SetFloatField(env, videoState, fidTimePerFrameS, timePerFrameS);
	(*env)->SetLongField(env, videoState, fidTimePerFrameNS, timePerFrameS * 1000000000);

	// Allocate video frames
	nVideoState->currFrameYUV = avcodec_alloc_frame(); //av_frame_alloc();
	if (nVideoState->currFrameYUV == NULL ) {
		fprintf(stderr, "YUVFrame allocation failed!\n");
		return false;
	}

	return true;
}

JNIEXPORT void JNICALL Java_VideoState_lnativeFreeVideoState(JNIEnv *env, jobject videoState) {
	jclass videoStateClass = (*env)->GetObjectClass(env, videoState);
	jfieldID fidNativeVideoState = (*env)->GetFieldID(env, videoStateClass, "pNativeVideoState", "J");
	if (!fidNativeVideoState) {
		fprintf(stderr, "Given video state has unexpected format!\n");
		return;
	}

	NativeVideoState* nVideoState = (NativeVideoState*)(*env)->GetLongField(env, videoState, fidNativeVideoState);

	nVideoState->videoStreamIdx = -1;

	// Close the codec
	if(nVideoState->pCodecCtx)
		avcodec_close(nVideoState->pCodecCtx);

	// Close the video file
	if(nVideoState->pFormatCtx)
		avformat_free_context(nVideoState->pFormatCtx);

	// Free the YUV frame
	if(nVideoState->currFrameYUV)
		av_free(nVideoState->currFrameYUV);

	nVideoState->pCodec = NULL;
}

JNIEXPORT jboolean JNICALL Java_VideoState_lnativeDecodeNextVideoFrame(JNIEnv *env, jobject videoState) {
	jclass videoStateClass = (*env)->GetObjectClass(env, videoState);
	jfieldID fidNativeVideoState = (*env)->GetFieldID(env, videoStateClass, "pNativeVideoState", "J");
	jmethodID midUpdateFrameCounter = (*env)->GetMethodID(env, videoStateClass, "updateFrameCounter", "()V");
	if (!fidNativeVideoState || !midUpdateFrameCounter) {
		fprintf(stderr, "Given video state has unexpected format!\n");
		return false;
	}
	NativeVideoState* nVideoState = (NativeVideoState*)(*env)->GetLongField(env, videoState, fidNativeVideoState);

	int frameFinished = 0;

	while(av_read_frame(nVideoState->pFormatCtx, &nVideoState->currPacket) >= 0) {

		// Is this a packet from the video stream?
		if (nVideoState->currPacket.stream_index == nVideoState->videoStreamIdx) {
			// Decode video frame
			avcodec_decode_video2(nVideoState->pCodecCtx, nVideoState->currFrameYUV, &frameFinished, &nVideoState->currPacket);

			// Did we get a video frame?
			if (frameFinished) {
			   (*env)->CallVoidMethod(env, videoState, midUpdateFrameCounter);
				return true;
			}
		}
	}

	return false;
}

#ifdef __cplusplus
}
#endif
