package mobileworkloads.jlagmarker.markermodes;

import java.nio.file.Path;

import mobileworkloads.jlagmarker.worker.Suggester;
import mobileworkloads.jlagmarker.worker.VStreamWorker;
import mobileworkloads.mlgovernor.res.CSVResourceTools;

public class SuggesterMode extends LagmarkerMode {

	protected final Suggester suggester;
	
	public SuggesterMode(String videoName, long inputFlashOffsetNS, Path inputData,
			Path sconfFile, String outputPrefix, Path outputFolder) {
		super(videoName, inputFlashOffsetNS, inputData, outputPrefix, outputFolder);
		
		suggester = new Suggester(outputFolder.resolve("sisuggestions"), sconfFile);
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
