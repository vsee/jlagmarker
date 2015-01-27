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
	
	public VideoState(String fileName) {
		
		videoFileName = fileName;
		
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
	
	public boolean decodeNextVideoFrame() {
		return lnativeDecodeNextVideoFrame();
	}
	
	@Override
	public String toString() {
		return currFrameId + " " + currTimeNS;
	}
	
	
	private native boolean lnativeAllocVideoState();
	private native void lnativeFreeVideoState();
	private native boolean lnativeDecodeNextVideoFrame();
	
	static {
		System.loadLibrary("VideoState");
	}
}
