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
		
		if(!lnativeAllocVideoState())
			lnativeFreeVideoState();
			throw new RuntimeException("Allocation of native video state failed.");
	}
	
	@Override
	protected void finalize() throws Throwable {
		lnativeFreeVideoState();
		super.finalize();
	}
	
	private native boolean lnativeAllocVideoState();
	private native void lnativeFreeVideoState();
	
	static {
		System.loadLibrary("VideoState");
	}
}
