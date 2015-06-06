package mobileworkloads.jlagmarker.video;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

public class JRGBFrameBuffer implements Cloneable {
	public static final int CHANNEL_NUM = 3;
	
	private int width;
	private int height;
	private int buffSize;
	
	/** Treat this as unsigned byte by interpreting with: b & 0xFF */
	private byte[] buffer;
	
	public int getWidth() { return width; }
	public int getHeight() { return height; }
	public int getSize() { return buffSize; }
	
	public int getRawChannel(int idx) {
		return buffer[idx] & 0xFF;
	}
	
	public int getChannel(int x, int y, int channel) {
		if (channel > CHANNEL_NUM - 1 || channel < 0)
			throw new IllegalArgumentException("Given channel must be between 0 and " + (CHANNEL_NUM - 1) + ": " + channel);
		if (x < 0 || x >= width)
			throw new IllegalArgumentException("Given x coordinate out of bounds: " + x);
		if (y < 0 || y >= height)
			throw new IllegalArgumentException("Given y coordinate out of bounds: " + y);
		
		int idx = (y * width + x) * CHANNEL_NUM + channel;
		
		return buffer[idx] & 0xFF;
	}
	
	public void setChannel(int x, int y, int channel, int value) {
		if (channel > CHANNEL_NUM - 1 || channel < 0)
			throw new IllegalArgumentException("Given channel must be between 0 and " + (CHANNEL_NUM - 1) + ": " + channel);
		if (x < 0 || x >= width)
			throw new IllegalArgumentException("Given x coordinate out of bounds: " + x);
		if (y < 0 || y >= height)
			throw new IllegalArgumentException("Given y coordinate out of bounds: " + y);
		
		int idx = (y * width + x) * CHANNEL_NUM + channel;
		
		buffer[idx] = (byte) value;
	}
	
	public void writeToFile(Path filename) throws IOException {
		FileOutputStream out = new FileOutputStream(filename.toString());
		out.write(("P6\n" + getWidth() + " " + getHeight() + "\n255\n").getBytes()); // write header
		out.write(buffer);
		out.close();
	}
	
	public JRGBFrameBuffer clone() {
		JRGBFrameBuffer dest = new JRGBFrameBuffer();
		dest.width = width;
		dest.height = height;
		dest.buffSize = buffSize;
		dest.buffer = buffer.clone();
		return dest;
	}
}
