package mobileworkloads.jlagmarker.worker;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import mobileworkloads.jlagmarker.lags.Lag;
import mobileworkloads.jlagmarker.video.RGBImage;
import mobileworkloads.jlagmarker.video.RGBImgUtils;
import mobileworkloads.jlagmarker.video.VideoFrame;
import mobileworkloads.jlagmarker.worker.DetectorConfig.DetectorConfParams;

public class ImgFinder extends VStreamWorker {

	public static final String FILE_NAME_IMAGE_FOUND_FORMAT = "lag_%03d_found_%d.ppm";
	public static final String FILE_NAME_DETECTION_CONFIG = "detector_config.csv";

	protected final Path suggImgs;
	protected final DetectorConfig dconf;
	
	protected Lag currLag;
	protected RGBImage refImg; // reference image to find the frame we are looking for
	protected DetectorConfParams dconfParams;
	
	protected VideoFrame detectedImgFrame;
	protected boolean imageDetected;;
	protected boolean skipLag;

	protected boolean changeSinceBegin; // true if the image looked different at least once since the begin frame
	
	public ImgFinder(Path outputFolder, Path dconfFile, Path suggImgs) {
		super(outputFolder, dconfFile);
		
		this.suggImgs = suggImgs;
		
		try {
			dconf = new DetectorConfig(dconfFile);
			System.out.println();
		} catch (IOException e) {
			throw new UncheckedIOException("Error parsing detector configuration file [" + dconfFile + "]", e);
		}
	}

	@Override
	public void update(VideoFrame currentFrame) {
		if(imageDetected || skipLag) return; // image already found or no need to find it

		// did the image look different from the lag start image at least once yet?
		if(!changeSinceBegin) {
			changeSinceBegin = !RGBImgUtils.cmpRGBImg(currentFrame.frameImg, currLag.startFrame.frameImg,
					dconfParams.mask, dconfParams.maxDiffThreshold, dconfParams.pixIgnore);
		} else {
			// current image is like the image to be found --> success
			if(RGBImgUtils.cmpRGBImg(currentFrame.frameImg, refImg, 
					dconfParams.mask, dconfParams.maxDiffThreshold, dconfParams.pixIgnore)) {
				
				imageDetected = true;
				detectedImgFrame = currentFrame.clone();
			}
		}
	}
	
	@Override
	public void start(Lag currLag, VideoFrame currFrame) {
		super.start(currLag, currFrame);
		
		this.dconfParams = (DetectorConfParams) dconf.getParams(currLag.lagId);
		this.currLag = currLag;
		
		detectedImgFrame = null;
		imageDetected = false;
		
		changeSinceBegin = false;
		
		// do we need to skip the lag?
		if(dconfParams.suggestionId < 0) {
			System.out.println("Lag " + currLag.lagId + " skipped.");
			skipLag = true;
			return;
		} else {
			skipLag = false;
		}
		
		Path refImgFileName = suggImgs.resolve(String.format(
				Suggester.FILE_NAME_SUGGESTION_FORMAT, currLag.lagId, dconfParams.suggestionId));
		try {		
			refImg = new RGBImage(refImgFileName);
		} catch (IOException e) {
			throw new UncheckedIOException("Error reading image file [" + refImgFileName + "]", e);
		}
	}
	
	@Override
	public void terminate() {
		super.terminate();
		
		if(!imageDetected) {
			if(skipLag) {
				currLag.setSkip();
			} else {
				System.out.println("Lag " + currLag.lagId + ": Ending not found in maximum search interval.");
			}
		} else {
			currLag.setEndFrame(detectedImgFrame);
			System.out.println("Lag " + currLag.lagId + ": Found at " + detectedImgFrame.videoFrameId + " with mask " + dconfParams.mask + ".");
			
			if(dconfParams.mask != null) detectedImgFrame.frameImg.applyMask(dconfParams.mask);
			try {
				detectedImgFrame.frameImg.writeToFile(outputFolder.resolve(String
						.format(FILE_NAME_IMAGE_FOUND_FORMAT, currLag.lagId, detectedImgFrame.videoFrameId)), false);
			} catch (IOException e) {
				throw new UncheckedIOException("Error saving detected image to file!", e);
			}
		}
	}
}
