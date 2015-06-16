package mobileworkloads.jlagmarker.video;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import mobileworkloads.jlagmarker.masking.ImgMask;
import mobileworkloads.jlagmarker.masking.MaskManager;

public class RGBImage {
	
	public final JRGBFrameBuffer dataBuffer;
	
	protected final List<String> appliedMasks = new ArrayList<String>();
	
	protected Path fileLocation;
	
	public RGBImage(JRGBFrameBuffer data) {
		dataBuffer = data;
		fileLocation = null;
	}
	
	public RGBImage(Path filename) throws IOException {
		dataBuffer = new JRGBFrameBuffer(filename);
		fileLocation = filename;
	}
	
	public Path getFileLocation() {
		return fileLocation;
	}

	public boolean applyMask(ImgMask mask) {
		if(MaskManager.getInstance().maskImage(this, mask)) {
			appliedMasks.add(mask.maskName);
			return true;
		} else {
			System.err.println("ERROR: Failed to apply mask to image: " + mask);
			return false;
		}
	}
	
	public RGBImage clone() {
		return new RGBImage(dataBuffer.clone());
	}
	
	public void writeToFile(Path filename, boolean convert) throws IOException {
		dataBuffer.writeToFile(filename);
		fileLocation = filename;
		
		if(convert) RGBImgUtils.convertImg(filename);
	}
}
