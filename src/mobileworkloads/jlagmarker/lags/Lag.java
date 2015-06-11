package mobileworkloads.jlagmarker.lags;

import java.util.ArrayList;
import java.util.List;

import mobileworkloads.jlagmarker.video.VideoFrame;
import mobileworkloads.mlgovernor.res.CSVResourceTools;

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
	
	protected final List<Integer> suggIds;
	
	public Lag(int lagId, VideoFrame startFrame) {
		this.lagId = lagId;
		this.startFrame = startFrame;
		state = LagState.NA;
		suggIds = new ArrayList<Integer>();
	}
	
	public void setEndFrame(VideoFrame endFrame) {
		this.endFrame = endFrame;
		state = LagState.ENDED;
	}
	
	public void addSuggestion(VideoFrame sugg) {
		System.out.println(String.format("LAG %d: New suggestion found at frame %s!", lagId, sugg.toString()));
		
		suggIds.add(sugg.videoFrameId);
	}
	
	public List<Integer> getSuggestions() {
		return suggIds;
	}
	
	public String toCSVEntry() {
		StringBuilder bld = new StringBuilder();
		bld.append(lagId).append(CSVResourceTools.SEPARATOR)
		.append(startFrame.videoFrameId).append(CSVResourceTools.SEPARATOR);
		
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
		
		bld.append(CSVResourceTools.SEPARATOR);
		
		for(Integer suggId : suggIds) {
			bld.append(suggId).append(" ");
		}
		
		return bld.toString();
	}
}
