package mobileworkloads.jlagmarker.lags;

import mobileworkloads.jlagmarker.video.VideoFrame;

public class Lag {

	public enum LagState {
		ENDED, 	// lag ending is set 
		SKIP,  	// lag needs to be skipped
		NA		// lag ending not available
	}
	
	public final int lagId;
	public final VideoFrame startFrame;

	public LagState state;
	private VideoFrame endFrame;
	
	public Lag(int lagId, VideoFrame startFrame) {
		this.lagId = lagId;
		this.startFrame = startFrame;
		state = LagState.NA;
	}
	
	public void setEndFrame(VideoFrame endFrame) {
		this.endFrame = endFrame;
		state = LagState.ENDED;
	}
	
	public String toCSVEntry() {
		StringBuilder bld = new StringBuilder();
		bld.append(lagId).append(";")
		.append(startFrame.videoFrameId).append(";");
		
		switch(state) {
			case ENDED:
				assert endFrame != null : "Lag state is set to ENDED but end frame is null.";
				bld.append(endFrame.videoFrameId);
				break;
			case SKIP:
			case NA:
				bld.append(state.name());
				break;
			default:
				throw new RuntimeException("Unknown lag state: " + state.name());
		}
		
		return bld.toString();
	}
}
