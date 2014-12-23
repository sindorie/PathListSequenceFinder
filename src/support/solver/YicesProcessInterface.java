package support.solver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class YicesProcessInterface implements ProblemSolver {
	private Process yicesProcess;
	private InputStream readChannel, errorChannel;
	private OutputStream writeChannel;
	private long maxtime = 1000 * 60; // 1min
	private String path;

	public YicesProcessInterface(String path) throws IOException {
		this.path = path;
//		startProcess();
	}

	public boolean solve(boolean newProcess, String... statements) {
		if(newProcess){
			this.terminateProcess();
		}
		
		if(this.yicesProcess == null){
			try {
				startProcess();
			} catch (IOException e) {
				System.out.println("start process fails");
				throw new AssertionError();
			}
		}
		
		try {
			long end = System.currentTimeMillis() + maxtime;
			boolean result = false, timeout = false;
			int count;

			count = readChannel.available();

			if (count > 0) {
				byte[] buf = new byte[count];
				readChannel.read(buf);
			}
			for (String stat : statements) {
				writeChannel.write(stat.getBytes());
			}
			writeChannel.flush();

			while (true) {
				long current = System.currentTimeMillis();
				count = readChannel.available();
				if (count > 0) {
					byte[] buf = new byte[count];
					readChannel.read(buf);
					String msg = new String(buf).trim();
					if (msg.equalsIgnoreCase("sat")) {
						result = true;
						break;
					} else if (msg.equalsIgnoreCase("unsat")) {
						result = false;
						break;
					} else {
						System.out.println("unidentified: " + msg);
						result = false;
						break;
					}
				}

				if (current >= end) {
					timeout = true;
					break;
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}

			if (timeout) {
				this.yicesProcess.destroy();
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
				startProcess();
			}

			if(newProcess) terminateProcess();
			
			return result;

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		return false;
	}

	private void terminateProcess(){
		if(yicesProcess!= null) yicesProcess.destroy();
		yicesProcess = null;
	}
	
	private void startProcess() throws IOException {
		yicesProcess = Runtime.getRuntime().exec(path);
		readChannel = yicesProcess.getInputStream();
		errorChannel = yicesProcess.getErrorStream();
		writeChannel = yicesProcess.getOutputStream();
	}
}
