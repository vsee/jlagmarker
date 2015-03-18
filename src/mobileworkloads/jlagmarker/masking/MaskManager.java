package mobileworkloads.jlagmarker.masking;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import mobileworkloads.jlagmarker.video.JRGBFrameBuffer;
import mobileworkloads.jlagmarker.video.VideoFrame;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class MaskManager {

	private static MaskManager instance;
	public static MaskManager getInstance() {
		if(instance == null) instance = new MaskManager();
		return instance;
	}
	
	protected final Hashtable<String, ImgMask> availableMasks;
	
	private MaskManager() {
		availableMasks = new Hashtable<String, ImgMask>();
		
		// TODO parse masks
		List<Rectangle> rects = new ArrayList<Rectangle>();
		rects.add(new Rectangle(0, 0, 438, 720));
		rects.add(new Rectangle(842, 0, 438, 720));
		rects.add(new Rectangle(437, 0, 405, 29));
		availableMasks.put("STATUS_BAR_MASK_PORTRAIT", new ImgMask("STATUS_BAR_MASK_PORTRAIT", rects));
	}
	
	public void parseMasks(Path maskSpecFile) {
		throw new NotImplementedException();
	}
	
	
	public boolean maskFrame(VideoFrame frame, String maskName) {
		ImgMask mask = availableMasks.get(maskName);
		if(mask == null) return false;
		
		for(Rectangle rect : mask.sections) {
			colourRectRGBBuff(frame.dataBuffer, rect, 0xFF);
		}
		
		return true;
	}

	public void colourRectRGBBuff(JRGBFrameBuffer fbuffer, Rectangle rect, int value) {
		if((rect.x0 + rect.width) > fbuffer.getWidth() || (rect.y0 + rect.height) > fbuffer.getHeight())
			throw new IllegalArgumentException("Invalid rectangle coordinates. Exceeding frame boundaries!");

		for(int y = rect.y0; y < rect.y0 + rect.height; y++) {
			for(int x = rect.x0; x < rect.x0 + rect.width; x++) {
				for(int channel = 0; channel < JRGBFrameBuffer.CHANNEL_NUM; channel++) {
					fbuffer.setChannel(x, y, channel, value);
				}
			}
		}
	}
}
