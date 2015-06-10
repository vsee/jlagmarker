package mobileworkloads.jlagmarker.worker;

import java.nio.file.Path;

import mobileworkloads.jlagmarker.video.VideoFrame;

public class ImgFinder extends VStreamWorker {

	public ImgFinder(Path outputFolder) {
		super(outputFolder);
	}

	@Override
	public void update(VideoFrame currentFrame) { }

}
