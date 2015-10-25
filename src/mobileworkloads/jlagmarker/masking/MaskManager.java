package mobileworkloads.jlagmarker.masking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Collectors;

import mobileworkloads.jlagmarker.video.JRGBFrameBuffer;
import mobileworkloads.jlagmarker.video.RGBImage;

public class MaskManager {

	protected enum ParsingState { INIT, RECTS, MASKS };
	protected static final String LABEL_RECTANGLES = "#RECTANGLES";
	protected static final String LABEL_MASKS = "#MASKS";
	
	public static final String NO_MASK_MARKER = "NO_MASK";
	
	private static final int DEFAULT_FRAME_WIDTH = 1280;
	private static final int DEFAULT_FRAME_HEIGHT = 720;
	
	
	private static MaskManager instance;
	public static MaskManager getInstance() {
		if(instance == null) instance = new MaskManager();
		return instance;
	}
	
	protected Hashtable<String, ImgMask> availableMasks;
	protected Hashtable<String, Rectangle> availableRects;
	
	protected Path loadedMasksFile;
	
	protected int frameWidth;
	protected int frameHeight;
	
	private MaskManager() {
		availableMasks = new Hashtable<String, ImgMask>();
		availableRects = new Hashtable<String, Rectangle>();
		setFrameDimensions(DEFAULT_FRAME_WIDTH, DEFAULT_FRAME_HEIGHT);
	}
	
	public void setFrameDimensions(int width, int height) {
		frameWidth = width;
		frameHeight = height;
	}
	
	public void clearMasks() {
		availableRects.clear();
		availableMasks.clear();
	}
	
	public boolean reloadMasks() {
		// TODO fix the problem with reloading masks that have been changed
		// it can happen that a reference to the changed mask still exists in a config parameter
		// this one is then not reloaded
		
		if(loadedMasksFile == null) {
			System.out.println("Unable to reload masks since no mask file is currently loaded.");
			return false;
		} else {
			Hashtable<String, ImgMask> masksBackup = (Hashtable<String, ImgMask>) availableMasks.clone();
			Hashtable<String, Rectangle> rectsBackup = (Hashtable<String, Rectangle>) availableRects.clone();
			
			availableMasks.clear();
			availableRects.clear();
			try {
				parseMasks(loadedMasksFile);
			} catch (Exception e) {
				System.out.println("Error reloading mask specification file [" + loadedMasksFile + "]: " + e.getMessage());
				availableMasks = masksBackup;
				availableRects = rectsBackup;
				return false;
			}
			
			return true;
		}
	}
	
	public void parseMasks(Path maskSpecFile) throws IOException {
		if(maskSpecFile == null || !Files.isRegularFile(maskSpecFile))
			throw new IOException("Given mask specification file invalid: " + maskSpecFile);

		System.out.println("Parsing mask configuration: " + maskSpecFile + " ...");
		
		loadedMasksFile = maskSpecFile;
		
		List<String> lines;
        try (BufferedReader reader = new BufferedReader(Files.newBufferedReader(maskSpecFile, Charset.forName("UTF-8")))) {      	
        	lines = 
    			reader.lines()
    			.filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        
        ParsingState pstate = ParsingState.INIT;
        for(String line : lines) {
        	switch (pstate) {
				case INIT:
					if(line.equals(LABEL_RECTANGLES)) pstate = ParsingState.RECTS;
					break;
				case RECTS:
					if(line.equals(LABEL_MASKS)) pstate = ParsingState.MASKS;
					else {
						// REC_PORTRAIT_LEFT,0,0,438,720
						String[] rectChunks = line.split(",");
						if(rectChunks.length < 5) {
							System.out.println("WARNING: Unexpected rectangle format: " + line);
							continue;
						}
						if(availableRects.containsKey(rectChunks[0])) {
							System.out.println("WARNING: Rectangle specified more than once: " + line);
							continue;
						}
						
						Rectangle rect = new Rectangle(Integer.parseInt(rectChunks[1]), 
								Integer.parseInt(rectChunks[2]), Integer.parseInt(rectChunks[3]), Integer.parseInt(rectChunks[4]));
						
						if(rect.x0+rect.width > frameWidth || rect.y0+rect.height > frameHeight)
							System.out.println("WARNING: Rectangle out of frame bounds: " + rect);
						else 
							availableRects.put(rectChunks[0], rect);
					}
					break;
				case MASKS:
					// STATUS_BAR_MASK_PORTRAIT,REC_PORTRAIT_LEFT,REC_PORTRAIT_RIGHT,REC_PORTRAIT_STATUS_BAR
					String[] mask = line.split(",");
					if(availableMasks.containsKey(mask[0])) {
						System.out.println("WARNING: Mask specified more than once: " + line);
						continue;
					}
					
					List<Rectangle> rects = Arrays.stream(mask, 1, mask.length)
							.filter(rectName -> availableRects.containsKey(rectName))
							.map(rectName -> availableRects.get(rectName))
							.collect(Collectors.toList());
					if(rects.size() < mask.length - 1) {
						System.out.println("WARNING: Not all rectangles found for given mask: " + line);
						continue;
					}
					availableMasks.put(mask[0], new ImgMask(mask[0], rects));
					break;
				default:
					throw new RuntimeException("Unknown parsing state: " + pstate);
			}
        }
        
        System.out.println(availableRects.size() + " rectangles and " + availableMasks.size() + " masks parsed successfully.");
	}
	
	
	public boolean maskImage(RGBImage img, ImgMask mask) {
		if(mask == null) return false;
		
		for(Rectangle rect : mask.sections) {
			if(img.getDataReleased()) 
				throw new RuntimeException("Data buffer released unexpectedly when trying to apply mask: " + mask);
			JRGBFrameBuffer dataBuffer = img.getDataBuffer();
			
			colourRectRGBBuff(dataBuffer, rect, 0xFF);
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

	public ImgMask getMask(String maskName) {
		if(!availableMasks.containsKey(maskName)) {
			System.err.println("ERROR: Requested mask not found: " + maskName);
			return null;
		} else {
			return availableMasks.get(maskName);
		}
	}
}
