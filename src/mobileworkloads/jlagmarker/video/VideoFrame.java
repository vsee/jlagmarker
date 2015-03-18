package mobileworkloads.jlagmarker.video;

import java.util.ArrayList;
import java.util.List;

import mobileworkloads.jlagmarker.masking.MaskManager;

public class VideoFrame implements Cloneable {

	public final String srcVideoName;
	public final int videoFrameId;
	public final JRGBFrameBuffer dataBuffer;
	
	protected final List<String> appliedMasks;
	
	public VideoFrame(String videoName, int frameId, JRGBFrameBuffer data) {
		srcVideoName = videoName;
		videoFrameId = frameId;
		dataBuffer = data;
		
		appliedMasks = new ArrayList<String>();
	}
	
	public boolean applyMask(String maskName) {
		if(MaskManager.getInstance().maskFrame(this, maskName)) {
			appliedMasks.add(maskName);
			return true;
		}
		
		return false;
	}
	
	public VideoFrame clone() {
		return new VideoFrame(srcVideoName, videoFrameId, dataBuffer.clone());
	}
}
