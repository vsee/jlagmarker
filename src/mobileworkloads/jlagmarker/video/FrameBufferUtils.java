package mobileworkloads.jlagmarker.video;

import java.util.ArrayList;
import java.util.List;

public final class FrameBufferUtils {
	
	private FrameBufferUtils() { }

	private static boolean maxMetricDiff(VideoFrame frame0, VideoFrame frame1, int threshold, int maxPixelIgnore) {
		if(frame0 == null || frame1 == null) throw new IllegalArgumentException("Given frames must not be null.");
		assert frame0.dataBuffer.getHeight() == frame1.dataBuffer.getHeight()
				&& frame0.dataBuffer.getWidth() == frame1.dataBuffer.getWidth() : "Given frame dimensions do not match.";
		
		int ignoredPixels = 0;
		
		for(int y = 0; y < frame0.dataBuffer.getHeight(); y++) {
			for(int x = 0; x < frame0.dataBuffer.getWidth(); x++) {
				for(int channel = 0; channel < JRGBFrameBuffer.CHANNEL_NUM; channel++) {
					boolean pixelDiffers = false;
					
					for (int channelIdx = 0; channelIdx < JRGBFrameBuffer.CHANNEL_NUM; channelIdx++) {
						int d0 = frame0.dataBuffer.getChannel(x, y, channel);
						int d1 = frame1.dataBuffer.getChannel(x, y, channel);
						int dist = Math.abs(d1 - d0);
						pixelDiffers |= dist > threshold;
					}
					
					if(pixelDiffers) {
						ignoredPixels++;
						if(ignoredPixels > maxPixelIgnore) return false;
					}
				}
			}
		}
		
		return true;
	}
	
	public static boolean cmpRGBBuff(VideoFrame frame0,
			VideoFrame frame1, int threshold, int maxPixelIgnore) {

		return cmpRGBBuff(frame0, frame1, new ArrayList<String>(), threshold, maxPixelIgnore);
	}

	public static boolean cmpRGBBuff(VideoFrame frame0,
			VideoFrame frame1, String mask, int threshold, int maxPixelIgnore) {
		
		List<String> masks = new ArrayList<String>();
		masks.add(mask);

		return cmpRGBBuff(frame0, frame1, masks, threshold, maxPixelIgnore);
	}

	public static boolean cmpRGBBuff(VideoFrame frame0,
			VideoFrame frame1, List<String> masks, int threshold, int maxPixelIgnore) {
		
		if ((frame0 == null && frame1 != null) || (frame0 != null && frame1 == null))
			return false; // if one of the buffers is null and the other is not

		if (frame0 == null && frame1 == null)
			return true; // if both buffers are null
		
		boolean framesEqual = false;

		int comparisonsLeft = !masks.isEmpty() ? masks.size() : 1; // if no masks specified compare only the unmasked images
		int currentMask = 0;
		
		while(comparisonsLeft > 0 && !framesEqual) {

			VideoFrame maskedFrame0 = frame0.clone();
			VideoFrame maskedFrame1 = frame1.clone();
			
			if(!masks.isEmpty()) {
				maskedFrame0.applyMask(masks.get(currentMask));
				maskedFrame1.applyMask(masks.get(currentMask));

			}
			
			framesEqual = maxMetricDiff(maskedFrame0, maskedFrame1, threshold, maxPixelIgnore);
			
			currentMask++;
			comparisonsLeft--;
		}
		
		return framesEqual;
	}

}
