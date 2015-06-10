package mobileworkloads.jlagmarker.worker;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import mobileworkloads.jlagmarker.video.VideoFrame;

public abstract class VStreamWorker {

	protected boolean active = false;

	protected Path outputFolder;

	public VStreamWorker(Path outputFolder) {
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
	
	public String getOutputFolder() {
		return outputFolder.toString();
	}
	
	public boolean isActive() {
		return active;
	}

	public abstract void update(VideoFrame currentFrame);

	public void terminate() {
		active = false;
	}

	public void start() {
		active = true;
	}
}
