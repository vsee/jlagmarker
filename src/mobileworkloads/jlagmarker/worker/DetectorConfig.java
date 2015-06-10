package mobileworkloads.jlagmarker.worker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import mobileworkloads.mlgovernor.res.CSVResourceTools;

public class DetectorConfig extends WorkerConfig {
	
	public class DetectorConfParams implements WorkerConfParams {

		public int suggestionId;  // the frame id of the suggested image we are looking for 
		public String mask;		  // the mask we need to use while looking for the image
		public int occurrence;	  // specifies how often the suggested image is to be found before it is accepted as valid
		
		@Override
		public String toString() {
			return new StringBuilder("[").append(suggestionId)
					.append(", ").append(mask)
					.append(", ").append(occurrence)
					.append("]").toString();
		}

	}
	
	protected static final int HEAD_LENGTH = 5;
	
	public DetectorConfig(Path configFile) throws IOException {
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
					"Given detector config file entry has unexpected format: " + String.join(CSVResourceTools.SEPARATOR, record));

		if(!record.get(0).equals(DEFAULT_PARAMS_HEAD) && defaultConfParams == null)
			throw new RuntimeException("Given detector configuration has no default parameters.");
				
		DetectorConfParams dcparams = new DetectorConfParams();
		
		dcparams.suggestionId = Integer.parseInt(record.get(1)); // this item has no default value
		
		if(record.get(2).equals(DEFAULT_MARKER)) {
			dcparams.mask = ((DetectorConfParams) defaultConfParams).mask;		
		} else {
			dcparams.mask = record.get(2).equals(NO_MASK_MARKER) ? null : record.get(4);
		}
		
		dcparams.occurrence = record.get(3).equals(DEFAULT_MARKER) ? ((DetectorConfParams) defaultConfParams).occurrence
				: Integer.parseInt(record.get(3));
		
		return dcparams;
	}
}
