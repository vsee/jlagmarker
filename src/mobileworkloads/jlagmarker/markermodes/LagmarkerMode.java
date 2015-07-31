package mobileworkloads.jlagmarker.markermodes;

import java.nio.file.Path;

import mobileworkloads.jlagmarker.video.VideoState;

public abstract class LagmarkerMode {

	public enum LagmarkerModeType { SUGGESTER, DETECTOR, FRAMEDUMP }

	protected final VideoState vstate;
	
	protected final String outputPrefix;
	protected final Path outputFolder;
	
	public LagmarkerMode(String videoName, String outputPrefix, Path outputFolder) {
		
		vstate = new VideoState(videoName);

		this.outputPrefix = outputPrefix;
		this.outputFolder = outputFolder;
	}
	
	public abstract void run();
	
	public abstract LagmarkerModeType getModeType();
}
