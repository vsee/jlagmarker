package mobileworkloads.jlagmarker.markermodes;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Calendar;

import mobileworkloads.jlagmarker.InputEventStream;
import mobileworkloads.jlagmarker.lags.Lag;
import mobileworkloads.jlagmarker.lags.LagProfile;
import mobileworkloads.jlagmarker.masking.ImgMask;
import mobileworkloads.jlagmarker.masking.MaskManager;
import mobileworkloads.jlagmarker.video.JRGBFrameBuffer;
import mobileworkloads.jlagmarker.video.VideoFrame;
import mobileworkloads.jlagmarker.video.VideoState;
import mobileworkloads.jlagmarker.worker.VStreamWorker;
import mobileworkloads.mlgovernor.res.CSVResourceTools;

public abstract class LagmarkerMode {

	public enum LagmarkerModeType { SUGGESTER, DETECTOR }

	private static final String CONTROL_PANEL_MASK_NAME = "STATUS_BAR_MASK_PORTRAIT";
	
	protected final VideoState vstate;
	protected final InputEventStream ieStream;
	protected final LagProfile lprofile;
	
	protected final String outputPrefix;
	protected final Path outputFolder;
	
	protected long inputFlashOffsetNS;
	protected VideoFrame wlStartFrame;
	
	protected final ImgMask whiteFrameMask;
	
	public LagmarkerMode(String videoName, long inputFlashOffsetNS, Path inputData,
			String outputPrefix, Path outputFolder) {
		
		vstate = new VideoState(videoName);
		this.inputFlashOffsetNS = inputFlashOffsetNS;

		this.lprofile = new LagProfile(outputFolder.resolve("beginFrames"));
		
		try {
			System.out.println("Parsing input file: " + inputData + " ...");
			ieStream = new InputEventStream(inputData);
			System.out.println();
		} catch (IOException e) {
			throw new UncheckedIOException("Error parsing input data file [" + inputData + "]", e);
		}
		
		this.outputPrefix = outputPrefix;
		this.outputFolder = outputFolder;
		
		whiteFrameMask = MaskManager.getInstance().getMask(CONTROL_PANEL_MASK_NAME);
	}
	
	public void run() {
		System.out.println("##### Running " + getModeType().name() + " Mode #####");
		System.out.println("Run Start at: " + Calendar.getInstance().getTime());
		long startTimeMS = System.currentTimeMillis();
		
		vstate.dumpVideoFormat();
		System.out.println("\n\n");
		
		processVideoStream();
		
		System.out.println("\nSaving Results ...");
		
		lprofile.dumpLagProfile(outputFolder.resolve(outputPrefix + ".lprofile"));

		long runtimeMS = (System.currentTimeMillis() - startTimeMS);
		dumpRunStats(outputFolder.resolve(outputPrefix + "_runstats.csv"), runtimeMS);
		
		System.out.println("Run terminated successfully at: " + Calendar.getInstance().getTime() + " after " + (runtimeMS / 1000f) + " seconds.");
	}
	
	public abstract LagmarkerModeType getModeType();

	protected abstract VStreamWorker getWorker();
	
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
		if(!frame.frameImg.applyMask(whiteFrameMask)) {
			throw new RuntimeException("Error applying mask to find white start frame.");
		}

		// look for completely white frame
		for (int i = 0; i < frame.frameImg.dataBuffer.getWidth()
				* frame.frameImg.dataBuffer.getHeight()
				* JRGBFrameBuffer.CHANNEL_NUM; i++) {
			
			if (frame.frameImg.dataBuffer.getRawChannel(i) != 0xFF)
				return false;
		}

		return true;
	}

	protected void findLags() {
		VideoFrame currFrame = vstate.extractCurrentFrame();
		while(currFrame != null) {
			processFrame(currFrame);
			currFrame = vstate.decodeNextVideoFrame();
		}
		
		if(getWorker().isActive())
			getWorker().terminate();
	}
	
	protected void processFrame(VideoFrame currFrame) {
		if(getWorker().isActive())
			getWorker().update(currFrame);
		
		if (isLagBeginFrame(currFrame)) {
			
			if(getWorker().isActive())
				getWorker().terminate();

			Lag newLag = lprofile.addNewLag(currFrame);
			System.out.println(String.format("\nLAG %d: Beginning found at frame %s.", newLag.lagId, currFrame.toString()));
			
			getWorker().start(newLag, currFrame);
		}
	}
	
	protected boolean isLagBeginFrame(VideoFrame currFrame) {
		return ieStream.didFingerGoDown(currFrame.startTimeUS
				- wlStartFrame.startTimeUS, currFrame.endTimeUS - wlStartFrame.startTimeUS);
	}
	
	protected abstract String getSpecificRunStats();
	
	protected void dumpRunStats(Path outputFileName, long runtimeMS) {
		
		try(BufferedWriter statWriter = Files.newBufferedWriter(outputFileName, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
			
			statWriter.write("Runtime in MS" + CSVResourceTools.SEPARATOR + runtimeMS);
			statWriter.newLine();
			statWriter.write("Output Prefix" + CSVResourceTools.SEPARATOR + outputPrefix);
			statWriter.newLine();
			statWriter.write("Output Folder" + CSVResourceTools.SEPARATOR + outputFolder);
			statWriter.newLine();
			statWriter.write("lag count" + CSVResourceTools.SEPARATOR + lprofile.lags.size());
			statWriter.newLine();
			statWriter.write("start frame" + CSVResourceTools.SEPARATOR + wlStartFrame.videoFrameId);
			statWriter.newLine();
			statWriter.write("start frame offset US" + CSVResourceTools.SEPARATOR + wlStartFrame.startTimeUS);
			statWriter.newLine();
			statWriter.write("video file" + CSVResourceTools.SEPARATOR + vstate.getVideoFileName());
			statWriter.newLine();
			statWriter.write("video fps" + CSVResourceTools.SEPARATOR + vstate.getFrameRate());
			statWriter.newLine();
			statWriter.write("input file" + CSVResourceTools.SEPARATOR + ieStream.getInputFileName());
			statWriter.newLine();
			statWriter.write("run mode" + CSVResourceTools.SEPARATOR + getModeType().toString());
			statWriter.newLine();
			statWriter.write("worker config" + CSVResourceTools.SEPARATOR + getWorker().getConfigFile());
			statWriter.newLine();
			statWriter.write("worker output" + CSVResourceTools.SEPARATOR + getWorker().getOutputFolder());
			statWriter.newLine();
			
			statWriter.write(getSpecificRunStats());

			System.out.println("Run statistics written to " + outputFileName);
		} catch (IOException e) {
			System.out.println("Writing run statistics to file failed!");
			throw new UncheckedIOException(e);
		}
	}
}
