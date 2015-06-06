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

	private static final String LAGPROFILE_HEADER = "lagId;startFrame;endFrame";
	private static final String FILE_NAME_BEGIN_FORMAT = "lag_%03d_beg_%d.ppm";

	private int nextLagId = 0;
	
	public final List<Lag> lags;
	
	// TODO add suggester configuration
	
	public LagProfile() {
		lags = new ArrayList<Lag>();
	}

	public Lag addNewLag(VideoFrame startFrame) {
		Lag l = new Lag(nextLagId++, startFrame);
		lags.add(l);
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
	
	public void dumpFrameBeginnings(Path outputFolder) {
		System.out.println("Dumping frame begin images to: " + outputFolder + " ...");
		
		if(!(Files.exists(outputFolder) && Files.isDirectory(outputFolder))) {
			try {
				Files.createDirectory(outputFolder);
				System.out.println("Frame beginning output directory created: " + outputFolder);
			} catch (IOException e) {
				throw new UncheckedIOException("Error creating begin image output directory: " + outputFolder, e);
			}
		}
		
		for(Lag l : lags) {
			try {
				l.startFrame.dataBuffer.writeToFile(outputFolder.resolve(String
						.format(FILE_NAME_BEGIN_FORMAT, l.lagId,l.startFrame.videoFrameId)));
			} catch (IOException e) {
				throw new UncheckedIOException("Error creating begin image for lag: " + l.lagId, e);
			}
		}
		
		System.out.println(lags.size() + " begin images created successfully.");
	}
}
