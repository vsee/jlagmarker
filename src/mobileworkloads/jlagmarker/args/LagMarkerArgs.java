package mobileworkloads.jlagmarker.args;

import java.nio.file.Path;
import java.nio.file.Paths;

import mobileworkloads.jlagmarker.markermodes.LagmarkerModeType;
import weka.core.Utils;

public class LagMarkerArgs {
	
	protected static final String USAGE = "Usage Args:\n" +
			"-mode str - Run mode of lagmarker [SUGGESTER, DETECTOR]\n" +
			"-video file - Path to the video file to be processed\n" +
			"-input file - Path to the movement input file to be processed\n" +
			"-masks file - Path to the mask configuration file to be used\n\n" +
			
			"-outPref String - Output prefix used as identifier for the resulting output\n" +
			"-outDir file - Path to the output folder for generated files\n";
	
	public LagmarkerModeType modeType;
	public Path videoFile;
	public Path maskSpec;
	public Path inputData;
	public String outputPrefix;
	public Path outputFolder;
	
	public LagMarkerArgs(String[] args) {
		try {
			parseArgs(args);
		} catch(Exception e) {
			System.err.println("Error parsing input arguments: " + e.getMessage());
			throw new IllegalArgumentException("Given input arguments invalid. Please use:\n" + USAGE);
		}
		
		System.out.println("MODE: " + modeType.name());
		System.out.println("VIDEO: " + videoFile);
		System.out.println("INPUT: " + inputData);
		System.out.println("MASKS: " + maskSpec);
		
		System.out.println("OUTPUT: " + outputFolder);
		System.out.println("OUTPUT PREFIX: " + outputPrefix);
		
		System.out.println();
	}

	
	private void parseArgs(String[] args) throws Exception {
		String optionString = "";
		
		optionString = Utils.getOption("mode", args);
		if(optionString.isEmpty()) throw new IllegalArgumentException("No valid lagmarker mode specified.");
		modeType = LagmarkerModeType.valueOf(optionString);
		
		optionString = Utils.getOption("video", args);
		if(optionString.isEmpty()) throw new IllegalArgumentException("No valid video path specified.");
		videoFile = Paths.get(optionString);
		
		optionString = Utils.getOption("masks", args);
		if(optionString.isEmpty()) throw new IllegalArgumentException("No valid mask specification given.");
		maskSpec = Paths.get(optionString);
		
		optionString = Utils.getOption("input", args);
		if(optionString.isEmpty()) throw new IllegalArgumentException("No valid input path specified.");
		inputData = Paths.get(optionString);		

		outputPrefix = Utils.getOption("outPref", args);
		if(outputPrefix.isEmpty()) throw new IllegalArgumentException("No valid output prefix specified");
		
		optionString = Utils.getOption("outDir", args);
		if(optionString.isEmpty()) throw new IllegalArgumentException("No valid output path specified.");
		outputFolder = Paths.get(optionString);		
	}
}

