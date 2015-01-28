import java.io.FileOutputStream;
import java.io.IOException;

public class JRGBFrameBuffer {
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
	
	public int getChannel(int pixel, int channel) {
		if (channel > 2 || channel < 0)
			throw new RuntimeException("Given channel must be between 0 and " + (CHANNEL_NUM - 1) + "!");
		
		int idx = pixel * CHANNEL_NUM + channel;
		
		return buffer[idx] & 0xFF;
	}
	
	public void writeToFile(String filename) throws IOException {
		FileOutputStream out = new FileOutputStream(filename);
		out.write(("P6\n" + getWidth() + " " + getHeight() + "\n255\n").getBytes()); // write header
		out.write(buffer);
		out.close();
	}
}
