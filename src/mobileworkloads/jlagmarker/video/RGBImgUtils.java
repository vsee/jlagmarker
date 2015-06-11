package mobileworkloads.jlagmarker.video;

import java.util.ArrayList;
import java.util.List;

public final class RGBImgUtils {
	
	private RGBImgUtils() { }

	private static boolean maxMetricDiff(RGBImage img0, RGBImage img1, int threshold, int maxPixelIgnore) {
		if(img0 == null || img1 == null) throw new IllegalArgumentException("Given image must not be null.");
		assert img0.dataBuffer.getHeight() == img1.dataBuffer.getHeight()
				&& img0.dataBuffer.getWidth() == img1.dataBuffer.getWidth() : "Given image dimensions do not match.";
		
		int ignoredPixels = 0;
		
		for(int y = 0; y < img0.dataBuffer.getHeight(); y++) {
			for(int x = 0; x < img0.dataBuffer.getWidth(); x++) {
				for(int channel = 0; channel < JRGBFrameBuffer.CHANNEL_NUM; channel++) {
					boolean pixelDiffers = false;
					
					for (int channelIdx = 0; channelIdx < JRGBFrameBuffer.CHANNEL_NUM; channelIdx++) {
						int d0 = img0.dataBuffer.getChannel(x, y, channel);
						int d1 = img1.dataBuffer.getChannel(x, y, channel);
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
	
	public static boolean cmpRGBImg(RGBImage img0,
			RGBImage img1, int threshold, int maxPixelIgnore) {

		return cmpRGBImg(img0, img1, new ArrayList<String>(), threshold, maxPixelIgnore);
	}

	public static boolean cmpRGBImg(RGBImage img0,
			RGBImage img1, String mask, int threshold, int maxPixelIgnore) {
		
		List<String> masks = new ArrayList<String>();
		if(mask != null) masks.add(mask);

		return cmpRGBImg(img0, img1, masks, threshold, maxPixelIgnore);
	}

	public static boolean cmpRGBImg(RGBImage img0,
			RGBImage img1, List<String> masks, int threshold, int maxPixelIgnore) {
		
		if ((img0 == null && img1 != null) || (img0 != null && img1 == null))
			return false; // if one of the buffers is null and the other is not

		if (img0 == null && img1 == null)
			return true; // if both buffers are null
		
		boolean imgsEqual = false;

		int comparisonsLeft = !masks.isEmpty() ? masks.size() : 1; // if no masks specified compare only the unmasked images
		int currentMask = 0;
		
		while(comparisonsLeft > 0 && !imgsEqual) {

			RGBImage maskedImg0 = img0.clone();
			RGBImage maskedImg1 = img1.clone();
			
			if(!masks.isEmpty()) {
				maskedImg0.applyMask(masks.get(currentMask));
				maskedImg1.applyMask(masks.get(currentMask));

			}
			
			imgsEqual = maxMetricDiff(maskedImg0, maskedImg1, threshold, maxPixelIgnore);
			
			currentMask++;
			comparisonsLeft--;
		}
		
		return imgsEqual;
	}

}
