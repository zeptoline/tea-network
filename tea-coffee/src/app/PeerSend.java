package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class PeerSend {
	
	
	public static void main(String[] args) {


		String line;

		BufferedReader d = null;
		PrintStream os = null;  
		Socket smtpSocket = null;
		

		try 
		{
			smtpSocket = new Socket("localhost", 8000);	

			d = new BufferedReader(new InputStreamReader(smtpSocket.getInputStream()));
			os = new PrintStream(smtpSocket.getOutputStream());

		} 
		catch (UnknownHostException e) {
			System.err.println("Don't know about host: hostname");
		} 
		catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to: hostname");
		} 


		if (smtpSocket != null && os != null & d!= null) {
			try {
				PeerGet listener = new PeerGet(d);
				Thread t = new Thread(listener);
				
				
				line = d.readLine();
				System.out.println(line);

				Scanner sc = new Scanner(System.in);
				System.out.println("Votre pseudo :");
				os.println(sc.nextLine());

				
				while(!(line = d.readLine()).equals("ok")) {
					System.out.println(line);
					os.println(sc.nextLine());
				}

				t.start();
				
				while(!os.checkError()) {
					System.out.println("Prochain message ? ('end' pour terminer)");
					String message = sc.nextLine();
					
					if (message.equals("end"))
						break;
					os.println(message);
					
				}
				System.out.println("Fin de co");

				listener.terminate();
				System.out.println("listener fermé");
				smtpSocket.close();
				System.out.println("socket fermé");
				d.close();
				System.out.println("input fermé");
				os.close();
				System.out.println("output fermé");
				sc.close();
				System.out.println("Scanner fermé");
				
			}   
			catch (IOException e) {
				System.out.println(e);
			}
		}
	}
}
