package mobileworkloads.jlagmarker.masking;

import java.util.List;

public class ImgMask {

	public final String maskName;
	
	protected final List<Rectangle> sections;
	
	protected ImgMask(String name, List<Rectangle> sections) {
		this.maskName = name;
		this.sections = sections;
	}
	
	@Override
	public String toString() {
		return maskName;
	}
}
