package mobileworkloads.jlagmarker;

import java.io.IOException;
import java.nio.file.Paths;

import mobileworkloads.jlagmarker.markermodes.LagmarkerMode;
import mobileworkloads.jlagmarker.markermodes.LagmarkerModeType;
import mobileworkloads.jlagmarker.markermodes.SuggesterMode;
import mobileworkloads.jlagmarker.masking.MaskManager;

public class JLagmarkerMain {
		
	private LagmarkerMode mode; 
	
	public JLagmarkerMain(String[] args) {
	
		// TODO implement mask parsing

		// args: jlagmarker.jar <mode> -modearg1 blabla -modearg2 blabla
		if(args.length < 3) // expected <mode> <videoname> <maskspec>
			// TODO implement some usage messages
			throw new IllegalArgumentException("Arguments not specified as needed.");
		
		String modeType = args[0];
		String videoName = args[1];
		String maskSpec = args[2];

		try {
			MaskManager.getInstance().parseMasks(Paths.get(maskSpec));
		} catch (IOException e) {
			System.out.println("Error parsing mask specification file [" + maskSpec + "]: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("Error initialising lagmarker main.");
		}
		
		if(modeType.equalsIgnoreCase(LagmarkerModeType.SUGGESTER.name())) {
			mode = new SuggesterMode(videoName);
		} else if (modeType.equalsIgnoreCase(LagmarkerModeType.DETECTOR.name())) {
			// TODO implement DetectorMode
			// mode = new DetectorMode(videoName);
		} else {
			throw new IllegalArgumentException("Given lagmarker mode unknown: " + modeType);
		}
	}
	
	public void run() {
		mode.run();
	}
	
	public static void main(String[] args) {
		new JLagmarkerMain(args).run();
	}
}
