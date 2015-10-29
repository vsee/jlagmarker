package mobileworkloads.jlagmarker.video;

import mobileworkloads.jlagmarker.JLagmarkerMain;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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
			throw new IllegalArgumentException("Video frame history lookup out of range: " + frameOffset);
		
		currentFrame = new VideoFrame(frameInfo.startTimeUS, frameInfo.durationUS,
				frameInfo.frameId, new RGBImage(buff));
	}
	
	public VideoFrame getFrameFromHistory(int frameOffset) {
		//TODO implement me
		throw new NotImplementedException();
//		if (frameOffset < 0 || frameOffset >= frameHistory.size())
//			throw new IllegalArgumentException("Video frame history lookup out of range: " + frameOffset);
//		
//		return frameHistory.get(frameOffset).clone();
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
	
	public native VideoFrame lnativeGetFrameFromHistory(int frameOffset);

	static {
		System.loadLibrary("VideoState");
	}
}
