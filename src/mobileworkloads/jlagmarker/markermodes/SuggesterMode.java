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
import mobileworkloads.jlagmarker.suggesting.SISuggester;
import mobileworkloads.jlagmarker.suggesting.Suggester;
import mobileworkloads.jlagmarker.suggesting.SuggesterConfParams;
import mobileworkloads.jlagmarker.video.JRGBFrameBuffer;
import mobileworkloads.jlagmarker.video.VideoFrame;
import mobileworkloads.jlagmarker.video.VideoState;


public class SuggesterMode implements LagmarkerMode {

	protected final VideoState vstate;
	protected final InputEventStream ieStream;
	protected final LagProfile lprofile;
	
	protected final String outputPrefix;
	protected final Path outputFolder;
	
	protected VideoFrame wlStartFrame;
	
	protected final Suggester suggester;
	
	public SuggesterMode(String videoName, InputEventStream ieStream,
			LagProfile lprofile, String outputPrefix, Path outputFolder) {
		vstate = new VideoState(videoName);
		this.ieStream = ieStream;
		this.lprofile = lprofile;
		this.outputPrefix = outputPrefix;
		this.outputFolder = outputFolder;
		
		suggester = new SISuggester(outputFolder.resolve("sisuggestions"));
	}

	@Override
	public void run() {
		System.out.println("##### Running Suggester Mode #####");
		System.out.println("Run Start at: " + Calendar.getInstance().getTime());
		long startTimeMS = System.currentTimeMillis();
		
		vstate.dumpVideoFormat();
		System.out.println("\n\n");
		
		processVideoStream();
		
		// TODO read white flash offset from args
		
		lprofile.dumpLagProfile(outputFolder.resolve(outputPrefix + "_suggest.lprofile"));
		lprofile.dumpFrameBeginnings(outputFolder.resolve("beginFrames"));

		long runtimeMS = (System.currentTimeMillis() - startTimeMS);
		dumpRunStats(outputFolder.resolve(outputPrefix + "_runstats.csv"), runtimeMS);
		
		System.out.println("Run terminated successfully at: " + Calendar.getInstance().getTime() + " after " + (runtimeMS / 1000f) + " seconds.");
	}

	protected void dumpRunStats(Path outputFileName, long runtimeMS) {
		try(BufferedWriter statWriter = Files.newBufferedWriter(outputFileName, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
			
			statWriter.write("Runtime in MS;" + runtimeMS);
			statWriter.newLine();
			statWriter.write("Runtime Prefix;" + outputPrefix);
			statWriter.newLine();
			statWriter.write("lag count;" + lprofile.lags.size());
			statWriter.newLine();
			statWriter.write("start frame;" + wlStartFrame.videoFrameId);
			statWriter.newLine();
			statWriter.write("start frame offset US;" + wlStartFrame.startTimeUS);
			statWriter.newLine();
			statWriter.write("video file;" + vstate.getVideoFileName());
			statWriter.newLine();
			statWriter.write("input file;" + ieStream.getInputFileName());
			statWriter.newLine();
			statWriter.write("suggester config;" + "TODO");
			statWriter.newLine();

			System.out.println("Run statistics written to " + outputFileName);
		} catch (IOException e) {
			System.out.println("Writing run statistics to file failed!");
			throw new UncheckedIOException(e);
		}
	}

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
				wlStartFrame = frame; // TODO adapt start time by white flash offset from first input
				System.out.println("Workload start frame ["
						+ wlStartFrame.videoFrameId + "] found at: "
						+ wlStartFrame.startTimeUS + " US");
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

	protected void processFrame(VideoFrame currFrame) {
		if(suggester.isActive())
			suggester.update(currFrame);
		
		if (isLagBeginFrame(currFrame)) {
			suggester.terminate();

			Lag newLag = lprofile.addNewLag(currFrame);
			System.out.println(String.format("LAG %d: Beginning found at frame %s.", newLag.lagId, currFrame.toString()));
			
			suggester.start(newLag, new SuggesterConfParams(), currFrame); // TODO parse sugg config
		}
	}

	protected boolean isLagBeginFrame(VideoFrame currFrame) {
		return ieStream.didFingerGoDown(currFrame.startTimeUS
				- wlStartFrame.startTimeUS, currFrame.endTimeUS - wlStartFrame.startTimeUS);
	}

}
