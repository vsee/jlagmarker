package mobileworkloads.jlagmarker.suggesting;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import mobileworkloads.mlgovernor.res.CSVResourceTools;

public class SuggesterConfig {

	protected static final int HEAD_LENGTH = 7;
	protected static final String DEFAULT_MARKER = "%D%";
	protected static final String DEFAULT_PARAMS_HEAD = "DEFAULT";
	protected static final String NO_MASK_MARKER = "NONE";
	
	public class SuggesterConfParams {

		public int maxDiffThreshold;
		public int stillFrames;
		public int pixIgnore;
		public String mask;
		public int occurrence;	 // specify how often the suggested image is found before it is accepted as valid
		public int runInterval; // specify how many new beginnings can be found before this worker is removed

	}

	protected SuggesterConfParams defaultConfParams;
	protected final List<SuggesterConfParams> confParams;
	
	protected final Path configFile;
	
	public SuggesterConfig(Path configFile) throws IOException {
		this.configFile = configFile;
		confParams = new ArrayList<SuggesterConfParams>();
		
		parseSuggConfig(configFile);
	}

	protected void parseSuggConfig(Path configFile) throws IOException {
		if(configFile == null || !Files.isRegularFile(configFile))
			throw new IllegalArgumentException("Given suggester configuration file invalid: " + configFile);

		System.out.println("Parsing suggester configuration: " + configFile + " ...");
		
		try {
			List<String> header = CSVResourceTools.readHeader(Files.newBufferedReader(configFile, Charset.forName("UTF-8")));
			if(header.size() != HEAD_LENGTH)
				throw new IllegalArgumentException("Given suggester config file has unexpected format: " + configFile + "\n"
						+ String.join(CSVResourceTools.SEPARATOR, header));
			
			List<List<String>> records = CSVResourceTools.readRecords(Files.newBufferedReader(configFile, Charset.forName("UTF-8")));
			if(records.size() < 1)
				throw new IllegalArgumentException("Given resource file has no records: " + configFile);
			
			defaultConfParams = parseSuggParams(records.get(0));
			
			for (List<String> record : records.subList(1, records.size())) {
				confParams.add(parseSuggParams(record));
			}
			
		} catch (IOException e) {
			throw new UncheckedIOException("Error opening suggester configuration file: " + configFile, e);
		}
		
        System.out.println(confParams.size() + " configuration entries parsed successfully.");
	}

	protected SuggesterConfParams parseSuggParams(List<String> params) {
		
		if(params.size() != HEAD_LENGTH)
			throw new IllegalArgumentException(
					"Given suggester config file entry has unexpected format: " + String.join(CSVResourceTools.SEPARATOR, params));

		if(!params.get(0).equals(DEFAULT_PARAMS_HEAD) && defaultConfParams == null)
			throw new RuntimeException("Given suggester configuration has no default parameters.");
				
		SuggesterConfParams scparams = new SuggesterConfParams();
		
		scparams.maxDiffThreshold = params.get(1).equals(DEFAULT_MARKER) ? defaultConfParams.maxDiffThreshold
				: Integer.parseInt(params.get(1));
		
		scparams.stillFrames = params.get(2).equals(DEFAULT_MARKER) ? defaultConfParams.stillFrames
				: Integer.parseInt(params.get(2));
		
		scparams.pixIgnore = params.get(3).equals(DEFAULT_MARKER) ? defaultConfParams.pixIgnore
				: Integer.parseInt(params.get(3));
		
		if(params.get(4).equals(DEFAULT_MARKER)) {
			scparams.mask = defaultConfParams.mask;		
		} else {
			scparams.mask = params.get(4).equals(NO_MASK_MARKER) ? null : params.get(4);
		}
		
		scparams.occurrence = params.get(5).equals(DEFAULT_MARKER) ? defaultConfParams.occurrence
				: Integer.parseInt(params.get(5));
		
		scparams.runInterval = params.get(6).equals(DEFAULT_MARKER) ? defaultConfParams.runInterval
				: Integer.parseInt(params.get(6));
		
		return scparams;
	}

	public SuggesterConfParams getParams(int lagId) {
		return confParams.get(lagId);
	}

	public String getConfigFileName() {
		return configFile.toString();
	}
}
