package mobileworkloads.jlagmarker.masking;

public class Rectangle {
	public final int x0;
	public final int y0;
	public final int width;
	public final int height;
	
	public Rectangle(int x0, int y0, int width, int height) {
		this.x0 = x0;
		this.y0 = y0;
		this.width = width;
		this.height = height;
		
		assert x0 >= 0 && y0 >= 0 && width > 0 && height > 0 : "Invalid Rectangle dimensions: " + toString();
	}
	
	@Override
	public String toString() {
		StringBuilder bld = new StringBuilder();
		bld.append(x0).append(" ")
		.append(y0).append(" ")
		.append(width).append(" ")
		.append(height);
		return bld.toString();
	}
}
