/**
*
* Copyright 2017 Volker Seeker <volker@seekerscience.co.uk>.
*
* This file is part of JLagmarker.
*
* JLagmarker is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* JLagmarker is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with JLagmarker. If not, see <http://www.gnu.org/licenses/>.
*
*/

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

	NativeRGBBuffer *nbuffer;

	struct NativeVideoFrame* next;
	struct NativeVideoFrame* prev;
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
