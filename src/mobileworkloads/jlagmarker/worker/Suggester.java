package mobileworkloads.jlagmarker.worker;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import mobileworkloads.jlagmarker.lags.Lag;
import mobileworkloads.jlagmarker.lags.LagProfile;
import mobileworkloads.jlagmarker.video.RGBImgUtils;
import mobileworkloads.jlagmarker.video.VideoFrame;
import mobileworkloads.jlagmarker.worker.SuggesterConfig.SuggesterConfParams;
import mobileworkloads.mlgovernor.res.CSVResourceTools;

public class Suggester extends VStreamWorker {
	
	public static final String FILE_NAME_SUGGESTION_FORMAT = "lag_%03d_sug_%d.ppm";
	public static final String FILE_NAME_SUGGESTION_CONFIG = "suggester_config.csv";
	
	// TODO imagemagick comparison
	// add cmp method [FUZZY, MAXMETRIC] to suggester and detector conf
	// add fuzz factor to suggester and detector conf
	// provide selection for both in menu 
	
	// TODO add verbose mode to show diff output
	
	protected boolean firstChangeFound;	// wait until a first change was found
	protected int stillFrameCount;	  	// look for a still period of at least x frames
	
	protected VideoFrame latestStillFrame; // the latest still frame encountered (the first of a row of consecutive frames which were the same) 
	
	protected final SuggesterConfig sconf;
	protected SuggesterConfParams sconfParams;	// suggester configuration parameters for the current lag
	protected Lag currLag;
	
	protected boolean dumpAll; // dump all frames you encounter
	

	// Still image suggester
	public Suggester(Path outputFolder, Path sconfFile) {
		super(outputFolder, sconfFile);

		if(configFile == null) {
			sconfFile = outputFolder.resolve(FILE_NAME_SUGGESTION_CONFIG);
			sconf = new SuggesterConfig();
		} else {
			try {
				sconf = new SuggesterConfig(sconfFile);
				System.out.println();
			} catch (IOException e) {
				throw new UncheckedIOException("Error parsing suggester configuration file [" + sconfFile + "]", e);
			}
		}
	}
	
	@Override
	public void start(Lag currLag, VideoFrame currFrame) {
		super.start(currLag, currFrame);
		
		firstChangeFound = false;
		stillFrameCount = 0;
		latestStillFrame = currFrame;
		
		this.currLag = currLag;
		this.sconfParams = (SuggesterConfParams) sconf.getParams(currLag.lagId);
		System.out.println("LAG " + currLag.lagId + ": Suggester params " + sconfParams);
	}
	
	public SuggesterConfParams getSuggesterParams() {
		return sconfParams;
	}
	
	public void toggleDumpAll() {
		dumpAll = !dumpAll;
		System.out.println("Frame dump: " + (dumpAll ? "ACTIVE" : "INACTIVE"));
	}
	
	public void deactivateDumpAll() {
		dumpAll = false;
	}

	public void changeSuggesterParams(int lagId, String line) {
		List<String> paramsList = 
				Arrays.asList((lagId + CSVResourceTools.SEPARATOR + line).split(CSVResourceTools.SEPARATOR));
		
		SuggesterConfParams params = (SuggesterConfParams) sconf.parseParams(paramsList);
		sconfParams.setCopy(params);
		
		System.out.println("LAG " + currLag.lagId + ": Suggester params " + sconfParams);
	}
	
	@Override
	public void update(VideoFrame currentFrame) {

		if(dumpAll) {
			saveSuggestion(currLag, currentFrame.clone(), sconfParams.mask);
			return;
		}
		
		boolean framesEqual = compareFrames(currentFrame, latestStillFrame, sconfParams.mask, sconfParams.maxDiffThreshold, sconfParams.pixIgnore);

		if (!firstChangeFound) {
			// current image differs from previous one --> found first change
			if (!framesEqual) {
				firstChangeFound = true;
			} else {
				return;
			}
		}

		// current image is like the previous one --> raise still counter
		if (framesEqual) {

			stillFrameCount++;

			if (stillFrameCount >= sconfParams.stillFrames) {
				// we found a still period with a valid length
				// save suggestion image and reset suggester
				saveSuggestion(currLag, latestStillFrame.clone(), sconfParams.mask);

				firstChangeFound = false;
			}

		} else { // reset still counter and save current frame as new reference
			stillFrameCount = 0;
			latestStillFrame = currentFrame;
		}
	}

	protected void saveSuggestion(Lag lag, VideoFrame suggFrame, String mask) {
		if(mask != null) suggFrame.frameImg.applyMask(mask);
		
		try {
			suggFrame.frameImg.writeToFile(outputFolder.resolve(String
					.format(FILE_NAME_SUGGESTION_FORMAT, lag.lagId,	suggFrame.videoFrameId)), true);
		} catch (IOException e) {
			throw new UncheckedIOException("Error saving latest suggestion to file!", e);
		}
		
		lag.addSuggestion(suggFrame);
	}

	protected boolean compareFrames(VideoFrame frame0, VideoFrame frame1, String mask, int threshold, int maxPixelIgnore) {

		if(frame0.equals(frame1)) return true;
		else return RGBImgUtils.cmpRGBImg(frame0.frameImg, frame1.frameImg, mask, threshold, maxPixelIgnore);
//		try {
//			String[] res = RGBImgUtils.generateDiffImage(outputFolder, mask, frame0.frameImg.clone(), frame1.frameImg.clone(), 5).trim().split(" ");
//			Integer diff = Integer.parseInt(res[res.length - 1]);
//			System.out.println("Diff: " + frame0.videoFrameId + " -- " + frame1.videoFrameId + ": " + diff);
//			return diff == 0;
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return false;
	}

	public void saveConfigToFile() {
		Path suggConfFileName = outputFolder.resolve(FILE_NAME_SUGGESTION_CONFIG);
		sconf.saveToFile(suggConfFileName);
		System.out.println("Suggester configuration written to " + suggConfFileName);
	}

	public void generateDetectorConfig(LagProfile lprofile) {
		Path detectConfFileName = outputFolder.resolve(ImgFinder.FILE_NAME_DETECTION_CONFIG);
		DetectorConfig dconf = new DetectorConfig();
		for(Lag l : lprofile.lags) {
			SuggesterConfParams sp = (SuggesterConfParams) sconf.getParams(l.lagId);
			dconf.createFromSuggParamsf(l, sp);
		}
		dconf.saveToFile(detectConfFileName);
		System.out.println("Detector configuration written to " + detectConfFileName);
	}
}
