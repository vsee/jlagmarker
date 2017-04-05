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
import java.nio.file.Path;
import java.util.List;

import mobileworkloads.jlagmarker.masking.ImgMask;
import mobileworkloads.jlagmarker.masking.MaskManager;
import mobileworkloads.mlgovernor.res.CSVResourceTools;

public class SuggesterConfig extends WorkerConfig {

	public class SuggesterConfParams extends WorkerConfParams {

		public int maxDiffThreshold;
		public int stillFrames;
		public int pixIgnore;
		public ImgMask mask;
		
		@Override
		public String toString() {
			return new StringBuilder("[").append(maxDiffThreshold)
					.append(",").append(stillFrames)
					.append(",").append(pixIgnore)
					.append(",").append(mask == null ? MaskManager.NO_MASK_MARKER : mask.maskName)
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
			res[4] = mask == null ? MaskManager.NO_MASK_MARKER : mask.maskName;
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
	protected WorkerConfParams getInitialDefaultParams() {
		SuggesterConfParams defaultP = new SuggesterConfParams();
		defaultP.lagId = -1;
		defaultP.maxDiffThreshold = 30;
		defaultP.stillFrames = 30;
		defaultP.pixIgnore = 0;
		defaultP.mask = null;
		return defaultP;
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
			scparams.mask = record.get(4).equals(MaskManager.NO_MASK_MARKER) ? null : 
				MaskManager.getInstance().getMask(record.get(4));
		}
		
		return scparams;
	}
}
