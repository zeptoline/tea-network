package app;

import java.io.BufferedReader;
import java.io.IOException;


public class PeerGet implements Runnable {

	private BufferedReader d;
	private volatile boolean running;
	
	public PeerGet( BufferedReader d) {
		this.d = d;
	}
	public void terminate() {
		running = false;
	}
	
	public void run() {
		running = true;
		while(running) {
			try {
				String message;
				while ((message = d.readLine())!=null) 
				{
					System.out.println(message);
				}
			}  
			catch (IOException e) {	}
		}
		System.out.println("end listener");

	}

}
