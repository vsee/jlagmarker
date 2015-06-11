package mobileworkloads.jlagmarker.worker;

import java.nio.file.Path;

public abstract class Suggester extends VStreamWorker {
	
	public static final String FILE_NAME_SUGGESTION_FORMAT = "lag_%03d_sug_%d.ppm";

	public Suggester(Path outputFolder, Path sconfFile) {
		super(outputFolder, sconfFile);
	}
}
