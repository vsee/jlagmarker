package mobileworkloads.jlagmarker.video;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import mobileworkloads.jlagmarker.masking.MaskManager;

public class RGBImage {
	
	public final JRGBFrameBuffer dataBuffer;
	
	protected final List<String> appliedMasks = new ArrayList<String>();
	
	public RGBImage(JRGBFrameBuffer data) {
		dataBuffer = data;
	}
	
	public RGBImage(Path filename) throws IOException {
		dataBuffer = new JRGBFrameBuffer(filename);
	}

	public void applyMask(String maskName) {
		if(MaskManager.getInstance().maskImage(this, maskName)) {
			appliedMasks.add(maskName);
		} else {
			throw new RuntimeException("Failed to apply mask to image: " + maskName);
		}
	}
	
	public RGBImage clone() {
		return new RGBImage(dataBuffer.clone());
	}
}
