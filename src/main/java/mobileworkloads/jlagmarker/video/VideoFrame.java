/**
*
* Copyright 2017 Volker Seeker <volker@seekerscience.co.uk>.
*
* This file is part of JLagmarker.
*
* JLagmarker is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* JLagmarker is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with JLagmarker. If not, see <http://www.gnu.org/licenses/>.
*
*/
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
