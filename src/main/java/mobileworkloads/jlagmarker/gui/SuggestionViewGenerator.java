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
package mobileworkloads.jlagmarker.gui;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import mobileworkloads.jlagmarker.lags.Lag;
import mobileworkloads.jlagmarker.lags.LagProfile;
import mobileworkloads.jlagmarker.lags.Lag.LagState;

public class SuggestionViewGenerator {

	private static final String HIGHLIGHT_STYLE = 
			"<style>\n" +
			"img.hl {\n" +
			"padding: 3px;\n" +
			"border: 2px dashed #0001fe;\n" +
            "background-color: #eded01;\n" +
			"}\n" +
			"</style>";

	public static void generateSuggestionView(Path outputFileName, LagProfile lprofile) {
		
		try {
			if (Files.exists(outputFileName)) {
				Files.delete(outputFileName);
			}
		} catch (IOException e) {
			System.out.println("Removing old html suggester view failed!");
			throw new UncheckedIOException(e);
		}
		
		try(BufferedWriter statWriter = Files.newBufferedWriter(outputFileName, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
		
			statWriter.write("<html>");
			statWriter.newLine();
			statWriter.write("<head>");
			statWriter.newLine();			
			statWriter.write(HIGHLIGHT_STYLE);
			statWriter.newLine();
			statWriter.write("</head>");
			statWriter.newLine();
			statWriter.write("<body>");
			statWriter.newLine();
			
			for (int lagIdx = lprofile.lags.size() - 1; lagIdx >= 0; lagIdx--) {
				Lag l = lprofile.lags.get(lagIdx);
				
				List<Integer> suggIds = l.getSuggestionIds();
				List<Path> suggFiles = l.getSuggestionFiles();
				
				statWriter.write("<div style=\"white-space:nowrap\">");
				statWriter.newLine();
				statWriter.write("<table class=\"image\">");
				statWriter.newLine();
				
				//  HEADER
				statWriter.write("<tr>");
				statWriter.newLine();
				statWriter.write("<td>Lag " + l.lagId + " Frame " + l.startFrame.videoFrameId + "</td>");
				statWriter.newLine();
				
				for(Integer suggId : suggIds) {
					statWriter.write("<td class=\"caption\">" + suggId + "</td>");
					statWriter.newLine();
				}
				
				statWriter.write("</tr>");
				statWriter.newLine();
				
				
				// IMAGES
				statWriter.write("<tr>");
				statWriter.newLine();
				
				String beginFileLocation = l.startFrame.frameImg.getFileLocation().toString().replace(".ppm", ".jpg");
				beginFileLocation = beginFileLocation.substring(beginFileLocation.indexOf("beginFrames"));
				
				statWriter.write("<td><img id=\"beg-"+ l.lagId +"\" src=\"" + 
						beginFileLocation + "\" width=\"200\"/></td>");
				statWriter.newLine();
				
				for (int i = 0; i < suggIds.size(); i++) {
					String suggFileLocation = suggFiles.get(i).toString().replace(".ppm", ".jpg");
					suggFileLocation = suggFileLocation.substring(suggFileLocation.indexOf("suggestions"));
					
					statWriter.write("<td><img "
							+ (l.getEndFrame() != null && l.getEndFrame().videoFrameId == suggIds.get(i) ? "class=\"hl\"" : "") + 
							"  id=\"" + l.lagId + "F" + suggIds.get(i) + "\" src=\"" + 
							suggFileLocation + "\" width=\"200\"/></td>");
					statWriter.newLine();
				}

				if(l.getState() == LagState.SKIP) {
					statWriter.write("<td style=\"font-size: 250%\">" + l.getState().name() + "</td>");
					statWriter.newLine();
				}
				
				statWriter.write("</tr>");
				statWriter.newLine();

				
				statWriter.write("</table>");
				statWriter.newLine();
			}
			
			statWriter.write("</body>");
			statWriter.newLine();
			statWriter.write("</html>");
			statWriter.newLine();
			
		} catch (IOException e) {
			System.out.println("Writing html suggester view failed!");
			throw new UncheckedIOException(e);
		}
	}
	
}
