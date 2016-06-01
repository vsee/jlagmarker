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
import mobileworkloads.jlagmarker.masking.ImgMask;
import mobileworkloads.jlagmarker.masking.MaskManager;
import mobileworkloads.jlagmarker.video.RGBImgUtils;
import mobileworkloads.jlagmarker.video.VideoFrame;
import mobileworkloads.jlagmarker.worker.DetectorConfig;
import mobileworkloads.jlagmarker.worker.DetectorConfig.DetectorConfParams;
import mobileworkloads.jlagmarker.worker.Suggester;
import mobileworkloads.jlagmarker.worker.SuggesterConfig.SuggesterConfParams;

public class InteractiveSuggMode extends SuggesterMode {

	protected Scanner in;
	protected Lag currLag = null;
	protected boolean frameProcessingFinished;
	protected boolean defaultSugg;
	protected int autoAcceptLimit = 0;
	
	protected final DetectorConfig dconf;
	
	public InteractiveSuggMode(String videoName, long inputFlashOffsetNS,
			Path inputData, Path sconfFile,	Path dconfFile, String outputPrefix, Path outputFolder, boolean defaultSugg) {
		
		super(videoName, inputFlashOffsetNS, inputData, sconfFile, outputPrefix, outputFolder);
		
		in = new Scanner(System.in);
		frameProcessingFinished = false;
		this.defaultSugg = defaultSugg;
		
		// parse detector config if specified
		if(dconfFile != null) {
			try {
				dconf = new DetectorConfig(dconfFile);
				System.out.println();
			} catch (IOException e) {
				throw new UncheckedIOException("Error parsing detector configuration file [" + dconfFile + "]", e);
			}
		} else {
			dconf = null;
		}
	}
	
	@Override
	protected void findLags() {
		VideoFrame currFrame = vstate.extractCurrentFrame();
		
		while(!frameProcessingFinished) {
			processFrame(currFrame);
			currFrame = vstate.decodeNextVideoFrame();
		}
	}
	
	@Override
	protected void processFrame(VideoFrame currFrame) {
		if(getWorker().isActive() && !vstate.isEndOfStream())
			getWorker().update(currFrame);
		
		if (vstate.isEndOfStream() || isLagBeginFrame(currFrame)) {
			
			suggester.deactivateDumpAll();
			
			if(currLag == null || acceptSuggestions(currFrame)) {
				if(getWorker().isActive())
					getWorker().terminate();
				
				// suggestions are accepted: remove recordings from last lag
				vstate.stopRecording();
				// save intermediate configuration file
				suggester.saveConfigToFile();
				// release buffer data
				if(currLag != null) lprofile.lags.get(currLag.lagId).releaseFrameResources();
				System.gc();
				
				if(vstate.isEndOfStream()) {
					frameProcessingFinished = true;
					return;
				}
				
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
		System.out.println("\nLAG " + currLag.lagId + ": Prepare Suggester Rerun:");
		System.out.println("1. Change Suggestion Parameters.");
		System.out.println("2. Change Default Suggestion Parameters.");
		System.out.println("3. Toggle Frame Dump.");
		System.out.println("4. Reload mask config.");
		System.out.println("5. Rerun Suggestion.");
		while(true) {
			System.out.print("Action [default 5]: ");
			String line = in.nextLine();

			if (line.equals("1")) {
				changeSuggParameters(false);
			} else if (line.equals("2")) {
				changeSuggParameters(true);
			} else if (line.equals("3")) {
				suggester.toggleDumpAll();
			}  else if (line.equals("4")) {
				MaskManager.getInstance().reloadMasks();
			} else if (line.equals("5") || line.isEmpty()) {
				break; // accept default params
			} else {
				System.out.println("Invalid input: " + line);
			}
		}
		
		// rewind video to one after the begin frame of the current lag
		vstate.skipBackwards(currFrame.videoFrameId - currLag.startFrame.videoFrameId - 1);
		ieStream.resetCache(); // TODO improve this by setting input back to input idx of corresponding begin frame
		
		removeSuggestions(currLag);
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
					
					ImgMask mask = null;
					if(frames.length >= 3) {
						mask = frames[2].equals(MaskManager.NO_MASK_MARKER) ? 
								null : MaskManager.getInstance().getMask(frames[2]);
					}
					
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

	protected void changeSuggParameters(boolean defaultParams) {
		SuggesterConfParams params = suggester.getSuggesterParams();
		
		while(true) {
			System.out.print("Set suggestions parameters? " + params + " ");
			String line = in.nextLine();
			
			if(line.isEmpty()) {
				break; // accept default params
			} else {
				try {
					suggester.changeSuggesterParams(currLag.lagId, line, defaultParams);
					break;
				} catch (IllegalArgumentException e) {
					System.out.println("Invalid input: " + line);
				}
			}
		}
	}

	protected boolean acceptSuggestions(VideoFrame currFrame) {
		SuggestionViewGenerator.generateSuggestionView(outputFolder.resolve("markup.html"), lprofile);
		
		Integer presetSuggId = dconf != null ? 
				((DetectorConfParams) dconf.getParams(currLag.lagId)).suggestionId : 
				null; 
		
		System.out.println("Suggester finished: ");
		System.out.println("1. Accept Suggestions.");
		System.out.println("2. Rerun Suggester.");
		System.out.println("3. Skip Lag.");
		System.out.println("4. Run Image Diff.");
		System.out.println("5. Toggle Auto Accept ["
				+ (autoAcceptLimit > 0 ? "ACTIVE for the next " + autoAcceptLimit : "INACTIVE") + "].");
		boolean suggAccept = false;
		while(true) {

			System.out.print("Pick an action [default 1]: ");
			String line = "";
			if(defaultSugg || 
			   (autoAcceptLimit > 0 && // autoaccept is active
					  (presetSuggId != null) || // suggestion id is given
					  (presetSuggId == null && currLag.getSuggestionIds().size() == 1) // only one suggestion id is possible
			    )
			   ) {
				
				if(autoAcceptLimit > 0) autoAcceptLimit--;
				if(presetSuggId == -1) line = "3"; // SKIP is preset in detector config
				else line = "1";
			} else {
				line = in.nextLine();
			}
	
			
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
			}  else if(line.equalsIgnoreCase("5")) {
				autoAcceptLimit = setAutoAcceptLimit();
				System.out.println("Auto accept: " + (autoAcceptLimit > 0 ? "ACTIVE for the next " + autoAcceptLimit : "INACTIVE"));
			} else {
				System.out.println("Invalid input: " + line);
			}
		}
		
		return suggAccept;
	}
	
	private int setAutoAcceptLimit() {
		while(true) {		
			System.out.print("Enter auto accept limit: ");
			String line = in.nextLine();

			try {
				int limit = Integer.parseInt(line);

				if (limit < 0) {
					System.out.println("Invalid input: " + line);
				} else {
					return limit;
				}

			} catch (IllegalArgumentException e) {
				System.out.println("Invalid input: " + line);
				continue;
			}
		}
	}

	protected void selectSuggestion() {
		while(true) {
			
			List<Integer> suggestionIds = currLag.getSuggestionIds();
			if(suggestionIds.size() == 0) {
				System.out.println("LAG " + currLag.lagId + ": No suggestions found!");
				break;
			} else if(suggestionIds.size() == 1 || defaultSugg) {
				currLag.acceptSuggestion(suggestionIds.get(0));
				break;
			} 
			
			System.out.print("Select suggestion: " + suggestionIds + " ");
			String line = null;
			if(dconf == null) line = in.nextLine();
			else line = "" + ((DetectorConfParams) dconf.getParams(currLag.lagId)).suggestionId;

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
