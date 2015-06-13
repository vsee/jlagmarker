package mobileworkloads.jlagmarker;

import java.io.IOException;
import java.nio.file.Path;

import mobileworkloads.mlgovernor.res.ResCommon;
import mobileworkloads.mlgovernor.res.ResourceManager;
import mobileworkloads.mlgovernor.res.ResCommon.FingerPositionType;
import mobileworkloads.mlgovernor.res.dp.DPResourceTrace;
import mobileworkloads.mlgovernor.res.dp.IDPResourceTrace;
import mobileworkloads.mlgovernor.res.dp.IntervalDataPoint;

public class InputEventStream {

	protected final Path inputFileName;
	protected final IDPResourceTrace inputStream;
	protected int latestMovementDPIdxCache = 0;
	
	public InputEventStream(Path resFile) throws IOException {
		inputFileName = resFile;
		inputStream = new IDPResourceTrace(ResourceManager.parseIntervalDataTrace(inputFileName));
	}
	
	public Path getInputFileName() {
		return inputFileName;
	}
	
	public void resetCache() {
		latestMovementDPIdxCache = 0;
	}
	
	public boolean didFingerGoDown(long startTimeUS, long endTimeUS) {
		DPResourceTrace<IntervalDataPoint>.DPsInIntervalRes idpRes = 
				inputStream.getDPsInInterval(startTimeUS, endTimeUS, latestMovementDPIdxCache);
		latestMovementDPIdxCache = idpRes.latestDPIdx;
		
		// special case for the first entry
		// is it the first value we are looking at during the first frame of the dataset
		// and is its value "DOWN"
		if (idpRes.DPs.get(0).startTimeUS == 0
				&& idpRes.DPs.get(0).dataValue.equals(ResCommon.FingerPositionType.DOWN.name())
				&& startTimeUS == 0)
			return true;
		
		// look if a change happened from "UP" to "DOWN"
		FingerPositionType lastPos = null;
		if(idpRes.DPs.size() > 1) {
			for(IntervalDataPoint idp : idpRes.DPs) {
				if(lastPos != null) {
					if(lastPos.equals(FingerPositionType.UP) &&
					   idp.dataValue.equals(FingerPositionType.DOWN.name())) return true;
				}

				lastPos = FingerPositionType.valueOf(idp.dataValue);
			}
		}
		
		return false;
	}
	
}
