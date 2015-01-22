public class LagmarkerNative {
	
	static {
		System.loadLibrary("LagmarkerNative");
	}
	
	private native int lnativeEcho(int x);
	
	public static void main(String[] args) {
		new HelloJNI().sayHello();
		System.out.println(new LagmarkerNative().lnativeEcho(1904));
	}

}
