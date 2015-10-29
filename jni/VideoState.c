#include <jni.h>
#include <jni_md.h>
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/log.h>
#include <libavutil/mem.h>
#include <libavutil/pixfmt.h>
#include <libavutil/rational.h>
#include <libswscale/swscale.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

#include "NativeVideoState.h"

#ifdef __cplusplus
extern "C" {
#endif

#define MAX_RECORDING_DEPTH 10000
#define MAX_AUTO_HISTORY_DEPTH 50


static NativeRGBBuffer* create_nrgbBuffer(int width, int height, uint8_t *buffer, AVFrame *pFrameRGB) {
	NativeRGBBuffer* nrgbBuffer = (NativeRGBBuffer*) malloc(sizeof(NativeRGBBuffer));
	nrgbBuffer->width = width;
	nrgbBuffer->height = height;
	nrgbBuffer->buffSize = width * height * 3; // assume 3 colour channels per pixel;
	nrgbBuffer->buffer = buffer;
	nrgbBuffer->pFrameRGB = pFrameRGB;
	return nrgbBuffer;
}

static NativeVideoFrame* create_nativeFrame(long startTimeUS, long durationUS, int frameId, NativeRGBBuffer* img) {
	NativeVideoFrame* videoFrame = (NativeVideoFrame*) malloc(sizeof(NativeVideoFrame));
	videoFrame->startTimeUS = startTimeUS;
	videoFrame->endTimeUS = startTimeUS + durationUS;
	videoFrame->durationUS = durationUS;

	videoFrame->videoFrameId = frameId;

	videoFrame->nbuffer = img;

	videoFrame->next = NULL;
	videoFrame->prev = NULL;
	return videoFrame;
}

static void free_frameBuffer(NativeRGBBuffer* nrgbBuffer) {
	if(nrgbBuffer != NULL) {
		av_free(nrgbBuffer->buffer);
		av_free(nrgbBuffer->pFrameRGB);
		free(nrgbBuffer);
	}
}

static void free_videoFrame(NativeVideoFrame* frame) {
	if(frame != NULL) {
		free_frameBuffer(frame->nbuffer);
		free(frame);
	}
}

static NativeVideoFrame* dequeue_videoFrame(NativeVideoState* vstate) {
	NativeVideoFrame* res = NULL;
	if(vstate->frameHistoryHead != NULL) {
		res = vstate->frameHistoryHead;

		vstate->frameHistoryHead = res->next;

		if(res->next != NULL) {
			res->next->prev = NULL;
			res->next = NULL;
		} else {
			vstate->historyPos = vstate->frameHistoryTail = NULL;
		}

		vstate->historySize--;
	}
	return res;
}

static void enqueue_videoFrame(NativeVideoState* vstate, NativeVideoFrame* videoFrame) {
	if(videoFrame == NULL) return;

	if(vstate->frameHistoryHead == NULL && vstate->frameHistoryTail == NULL) {
		vstate->historyPos = vstate->frameHistoryHead = vstate->frameHistoryTail = videoFrame;
		vstate->historySize++;
	} else if(vstate->frameHistoryHead != NULL && vstate->frameHistoryTail != NULL) {
		vstate->frameHistoryTail->next = videoFrame;
		videoFrame->prev = vstate->frameHistoryTail;
		vstate->historyPos = vstate->frameHistoryTail = videoFrame;
		vstate->historySize++;
	} else {
		fprintf(stderr, "ERROR: Corrupted video frame queue!\n");
	}
}

static void clear_videoFrameHistory(NativeVideoState* vstate) {
	NativeVideoFrame* curr = dequeue_videoFrame(vstate);
	while(curr != NULL) {
		free_videoFrame(curr);
		curr = dequeue_videoFrame(vstate);
	}
}



static bool add_video_frame_to_history(NativeVideoFrame **nVideoFrame, NativeVideoState *vstate, uint8_t *buffer, AVFrame *pFrameRGB) {
	NativeRGBBuffer* nrgbBuffer = create_nrgbBuffer(vstate->pCodecCtx->width, vstate->pCodecCtx->height, buffer, pFrameRGB);
	if(nrgbBuffer == NULL) {
		fprintf(stderr, "Native RGB Buffer allocation failed!\n");
		return false;
	}
	*nVideoFrame = create_nativeFrame(vstate->nativeVideoTimeNS / 1000,
			vstate->timePerFrameNS / 1000, vstate->nativeCurrFrameId, nrgbBuffer);
	if(*nVideoFrame == NULL) {
		fprintf(stderr, "Native Video Frame allocation failed!\n");
		return false;
	}

	enqueue_videoFrame(vstate, *nVideoFrame);

	int maxDepth = vstate->isRecording ? MAX_RECORDING_DEPTH : MAX_AUTO_HISTORY_DEPTH;
	while(vstate->historySize > maxDepth) {
		NativeVideoFrame* vframe = dequeue_videoFrame(vstate);
		free_videoFrame(vframe);
	}

	return true;
}




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

JNIEXPORT jboolean JNICALL Java_mobileworkloads_jlagmarker_video_VideoState_lnativeAllocVideoState(JNIEnv *env, jobject videoState) {
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

	nVideoState->timePerFrameNS = timePerFrameS * 1000000000;

	// Allocate video frames
	nVideoState->currFrameYUV = avcodec_alloc_frame(); //av_frame_alloc();
	if (nVideoState->currFrameYUV == NULL ) {
		fprintf(stderr, "YUVFrame allocation failed!\n");
		return false;
	}

	nVideoState->nativeCurrFrameId = 0;
	nVideoState->nativeVideoTimeNS = 0;

	// init video frame history
	nVideoState->isRecording = false;
	nVideoState->historySize = 0;
	nVideoState->historyPos = NULL;
	nVideoState->frameHistoryHead = NULL;
	nVideoState->frameHistoryTail = NULL;

	return true;
}

JNIEXPORT void JNICALL Java_mobileworkloads_jlagmarker_video_VideoState_lnativeFreeVideoState(JNIEnv *env, jobject videoState) {
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

	// free native video state
	clear_videoFrameHistory(nVideoState);

	free(nVideoState);
}



static bool setJRGBbuffer(JNIEnv *env, jobject buff, jint width, jint height, jbyte* nativeBuff) {
	if(buff == NULL) {
		fprintf(stderr, "Given target buffer must not be null!\n");
		return false;
	}

	if(nativeBuff == NULL) {
		fprintf(stderr, "Given native source buffer must not be null!\n");
		return false;
	}

	jclass rgbBuffClass = (*env)->GetObjectClass(env, buff);
	jfieldID fidWidth = (*env)->GetFieldID(env, rgbBuffClass, "width", "I");
	jfieldID fidHeight = (*env)->GetFieldID(env, rgbBuffClass, "height", "I");
	jfieldID fidBuffSize = (*env)->GetFieldID(env, rgbBuffClass, "buffSize", "I");
	jfieldID fidbuffer = (*env)->GetFieldID(env, rgbBuffClass, "buffer", "[B");
	if (!fidWidth || !fidHeight || !fidBuffSize || !fidbuffer) {
		fprintf(stderr, "Given target buffer has unexpected format!\n");
		return false;
	}

	jsize buffSize = width * height * 3; // assume 3 colour channels per pixel
	(*env)->SetIntField(env, buff, fidWidth, width);
	(*env)->SetIntField(env, buff, fidHeight, height);
	(*env)->SetIntField(env, buff, fidBuffSize, buffSize);

	// put native buffer into java object
	jbyteArray jbuffer = (*env)->NewByteArray(env, buffSize);
	(*env)->SetByteArrayRegion(env, jbuffer, 0, buffSize, nativeBuff);
	(*env)->SetObjectField(env, buff, fidbuffer, jbuffer);

	return true;
}

static bool setJVideoFrame(JNIEnv *env, jobject jframeInfo, NativeVideoFrame* nframe) {
	if(jframeInfo == NULL) {
		fprintf(stderr, "Given target java frame info must not be null!\n");
		return false;
	}

	if(nframe == NULL) {
		fprintf(stderr, "Given native source frame must not be null!\n");
		return false;
	}

	jclass jvideoFrameInfoClass = (*env)->GetObjectClass(env, jframeInfo);
	jfieldID fidstartTimeUS = (*env)->GetFieldID(env, jvideoFrameInfoClass, "startTimeUS", "J");
	jfieldID fiddurationUS = (*env)->GetFieldID(env, jvideoFrameInfoClass, "durationUS", "J");
	jfieldID fidframeId = (*env)->GetFieldID(env, jvideoFrameInfoClass, "frameId", "I");
	if (!fidstartTimeUS || !fiddurationUS || !fidframeId ) {
		fprintf(stderr, "Given target videoFrame info has unexpected format!\n");
		return false;
	}

	(*env)->SetLongField(env, jframeInfo, fidstartTimeUS, nframe->startTimeUS);
	(*env)->SetLongField(env, jframeInfo, fiddurationUS, nframe->durationUS);
	(*env)->SetIntField(env, jframeInfo, fidframeId, nframe->videoFrameId);

	return true;
}

static AVFrame* allocate_RGB_frame(uint8_t ** buffer, AVCodecContext *pCodecCtx) {
	int numBytes;
	AVFrame* pFrameRGB = avcodec_alloc_frame();
	if (pFrameRGB == NULL )	return NULL;

	// Determine required buffer size and allocate buffer
	numBytes = avpicture_get_size(PIX_FMT_RGB24, pCodecCtx->width,
			pCodecCtx->height);
	*buffer = (uint8_t *) av_malloc(numBytes * sizeof(uint8_t));

	// Assign appropriate parts of buffer to image planes in pFrameRGB
	// Note that pFrameRGB is an AVFrame, but AVFrame is a superset
	// of AVPicture
	avpicture_fill((AVPicture *) pFrameRGB, *buffer, PIX_FMT_RGB24,
			pCodecCtx->width, pCodecCtx->height);

	return pFrameRGB;
}

static int img_convert(AVPicture* dst, enum PixelFormat dst_pix_fmt,
		AVPicture* src, enum PixelFormat pix_fmt, int width, int height) {
	int av_log = av_log_get_level();
	av_log_set_level(AV_LOG_QUIET);

	struct SwsContext *img_convert_ctx = sws_getContext(width, height, pix_fmt,
			width, height, dst_pix_fmt, SWS_BICUBIC, NULL, NULL, NULL );

	int result = sws_scale(img_convert_ctx, (const uint8_t* const*) src->data, src->linesize, 0, height, dst->data, dst->linesize);

	sws_freeContext(img_convert_ctx);

	av_log_set_level(av_log);

	return result;
}


/*
 * TODO
 * add extract frame from history native call
 * test test test
 */

JNIEXPORT jboolean JNICALL Java_mobileworkloads_jlagmarker_video_VideoState_lnativeDecodeAndExtractNextVideoFrame
  (JNIEnv *env, jobject videoState, jobject frame, jobject frameBuffer) {

	jclass videoStateClass = (*env)->GetObjectClass(env, videoState);
	jfieldID fidNativeVideoState = (*env)->GetFieldID(env, videoStateClass, "pNativeVideoState", "J");
	if (!fidNativeVideoState) {
		fprintf(stderr, "Given video state has unexpected format!\n");
		return false;
	}
	NativeVideoState* nVideoState = (NativeVideoState*)(*env)->GetLongField(env, videoState, fidNativeVideoState);

	NativeVideoFrame* nVideoFrame;
	if(nVideoState->historyPos == nVideoState->frameHistoryTail) {
		int frameFinished = 0;

		while(av_read_frame(nVideoState->pFormatCtx, &nVideoState->currPacket) >= 0) {

			// Is this a packet from the video stream?
			if (nVideoState->currPacket.stream_index == nVideoState->videoStreamIdx) {
				// Decode video frame
				avcodec_decode_video2(nVideoState->pCodecCtx, nVideoState->currFrameYUV, &frameFinished, &nVideoState->currPacket);

				// Did we get a video frame?
				if (frameFinished) {
				   	nVideoState->nativeCurrFrameId++;
				   	nVideoState->nativeVideoTimeNS += nVideoState->timePerFrameNS;
				   	break;
				}
			}
		}

		if(frameFinished) {
			// Allocate RGB video frames
			uint8_t *buffer;
			AVFrame *pFrameRGB = allocate_RGB_frame(&buffer, nVideoState->pCodecCtx);
			if (pFrameRGB == NULL) {
				fprintf(stderr, "RGBFrame allocation failed!\n");
				return false;
			}

			// Convert the begin frame image from its native format to RGB
			img_convert((AVPicture *) pFrameRGB, PIX_FMT_RGB24,
					(AVPicture*) nVideoState->currFrameYUV, nVideoState->pCodecCtx->pix_fmt,
					nVideoState->pCodecCtx->width, nVideoState->pCodecCtx->height);
			av_free_packet(&nVideoState->currPacket);

			if(!add_video_frame_to_history(&nVideoFrame, nVideoState, buffer, pFrameRGB)){
				fprintf(stderr, "Adding Native Video Frame to history failed!\n");
				return false;
			}


		} else {
			jfieldID fidEndOfStream = (*env)->GetFieldID(env, videoStateClass, "endOfStream", "Z");
			(*env)->SetBooleanField(env, videoState, fidEndOfStream, true);

			return false;
		}


	} else { // browsing history
		nVideoState->historyPos = nVideoState->historyPos->next;
		nVideoFrame = nVideoState->historyPos;
	}

	// fill in java objects with native ones
	if (!setJRGBbuffer(env, frameBuffer, nVideoFrame->nbuffer->width,
			nVideoFrame->nbuffer->height, (jbyte*) nVideoFrame->nbuffer->pFrameRGB->data[0])) {
		fprintf(stderr, "RGBFrameBuffer allocation failed!\n");
		return false;
	}

	if (!setJVideoFrame(env, frame, nVideoFrame)) {
		fprintf(stderr, "Java Video Frame allocation failed!\n");
		return false;
	}
	return true;
}

JNIEXPORT void JNICALL Java_mobileworkloads_jlagmarker_video_VideoState_lnativeStartRecording(JNIEnv *env, jobject videoState) {
	jclass videoStateClass = (*env)->GetObjectClass(env, videoState);
	jfieldID fidNativeVideoState = (*env)->GetFieldID(env, videoStateClass, "pNativeVideoState", "J");
	if (!fidNativeVideoState) fprintf(stderr, "Given video state has unexpected format!\n");

	NativeVideoState* nVideoState = (NativeVideoState*)(*env)->GetLongField(env, videoState, fidNativeVideoState);
	nVideoState->isRecording = true;
}

JNIEXPORT void JNICALL Java_mobileworkloads_jlagmarker_video_VideoState_lnativeStopRecording(JNIEnv *env, jobject videoState) {
	jclass videoStateClass = (*env)->GetObjectClass(env, videoState);
	jfieldID fidNativeVideoState = (*env)->GetFieldID(env, videoStateClass, "pNativeVideoState", "J");
	if (!fidNativeVideoState) fprintf(stderr, "Given video state has unexpected format!\n");

	NativeVideoState* nVideoState = (NativeVideoState*)(*env)->GetLongField(env, videoState, fidNativeVideoState);
	nVideoState->isRecording = false;

	while(nVideoState->historySize > MAX_AUTO_HISTORY_DEPTH) {
		NativeVideoFrame* vframe = dequeue_videoFrame(nVideoState);
		free_videoFrame(vframe);
	}
}

JNIEXPORT bool JNICALL Java_mobileworkloads_jlagmarker_video_VideoState_lnativeSkipBackwards
	(JNIEnv *env, jobject videoState, jint frameOffset, jobject frame, jobject frameBuffer) {
	jclass videoStateClass = (*env)->GetObjectClass(env, videoState);
	jfieldID fidNativeVideoState = (*env)->GetFieldID(env, videoStateClass, "pNativeVideoState", "J");
	if (!fidNativeVideoState) fprintf(stderr, "Given video state has unexpected format!\n");

	NativeVideoState* nVideoState = (NativeVideoState*)(*env)->GetLongField(env, videoState, fidNativeVideoState);

	if(frameOffset < 0 || frameOffset >= nVideoState->historySize ||
			nVideoState->historyPos != nVideoState->frameHistoryTail) { // already rewinded
		fprintf(stderr, "ERROR: Video frame history rewind out of range: %d\n", frameOffset);
		return false;
	}

	while(frameOffset > 0) {
		nVideoState->historyPos = nVideoState->historyPos->prev;
		frameOffset--;
	}

	// fill in java objects with native ones
	NativeVideoFrame* nVideoFrame = nVideoState->historyPos;
	if (!setJRGBbuffer(env, frameBuffer, nVideoFrame->nbuffer->width,
			nVideoFrame->nbuffer->height, (jbyte*) nVideoFrame->nbuffer->pFrameRGB->data[0])) {
		fprintf(stderr, "RGBFrameBuffer allocation failed!\n");
		return false;
	}

	if (!setJVideoFrame(env, frame, nVideoFrame)) {
		fprintf(stderr, "Java Video Frame allocation failed!\n");
		return false;
	}

	return true;
}

JNIEXPORT jobject JNICALL Java_mobileworkloads_jlagmarker_video_VideoState_lnativeGetFrameFromHistory(JNIEnv *env, jobject videoState) {
	return NULL;
}


JNIEXPORT void JNICALL Java_mobileworkloads_jlagmarker_video_VideoState_lnativeDumpVideoFormat(JNIEnv *env, jobject videoState) {
	jclass videoStateClass = (*env)->GetObjectClass(env, videoState);
	jfieldID fidNativeVideoState = (*env)->GetFieldID(env, videoStateClass, "pNativeVideoState", "J");
	jfieldID fidVideoFileName = (*env)->GetFieldID(env, videoStateClass, "videoFileName", "Ljava/lang/String;");
	if (!fidNativeVideoState || !fidVideoFileName) {
		fprintf(stderr, "Given video state has unexpected format!\n");
		return;
	}
	NativeVideoState* nVideoState = (NativeVideoState*)(*env)->GetLongField(env, videoState, fidNativeVideoState);

	jstring videoFileName = (*env)->GetObjectField(env, videoState, fidVideoFileName);
	const char *cVideoFileName = (*env)->GetStringUTFChars(env, videoFileName, NULL);

	av_dump_format(nVideoState->pFormatCtx, 0, cVideoFileName, 0);
}

#ifdef __cplusplus
}
#endif
