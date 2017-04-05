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
package mobileworkloads.jlagmarker.video;

import java.io.IOException;

import cz.adamh.utils.NativeUtils;



public class VideoState {

	private static final int MAX_AUTO_HISTORY_DEPTH = 50;
	private static final int MAX_RECORDING_DEPTH = 50000;

	private String videoFileName;

	private float frameRate;
	private float timePerFrameS;
	private long timePerFrameNS;

	private long pNativeVideoState;

	private VideoFrame currentFrame;
	
	private boolean endOfStream;

	public VideoState(String fileName) {
		videoFileName = fileName;
		currentFrame = null;
		endOfStream = false;

		if (!lnativeAllocVideoState()) {
			lnativeFreeVideoState();
			throw new RuntimeException(
					"Allocation of native video state failed.");
		}
	}

	@Override
	protected void finalize() throws Throwable {
		lnativeFreeVideoState();
		super.finalize();
	}

	public String getVideoFileName() {
		return videoFileName;
	}

	public float getFrameRate() {
		return frameRate;
	}

	public VideoFrame decodeNextVideoFrame() {
		if (isEndOfStream()) {
			
		} else {
			JRGBFrameBuffer buff = new JRGBFrameBuffer();
			NativeVideoFrameInfo frameInfo = new NativeVideoFrameInfo();
			if(!lnativeDecodeAndExtractNextVideoFrame(frameInfo, buff)) {
				if(isEndOfStream()) {
					System.out.println("VIDEO: Video state reached the end of the stream.");
				} else {
					throw new RuntimeException("Decoding of next video frame failed.");
				}
			} else {
				currentFrame = new VideoFrame(frameInfo.startTimeUS, frameInfo.durationUS,
						frameInfo.frameId, new RGBImage(buff));
			}
		}
		
//		JLagmarkerMain.memOutput();
		
		return extractCurrentFrame();
	}
	
	public VideoFrame extractCurrentFrame() {
		return currentFrame != null ? currentFrame.clone() : null;
	}

	public void dumpVideoFormat() {
		lnativeDumpVideoFormat();
	}

	@Override
	public String toString() {
		StringBuilder bld = new StringBuilder();
		bld.append("Current Frame: ").append(currentFrame);
		return bld.toString();
	}

	public void skipBackwards(int frameOffset) {
		JRGBFrameBuffer buff = new JRGBFrameBuffer();
		NativeVideoFrameInfo frameInfo = new NativeVideoFrameInfo();
		if(!lnativeSkipBackwards(frameOffset, frameInfo, buff))
			throw new IllegalArgumentException("Video frame history skip out of range: " + frameOffset);
		
		currentFrame = new VideoFrame(frameInfo.startTimeUS, frameInfo.durationUS,
				frameInfo.frameId, new RGBImage(buff));
	}
	
	public VideoFrame getFrameFromHistory(int frameOffset) {
		JRGBFrameBuffer buff = new JRGBFrameBuffer();
		NativeVideoFrameInfo frameInfo = new NativeVideoFrameInfo();
		if(!lnativeGetFrameFromHistory(frameOffset, frameInfo, buff))
			throw new IllegalArgumentException("Video frame history lookup out of range: " + frameOffset);
		
		return new VideoFrame(frameInfo.startTimeUS, frameInfo.durationUS,	frameInfo.frameId, new RGBImage(buff));
	}
	
	public void startRecording() {
		lnativeStartRecording();
	}
	
	public void stopRecording() {
		lnativeStopRecording();
	}

	public boolean isEndOfStream() {
		return endOfStream;
	}

	
	
	private native boolean lnativeAllocVideoState();

	private native void lnativeFreeVideoState();
	
	private native boolean lnativeDecodeAndExtractNextVideoFrame(NativeVideoFrameInfo frameInfo, JRGBFrameBuffer frameBuffer);
	
	private native void lnativeDumpVideoFormat();
	
	public native void lnativeStartRecording();
	
	public native void lnativeStopRecording();
	
	public native boolean lnativeSkipBackwards(int frameOffset, NativeVideoFrameInfo frameInfo, JRGBFrameBuffer frameBuffer);
	
	public native boolean lnativeGetFrameFromHistory(int frameOffset, NativeVideoFrameInfo frameInfo, JRGBFrameBuffer frameBuffer);

	static {
	    try {
			System.loadLibrary("VideoState");
	    } catch (UnsatisfiedLinkError e) {
	        try {
	            NativeUtils.loadLibraryFromJar("/libVideoState.so");
	        } catch (IOException e1) {
	            throw new RuntimeException(e1);
	        }
	    }
	}
}
