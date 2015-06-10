package mobileworkloads.jlagmarker.worker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import mobileworkloads.mlgovernor.res.CSVResourceTools;

public class SuggesterConfig extends WorkerConfig {

	public class SuggesterConfParams implements WorkerConfParams {

		public int maxDiffThreshold;
		public int stillFrames;
		public int pixIgnore;
		public String mask;
		
		@Override
		public String toString() {
			return new StringBuilder("[").append(maxDiffThreshold)
					.append(", ").append(stillFrames)
					.append(", ").append(pixIgnore)
					.append(", ").append(mask)
					.append("]").toString();
		}

	}
	
	protected static final int HEAD_LENGTH = 5;
	
	public SuggesterConfig(Path configFile) throws IOException {
		super(configFile);
	}

	@Override
	protected int getHeadLength() {
		return HEAD_LENGTH;
	}
	
	@Override
	protected WorkerConfParams parseParams(List<String> record) {
		
		if(record.size() != HEAD_LENGTH)
			throw new IllegalArgumentException(
					"Given suggester config file entry has unexpected format: " + String.join(CSVResourceTools.SEPARATOR, record));

		if(!record.get(0).equals(DEFAULT_PARAMS_HEAD) && defaultConfParams == null)
			throw new RuntimeException("Given suggester configuration has no default parameters.");
				
		SuggesterConfParams scparams = new SuggesterConfParams();
		
		scparams.maxDiffThreshold = record.get(1).equals(DEFAULT_MARKER) ? ((SuggesterConfParams) defaultConfParams).maxDiffThreshold
				: Integer.parseInt(record.get(1));
		
		scparams.stillFrames = record.get(2).equals(DEFAULT_MARKER) ? ((SuggesterConfParams) defaultConfParams).stillFrames
				: Integer.parseInt(record.get(2));
		
		scparams.pixIgnore = record.get(3).equals(DEFAULT_MARKER) ? ((SuggesterConfParams) defaultConfParams).pixIgnore
				: Integer.parseInt(record.get(3));
		
		if(record.get(4).equals(DEFAULT_MARKER)) {
			scparams.mask = ((SuggesterConfParams) defaultConfParams).mask;		
		} else {
			scparams.mask = record.get(4).equals(NO_MASK_MARKER) ? null : record.get(4);
		}
		
		return scparams;
	}
}
