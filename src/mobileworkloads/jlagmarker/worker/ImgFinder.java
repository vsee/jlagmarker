package mobileworkloads.jlagmarker.worker;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import mobileworkloads.jlagmarker.lags.Lag;
import mobileworkloads.jlagmarker.masking.MaskManager;
import mobileworkloads.jlagmarker.video.RGBImage;
import mobileworkloads.jlagmarker.video.RGBImgUtils;
import mobileworkloads.jlagmarker.video.VideoFrame;
import mobileworkloads.jlagmarker.worker.DetectorConfig.DetectorConfParams;

public class ImgFinder extends VStreamWorker {

	private class FrameFlankEntry {
		int frameId;
		boolean flank;
		
		public FrameFlankEntry(boolean flank, int frameId) {
			this.flank = flank;
			this.frameId = frameId;
		}
		
		public String toCSVEntry() {
			return (flank ? "1" : "0") + "," + frameId;
		}
	}	

	protected static final String FRAMEFLANK_HEADER = "frameflank,frameId";
	
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
	
	// frame flank properties
	protected VideoFrame prevFrame;
	protected boolean lastFlank;
	protected List<FrameFlankEntry> frameFlanks;
	
	public ImgFinder(Path outputFolder, Path dconfFile, Path suggImgs) {
		super(outputFolder.resolve("detections"), dconfFile);
		
		this.suggImgs = suggImgs;
		
		try {
			dconf = new DetectorConfig(dconfFile);
			System.out.println();
		} catch (IOException e) {
			throw new UncheckedIOException("Error parsing detector configuration file [" + dconfFile + "]", e);
		}
		
		prevFrame = null;
		lastFlank = false;
		frameFlanks = new ArrayList<FrameFlankEntry>();
	}
	
	@Override
	public void update(VideoFrame currentFrame) {
		generateFrameFlank(currentFrame);
		
		detectImage(currentFrame);
	}
	

	private void generateFrameFlank(VideoFrame currentFrame) {
		if(prevFrame != null) {
			boolean imgEqual = RGBImgUtils.cmpRGBImg(currentFrame.frameImg, prevFrame.frameImg, 
					dconfParams.mask, dconfParams.maxDiffThreshold, dconfParams.pixIgnore);
			if(!imgEqual) lastFlank = !lastFlank;
			
			frameFlanks.add(new FrameFlankEntry(lastFlank, currentFrame.videoFrameId));
		}
		prevFrame = currentFrame;
	}
	
	private void detectImage(VideoFrame currentFrame) {
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
			System.out.println("Lag " + currLag.lagId + ": Found at "
					+ detectedImgFrame.videoFrameId + " with mask "
					+ (dconfParams.mask == null ? MaskManager.NO_MASK_MARKER : dconfParams.mask) + ".");
			
			if(dconfParams.mask != null) detectedImgFrame.frameImg.applyMask(dconfParams.mask);
			try {
				detectedImgFrame.frameImg.writeToFile(outputFolder.resolve(String
						.format(FILE_NAME_IMAGE_FOUND_FORMAT, currLag.lagId, detectedImgFrame.videoFrameId)), false);
			} catch (IOException e) {
				throw new UncheckedIOException("Error saving detected image to file!", e);
			}
		}
	}

	public void dumpFrameFlanks(Path outputFileName) {
		try(BufferedWriter statWriter = Files.newBufferedWriter(outputFileName, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
			
			statWriter.write(FRAMEFLANK_HEADER);
			statWriter.newLine();
			
			for (FrameFlankEntry ff : frameFlanks) {
				statWriter.write(ff.toCSVEntry());
				statWriter.newLine();
			}
			
			System.out.println("Frame flanks written to " + outputFileName);
		} catch (IOException e) {
			System.out.println("Writing frame flanks to file failed!");
			throw new UncheckedIOException(e);
		}
	}
}
