package mobileworkloads.jlagmarker.worker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import mobileworkloads.jlagmarker.masking.MaskManager;
import mobileworkloads.mlgovernor.res.CSVResourceTools;

public class SuggesterConfig extends WorkerConfig {

	public class SuggesterConfParams extends WorkerConfParams {

		public int maxDiffThreshold;
		public int stillFrames;
		public int pixIgnore;
		public String mask;
		
		@Override
		public String toString() {
			return new StringBuilder("[").append(maxDiffThreshold)
					.append(",").append(stillFrames)
					.append(",").append(pixIgnore)
					.append(",").append(mask == null ? MaskManager.NO_MASK_MARKER : mask)
					.append("]").toString();
		}
		
		public void setCopy(SuggesterConfParams params) {
			maxDiffThreshold = params.maxDiffThreshold;
			stillFrames = params.stillFrames;
			pixIgnore = params.pixIgnore;
			mask = params.mask;
		}

		@Override
		public String[] toCSVArray() {
			String[] res = new String[HEAD_LENGTH];
			res[0] = lagId == -1 ? DEFAULT_PARAMS_HEAD : "" + lagId;
			res[1] = "" + maxDiffThreshold;
			res[2] = "" + stillFrames;
			res[3] = "" + pixIgnore;
			res[4] = mask == null ? MaskManager.NO_MASK_MARKER : mask;
			return res;
		}

		@Override
		public WorkerConfParams clone() {
			SuggesterConfParams sp = new SuggesterConfParams();
			sp.lagId = lagId;
			sp.maxDiffThreshold = maxDiffThreshold;
			sp.stillFrames = stillFrames;
			sp.pixIgnore = pixIgnore;
			sp.mask = mask;
			return sp;
		}
	}
	
	protected static final int HEAD_LENGTH = 5;

	public SuggesterConfig() { 
		super();
	}
	
	public SuggesterConfig(Path configFile) throws IOException {
		super(configFile);
	}

	@Override
	protected void setDefaultParams() {
		SuggesterConfParams defaultP = new SuggesterConfParams();
		defaultP.lagId = -1;
		defaultP.maxDiffThreshold = 30;
		defaultP.stillFrames = 30;
		defaultP.pixIgnore = 0;
		defaultP.mask = null;
		defaultConfParams = defaultP;
	}
	
	@Override
	protected int getHeadLength() {
		return HEAD_LENGTH;
	}
	
	@Override
	protected String[] getHeader() {
		String[] header = { "lagId", "diffTh", "stillFrames", "pixIgnore", "maskName" };
		return header;
	}
	
	@Override
	protected WorkerConfParams parseParams(List<String> record) {
		
		if(record.size() != HEAD_LENGTH)
			throw new IllegalArgumentException(
					"Given suggester config file entry has unexpected format: " + String.join(CSVResourceTools.SEPARATOR, record));

		if(!record.get(0).equals(DEFAULT_PARAMS_HEAD) && defaultConfParams == null)
			throw new RuntimeException("Given suggester configuration has no default parameters.");
				
		SuggesterConfParams scparams = new SuggesterConfParams();
		
		scparams.lagId = record.get(0).equals(DEFAULT_PARAMS_HEAD) ? -1 : Integer.parseInt(record.get(0));
		
		scparams.maxDiffThreshold = record.get(1).equals(DEFAULT_MARKER) ? ((SuggesterConfParams) defaultConfParams).maxDiffThreshold
				: Integer.parseInt(record.get(1));
		
		scparams.stillFrames = record.get(2).equals(DEFAULT_MARKER) ? ((SuggesterConfParams) defaultConfParams).stillFrames
				: Integer.parseInt(record.get(2));
		
		scparams.pixIgnore = record.get(3).equals(DEFAULT_MARKER) ? ((SuggesterConfParams) defaultConfParams).pixIgnore
				: Integer.parseInt(record.get(3));
		
		if(record.get(4).equals(DEFAULT_MARKER)) {
			scparams.mask = ((SuggesterConfParams) defaultConfParams).mask;		
		} else {
			scparams.mask = record.get(4).equals(MaskManager.NO_MASK_MARKER) ? null : record.get(4);
		}
		
		return scparams;
	}
}