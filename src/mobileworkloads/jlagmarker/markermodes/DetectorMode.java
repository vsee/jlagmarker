package mobileworkloads.jlagmarker.markermodes;

import java.nio.file.Path;

import mobileworkloads.jlagmarker.InputEventStream;
import mobileworkloads.jlagmarker.lags.Lag.LagState;
import mobileworkloads.jlagmarker.lags.LagProfile;
import mobileworkloads.jlagmarker.worker.ImgFinder;
import mobileworkloads.jlagmarker.worker.VStreamWorker;
import mobileworkloads.mlgovernor.res.CSVResourceTools;

public class DetectorMode extends LagmarkerMode {

	protected final ImgFinder imgfinder;
	
	public DetectorMode(String videoName, long inputFlashOffsetNS, InputEventStream ieStream, 
			Path dconfFile, Path suggImgs, LagProfile lprofile, String outputPrefix, 
			Path outputFolder) {
		super(videoName, inputFlashOffsetNS, ieStream, lprofile, outputPrefix, outputFolder);
		
		imgfinder = new ImgFinder(outputFolder.resolve("detections"), dconfFile, suggImgs);
	}

	@Override
	protected VStreamWorker getWorker() {
		return imgfinder;
	}
	
	@Override
	public LagmarkerModeType getModeType() {
		return LagmarkerModeType.DETECTOR;
	}

	@Override
	protected String getSpecificRunStats() {
		StringBuilder bld = new StringBuilder();
		
		long detectedLags = lprofile.lags.stream().filter(l -> l.getState() != LagState.NA).count();
		
		bld.append("lags detected").append(CSVResourceTools.SEPARATOR).append(detectedLags).append("\n");
		bld.append("lags not found").append(CSVResourceTools.SEPARATOR).append(lprofile.lags.size() - detectedLags).append("\n");
		
		return bld.toString();
	}	
}
