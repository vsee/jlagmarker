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
}
