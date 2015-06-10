package mobileworkloads.jlagmarker.markermodes;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import mobileworkloads.jlagmarker.InputEventStream;
import mobileworkloads.jlagmarker.lags.Lag;
import mobileworkloads.jlagmarker.lags.LagProfile;
import mobileworkloads.jlagmarker.video.VideoFrame;
import mobileworkloads.jlagmarker.worker.SISuggester;
import mobileworkloads.jlagmarker.worker.Suggester;
import mobileworkloads.jlagmarker.worker.SuggesterConfig;
import mobileworkloads.jlagmarker.worker.SuggesterConfig.SuggesterConfParams;
import mobileworkloads.jlagmarker.worker.VStreamWorker;
import mobileworkloads.mlgovernor.res.CSVResourceTools;

public class SuggesterMode extends LagmarkerMode {

	protected final SuggesterConfig sconf;
	protected final Suggester suggester;
	
	public SuggesterMode(String videoName, long inputFlashOffsetNS, InputEventStream ieStream,
			SuggesterConfig sconf, LagProfile lprofile, String outputPrefix, Path outputFolder) {
		super(videoName, inputFlashOffsetNS, ieStream, lprofile,outputPrefix, outputFolder);
		this.sconf = sconf;
		
		suggester = new SISuggester(outputFolder.resolve("sisuggestions"));
	}

	@Override
	protected VStreamWorker getWorker() {
		return suggester;
	}
	
	@Override
	public LagmarkerModeType getModeType() {
		return LagmarkerModeType.SUGGESTER;
	}
	
	@Override
	protected void setupWorker(Lag currLag, VideoFrame currFrame) {
		suggester.setupSuggester(currLag, (SuggesterConfParams) sconf.getParams(currLag.lagId), currFrame);
	}

	protected void dumpRunStats(Path outputFileName, long runtimeMS) {
		
		lprofile.dumpLagProfile(outputFolder.resolve(outputPrefix + "_suggest.lprofile"));
		
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
			statWriter.write("input file" + CSVResourceTools.SEPARATOR + ieStream.getInputFileName());
			statWriter.newLine();
			statWriter.write("suggester config" + CSVResourceTools.SEPARATOR + sconf.getConfigFileName());
			statWriter.newLine();
			statWriter.write("suggestion output" + CSVResourceTools.SEPARATOR + suggester.getOutputFolder());
			statWriter.newLine();

			System.out.println("Run statistics written to " + outputFileName);
		} catch (IOException e) {
			System.out.println("Writing run statistics to file failed!");
			throw new UncheckedIOException(e);
		}
	}
}
