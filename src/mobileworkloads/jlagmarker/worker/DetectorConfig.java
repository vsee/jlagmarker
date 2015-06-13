package mobileworkloads.jlagmarker.worker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import mobileworkloads.jlagmarker.lags.Lag;
import mobileworkloads.jlagmarker.lags.Lag.LagState;
import mobileworkloads.jlagmarker.masking.MaskManager;
import mobileworkloads.jlagmarker.worker.SuggesterConfig.SuggesterConfParams;
import mobileworkloads.mlgovernor.res.CSVResourceTools;

public class DetectorConfig extends WorkerConfig {
	
	public class DetectorConfParams extends WorkerConfParams {

		public int suggestionId;  // the frame id of the suggested image we are looking for
		public int maxDiffThreshold;
		public int pixIgnore;
		public String mask;		  // the mask we need to use while looking for the image
		public int occurrence;	  // specifies how often the suggested image is to be found before it is accepted as valid
		
		@Override
		public String toString() {
			return new StringBuilder("[").append(suggestionId)
					.append(", ").append(maxDiffThreshold)
					.append(", ").append(pixIgnore)
					.append(", ").append(mask)
					.append(", ").append(occurrence)
					.append("]").toString();
		}
		
		@Override
		public String[] toCSVArray() {
			String[] res = new String[HEAD_LENGTH];
			res[0] = lagId == -1 ? DEFAULT_PARAMS_HEAD : "" + lagId;
			res[1] = "" + (suggestionId < 0 ? "SKIP" : suggestionId);
			res[2] = "" + maxDiffThreshold;
			res[3] = "" + pixIgnore;
			res[4] = mask == null ? MaskManager.NO_MASK_MARKER : mask;
			res[5] = "" + occurrence;
			return res;
		}

		@Override
		public WorkerConfParams clone() {
			DetectorConfParams dp = new DetectorConfParams();
			dp.lagId = lagId;
			dp.suggestionId = suggestionId;
			dp.maxDiffThreshold = maxDiffThreshold;
			dp.pixIgnore = pixIgnore;
			dp.mask = mask;
			dp.occurrence = occurrence;
			return dp;
		}
	}
	
	protected static final int HEAD_LENGTH = 6;
	
	public DetectorConfig(Path configFile) throws IOException {
		super(configFile);
	}
	
	public DetectorConfig() {
		super();
	}
	
	@Override
	protected void setDefaultParams() {
		DetectorConfParams defaultP = new DetectorConfParams();
		defaultP.lagId = -1;
		defaultP.suggestionId = -1;
		defaultP.maxDiffThreshold = 30;
		defaultP.pixIgnore = 0;
		defaultP.mask = null;
		defaultP.occurrence = 1;
		defaultConfParams = defaultP;
	}

	@Override
	protected int getHeadLength() {
		return HEAD_LENGTH;
	}
	
	@Override
	protected String[] getHeader() {
		String[] header = { "lagId", "suggId", "diffTh", "pixIgnore", "maskName", "occurrence" };
		return header;
	}
	
	@Override
	protected WorkerConfParams parseParams(List<String> record) {
		
		if(record.size() != HEAD_LENGTH)
			throw new IllegalArgumentException(
					"Given detector config file entry has unexpected format: " + String.join(CSVResourceTools.SEPARATOR, record));

		if(!record.get(0).equals(DEFAULT_PARAMS_HEAD) && defaultConfParams == null)
			throw new RuntimeException("Given detector configuration has no default parameters.");
				
		DetectorConfParams dcparams = new DetectorConfParams();
		
		dcparams.lagId = record.get(0).equals(DEFAULT_PARAMS_HEAD) ? -1 : Integer.parseInt(record.get(0));
		
		dcparams.suggestionId = record.get(1).equals("SKIP") ? -1 : Integer.parseInt(record.get(1));
		
		dcparams.maxDiffThreshold = record.get(2).equals(DEFAULT_MARKER) ? ((DetectorConfParams) defaultConfParams).maxDiffThreshold
				: Integer.parseInt(record.get(2));
		
		dcparams.pixIgnore = record.get(3).equals(DEFAULT_MARKER) ? ((DetectorConfParams) defaultConfParams).pixIgnore
				: Integer.parseInt(record.get(3));
		
		if(record.get(4).equals(DEFAULT_MARKER)) {
			dcparams.mask = ((DetectorConfParams) defaultConfParams).mask;		
		} else {
			dcparams.mask = record.get(4).equals(MaskManager.NO_MASK_MARKER) ? null : record.get(4);
		}
		
		dcparams.occurrence = record.get(5).equals(DEFAULT_MARKER) ? ((DetectorConfParams) defaultConfParams).occurrence
				: Integer.parseInt(record.get(5));
		
		return dcparams;
	}

	public void createFromSuggParamsf(Lag l, SuggesterConfParams sp) {
		DetectorConfParams dp = new DetectorConfParams();
		dp.lagId = sp.lagId;
		dp.suggestionId = l.getState() == LagState.ENDED ? l.getEndFrame().videoFrameId : -1;
		dp.maxDiffThreshold = sp.maxDiffThreshold;
		dp.pixIgnore = sp.pixIgnore;
		dp.mask = sp.mask;
		dp.occurrence = 1; // TODO fix this
		
		confParams.add(dp);
	}
}
