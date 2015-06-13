package mobileworkloads.jlagmarker.args;

import java.nio.file.Path;
import java.nio.file.Paths;

import mobileworkloads.jlagmarker.markermodes.LagmarkerMode.LagmarkerModeType;
import weka.core.Utils;

public class LagMarkerArgs {
	
	protected static final String USAGE = 
			"Usage Args:\n" +
	
			"-mode str - Run mode of lagmarker\n" +
			"\tSUGGESTER: suggest input lag endings for a given video\n" +
			"\t\t[-sconf file - Path to the suggester configuration file to be used]\n" +
			"\tDETECTOR: find a list of given input lag ending images in a given video\n" +
			"\t\t-dconf file - Path to the detector configuration file to be used\n" +
			"\t\t-suggImgs file  - Path to where the suggestions images can be found\n" +
			
			"\n" +
			"-video file - Path to the video file to be processed\n" +
			"-ioffset num - Offset between white flash and input start in nano seconds\n" +
			"-input file - Path to the movement input file to be processed\n" +
			"-masks file - Path to the mask configuration file to be used\n" +
			
			"\n" +
			"-outPref String - Output prefix used as identifier for the resulting output\n" +
			"-outDir file - Path to the output folder for generated files\n";
	
	
	public Path videoFile;
	public long inputFlashOffsetNS;
	public Path maskSpec;
	public Path inputData;
	
	public String outputPrefix;
	public Path outputFolder;
	
	
	public LagmarkerModeType modeType;
	public Path sconfFile;
	public Path dconfFile;
	public Path suggImgs;
	
	public LagMarkerArgs(String[] args) {
		try {
			parseArgs(args);
		} catch(Exception e) {
			System.err.println("Error parsing input arguments: " + e.getMessage());
			throw new IllegalArgumentException("Given input arguments invalid. Please use:\n" + USAGE);
		}
		
		
		System.out.println("VIDEO: " + videoFile);
		System.out.println("IOFFSET: " + inputFlashOffsetNS);
		System.out.println("INPUT: " + inputData);
		System.out.println("MASKS: " + maskSpec);

		System.out.println("OUTPUT: " + outputFolder);
		System.out.println("OUTPUT PREFIX: " + outputPrefix);
		
		System.out.println("MODE: " + modeType.name());
		switch(modeType) {
			case SUGGESTER:
				System.out.println("SUGGESTER CONFIG: " + (sconfFile == null ? "GENERATE" : sconfFile));
				break;
			case DETECTOR:
				System.out.println("DETECTOR CONFIG: " + dconfFile);
				System.out.println("SUGGESTION IMGS: " + suggImgs);
				break;
			default: throw new IllegalArgumentException("Unknown mode type: " + modeType.name());
		}
		
		System.out.println();
	}

	
	private void parseArgs(String[] args) throws Exception {
		String optionString = "";
				
		optionString = Utils.getOption("video", args);
		if(optionString.isEmpty()) throw new IllegalArgumentException("No valid video path specified.");
		videoFile = Paths.get(optionString);
		
		optionString = Utils.getOption("ioffset", args);
		if(optionString.isEmpty()) throw new IllegalArgumentException("No valid input offset specified.");
		inputFlashOffsetNS = Long.parseLong(optionString);
		
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
		
		
		optionString = Utils.getOption("mode", args);
		if(optionString.isEmpty()) throw new IllegalArgumentException("No valid lagmarker mode specified.");
		modeType = LagmarkerModeType.valueOf(optionString);
		
		switch(modeType) {
		case SUGGESTER:
			optionString = Utils.getOption("sconf", args);
			if(optionString.isEmpty()) {
				sconfFile = null;
			} else {
				sconfFile = Paths.get(optionString);				
			}
			break;
		case DETECTOR:
			optionString = Utils.getOption("dconf", args);
			if(optionString.isEmpty()) throw new IllegalArgumentException("No valid detector configuration given.");
			dconfFile = Paths.get(optionString);
			
			optionString = Utils.getOption("suggImgs", args);
			if(optionString.isEmpty()) throw new IllegalArgumentException("No valid suggestion images path given.");
			suggImgs = Paths.get(optionString);
			break;
		default: throw new IllegalArgumentException("Unknown mode type: " + modeType.name());
	}
	
	}
}

