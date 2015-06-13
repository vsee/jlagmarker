package mobileworkloads.jlagmarker;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import mobileworkloads.jlagmarker.args.LagMarkerArgs;
import mobileworkloads.jlagmarker.lags.LagProfile;
import mobileworkloads.jlagmarker.markermodes.DetectorMode;
import mobileworkloads.jlagmarker.markermodes.InteractiveSuggMode;
import mobileworkloads.jlagmarker.markermodes.LagmarkerMode;
import mobileworkloads.jlagmarker.masking.MaskManager;

public class JLagmarkerMain {
		
	private static final int MAX_OUT_DIR_TRIES = 100;
	
	private LagmarkerMode mode; 
	
	public JLagmarkerMain(String[] cmdArgs) {
	
		LagMarkerArgs args = new LagMarkerArgs(cmdArgs);
		
		System.out.println("Creating output folder at: " + args.outputFolder + " ...");
		args.outputFolder = createOutputFolder(args.outputPrefix, 
				args.outputFolder.resolve("lmrun_" + args.modeType.name() + "_" + args.outputPrefix));
		System.out.println();
		
		try {
			MaskManager.getInstance().parseMasks(args.maskSpec);
			System.out.println();
		} catch (IOException e) {
			throw new UncheckedIOException("Error parsing mask specification file [" + args.maskSpec + "]", e);
		}
		
		InputEventStream ieStream = null;
		try {
			System.out.println("Parsing input file: " + args.inputData + " ...");
			ieStream = new InputEventStream(args.inputData);
			System.out.println();
		} catch (IOException e) {
			throw new UncheckedIOException("Error parsing input data file [" + args.inputData + "]", e);
		}
			
		LagProfile lprofile = new LagProfile();
		
		switch(args.modeType) {
			case SUGGESTER:			
				mode = new InteractiveSuggMode(args.videoFile.toString(), args.inputFlashOffsetNS, 
						ieStream, args.sconfFile, lprofile, args.outputPrefix, args.outputFolder);
				break;
			case DETECTOR:
				mode = new DetectorMode(args.videoFile.toString(), args.inputFlashOffsetNS, 
						ieStream, args.dconfFile, args.suggImgs, lprofile, args.outputPrefix, args.outputFolder);
				break;
			default:
				throw new IllegalArgumentException("Unknown run mode: " + args.modeType.name());
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
