package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class PeersUtility {
	public static void sendToIP(String IP, String message) throws IOException{


		try(Socket sc = new Socket(IP, 2017);
				PrintStream os = new PrintStream (sc.getOutputStream(), true);) {
			os.println(message);
		}
		catch (IOException e) {
			throw e;
		}
	}
	
	
	public static void sendToIP(String IP, String message, int port) throws IOException{


		try(Socket sc = new Socket(IP, port);
				PrintStream os = new PrintStream (sc.getOutputStream(), true);) {
			os.println(message);
		}
		catch (IOException e) {
			throw e;
		}
	}


	public static String getResponseIP(String IP, String message) throws IOException{
		sendToIP(IP, message, 2016);
		String response = "";

		try(ServerSocket responseGetter = new ServerSocket(2017); 
				Socket cl = responseGetter.accept();
				BufferedReader d = new BufferedReader(new InputStreamReader(cl.getInputStream()));
				) 
		{
			response = d.readLine();
		} catch (IOException e) {throw e;}
		return response;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	// Parsing d'une chaîne de caractères pour trouver un entier
	// En cas d'erreur l'entier retourné est négatif
	@SuppressWarnings("finally")
	public static int safeParseInt(String i) {

		int res = -1;

		try {
			res = Integer.parseInt(i);
		} finally {
			return res;
		}

	}








}
