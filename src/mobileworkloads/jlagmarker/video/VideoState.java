package mobileworkloads.jlagmarker.video;

import java.util.ArrayList;

public class VideoState {

	private static final int MAX_AUTO_HISTORY_DEPTH = 50;
	private static final int MAX_RECORDING_DEPTH = 50000;

	private String videoFileName;

	private float frameRate;
	private float timePerFrameS;
	private long timePerFrameNS;

	/** the current frame id of the native video stream */
	private int nativeCurrFrameId = 0;
	/** the start time of the current frame in the native video stream */
	private long nativeVideoTimeNS = 0;

	private long pNativeVideoState;

	private boolean endOfStream;

	private final ArrayList<VideoFrame> frameHistory;
	private int historyPos; // greater than 0 if we have currently skipped backwards in the video
	
	private boolean isRecording;

	public VideoState(String fileName) {

		videoFileName = fileName;
		endOfStream = false;

		frameHistory = new ArrayList<VideoFrame>();
		historyPos = 0;
		
		isRecording = false;

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

	private VideoFrame getCurrFrame() {
		if(frameHistory.isEmpty()) return null;
		else return frameHistory.get(historyPos);
	}

	private void setCurrFrame(VideoFrame currFrame) {
		frameHistory.add(0, currFrame);

		int maxDepth = isRecording ? MAX_RECORDING_DEPTH : MAX_AUTO_HISTORY_DEPTH;
		while(frameHistory.size() > maxDepth) {
			frameHistory.remove(frameHistory.size() - 1);
			
			System.out.println("JDEBUG: SIZE: " + frameHistory.size() + " HEAD: "
					+ frameHistory.get(0).videoFrameId + " TAIL: "
					+ frameHistory.get(frameHistory.size() - 1).videoFrameId);
		}
		frameHistory.trimToSize();
	}

	public float getFrameRate() {
		return frameRate;
	}

	private void updateFrameCounter() {
		nativeCurrFrameId++;
		nativeVideoTimeNS += timePerFrameNS;
	}

	public VideoFrame decodeNextVideoFrame() {
		if(historyPos == 0) {
			if (isEndOfStream() || !lnativeDecodeNextVideoFrame()) {
				if (isEndOfStream()) {
					System.out.println("VIDEO: Video state reached the end of the stream.");
					return getCurrFrame().clone();
				} else {
					throw new RuntimeException("Decoding of next video frame failed.");
			 	}
			}
	
			JRGBFrameBuffer buff = new JRGBFrameBuffer();
	
			if (!lnativeExtractCurrFrame(buff))
				throw new RuntimeException("Converting current video frame to RGB buffer failed.");
	
			setCurrFrame(new VideoFrame(nativeVideoTimeNS / 1000, timePerFrameNS / 1000,
					nativeCurrFrameId, new RGBImage(buff)));
	
			return getCurrFrame().clone();
			
		} else {
			historyPos--;
			return getCurrFrame().clone();
		}
	}

	public void dumpVideoFormat() {
		lnativeDumpVideoFormat();
	}

	@Override
	public String toString() {
		StringBuilder bld = new StringBuilder();
		
		bld.append("Current Frame: ").append(getCurrFrame()).append("\n")
		.append("History Length: ").append(frameHistory.size()).append("\n")
		.append("History Pointer: ").append(historyPos).append("\n")
		.append("History Bounds: ").append(frameHistory.get(frameHistory.size() - 1).videoFrameId)
		.append(" --> ").append(frameHistory.get(0).videoFrameId).append("\n")
		.append("Is recording: ").append(isRecording);
		
		return bld.toString();
	}

	public VideoFrame extractCurrentFrame() {
		return getCurrFrame() != null ? getCurrFrame().clone() : null;
	}

	public void skipBackwards(int frameOffset) {
		if (frameOffset < 0 || frameOffset >= frameHistory.size())
			throw new IllegalArgumentException("Video frame history lookup out of range: " + frameOffset);
		
		historyPos = frameOffset;
	}
	
	public VideoFrame getFrameFromHistory(int frameOffset) {
		if (frameOffset < 0 || frameOffset >= frameHistory.size())
			throw new IllegalArgumentException("Video frame history lookup out of range: " + frameOffset);
		
		return frameHistory.get(frameOffset).clone();
	}
	
	public void startRecording() {
		isRecording = true;
		lnativeStartRecording();
	}
	
	public void stopRecording() {
		isRecording = false;
		while(frameHistory.size() > MAX_AUTO_HISTORY_DEPTH) {
			frameHistory.remove(frameHistory.size() - 1);
			
			System.out.println("JDEBUG: SIZE: " + frameHistory.size() + " HEAD: "
					+ frameHistory.get(0).videoFrameId + " TAIL: "
					+ frameHistory.get(frameHistory.size() - 1).videoFrameId);
		}
		frameHistory.trimToSize();
		System.gc();
		lnativeStopRecording();
	}

	public boolean isEndOfStream() {
		return historyPos == 0 && endOfStream;
	}

	private native boolean lnativeAllocVideoState();

	private native void lnativeFreeVideoState();

	private native boolean lnativeDecodeNextVideoFrame();

	private native boolean lnativeExtractCurrFrame(JRGBFrameBuffer currFrame);

	private native void lnativeDumpVideoFormat();
	
	
	public native void lnativeStartRecording();
	
	public native void lnativeStopRecording();
	
	public native void lnativeSkipBackwards(int frameOffset);
	
	public native VideoFrame lnativeGetFrameFromHistory(int frameOffset);

	static {
		System.loadLibrary("VideoState");
	}
}
