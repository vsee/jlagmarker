public class JLagmarkerMain {
	
	private LagmarkerMode mode; 
	
	public JLagmarkerMain(String modearg) {
		
		if(modearg.equals(LagmarkerModeType.SUGGESTER.name())) {
			mode = new SuggesterMode();
		} else if (modearg.equals(LagmarkerModeType.DETECTOR.name())) {
			mode = new DetectorMode();
		} else {
			throw new IllegalArgumentException("Given lagmarker mode unknown: " + modearg);
		}
	}
	
	public void run() {
		mode.run();
	}

	
	
	public static void main(String[] args) {
		
		String modearg = "SUGGESTER";
		
		JLagmarkerMain lagmarker = new JLagmarkerMain(modearg);
		lagmarker.run();
	}
}
