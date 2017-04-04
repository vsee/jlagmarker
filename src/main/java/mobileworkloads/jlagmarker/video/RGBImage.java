package mobileworkloads.jlagmarker.video;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import mobileworkloads.jlagmarker.masking.ImgMask;
import mobileworkloads.jlagmarker.masking.MaskManager;

public class RGBImage {

	private JRGBFrameBuffer dataBuffer;

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

	public JRGBFrameBuffer getDataBuffer() {
		return dataBuffer;
	}

	public boolean getDataReleased() {
		return dataBuffer == null;
	}

	public Path getFileLocation() {
		return fileLocation;
	}

	public boolean applyMask(ImgMask mask) {
		if (MaskManager.getInstance().maskImage(this, mask)) {
			appliedMasks.add(mask.maskName);
			return true;
		} else {
			System.err.println("ERROR: Failed to apply mask to image: " + mask);
			return false;
		}
	}

	public RGBImage clone() {
		if(getDataReleased()) 
			throw new RuntimeException("Trying to clone without loaded data buffer!");
		
		return new RGBImage(dataBuffer.clone());
	}

	public void writeToFile(Path filename, boolean convert) throws IOException {
		if(getDataReleased()) 
			throw new RuntimeException("Trying to write to file without loaded data buffer: " + filename);
		
		dataBuffer.writeToFile(filename);
		fileLocation = filename;

		if (convert)
			RGBImgUtils.convertImg(filename);
	}

	public void releaseDataResources() {
		dataBuffer = null;
	}
}
