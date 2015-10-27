#ifndef __NATIVE_VIDEO_STATE_H
#define __NATIVE_VIDEO_STATE_H

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>

typedef struct NativeRGBBuffer {
	int width;
	int height;
	int buffSize;

	uint8_t *buffer;
	AVFrame *pFrameRGB;
} NativeRGBBuffer;

typedef struct NativeVideoFrame {
	long startTimeUS;
	long endTimeUS;
	long durationUS;

	int videoFrameId;

	NativeRGBBuffer *frameImg;

	struct NativeVideoFrame* next;
} NativeVideoFrame;


typedef struct NativeVideoState {
	AVFormatContext *pFormatCtx;
	AVCodecContext *pCodecCtx;
	int videoStreamIdx;
	AVCodec *pCodec;

	AVPacket currPacket;
	AVFrame *currFrameYUV;

	/** the current frame id of the native video stream */
	int nativeCurrFrameId;
	/** the start time of the current frame in the native video stream */
	long nativeVideoTimeNS;
	long timePerFrameNS;

	bool isRecording;
	int historySize;
	NativeVideoFrame* historyPos; // not equal to Tail if we have currently skipped backwards in the video
	NativeVideoFrame* frameHistoryHead; // always points to the oldest frame in the history
	NativeVideoFrame* frameHistoryTail; // always points to the youngest frame in the history
} NativeVideoState;


#endif // __NATIVE_VIDEO_STATE_H
