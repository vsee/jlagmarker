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
