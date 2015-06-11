package mobileworkloads.jlagmarker.worker;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import mobileworkloads.jlagmarker.lags.Lag;
import mobileworkloads.jlagmarker.video.RGBImgUtils;
import mobileworkloads.jlagmarker.video.VideoFrame;
import mobileworkloads.jlagmarker.worker.SuggesterConfig.SuggesterConfParams;

public class SISuggester extends Suggester {
	
	// TODO imagemagick comparison
	
	protected boolean firstChangeFound;	// wait until a first change was found
	protected int stillFrameCount;	  	// look for a still period of at least x frames
	
	protected VideoFrame latestStillFrame; // the latest still frame encountered (the first of a row of consecutive frames which were the same) 
	
	protected final SuggesterConfig sconf;
	protected SuggesterConfParams sconfParams;	// suggester configuration parameters for the current lag
	protected Lag currLag;
			
	// Still image suggester
	public SISuggester(Path outputFolder, Path sconfFile) {
		super(outputFolder, sconfFile);
		
		try {
			sconf = new SuggesterConfig(sconfFile);
			System.out.println();
		} catch (IOException e) {
			throw new UncheckedIOException("Error parsing suggester configuration file [" + sconfFile + "]", e);
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
	
	@Override
	public void update(VideoFrame currentFrame) {

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
		lag.addSuggestion(suggFrame);
 		
		if(mask != null) suggFrame.frameImg.applyMask(mask);
		try {
			suggFrame.frameImg.dataBuffer.writeToFile(outputFolder.resolve(String
					.format(FILE_NAME_SUGGESTION_FORMAT, lag.lagId,	suggFrame.videoFrameId)));
		} catch (IOException e) {
			throw new UncheckedIOException("Error saving latest suggestion to file!", e);
		}
	}

	protected boolean compareFrames(VideoFrame frame0, VideoFrame frame1, String mask, int threshold, int maxPixelIgnore) {

		if(frame0.equals(frame1)) return true;
		else return RGBImgUtils.cmpRGBImg(frame0.frameImg, frame1.frameImg, mask, threshold, maxPixelIgnore);
	}	
}
