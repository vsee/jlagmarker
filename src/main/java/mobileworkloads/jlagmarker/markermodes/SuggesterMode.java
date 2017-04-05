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

import mobileworkloads.jlagmarker.worker.Suggester;
import mobileworkloads.jlagmarker.worker.VStreamWorker;
import mobileworkloads.mlgovernor.res.CSVResourceTools;

public class SuggesterMode extends LagmarkerAnalysisMode {

	protected final Suggester suggester;
	
	public SuggesterMode(String videoName, long inputFlashOffsetNS, Path inputData,
			Path sconfFile, String outputPrefix, Path outputFolder) {
		super(videoName, inputFlashOffsetNS, inputData, outputPrefix, outputFolder);
		
		suggester = new Suggester(outputFolder, sconfFile);
	}

	@Override
	protected VStreamWorker getWorker() {
		return suggester;
	}
	
	@Override
	public LagmarkerModeType getModeType() {
		return LagmarkerModeType.SUGGESTER;
	}

	@Override
	protected String getSpecificRunStats() {
		StringBuilder bld = new StringBuilder();
		
		long noSuggestions = lprofile.lags.stream().filter(l -> l.getSuggestionIds().size() == 0).count();
		
		bld.append("lags with no suggestions").append(CSVResourceTools.SEPARATOR).append(noSuggestions).append("\n");
		
		return bld.toString();
	}
}
