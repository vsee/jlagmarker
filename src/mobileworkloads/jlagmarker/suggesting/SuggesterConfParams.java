package mobileworkloads.jlagmarker.suggesting;

public class SuggesterConfParams {

	public int maxDiffThreshold = 30;
	public int stillFrames = 15;
	public int pixIgnore = 0;
	public String mask = "CLOCK_MASK";
	public int occurrence = 1;	 // specify how often the suggested image is found before it is accepted as valid
	public int runInterval = 1; // specify how many new beginnings can be found before this worker is removed

}
