package mobileworkloads.jlagmarker.suggesting;

import mobileworkloads.jlagmarker.lags.Lag;
import mobileworkloads.jlagmarker.suggesting.SuggesterConfig.SuggesterConfParams;
import mobileworkloads.jlagmarker.video.VideoFrame;

public abstract class Suggester {
	
	protected static final String FILE_NAME_SUGGESTION_FORMAT = "lag_%03d_sug_%d.ppm";

	protected boolean active = false;
	
	public boolean isActive() {
		return active;
	}

	public abstract void update(VideoFrame currentFrame);

	public void terminate() {
		active = false;
	}

	public void start(Lag currLag, SuggesterConfParams sconf, VideoFrame currFrame) {
		active = true;
	}
}
