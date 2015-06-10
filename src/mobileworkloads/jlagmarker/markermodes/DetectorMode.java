package mobileworkloads.jlagmarker.markermodes;

import java.nio.file.Path;

import mobileworkloads.jlagmarker.InputEventStream;
import mobileworkloads.jlagmarker.lags.Lag;
import mobileworkloads.jlagmarker.lags.LagProfile;
import mobileworkloads.jlagmarker.video.VideoFrame;
import mobileworkloads.jlagmarker.worker.ImgFinder;
import mobileworkloads.jlagmarker.worker.VStreamWorker;

public class DetectorMode extends LagmarkerMode {

	protected final ImgFinder imgfinder;
	
	public DetectorMode(String videoName, long inputFlashOffsetNS, InputEventStream ieStream, LagProfile lprofile,
			String outputPrefix, Path outputFolder) {
		super(videoName, inputFlashOffsetNS, ieStream, lprofile, outputPrefix, outputFolder);
		
		imgfinder = new ImgFinder(outputFolder);
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
	protected void setupWorker(Lag currLag, VideoFrame currFrame) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void dumpRunStats(Path outputFileName, long runtimeMS) {
		// TODO Auto-generated method stub
		
	}

}
