/**
*
* Copyright 2017 Volker Seeker <volker@seekerscience.co.uk>.
*
* This file is part of JLagmarker.
*
* JLagmarker is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* JLagmarker is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with JLagmarker. If not, see <http://www.gnu.org/licenses/>.
*
*/
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
