package mobileworkloads.jlagmarker.markermodes;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

import mobileworkloads.jlagmarker.gui.SuggestionViewGenerator;
import mobileworkloads.jlagmarker.lags.Lag;
import mobileworkloads.jlagmarker.masking.MaskManager;
import mobileworkloads.jlagmarker.video.RGBImgUtils;
import mobileworkloads.jlagmarker.video.VideoFrame;
import mobileworkloads.jlagmarker.worker.Suggester;
import mobileworkloads.jlagmarker.worker.SuggesterConfig.SuggesterConfParams;

public class InteractiveSuggMode extends SuggesterMode {

	protected Scanner in;
	protected Lag currLag = null;
	
	public InteractiveSuggMode(String videoName, long inputFlashOffsetNS,
			Path inputData, Path sconfFile,	String outputPrefix, Path outputFolder) {
		
		super(videoName, inputFlashOffsetNS, inputData, sconfFile, outputPrefix, outputFolder);
		
		in = new Scanner(System.in);
	}
	
	@Override
	protected void findLags() {
		VideoFrame currFrame = vstate.extractCurrentFrame();
		while(currFrame != null) {
			processFrame(currFrame);
			currFrame = vstate.decodeNextVideoFrame();
		}
		
		if(getWorker().isActive()) {
			getWorker().terminate();
			acceptSuggestions(currFrame);
			vstate.stopRecording();
		}
	}
	
	@Override
	protected void processFrame(VideoFrame currFrame) {
		if(getWorker().isActive())
			getWorker().update(currFrame);
		
		if (isLagBeginFrame(currFrame)) {
			
			suggester.deactivateDumpAll();
			
			if(currLag == null || acceptSuggestions(currFrame)) {
				if(getWorker().isActive())
					getWorker().terminate();

				// suggestions are accepted: remove recordings from last lag
				vstate.stopRecording();
				
				
				// start recording of upcoming lag
				vstate.startRecording();
				
				currLag = lprofile.addNewLag(currFrame);
				System.out.println("\nLAG " + currLag.lagId + ": Beginning found at frame " + currFrame + ".");
				
				getWorker().start(currLag, currFrame);
			} else {
				prepareSuggRerun(currFrame);
			}
		}
	}

	protected void prepareSuggRerun(VideoFrame currFrame) {
		// rewind video to one after the begin frame of the current lag
		vstate.skipBackwards(currFrame.videoFrameId - currLag.startFrame.videoFrameId - 1);
		ieStream.resetCache(); // TODO improve this by setting input back to input idx of corresponding begin frame
		
		removeSuggestions(currLag);
		
		System.out.println("\nLAG " + currLag.lagId + ": Prepare Suggester Rerun:");
		System.out.println("1. Change Suggestion Parameters.");
		System.out.println("2. Toggle Frame Dump.");
		System.out.println("3. Rerun Suggestion.");
		while(true) {
			System.out.print("Action [default 3]: ");
			String line = in.nextLine();
			
			if(line.isEmpty() || line.equals("3")) {
				break; // accept default params
			} else if(line.equals("1")) {
				changeSuggParameters();
			} else if(line.equals("2")) {
				suggester.toggleDumpAll();
			} else {
				System.out.println("Invalid input: " + line);
			}
		}
	}

	protected void createDiffImage(VideoFrame currFrame) {
		while(true) {
			System.out.print("Select frames to diff or leave empty to cancel: ");
			String line = in.nextLine();

			if(line.isEmpty()) {
				break; // cancel diff image creation
			} else {
				String[] frames = line.split(" ");
				if(frames.length < 2 || frames.length > 4) {
					System.out.println("Invalid input: " + line);
					continue;
				}
				
				try {
					int frame0 = Integer.parseInt(frames[0]);
					int frame1 = Integer.parseInt(frames[1]);
					
					String mask = null;
					if(frames.length >= 3)
						mask = frames[2].equals(MaskManager.NO_MASK_MARKER) ? null : frames[2];
					
					int fuzz = 5;
					if(frames.length == 4) fuzz = Integer.parseInt(frames[3]);
					
					String[] res = RGBImgUtils.generateDiffImage(outputFolder, mask, 
							vstate.getFrameFromHistory(currFrame.videoFrameId - frame0).frameImg, 
							vstate.getFrameFromHistory(currFrame.videoFrameId - frame1).frameImg,
							fuzz).trim().split(" ");
					Integer diff = Integer.parseInt(res[res.length - 1]);
					System.out.println("Diff: " + frame0 + " -- " + frame1 + ": " + diff);
				} catch (IllegalArgumentException e) {
					System.out.println("Invalid input: " + line);
				} catch (IOException e) {
					System.out.println("Error writing diff image to file.");
					e.printStackTrace();
				}
			}
		}
	}

	protected void changeSuggParameters() {
		SuggesterConfParams params = suggester.getSuggesterParams();
		
		while(true) {
			System.out.print("Set suggestions parameters? " + params + " ");
			String line = in.nextLine();
			
			if(line.isEmpty()) {
				break; // accept default params
			} else {
				try {
					suggester.changeSuggesterParams(currLag.lagId, line);
					break;
				} catch (IllegalArgumentException e) {
					System.out.println("Invalid input: " + line);
				}
			}
		}
	}

	protected boolean acceptSuggestions(VideoFrame currFrame) {
		SuggestionViewGenerator.generateSuggestionView(outputFolder.resolve("markup.html"), lprofile);
		
		System.out.println("Suggester finished: ");
		System.out.println("1. Accept Suggestions.");
		System.out.println("2. Rerun Suggester.");
		System.out.println("3. Skip Lag.");
		System.out.println("4. Run Image Diff.");
		boolean suggAccept = false;
		while(true) {

			System.out.print("Pick an action [default 1]: ");
			String line = in.nextLine();
			
			if (line.isEmpty() || line.equalsIgnoreCase("1")) {
				selectSuggestion();
				suggAccept = true;
				break;
			} else if (line.equalsIgnoreCase("2")) {
				break;
			} else if (line.equals("3")) {
				System.out.println("Skipping lag.");
				currLag.setSkip();
				suggAccept = true;
				break;
			} else if (line.equalsIgnoreCase("4")) {
				createDiffImage(currFrame);
			}  else {
				System.out.println("Invalid input: " + line);
			}
		}
		
		return suggAccept;
	}
	
	protected void selectSuggestion() {
		while(true) {
			
			List<Integer> suggestionIds = currLag.getSuggestionIds();
			if(suggestionIds.size() == 1) {
				currLag.acceptSuggestion(suggestionIds.get(0));
				break;
			} else if(suggestionIds.size() == 0) {
				System.out.println("LAG " + currLag.lagId + ": No suggestions found!");
				break;
			}
			
			System.out.print("Select suggestion: " + suggestionIds + " ");
			String line = in.nextLine();

			try {
				int selectedId = Integer.parseInt(line);

				if (!suggestionIds.contains(selectedId)) {
					System.out.println("Invalid input: " + line);
				} else {
					currLag.acceptSuggestion(selectedId);
					break;
				}

			} catch (IllegalArgumentException e) {
				System.out.println("Invalid input: " + line);
				continue;
			}
		}
		
		System.out.println("LAG " + currLag.lagId + ": Suggestion accepted!\n" + currLag);
	}

	protected void removeSuggestions(Lag lag) {
		lag.getSuggestionIds().stream().forEach(
			id -> {
				Path sugFile = Paths.get(suggester.getOutputFolder()).resolve(
						String.format(Suggester.FILE_NAME_SUGGESTION_FORMAT, currLag.lagId, id)
						);
				try {
					Files.delete(sugFile);
					Files.delete(Paths.get(sugFile.toString().replace(".ppm", ".jpg")));
				} catch (IOException e) {
					throw new UncheckedIOException("Error deleting suggested image: " + sugFile, e);
				}
			});

		lag.clearSuggestion();
	}
	
	@Override
	protected void dumpRunStats(Path outputFileName, long runtimeMS) {
		super.dumpRunStats(outputFileName, runtimeMS);
		suggester.saveConfigToFile();
		suggester.generateDetectorConfig(lprofile);
	}
}
