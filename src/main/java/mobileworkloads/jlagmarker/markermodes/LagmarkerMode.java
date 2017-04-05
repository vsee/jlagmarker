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
