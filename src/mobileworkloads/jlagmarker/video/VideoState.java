package mobileworkloads.jlagmarker.video;

import java.util.LinkedList;

public class VideoState {

	private static final int MAX_HISTORY_DEPTH = 50;

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

	private final LinkedList<VideoFrame> frameHistory;
	private int historyPos; // greater than 0 if we have currently skipped backwards in the video

	public VideoState(String fileName) {

		videoFileName = fileName;
		endOfStream = false;

		frameHistory = new LinkedList<VideoFrame>();
		historyPos = 0;

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
		frameHistory.addFirst(currFrame);
		if (frameHistory.size() > MAX_HISTORY_DEPTH)
			frameHistory.removeLast();
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
			if (!lnativeDecodeNextVideoFrame()) {
				if (isEndOfStream())
					return null;
				else
					throw new RuntimeException(
							"Decoding of next video frame failed.");
			}
	
			JRGBFrameBuffer buff = new JRGBFrameBuffer();
	
			if (!lnativeExtractCurrFrame(buff))
				throw new RuntimeException(
						"Converting current video frame to RGB buffer failed.");
	
			setCurrFrame(new VideoFrame(nativeVideoTimeNS / 1000, timePerFrameNS / 1000,
					nativeCurrFrameId, buff));
	
			return getCurrFrame().clone();
			
		} else {
			historyPos--;
			return getCurrFrame();
		}
	}

	public void dumpVideoFormat() {
		lnativeDumpVideoFormat();
	}

	@Override
	public String toString() {
		return getCurrFrame().toString();
	}

	public VideoFrame extractCurrentFrame() {
		return getCurrFrame() != null ? getCurrFrame().clone() : null;
	}

	public void skipBackwards(int frameOffset) {
		if (frameOffset < 0 || frameOffset >= frameHistory.size())
			throw new IllegalArgumentException("Video frame history lookup out of range: " + frameOffset);
		
		historyPos = frameOffset;
	}

	public boolean isEndOfStream() {
		return endOfStream;
	}

	private native boolean lnativeAllocVideoState();

	private native void lnativeFreeVideoState();

	private native boolean lnativeDecodeNextVideoFrame();

	private native boolean lnativeExtractCurrFrame(JRGBFrameBuffer currFrame);

	private native void lnativeDumpVideoFormat();

	static {
		System.loadLibrary("VideoState");
	}
}
