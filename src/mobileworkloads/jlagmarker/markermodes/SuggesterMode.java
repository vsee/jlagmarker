package mobileworkloads.jlagmarker.markermodes;

import mobileworkloads.jlagmarker.InputEventStream;
import mobileworkloads.jlagmarker.video.JRGBFrameBuffer;
import mobileworkloads.jlagmarker.video.VideoFrame;
import mobileworkloads.jlagmarker.video.VideoState;
import mobileworkloads.mlgovernor.res.dp.DPResourceTrace;
import mobileworkloads.mlgovernor.res.dp.IntervalDataPoint;


public class SuggesterMode implements LagmarkerMode {

	protected final VideoState vstate;
	protected final InputEventStream ieStream;
	protected long startTimeOffsetUS;
	
	public SuggesterMode(String videoName, InputEventStream ieStream) {
		vstate = new VideoState(videoName);
		this.ieStream = ieStream;
		
		startTimeOffsetUS = -1;
	}

	@Override
	public void run() {
		System.out.println("##### Running Suggester Mode #####");
		vstate.dumpVideoFormat();
		System.out.println("\n\n");
		
		processVideoStream();
		
		// TODO
		// parse input events
		// parse suggester configuration
		// read white flash offset from args
	}

	protected void processVideoStream() {
		System.out.println("### Searching for workload replay start frame ...");
		
		if(!findStartFrame()) throw new RuntimeException("No start frame found in given video.");
		
		System.out.println("### Marking lags ...");
		
		findLags();
	}
	
	protected boolean findStartFrame() {
		while(true) {
			VideoFrame frame = vstate.decodeNextVideoFrame();
			if(frame == null) return false;
			
			if(isStartFrame(frame)) {
				startTimeOffsetUS = frame.startTimeUS; // TODO adapt by white flash offset from first input
				System.out.println("Start frame [" + frame.videoFrameId + "] found at: " + startTimeOffsetUS + " US");
				return true;
			}
		}
	}
	
	protected boolean isStartFrame(VideoFrame frame) {
		// mask out control panel
		if(!frame.applyMask("STATUS_BAR_MASK_PORTRAIT")) 
			throw new RuntimeException("Failed to apply mask to frame: STATUS_BAR_MASK_PORTRAIT");

		// look for completely white frame
		for (int i = 0; i < frame.dataBuffer.getWidth() * frame.dataBuffer.getHeight() * JRGBFrameBuffer.CHANNEL_NUM; i++) {
			if(frame.dataBuffer.getRawChannel(i) != 0xFF) return false;
		}

		return true;
	}

	protected void findLags() {
		VideoFrame currFrame = vstate.extractCurrentFrame();
		while(currFrame != null) {
			processFrame(currFrame);
			currFrame = vstate.decodeNextVideoFrame();
		}
	}

	int lagId = 0;
	protected void processFrame(VideoFrame currFrame) {
		if (isLagBeginFrame(currFrame)) {
			System.out.println(String.format("LAG %d: Beginning found at %s.", lagId++, currFrame.toString()));
		}
	}

	protected boolean isLagBeginFrame(VideoFrame currFrame) {
		return ieStream.didFingerGoDown(currFrame.startTimeUS - startTimeOffsetUS, currFrame.endTimeUS - startTimeOffsetUS);
	}

}
