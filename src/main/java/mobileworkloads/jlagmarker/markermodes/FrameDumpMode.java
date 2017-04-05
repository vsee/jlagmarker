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
package mobileworkloads.jlagmarker.markermodes;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.List;

import mobileworkloads.jlagmarker.masking.ImgMask;
import mobileworkloads.jlagmarker.video.VideoFrame;

public class FrameDumpMode extends LagmarkerMode {

	public static class FrameInterval implements Comparable<FrameInterval> {
		
		public final int leftBorder;
		public final int rightBorder;
		
		public FrameInterval(int l, int r) {
			if(l < 0 || r < 0 || l >= r) throw new RuntimeException("Invalid frame interval: " + l + " " + r +
					". Frames must be larger than 0 and right border larger than left border.");

			leftBorder = l;
			rightBorder = r;
		}

		@Override
		public String toString() {
			return "[" + leftBorder + " -- " + rightBorder + "]";
		}

		public boolean intersects(FrameInterval other) {
			// this  ------------#########---
			// other --#######---------------
			if(other.rightBorder < leftBorder) {
				return false;
			}
			// this  --#########---------------
			// other -------------########-----
			else if(other.leftBorder > rightBorder) {
				return false;
			} else {
				return true;
			}
		}

		@Override
		public int compareTo(FrameInterval o) {
			if(o == null) return 1;
			else if(o.leftBorder == leftBorder) return 0;
			else if(o.leftBorder < leftBorder) return 1;
			else return -1;
		}
	}
	
	
	protected final List<FrameInterval> frameIntervals;
	protected final List<ImgMask> dumpMasks;
	
	public FrameDumpMode(String videoName, String outputPrefix, Path outputFolder, 
			List<FrameInterval> frameI, List<ImgMask> dumpMasks) {
		super(videoName, outputPrefix, outputFolder);
		
		frameIntervals = frameI;
		this.dumpMasks = dumpMasks;
	}

	@Override
	public void run() {
		System.out.println("##### Running " + getModeType().name() + " Mode #####");
		System.out.println("Run Start at: " + Calendar.getInstance().getTime());
		long startTimeMS = System.currentTimeMillis();
		
		vstate.dumpVideoFormat();
		System.out.println("\n\n");
		
		dumpFrames();
		
		System.out.println("\nSaving Results ...");

		long runtimeMS = (System.currentTimeMillis() - startTimeMS);
		System.out.println("Run terminated successfully at: " + Calendar.getInstance().getTime() + " after " + (runtimeMS / 1000f) + " seconds.");
	}

	protected void dumpFrames() {
		int nextInterval = 0;
		FrameInterval currInterval = frameIntervals.get(nextInterval++);
		System.out.println("Handling interval: " + currInterval);
		
		VideoFrame currFrame = null;
		
		while(!vstate.isEndOfStream()) {
			currFrame = vstate.decodeNextVideoFrame();
			
			if(currFrame.videoFrameId % 500 == 0)
				System.out.println(currFrame.videoFrameId);

			if(currFrame.videoFrameId >= currInterval.leftBorder) {
				if(currFrame.videoFrameId <= currInterval.rightBorder) {

					if(currFrame.videoFrameId % 500 != 0 && currFrame.videoFrameId % 100 == 0)
						System.out.println("Dumping: " + currFrame.videoFrameId);
					
					dumpFrame(currFrame, "NO_MASK");
					for(ImgMask mask : dumpMasks) {
						VideoFrame maskedFrame = currFrame.clone();
						maskedFrame.frameImg.applyMask(mask);
						dumpFrame(maskedFrame, mask.maskName);
					}
					
				} else {
					
					if(nextInterval >= frameIntervals.size()) break;
					currInterval = frameIntervals.get(nextInterval++);
					System.out.println("Handling interval: " + currInterval);
				}
			}
			
		}
	}

	protected void dumpFrame(VideoFrame currFrame, String maskName) {
		Path filename = outputFolder.resolve(String.format("frame_%06d_%s.ppm", currFrame.videoFrameId, maskName));
		try {
			currFrame.frameImg.writeToFile(filename, false);
			//System.out.println("Frame " + currFrame.videoFrameId + " dumped with mask " + maskName);
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to dump video frame: " + currFrame, e);
		}
	}

	@Override
	public LagmarkerModeType getModeType() {
		return LagmarkerModeType.FRAMEDUMP;
	}

}
