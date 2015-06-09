package mobileworkloads.jlagmarker.suggesting;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import mobileworkloads.jlagmarker.lags.Lag;
import mobileworkloads.jlagmarker.video.FrameBufferUtils;
import mobileworkloads.jlagmarker.video.VideoFrame;

public class SISuggester extends Suggester {
	
	// TODO cached comparison
	
	protected boolean firstChangeFound;	// wait until a first change was found
	protected int stillFrameCount;	  	// look for a still period of at least x frames
	
	protected VideoFrame latestStillFrame; // the latest still frame encountered (the first of a row of consecutive frames which were the same) 
	
	protected SuggesterConfParams sconf;	// suggester configuration parameters for the current lag
	protected Lag currLag;
	
	protected Path outputFolder;
	
	// Still image suggester
	public SISuggester(Path outputFolder) {
		if(!(Files.exists(outputFolder) && Files.isDirectory(outputFolder))) {
			try {
				Files.createDirectory(outputFolder);
				System.out.println("SI suggester output directory created: " + outputFolder);
			} catch (IOException e) {
				throw new UncheckedIOException("Error creating SI suggester output directory: " + outputFolder, e);
			}
		}
		
		this.outputFolder = outputFolder; 
	}
	
	@Override
	public void start(Lag currLag, SuggesterConfParams sconf, VideoFrame currFrame) {
		super.start(currLag, sconf, currFrame);
		
		firstChangeFound = false;
		stillFrameCount = 0;
		latestStillFrame = currFrame;
		
		this.currLag = currLag;
		this.sconf = sconf;
	}
	
	@Override
	public void update(VideoFrame currentFrame) {

		boolean framesEqual = compareFrames(currentFrame, latestStillFrame, sconf.mask, sconf.maxDiffThreshold, sconf.pixIgnore);

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

			if (stillFrameCount >= sconf.stillFrames) {
				// we found a still period with a valid length
				// save suggestion image and reset suggester
				
				saveSuggestion(currLag, latestStillFrame.clone(), sconf.mask);
				// save_sugg(siParams->lentry->lagId, siParams->stillStartId,
				// 			 siParams->rgbBuff_ref, siParams->mask);

				firstChangeFound = false;
			}

		} else { // reset still counter and save current frame as new reference
			stillFrameCount = 0;
			latestStillFrame = currentFrame;
		}
	}

	protected void saveSuggestion(Lag lag, VideoFrame suggFrame, String mask) {
		System.out.println(String.format("LAG %d: New suggestion found at frame %s!", lag.lagId, suggFrame.toString()));

		suggFrame.applyMask(mask);
		try {
			suggFrame.dataBuffer.writeToFile(outputFolder.resolve(String
					.format(FILE_NAME_SUGGESTION_FORMAT, lag.lagId,	suggFrame.videoFrameId)));
		} catch (IOException e) {
			throw new UncheckedIOException("Error saving latest suggestion to file!", e);
		}
	}

	protected boolean compareFrames(VideoFrame frame0, VideoFrame frame1, String mask, int threshold, int maxPixelIgnore) {

		if(frame0.equals(frame1)) return true;
		else return FrameBufferUtils.cmpRGBBuff(frame0, frame1, mask, threshold, maxPixelIgnore);
	}
	
}
