/**
*
* Copyright 2017 Volker Seeker <volker@seekerscience.co.uk>.
*
* This file is part of JLagmarker.
*
* JLagmarker is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* JLagmarker is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with JLagmarker. If not, see <http://www.gnu.org/licenses/>.
*
*/
package mobileworkloads.jlagmarker.video;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import mobileworkloads.jlagmarker.masking.ImgMask;

public final class RGBImgUtils {
	
	private RGBImgUtils() { }

	private static boolean maxMetricDiff(RGBImage img0, RGBImage img1, int threshold, int maxPixelIgnore) {
		if(img0.getDataReleased() || img1.getDataReleased()) 
			throw new RuntimeException("Data buffer released unexpectedly when trying comparing images.");
		JRGBFrameBuffer buff0 = img0.getDataBuffer();
		JRGBFrameBuffer buff1 = img1.getDataBuffer();
		
		if(img0 == null || img1 == null) throw new IllegalArgumentException("Given image must not be null.");
		assert buff0.getHeight() == buff1.getHeight()
				&& buff0.getWidth() == buff1.getWidth() : "Given image dimensions do not match.";
		
		int ignoredPixels = 0;
		
		for(int y = 0; y < buff0.getHeight(); y++) {
			for(int x = 0; x < buff0.getWidth(); x++) {
				boolean pixelDiffers = false;

				for (int channelIdx = 0; channelIdx < JRGBFrameBuffer.CHANNEL_NUM; channelIdx++) {
					int d0 = buff0.getChannel(x, y, channelIdx);
					int d1 = buff1.getChannel(x, y, channelIdx);
					int dist = Math.abs(d1 - d0);
					pixelDiffers |= dist > threshold;
				}

				if (pixelDiffers) {
					ignoredPixels++;
					if (ignoredPixels > maxPixelIgnore)
						return false;
				}
			}
		}
		
		return true;
	}
	
	public static boolean cmpRGBImg(RGBImage img0,
			RGBImage img1, int threshold, int maxPixelIgnore) {

		return cmpRGBImg(img0, img1, new ArrayList<ImgMask>(), threshold, maxPixelIgnore);
	}

	public static boolean cmpRGBImg(RGBImage img0,
			RGBImage img1, ImgMask mask, int threshold, int maxPixelIgnore) {
		
		List<ImgMask> masks = new ArrayList<ImgMask>();
		if(mask != null) masks.add(mask);

		return cmpRGBImg(img0, img1, masks, threshold, maxPixelIgnore);
	}

	public static boolean cmpRGBImg(RGBImage img0,
			RGBImage img1, List<ImgMask> masks, int threshold, int maxPixelIgnore) {
		
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

	
	private static String runCmd(String command) {
        StringBuilder out = new StringBuilder();
        List<String> commands = new ArrayList<String>();
        commands.add("/bin/sh");
        commands.add("-c");
        commands.add(command);
    
        for (String cmd : commands) {
            out.append(cmd + " ");
        }
    
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(commands);
            pb.redirectErrorStream(true);
    
            process = pb.start();
    
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
    
            String line = null, previous = null;
            while ((line = br.readLine()) != null)
                if (!line.equals(previous)) {
                    previous = line;
                    out.append(line).append('\n');
                }

            process.waitFor();
            br.close();
    
		} catch (IOException e) {
			throw new UncheckedIOException("Error converting ppm with command: " + commands, e);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    
        return out.toString();
	}
	
    public static String convertImg(Path startImgFile) {
    	return runCmd("convert -crop '400x720+440+0' " + startImgFile + " "
				+ startImgFile.toString().replace(".ppm", ".jpg"));
    }

	public static String generateDiffImage(Path outputFolder, ImgMask mask, 
			RGBImage frameImg0, RGBImage frameImg1, int fuzz) throws IOException {
		
		if(fuzz < 0 || fuzz > 100) {
			throw new IllegalArgumentException("Fuzz factor out of valid range: " + fuzz);
		}
		
		if(mask != null) {
			frameImg0.applyMask(mask);
			frameImg1.applyMask(mask);
		}
		frameImg0.writeToFile(outputFolder.resolve("img0.ppm"), false);
		frameImg1.writeToFile(outputFolder.resolve("img1.ppm"), false);
		
		return runCmd("compare -metric AE -fuzz " + fuzz + "% " 
        		+ outputFolder.resolve("img0.ppm") + " "
				+ outputFolder.resolve("img1.ppm") + " "
				+ outputFolder.resolve("cmp.ppm"));
	}   
}

