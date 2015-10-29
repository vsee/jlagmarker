package mobileworkloads.jlagmarker;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import mobileworkloads.jlagmarker.args.LagMarkerArgs;
import mobileworkloads.jlagmarker.markermodes.DetectorMode;
import mobileworkloads.jlagmarker.markermodes.FrameDumpMode;
import mobileworkloads.jlagmarker.markermodes.InteractiveSuggMode;
import mobileworkloads.jlagmarker.markermodes.LagmarkerMode;
import mobileworkloads.jlagmarker.markermodes.LagmarkerMode.LagmarkerModeType;
import mobileworkloads.jlagmarker.masking.ImgMask;
import mobileworkloads.jlagmarker.masking.MaskManager;

public class JLagmarkerMain {
		
	private static final int MAX_OUT_DIR_TRIES = 100;
	
	private LagmarkerMode mode; 
	
	public static void memOutput() {
		int mb = 1024*1024;
		
		//Getting the runtime reference from system
		Runtime runtime = Runtime.getRuntime();
		
		System.out.println("##### Heap utilization statistics [MB] #####");
		
		//Print used memory
		System.out.println("Used Memory:" 
			+ (runtime.totalMemory() - runtime.freeMemory()) / mb);

		//Print free memory
		System.out.println("Free Memory:" 
			+ runtime.freeMemory() / mb);
		
		//Print total available memory
		System.out.println("Total Memory:" + runtime.totalMemory() / mb);

		//Print Maximum available memory
		System.out.println("Max Memory:" + runtime.maxMemory() / mb);
	}
	
	public JLagmarkerMain(String[] cmdArgs) {

		LagMarkerArgs args = new LagMarkerArgs(cmdArgs);

		System.out.println("Creating output folder at: " + args.outputFolder + " ...");
		args.outputFolder = createOutputFolder(args.outputPrefix, 
				args.outputFolder.resolve("lmrun_" + args.modeType.name()
						+ (args.modeType == LagmarkerModeType.SUGGESTER && args.defaultSugg ? "_DEFAULT" : "") 
						+ "_" + args.outputPrefix));
		System.out.println();
		
		try {
			MaskManager.getInstance().parseMasks(args.maskSpec);
			System.out.println();
		} catch (IOException e) {
			throw new UncheckedIOException("Error parsing mask specification file [" + args.maskSpec + "]", e);
		}
		
		switch(args.modeType) {
			case SUGGESTER:			
				mode = new InteractiveSuggMode(args.videoFile.toString(), args.inputFlashOffsetNS, 
						args.inputData, args.sconfFile, args.outputPrefix, args.outputFolder, args.defaultSugg);
				break;
			case DETECTOR:
				mode = new DetectorMode(args.videoFile.toString(), args.inputFlashOffsetNS, 
						args.inputData, args.dconfFile, args.suggImgs, args.outputPrefix, args.outputFolder);
				break;
			case FRAMEDUMP:
				List<ImgMask> dumpMasks = args.dumpMaks.stream().map(name -> MaskManager.getInstance().getMask(name)).collect(Collectors.toList());
				mode = new FrameDumpMode(args.videoFile.toString(), args.outputPrefix, args.outputFolder,
						args.frameIntervals, dumpMasks);
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
		} catch(Exception e2) {
			System.out.println("Something went wrong: " + e2);
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
