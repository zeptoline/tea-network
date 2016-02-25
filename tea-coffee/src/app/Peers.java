package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Peers {
	private static final String IP_SERVEUR = "192.168.0.10";

	private static String hash ="";

	private static String IPsuccesseur = "";
	private static int idSuccesseur;
	private static String IPpredecesseur = "";
	private static int idPredecesseur;

	private static String ip;

	public static void main(String[] args) {

		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e1) {System.out.println("Can't find ip"); return;}


		try 
		(		Socket hashSocket = new Socket(IP_SERVEUR, 8001);
				BufferedReader hashEntree = new BufferedReader(new InputStreamReader (hashSocket.getInputStream()));
				PrintStream hashSortie = new PrintStream (hashSocket.getOutputStream(), true);)
		{
			hashSortie.println(ip);
			hash = hashEntree.readLine();
		} 	
		catch(UnknownHostException e) {System.out.println("unknown host"); return;}
		catch ( IOException e ) {System.out.println("erreur I/O HashServer"); return;}


		try (Socket WSsock = new Socket (IP_SERVEUR, 8000);
				BufferedReader entree = new BufferedReader(new InputStreamReader (WSsock.getInputStream()));
				PrintStream sortie = new PrintStream (WSsock.getOutputStream(), true);
				)
		{
			String connec = "yo:"+hash+":"+ip;
			sortie.println(connec);

			String entreeLue = entree.readLine();
			if(entreeLue.equals("yaf")) { 
				System.out.println("You're the first on the network");
				IPsuccesseur = ip;
				idSuccesseur = Integer.valueOf(hash);
				IPpredecesseur = ip;
				idPredecesseur = Integer.valueOf(hash);
			}
			else if(!entreeLue.equals("wrq")) {
				System.out.println("Your successor is at " + entreeLue);
				ClientJoining(entreeLue);

			}
			else
				System.out.println("The connexion failed");

		} 	
		catch(UnknownHostException e) {System.out.println("unknown host"); return;}
		catch ( IOException e ) {System.out.println("erreur I/O Welcome Server"); return;}


		Thread te = new Thread(new Runnable() {
			public void run() {
				try (ServerSocket ss = new ServerSocket(2016);){
					System.out.println("Start listening server");
					while(true) {
						try(Socket cs = ss.accept();
								BufferedReader d = new BufferedReader(new InputStreamReader(cs.getInputStream()));
								PrintStream os = new PrintStream (cs.getOutputStream(), true);
								) 
						{
							String message = "";
							while((message = d.readLine())!= null) 
							{
								System.out.println(message);
								TreatMessage(message, os, d, cs);
								
							}
						}
						catch (IOException e) {
							System.out.println("Listening server IO Erreur");
						}
					}

				} catch (IOException e) {}
			}
		});
		te.start();




	}


	private static void TreatMessage(String message, PrintStream os, BufferedReader d, Socket cs) {
		String[] cmds = message.split(":");
		int key;
		switch (cmds[0]) {
		case "addme_pls" :
			AddToNetwork(os, d, cs);
			break;
		case "lookin'for_someone" :
			
			break;
		case "I'm leaving" :
			System.out.println("fuck, he's leaving");
			break;
		case "Hey Im new" :
			setSuccessor(os, d, cs);
			break;
		case "findPred" :
			 key= Integer.valueOf(cmds[1]);
			os.println(findPredecessor(key));
			break;
		case "findSucc" :
			key = Integer.valueOf(cmds[1]);
			os.println(findSuccessor(key));
			break;
		default:
			break;
		}
	}


	private static void AddToNetwork(PrintStream os, BufferedReader d, Socket cs) {
		System.out.println("Adding a peer to the network");
		os.println("K.");
		os.println(IPpredecesseur);
		IPpredecesseur = cs.getInetAddress().getHostAddress();
		try {
			idPredecesseur = Integer.valueOf(d.readLine());
		} catch (IOException e) {
			System.out.println("fuck he's ded");
		}
	}

	private static void setSuccessor(PrintStream os, BufferedReader d, Socket cs) {
		System.out.println("Setting new successor");
		IPsuccesseur = cs.getInetAddress().getHostAddress();
		os.println("hash pls?");
		try {
			idSuccesseur = Integer.valueOf(d.readLine());
		} catch (IOException e) {
			System.out.println("fuck he's ded");
		}
	}
	
	
	private static void ClientJoining(String IPKnown) {
		System.out.println("Trying to join the server");

		IPsuccesseur = IPKnown;
		
		try 
		(		Socket clientPresent = new Socket(IPKnown, 8001);
				BufferedReader in = new BufferedReader(new InputStreamReader (clientPresent.getInputStream()));
				PrintStream out = new PrintStream (clientPresent.getOutputStream(), true);)
		{

			out.println("addme_pls");
			if(in.readLine().equals("K.")) {
				IPpredecesseur = in.readLine();
				out.println(hash);
			}
			
		} 	
		
		catch(UnknownHostException e) {System.out.println("unknown host"); return;}
		catch ( IOException e ) {System.out.println("I/O error"); return;}
		
		try 
		(		Socket Precedant = new Socket(IPKnown, 8001);
				BufferedReader in = new BufferedReader(new InputStreamReader (Precedant.getInputStream()));
				PrintStream out = new PrintStream (Precedant.getOutputStream(), true);)
		{

			out.println("Hey Im new");
			if(in.readLine().equals("hash pls?")) {
				out.println(hash);				
			}
		} 	
		catch(UnknownHostException e) {System.out.println("unknown host"); return;}
		catch ( IOException e ) {System.out.println("I/O error"); return;}
	}
	
	
	
	
	
	
	private static String findPredecessor(int key) {
		// Methode recursive cherchant le predecesseur d'une certaine clef
		System.out.println(IPpredecesseur);
		System.out.println(idPredecesseur);
		
		return "";
	}
	private static String findSuccessor(int key) {
		// Methode recursive cherchant le successeur d'une certaine clef
		
		//Il faudra parcourir la table des fingers (si on la fait) et renvoye cette fonction au client le plus proche
		System.out.println(IPsuccesseur);
		System.out.println(idSuccesseur);
		return "";		
	}
}
