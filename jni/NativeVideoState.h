#ifndef __NATIVE_VIDEO_STATE_H
#define __NATIVE_VIDEO_STATE_H

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>

typedef struct NativeVideoState {
	AVFormatContext *pFormatCtx;
	AVCodecContext *pCodecCtx;
	int videoStreamIdx;
	AVCodec *pCodec;

	AVPacket currPacket;
	AVFrame *currFrameYUV;
} NativeVideoState;

#endif // __NATIVE_VIDEO_STATE_H
