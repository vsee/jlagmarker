package mobileworkloads.jlagmarker.worker;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import mobileworkloads.mlgovernor.res.CSVResourceTools;

public abstract class WorkerConfig {
	
	public interface WorkerConfParams { }

	protected static final String DEFAULT_MARKER = "%D%";
	protected static final String DEFAULT_PARAMS_HEAD = "DEFAULT";
	protected static final String NO_MASK_MARKER = "NONE";
	
	protected final Path configFile;
	
	protected WorkerConfParams defaultConfParams;
	protected final List<WorkerConfParams> confParams;
		
	
	public WorkerConfig(Path configFile) throws IOException {
		this.configFile = configFile;

		confParams = new ArrayList<WorkerConfParams>();
		
		parseConfig(configFile);
	}
	
	public WorkerConfParams getParams(int lagId) {
		return confParams.get(lagId);
	}

	public String getConfigFileName() {
		return configFile.toString();
	}
	
	protected abstract int getHeadLength();
	
	protected void parseConfig(Path configFile) throws IOException {
		if(configFile == null || !Files.isRegularFile(configFile))
			throw new IllegalArgumentException("Given worker configuration file invalid: " + configFile);

		System.out.println("Parsing worker configuration: " + configFile + " ...");
		
		try {
			List<String> header = CSVResourceTools.readHeader(Files.newBufferedReader(configFile, Charset.forName("UTF-8")));
			if(header.size() != getHeadLength())
				throw new IllegalArgumentException("Given worker config file has unexpected format: " + configFile + "\n"
						+ String.join(CSVResourceTools.SEPARATOR, header));
			
			List<List<String>> records = CSVResourceTools.readRecords(Files.newBufferedReader(configFile, Charset.forName("UTF-8")));
			if(records.size() < 1)
				throw new IllegalArgumentException("Given resource file has no records: " + configFile);
			
			defaultConfParams = parseParams(records.get(0));
			
			for (List<String> record : records.subList(1, records.size())) {
				confParams.add(parseParams(record));
			}
			
		} catch (IOException e) {
			throw new UncheckedIOException("Error opening worker configuration file: " + configFile, e);
		}
		
        System.out.println(confParams.size() + " worker configuration entries parsed successfully.");
	}

	protected abstract WorkerConfParams parseParams(List<String> record);
}
