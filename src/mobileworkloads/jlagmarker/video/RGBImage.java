package mobileworkloads.jlagmarker.video;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

	public void applyMask(String maskName) {
		if(MaskManager.getInstance().maskImage(this, maskName)) {
			appliedMasks.add(maskName);
		} else {
			throw new IllegalArgumentException("Failed to apply mask to image: " + maskName);
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
