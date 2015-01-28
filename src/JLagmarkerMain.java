public class JLagmarkerMain {
	
	public static void main(String[] args) {
		VideoState vstate = new VideoState("res/testvideo.ts");
		
		for(int i = 0; i < 4; i++) {
			System.out.println(vstate);
			JRGBFrameBuffer currFrame = vstate.decodeNextVideoFrame();
		}
	}

}
