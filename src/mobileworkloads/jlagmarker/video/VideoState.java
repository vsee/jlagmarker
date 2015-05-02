package mobileworkloads.jlagmarker.video;
import mobileworkloads.jlagmarker.video.JRGBFrameBuffer;
import mobileworkloads.jlagmarker.video.VideoFrame;

public class VideoState {

	private String videoFileName;
	
	private float frameRate;
	private float timePerFrameS;
	private long timePerFrameNS;
	
	/** the current frame of the video stream */
	private int currFrameId = 0;
	/** the start time of the current frame */
	private long currTimeNS = 0;
	
	private long pNativeVideoState;
	
	private VideoFrame currFrame;
	
	public VideoState(String fileName) {
		
		videoFileName = fileName;
		currFrame = null;
		
		if(!lnativeAllocVideoState()) {
			lnativeFreeVideoState();
			throw new RuntimeException("Allocation of native video state failed.");
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		lnativeFreeVideoState();
		super.finalize();
	}
	
	private void updateFrameCounter() {
		currFrameId++;
		currTimeNS += timePerFrameNS;		
	}
	
	public VideoFrame decodeNextVideoFrame() {
		if(!lnativeDecodeNextVideoFrame())
			throw new RuntimeException("Decoding of next video frame failed.");
		
		JRGBFrameBuffer buff = new JRGBFrameBuffer();
		
		if(!lnativeExtractCurrFrame(buff))
			throw new RuntimeException("Converting current video frame to RGB buffer failed.");
		
		currFrame = new VideoFrame(getCurrentTimeUS(), timePerFrameNS / 1000, currFrameId, buff); 
		return currFrame.clone();
	}
	
	public void dumpVideoFormat() {
		lnativeDumpVideoFormat();
	}
	
	@Override
	public String toString() {
		// TODO make this more pretty
		return currFrameId + " " + currTimeNS;
	}
	
	public int getCurrentFrameId() {
		return currFrameId;
	}
	
	public long getCurrentTimeUS() {
		return currTimeNS / 1000;
	}
	
	public VideoFrame extractCurrentFrame() {
		return currFrame != null ? currFrame.clone() : null;
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
