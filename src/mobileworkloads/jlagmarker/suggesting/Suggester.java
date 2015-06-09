package mobileworkloads.jlagmarker.suggesting;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import mobileworkloads.jlagmarker.lags.Lag;
import mobileworkloads.jlagmarker.suggesting.SuggesterConfig.SuggesterConfParams;
import mobileworkloads.jlagmarker.video.VideoFrame;

public abstract class Suggester {
	
	protected static final String FILE_NAME_SUGGESTION_FORMAT = "lag_%03d_sug_%d.ppm";

	protected boolean active = false;
	protected Path outputFolder;

	public Suggester(Path outputFolder) {
		if(!(Files.exists(outputFolder) && Files.isDirectory(outputFolder))) {
			try {
				Files.createDirectory(outputFolder);
				System.out.println("SI suggester output directory created: " + outputFolder);
			} catch (IOException e) {
				throw new UncheckedIOException("Error creating SI suggester output directory: " + outputFolder, e);
			}
		}
		
		this.outputFolder = outputFolder;
	}
	
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

	public String getOutputFolder() {
		return outputFolder.toString();
	}
}
