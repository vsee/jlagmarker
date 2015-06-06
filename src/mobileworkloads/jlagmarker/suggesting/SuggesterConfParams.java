package mobileworkloads.jlagmarker.suggesting;

public class SuggesterConfParams {

	public int maxDiffThreshold;
	public int stillFrames = 5;
	public int pixIgnore;
	public String mask = "CLOCK_MASK";
	public int occurrence;	 // specify how often the suggested image is found before it is accepted as valid
	public int runInterval; // specify how many new beginnings can be found before this worker is removed

}
