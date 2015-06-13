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
import mobileworkloads.jlagmarker.video.VideoFrame;
import mobileworkloads.jlagmarker.worker.Suggester;
import mobileworkloads.jlagmarker.worker.SuggesterConfig.SuggesterConfParams;

public class InteractiveSuggMode extends SuggesterMode {

	public InteractiveSuggMode(String videoName, long inputFlashOffsetNS,
			Path inputData, Path sconfFile,	String outputPrefix, Path outputFolder) {
		
		super(videoName, inputFlashOffsetNS, inputData, sconfFile, outputPrefix, outputFolder);
	}
	
	Scanner in = new Scanner(System.in);
	Lag currLag = null;
	
	@Override
	protected void findLags() {
		VideoFrame currFrame = vstate.extractCurrentFrame();
		while(currFrame != null) {
			processFrame(currFrame);
			currFrame = vstate.decodeNextVideoFrame();
		}
		
		if(getWorker().isActive()) {
			getWorker().terminate();
			acceptSuggestions();
			vstate.stopRecording();
		}
	}
	
	@Override
	protected void processFrame(VideoFrame currFrame) {
		if(getWorker().isActive())
			getWorker().update(currFrame);
		
		if (isLagBeginFrame(currFrame)) {
			
			if(currLag == null || acceptSuggestions()) {
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
		
		System.out.println("\nLAG " + currLag.lagId + ": Rerunning suggester.");
		changeSuggParameters();
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

	protected boolean acceptSuggestions() {
		SuggestionViewGenerator.generateSuggestionView(outputFolder.resolve("markup.html"), lprofile);
		
		boolean suggAccept = false;
		while(true) {
			System.out.print("Accept Suggestions? [Y/n/s(kip)] ");
			String line = in.nextLine();
			
			if(line.isEmpty() || line.equalsIgnoreCase("y")) {
				selectSuggestion();
				suggAccept = true;
				break;
			} else if(line.equalsIgnoreCase("s")) {
				System.out.println("Skipping lag.");
				currLag.setSkip();
				suggAccept = true;
				break;
			} else {
				if(line.equalsIgnoreCase("n")) {
					break;
				} else {
					System.out.println("Invalid input: " + line);
				}
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
