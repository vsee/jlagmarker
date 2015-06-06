package mobileworkloads.jlagmarker;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import mobileworkloads.jlagmarker.lags.LagProfile;
import mobileworkloads.jlagmarker.markermodes.LagmarkerMode;
import mobileworkloads.jlagmarker.markermodes.LagmarkerModeType;
import mobileworkloads.jlagmarker.markermodes.SuggesterMode;
import mobileworkloads.jlagmarker.masking.MaskManager;

public class JLagmarkerMain {
		
	private static final int MAX_OUT_DIR_TRIES = 100;
	
	private LagmarkerMode mode; 
	
	public JLagmarkerMain(String[] args) {
	
		// args: jlagmarker.jar <mode> -modearg1 blabla -modearg2 blabla
		if(args.length < 6) // expected <mode> <videoname> <maskspec> <inputData> <outputprefix> <outputFolder>
			// TODO implement some usage messages
			throw new IllegalArgumentException("Arguments not specified as needed.");
		
		String modeType = args[0];
		String videoName = args[1];
		String maskSpec = args[2];
		String inputData = args[3];
		String outputPrefix = args[4];
		Path outputFolder = Paths.get(args[5]);
		System.out.println("Args: " + Arrays.toString(args));
		System.out.println();
		
		System.out.println("Creating output folder at: " + outputFolder + " ...");
		outputFolder = createOutputFolder(outputPrefix, outputFolder.resolve("lmrun_" + modeType + "_" + outputPrefix));
		System.out.println();
		
		try {
			MaskManager.getInstance().parseMasks(Paths.get(maskSpec));
			System.out.println();
		} catch (IOException e) {
			throw new UncheckedIOException("Error parsing mask specification file [" + maskSpec + "]", e);
		}
		
		InputEventStream ieStream = null;
		try {
			System.out.println("Parsing input file: " + inputData + " ...");
			ieStream = new InputEventStream(Paths.get(inputData));
			System.out.println();
		} catch (IOException e) {
			throw new UncheckedIOException("Error parsing input data file [" + inputData + "]", e);
		}
		
		LagProfile lprofile = new LagProfile();
		
		if(modeType.equalsIgnoreCase(LagmarkerModeType.SUGGESTER.name())) {
			mode = new SuggesterMode(videoName, ieStream, lprofile, outputPrefix, outputFolder);
		} else if (modeType.equalsIgnoreCase(LagmarkerModeType.DETECTOR.name())) {
			// TODO implement DetectorMode
			// mode = new DetectorMode(videoName);
		} else {
			throw new IllegalArgumentException("Given lagmarker mode unknown: " + modeType);
		}
	}
	
	private Path createOutputFolder(String outputPrefix, Path outputFolder) {
		try {
			int outDirCounter = 0;
			Path outDirOrig = outputFolder;
			while (Files.exists(outputFolder)
					&& Files.isDirectory(outputFolder)
					&& outDirCounter < MAX_OUT_DIR_TRIES) {
				
				outDirCounter++;
				if(outDirCounter == MAX_OUT_DIR_TRIES)
					throw new RuntimeException("Gave up output directory creation after " + MAX_OUT_DIR_TRIES + " tries.");
				
				outputFolder = Paths.get(outDirOrig.toAbsolutePath() + "_" + outDirCounter);
				System.out.println("Given output directory already exists. Trying again with: " + outputFolder);
			}
			
			Files.createDirectory(outputFolder);
			System.out.println("Output directory created: " + outputFolder);
		} catch (FileAlreadyExistsException existEx) {
			System.out.println("Output directory already exists.");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		
		return outputFolder;
	}

	public void run() {
		mode.run();
	}
	
	public static void main(String[] args) {
		new JLagmarkerMain(args).run();
	}
}
