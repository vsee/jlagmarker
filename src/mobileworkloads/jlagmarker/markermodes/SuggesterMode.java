package mobileworkloads.jlagmarker.markermodes;

import mobileworkloads.jlagmarker.video.JRGBFrameBuffer;
import mobileworkloads.jlagmarker.video.VideoFrame;
import mobileworkloads.jlagmarker.video.VideoState;


public class SuggesterMode implements LagmarkerMode {

	protected VideoState vstate;
	
	public SuggesterMode(String videoName) {
		vstate = new VideoState(videoName);
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
		// TODO adapt by white flash offset from first input
		while(true) {
			VideoFrame frame = vstate.decodeNextVideoFrame();
			if(frame == null) return false;
			
			if(isStartFrame(vstate.getCurrentFrame())) {
				System.out.println("Start frame [" + frame.videoFrameId + "] found.");
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
		// TODO Auto-generated method stub
		
	}

}
