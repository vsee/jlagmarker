public class LagmarkerNative {
	static {
		System.loadLibrary("LagmarkerNative");
	}
	
	private native int lnativeEcho(int x);
	
	private native boolean lnativeLoadRGBFrameBuffer(String filename, JRGBFrameBuffer targetBuff);
	
	private native boolean lnativeSaveRGBFrameBuffer(String filename, JRGBFrameBuffer sourceBuff); 
	
	public static void main(String[] args) {
		LagmarkerNative lnative = new LagmarkerNative();
		JRGBFrameBuffer buff = new JRGBFrameBuffer();
		if(!lnative.lnativeLoadRGBFrameBuffer("res/bw_frame.ppm", buff)) {
			System.err.println("ERROR: Reading RGB buffer failed!");
			return;
		}

		System.out.println("Reading RGB buffer successfull: " 
				+ buff.getWidth() + "x" + buff.getHeight() 
				+ " size: " + buff.getSize());
		
		if(!lnative.lnativeSaveRGBFrameBuffer("res/jbw_frame.ppm", buff)) {
			System.err.println("ERROR: Saving RGB buffer failed!");
			return;
		}
	}

}
