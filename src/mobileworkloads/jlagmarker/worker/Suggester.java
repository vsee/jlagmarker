package mobileworkloads.jlagmarker.worker;

import java.nio.file.Path;

import mobileworkloads.jlagmarker.lags.Lag;
import mobileworkloads.jlagmarker.video.VideoFrame;
import mobileworkloads.jlagmarker.worker.SuggesterConfig.SuggesterConfParams;

public abstract class Suggester extends VStreamWorker {
	
	protected static final String FILE_NAME_SUGGESTION_FORMAT = "lag_%03d_sug_%d.ppm";

	public Suggester(Path outputFolder) {
		super(outputFolder);
	}
	
	public abstract void setupSuggester(Lag currLag, SuggesterConfParams params, VideoFrame currFrame);
}
