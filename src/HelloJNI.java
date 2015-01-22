public class HelloJNI {
	static {
		System.loadLibrary("HelloJNI");
	}
	
	public native void sayHello();
}
