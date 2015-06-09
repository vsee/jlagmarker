package mobileworkloads.jlagmarker.video;

import java.util.ArrayList;
import java.util.List;

import mobileworkloads.jlagmarker.masking.MaskManager;

public class VideoFrame implements Cloneable {

	public final long startTimeUS;
	public final long endTimeUS;
	public final long durationUS;
	
	public final int videoFrameId;
	public final JRGBFrameBuffer dataBuffer;
	
	protected final List<String> appliedMasks;
	
	public VideoFrame(long currentTimeUS, long timePerFrameUS, int frameId, JRGBFrameBuffer data) {
		startTimeUS = currentTimeUS;
		endTimeUS = currentTimeUS + timePerFrameUS;
		durationUS = timePerFrameUS;
		
		videoFrameId = frameId;
		dataBuffer = data;
		
		appliedMasks = new ArrayList<String>();
	}

	public void applyMask(String maskName) {
		if(MaskManager.getInstance().maskFrame(this, maskName)) {
			appliedMasks.add(maskName);
		} else {
			throw new RuntimeException("Failed to apply mask to frame: " + maskName);
		}
	}
	
	public VideoFrame clone() {
		return new VideoFrame(startTimeUS, durationUS, videoFrameId, dataBuffer.clone());
	}
	
	@Override
	public String toString() {
		return new StringBuilder()
		.append("Frame: ").append(videoFrameId)
		.append(" - Start: ").append(startTimeUS)
		.append(" - End: ").append(endTimeUS).toString();
	}
}
