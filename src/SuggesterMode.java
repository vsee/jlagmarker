
public class SuggesterMode implements LagmarkerMode {

	protected VideoState vstate;
	
	public SuggesterMode(String videoName) {
		vstate = new VideoState(videoName);
	}

	@Override
	public void run() {
		System.out.println("##### Running Suggester Mode #####");
		vstate.dumpVideoFormat();
		System.out.println("\n\n");
		
		// TODO
		// parse input events
		// parse suggester configuration
		// read white flash offset from args
	}

}
