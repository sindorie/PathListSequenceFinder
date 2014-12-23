package support.solver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class testYices {
	static String path = "/home/zhenxu/Tools/Solver/yices-2.2.2/bin/yices";
	
	public static void main(String[] args) throws IOException, InterruptedException {
//		terminalTest();
		testInterface();
	}
	
	static void testInterface() throws IOException{
		YicesProcessInterface yices = new YicesProcessInterface( path);
		String[] toWrite = {
				"(define x::real)",
				"(define y::real)",
				"(assert (> x 0))",
				"(assert (> y 0))",
				"(assert (= 1 (+ x y)))",
				"(check)\n",
			};
		boolean result = yices.solve(false,toWrite);
		System.out.println(result);
	}
	
	static void terminalTest() throws IOException, InterruptedException{
		
		Process p = Runtime.getRuntime().exec(path);
		InputStream errorChannel = p.getErrorStream();
		InputStream readChannel = p.getInputStream();
		OutputStream writeChannel = p.getOutputStream();
		
		
		String[] toWrite = {
				"(define x::int)",
				"(define y::int)",
				"(assert (> x 0))",
				"(assert (> y 0))",
				"(assert (= 0 (+ x y)))",
				"(check)\n",
			};
		
		
		for(String s : toWrite){
			System.out.println(s);
			writeChannel.write(s.getBytes());
		}
		writeChannel.flush();
		
		System.out.println("waiting result");
		Thread.sleep(1000);
		
		
		int count = readChannel.available();
		byte[] buf = new byte[count];
		readChannel.read(buf);
		System.out.println(new String(buf));
		
		
//		count = errorChannel.available();
//		buf = new byte[count];
//		errorChannel.read(buf);
//		System.out.println(new String(buf));
	}

}
