package mobileworkloads.jlagmarker.worker;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import mobileworkloads.jlagmarker.lags.Lag;
import mobileworkloads.jlagmarker.video.VideoFrame;

public abstract class VStreamWorker {

	protected boolean active = false;

	protected Path outputFolder;
	protected Path configFile;

	public VStreamWorker(Path outputFolder, Path configFile) {
		if(!(Files.exists(outputFolder) && Files.isDirectory(outputFolder))) {
			try {
				Files.createDirectory(outputFolder);
				System.out.println("Worker output directory created: " + outputFolder);
			} catch (IOException e) {
				throw new UncheckedIOException("Error creating worker output directory: " + outputFolder, e);
			}
		}
		
		this.configFile = configFile;
		this.outputFolder = outputFolder;
	}
	
	public String getOutputFolder() {
		return outputFolder.toString();
	}
	
	public String getConfigFile() {
		if(configFile != null) return configFile.toString();
		else return "NONE";
	}
	
	public boolean isActive() {
		return active;
	}

	public abstract void update(VideoFrame currentFrame);

	public void terminate() {
		active = false;
	}

	public void start(Lag currLag, VideoFrame currFrame) {
		active = true;
	}
}
