package mobileworkloads.jlagmarker.masking;

import java.util.List;

public class ImgMask {

	public final String maskName;
	public final List<Rectangle> sections;
	
	public ImgMask(String name, List<Rectangle> sections) {
		this.maskName = name;
		this.sections = sections;
	}
}
