package mobileworkloads.jlagmarker.video;


public class VideoFrame implements Cloneable {

	public final long startTimeUS;
	public final long endTimeUS;
	public final long durationUS;
	
	public final int videoFrameId;
	public final RGBImage frameImg;
	
	public VideoFrame(long startTimeUS, long durationUS, int frameId, RGBImage img) {
		this.startTimeUS = startTimeUS;
		endTimeUS = startTimeUS + durationUS;
		this.durationUS = durationUS;
		
		videoFrameId = frameId;
		frameImg = img;
	}

	public VideoFrame clone() {
		return new VideoFrame(startTimeUS, durationUS, videoFrameId, frameImg.clone());
	}
	
	@Override
	public String toString() {
		return new StringBuilder()
		.append("Frame: ").append(videoFrameId)
		.append(" - Start: ").append(startTimeUS)
		.append(" - End: ").append(endTimeUS).toString();
	}
}
