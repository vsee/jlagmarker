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

import mobileworkloads.jlagmarker.lags.Lag.LagState;
import mobileworkloads.jlagmarker.worker.ImgFinder;
import mobileworkloads.jlagmarker.worker.VStreamWorker;
import mobileworkloads.mlgovernor.res.CSVResourceTools;

public class DetectorMode extends LagmarkerAnalysisMode {

	protected final ImgFinder imgfinder;
	
	public DetectorMode(String videoName, long inputFlashOffsetNS, Path inputData, 
			Path dconfFile, Path suggImgs, String outputPrefix, Path outputFolder) {
		super(videoName, inputFlashOffsetNS, inputData, outputPrefix, outputFolder);
		
		imgfinder = new ImgFinder(outputFolder, dconfFile, suggImgs);
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
	protected void saveRunResults(long runtimeMS) {
		super.saveRunResults(runtimeMS);
		
		imgfinder.dumpFrameFlanks(outputFolder.resolve(outputPrefix + "_frameflanks.csv"));
	}
	
	@Override
	protected String getSpecificRunStats() {
		StringBuilder bld = new StringBuilder();
		
		long detectedLags = lprofile.lags.stream().filter(l -> l.getState() != LagState.NA).count();
		
		bld.append("lags detected").append(CSVResourceTools.SEPARATOR).append(detectedLags).append("\n");
		bld.append("lags not found").append(CSVResourceTools.SEPARATOR).append(lprofile.lags.size() - detectedLags).append("\n");
		
		return bld.toString();
	}
}
