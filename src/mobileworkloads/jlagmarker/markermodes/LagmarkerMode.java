package mobileworkloads.jlagmarker.markermodes;

import java.nio.file.Path;
import java.util.Calendar;

import mobileworkloads.jlagmarker.InputEventStream;
import mobileworkloads.jlagmarker.lags.Lag;
import mobileworkloads.jlagmarker.lags.LagProfile;
import mobileworkloads.jlagmarker.video.JRGBFrameBuffer;
import mobileworkloads.jlagmarker.video.VideoFrame;
import mobileworkloads.jlagmarker.video.VideoState;
import mobileworkloads.jlagmarker.worker.VStreamWorker;

public abstract class LagmarkerMode {

	public enum LagmarkerModeType { SUGGESTER, DETECTOR }
	
	protected final VideoState vstate;
	protected final InputEventStream ieStream;
	protected final LagProfile lprofile;
	
	protected final String outputPrefix;
	protected final Path outputFolder;
	
	protected long inputFlashOffsetNS;
	protected VideoFrame wlStartFrame;
	
	public LagmarkerMode(String videoName, long inputFlashOffsetNS, InputEventStream ieStream,
			LagProfile lprofile, String outputPrefix, Path outputFolder) {
		
		vstate = new VideoState(videoName);
		this.inputFlashOffsetNS = inputFlashOffsetNS;
		this.ieStream = ieStream;
		this.lprofile = lprofile;
		this.outputPrefix = outputPrefix;
		this.outputFolder = outputFolder;
	}
	
	public void run() {
		System.out.println("##### Running " + getModeType().name() + " Mode #####");
		System.out.println("Run Start at: " + Calendar.getInstance().getTime());
		long startTimeMS = System.currentTimeMillis();
		
		vstate.dumpVideoFormat();
		System.out.println("\n\n");
		
		processVideoStream();
		
		lprofile.dumpFrameBeginnings(outputFolder.resolve("beginFrames"));

		long runtimeMS = (System.currentTimeMillis() - startTimeMS);
		dumpRunStats(outputFolder.resolve(outputPrefix + "_runstats.csv"), runtimeMS);
		
		System.out.println("Run terminated successfully at: " + Calendar.getInstance().getTime() + " after " + (runtimeMS / 1000f) + " seconds.");
	}
	
	public abstract LagmarkerModeType getModeType();

	protected abstract VStreamWorker getWorker();
	
	protected abstract void setupWorker(Lag currLag, VideoFrame currFrame);
	
	protected void processVideoStream() {
		System.out.println("### Searching for workload replay start frame ...");
		
		if(!findWorkloadStartFrame()) throw new RuntimeException("No workload start frame found in given video.");
		
		System.out.println("### Marking lags ...");
		
		findLags();
	}
	
	protected boolean findWorkloadStartFrame() {
		while(true) {
			VideoFrame frame = vstate.decodeNextVideoFrame();
			if(frame == null) return false;
			
			if(isStartFrame(frame)) {
				
				/* It can happen, especially for slow frequencies, that the input events belonging
				 * to the white flash happen significantly earlier (more than a frame) than the 
				 * flash event on the screen.
				 * 
				 * This leads to an offset of the beginning of all successive lag events since
				 * the start time is normalised to the start time of the flash frame and not 
				 * the actual start input. Hence all successive input events would happen later.
				 * 
				 * To avoid this, we reverse the video by the given input-flash-offset we read from
				 * the trace-cmd data, as soon as we find the white flash. The reversed frame is then
				 * the frame where the start input happens and therefore the correct start frame.
				 */
				int startFrameId = frame.videoFrameId - 
						(int) Math.ceil(inputFlashOffsetNS / 1000000000.0 * vstate.getFrameRate());
				
				vstate.skipBackwards(frame.videoFrameId - startFrameId);
				wlStartFrame = vstate.extractCurrentFrame();
				
				System.out.println("Workload start frame ["
						+ wlStartFrame.videoFrameId + "] found at: "
						+ wlStartFrame.startTimeUS + " US");
				return true;
			}
		}
	}
	
	protected boolean isStartFrame(VideoFrame frame) {
		// mask out control panel
		frame.applyMask("STATUS_BAR_MASK_PORTRAIT");

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
	
	protected void processFrame(VideoFrame currFrame) {
		if(getWorker().isActive())
			getWorker().update(currFrame);
		
		if (isLagBeginFrame(currFrame)) {
			getWorker().terminate();

			Lag newLag = lprofile.addNewLag(currFrame);
			System.out.println(String.format("\nLAG %d: Beginning found at frame %s.", newLag.lagId, currFrame.toString()));
			
			setupWorker(newLag, currFrame);
			getWorker().start();
		}
	}
	
	protected boolean isLagBeginFrame(VideoFrame currFrame) {
		return ieStream.didFingerGoDown(currFrame.startTimeUS
				- wlStartFrame.startTimeUS, currFrame.endTimeUS - wlStartFrame.startTimeUS);
	}
	
	protected abstract void dumpRunStats(Path outputFileName, long runtimeMS);

}
