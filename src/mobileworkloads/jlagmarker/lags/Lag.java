package mobileworkloads.jlagmarker.lags;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import mobileworkloads.jlagmarker.video.VideoFrame;
import mobileworkloads.mlgovernor.res.CSVResourceTools;

public class Lag {

	private class SuggestionEntry {
		VideoFrame frame;
		Path imgFile;
		
		@Override
		public String toString() {
			return frame.toString();
		}
	}
	
	public enum LagState {
		ENDED, 	// lag ending is set 
		SKIP,  	// lag needs to be skipped
		NA		// lag ending not available
	}
	
	public final int lagId;
	public final VideoFrame startFrame;
	public Path startImgFile;

	private LagState state;
	private VideoFrame endFrame;
	
	protected final List<SuggestionEntry> suggestions;
	
	public Lag(int lagId, VideoFrame startFrame) {
		this.lagId = lagId;
		this.startFrame = startFrame;
		state = LagState.NA;
		suggestions = new ArrayList<SuggestionEntry>();
	}
	
	public LagState getState() {
		return state;
	}
	
	public void setSkip() {
		state = LagState.SKIP;
	}

	public VideoFrame getEndFrame() {
		return endFrame;
	}
	
	public void setEndFrame(VideoFrame endFrame) {
		this.endFrame = endFrame;
		state = LagState.ENDED;
	}
	
	public void acceptSuggestion(int selectedId) {
		Optional<VideoFrame> selectedSugg = suggestions.stream().map(sugEntry -> sugEntry.frame)
				.filter(sug -> sug.videoFrameId == selectedId).findAny();
		
		if(selectedSugg.isPresent()) {
			setEndFrame(selectedSugg.get());
		} else {
			throw new IllegalArgumentException("Given id is not among suggested ids: " + selectedId);
		}
	}
	
	public void addSuggestion(VideoFrame sugg, Path suggImgFile) {
		System.out.println(String.format("LAG %d: New suggestion found at frame %s!", lagId, sugg.toString()));
		
		SuggestionEntry se = new SuggestionEntry();
		se.frame = sugg;
		se.imgFile = suggImgFile;
		
		suggestions.add(se);
	}
	
	public List<Integer> getSuggestionIds() {
		return suggestions.stream().map(sugg -> sugg.frame.videoFrameId).collect(Collectors.toList());
	}
	
	public List<Path> getSuggestionFiles() {
		return suggestions.stream().map(sugg -> sugg.imgFile).collect(Collectors.toList());
	}
	
	public void clearSuggestion() {
		suggestions.clear();
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
		
//		bld.append(CSVResourceTools.SEPARATOR);
//		
//		for(Integer suggId : getSuggestionIds()) {
//			bld.append(suggId).append(" ");
//		}
		
		return bld.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder bld = new StringBuilder();
		
		bld.append("Id: ").append(lagId).append("\n")
		.append("Start: ").append(startFrame).append("\n")
		.append("End: ").append(endFrame).append("\n")
		.append("End State: ").append(state.name()).append("\n")
		.append("Suggestions: ").append(suggestions);
		
		return bld.toString();
	}

}
