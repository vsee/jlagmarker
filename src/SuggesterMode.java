
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
			JRGBFrameBuffer frame = vstate.decodeNextVideoFrame();
			if(frame == null) return false;
			
			if(isStartFrame(frame)) {
				System.out.println("Start frame [" + vstate.getCurrentFrame() + "] found at: " + vstate.getCurrentTimeNS() + " ns.");
				return true;
			}
		}
	}
	
	protected boolean isStartFrame(JRGBFrameBuffer frame) {
		return false;
	}

	protected void findLags() {
		// TODO Auto-generated method stub
		
	}

}
