import java.io.IOException;

public class LagmarkerNative {
	static {
		System.loadLibrary("LagmarkerNative");
	}
	
	private native boolean lnativeLoadRGBFrameBuffer(String filename, JRGBFrameBuffer targetBuff);
	
	private native boolean lnativeSaveRGBFrameBuffer(String filename, JRGBFrameBuffer sourceBuff);
	
	public static void main(String[] args) {
		VideoState vstate = new VideoState("res/testvideo.ts");
		
		for(int i = 0; i < 4; i++) {
			System.out.println(vstate);
			JRGBFrameBuffer currFrame = vstate.decodeNextVideoFrame();
		}
	}

}
