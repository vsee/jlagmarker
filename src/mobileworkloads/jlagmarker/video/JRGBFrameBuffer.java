package mobileworkloads.jlagmarker.video;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.StringTokenizer;

public class JRGBFrameBuffer implements Cloneable {
	public static final int CHANNEL_NUM = 3;
	
	private int width;
	private int height;
	private int buffSize;
	
	/** Treat this as unsigned byte by interpreting with: b & 0xFF */
	private byte[] buffer;
	
	public JRGBFrameBuffer() { }
	
	public JRGBFrameBuffer(Path filename) throws IOException {
		readFromFile(filename);
	}
	
	
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
	
	protected void writeToFile(Path filename) throws IOException {
		FileOutputStream out = new FileOutputStream(filename.toString());
		out.write(("P6\n" + getWidth() + " " + getHeight() + "\n255\n").getBytes()); // write header
		out.write(buffer);
		out.close();
	}
	
	protected void readFromFile(Path filename) throws IOException {
		if(filename == null || !Files.isRegularFile(filename))
			throw new IllegalArgumentException("Given image buffer file invalid: " + filename);
		
        String line;
        StringTokenizer st;

        try {
            BufferedReader in =
              new BufferedReader(new InputStreamReader(
                new BufferedInputStream(
                  new FileInputStream(filename.toString()))));

            DataInputStream in2 =
              new DataInputStream(
                new BufferedInputStream(
                  new FileInputStream(filename.toString())));

            // read PPM image header

            // skip comments
            line = in.readLine();
            in2.skip((line+"\n").getBytes().length);
            do {
                line = in.readLine();
                in2.skip((line+"\n").getBytes().length);
            } while (line.charAt(0) == '#');

            // the current line has dimensions
            st = new StringTokenizer(line);
            width = Integer.parseInt(st.nextToken());
            height = Integer.parseInt(st.nextToken());
            buffSize = width * height * CHANNEL_NUM;

            // next line has pixel depth but we don't need it
            line = in.readLine();
            in2.skip((line+"\n").getBytes().length);
            st = new StringTokenizer(line);
            Integer.parseInt(st.nextToken());

            buffer = new byte[buffSize];
            
            for (int i = 0; i < buffer.length; i++) {
				buffer[i] = (byte)in2.readUnsignedByte();
			}
            
            in.close();
            in2.close();
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("Error: image in " + filename + " too big");
		}
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
