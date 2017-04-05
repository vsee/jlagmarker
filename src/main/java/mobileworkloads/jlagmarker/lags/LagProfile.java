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
package mobileworkloads.jlagmarker.lags;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import mobileworkloads.jlagmarker.video.VideoFrame;

public class LagProfile {

//	private static final String LAGPROFILE_HEADER = "lagId,startFrame,endFrame,suggestionids";
	private static final String LAGPROFILE_HEADER = "lagId,startFrame,endFrame";
	private static final String FILE_NAME_BEGIN_FORMAT = "lag_%03d_beg_%d.ppm";

	private int nextLagId = 0;
	
	public final List<Lag> lags;
	
	protected Path beginImgOutputFolder;
	
	public LagProfile(Path beginImgOutputFolder) {
		lags = new ArrayList<Lag>();
		
		this.beginImgOutputFolder = beginImgOutputFolder; 
		if(!(Files.exists(beginImgOutputFolder) && Files.isDirectory(beginImgOutputFolder))) {
			try {
				Files.createDirectory(beginImgOutputFolder);
				System.out.println("Frame beginning output directory created: " + beginImgOutputFolder);
			} catch (IOException e) {
				throw new UncheckedIOException("Error creating begin image output directory: " + beginImgOutputFolder, e);
			}
		}
	}

	public Lag addNewLag(VideoFrame startFrame) {
		Lag l = new Lag(nextLagId++, startFrame);
		lags.add(l);
		
		try {
			l.startFrame.frameImg.writeToFile(beginImgOutputFolder.resolve(String
					.format(FILE_NAME_BEGIN_FORMAT, l.lagId,l.startFrame.videoFrameId)), true);
		} catch (IOException e) {
			throw new UncheckedIOException("Error creating begin image for lag: " + l.lagId, e);
		}
		
		return l;
	}
	
	public void dumpLagProfile(Path outputFileName) {
		try(BufferedWriter statWriter = Files.newBufferedWriter(outputFileName, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
			
			statWriter.write(LAGPROFILE_HEADER);
			statWriter.newLine();
			
			for(Lag l : lags) {
				statWriter.write(l.toCSVEntry());
				statWriter.newLine();
			}

			System.out.println("Lag profile written to " + outputFileName);
		} catch (IOException e) {
			System.out.println("Writing lag profile to file failed!");
			throw new UncheckedIOException(e);
		}
	}
}
