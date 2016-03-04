package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Scanner;

public class Peers_v2 {
	private static final String IP_SERVEUR = "192.168.0.10";
	//private static final int SERVER_SIZE = 100;

	private static int hash = -1;

	private static HashMap<Integer, String> finger = new HashMap<Integer, String>();
	private static String IPsuccesseur = "";
	private static int idSuccesseur;
	private static String IPpredecesseur = "";
	private static int idPredecesseur;


	private static String ip;

	public static void main(String[] args) {

		try {
			//IP in the same network
			ip = InetAddress.getLocalHost().getHostAddress();
		}catch(Exception e) {System.err.println("error ip"); return;}
		//public IP
		/*URL whatismyip = new URL("http://checkip.amazonaws.com");
			BufferedReader in = new BufferedReader(new InputStreamReader(
					whatismyip.openStream()));

			ip = in.readLine(); //you get the IP as a String
			System.out.println(ip);
		} catch (UnknownHostException e1) {System.err.println("Can't find ip"); return;}
		catch (IOException e) {
			e.printStackTrace();
		}*/


		try 
		(		Socket hashSocket = new Socket(IP_SERVEUR, 8001);
				BufferedReader hashEntree = new BufferedReader(new InputStreamReader (hashSocket.getInputStream()));
				PrintStream hashSortie = new PrintStream (hashSocket.getOutputStream(), true);)
		{
			hashSortie.println(ip);
			hash = Integer.valueOf(hashEntree.readLine());
		} 	
		catch(UnknownHostException e) {System.err.println("unknown host"); return;}
		catch ( IOException e ) {System.err.println("erreur I/O HashServer"); return;}


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
				idSuccesseur = hash;
				finger.put(hash, ip);
				IPpredecesseur = ip;
				idPredecesseur = hash;
				System.out.println("Your hash : "+hash);
				System.out.println("Your ip : "+ip);
			}
			else if(!entreeLue.equals("wrq")) {
				System.out.println("Joining network...");
				if(!ClientJoining(entreeLue)){
					System.err.println("Client cannot join network");
					return;
				}
				System.out.println("Network joined");
				System.out.println("Your hash : "+hash);
				System.out.println("Your ip : "+ip);

			}
			else {
				System.err.println("The connexion failed");
				return;
			}
		} 	
		catch(UnknownHostException e) {System.err.println("unknown host"); return;}
		catch ( IOException e ) {System.err.println("erreur I/O Welcome Server"); return;}





		Thread te = new Thread(new Runnable() {
			public void run() {
				serverListener();
			}
		});
		te.start();

		scanCommandLine();
	}



	//message format :
	//[command]:[hashTo]:[hashHrom]:[IPFrom]:[message]
	private static void TreatMessage(String message, PrintStream os, BufferedReader d, Socket cs) {
		String[] cmds = message.split(":");
		switch (cmds[0]) {
		case "addme":			
			sendToIP(cmds[3], idPredecesseur+"-"+IPpredecesseur);

			IPpredecesseur=cmds[3];
			idPredecesseur = PeersUtility.safeParseInt(cmds[2]);

			System.out.println("New Peer added to the network");
			break;
			
		case "newSucc" :
			IPsuccesseur = cmds[3];
			idSuccesseur = PeersUtility.safeParseInt(cmds[2]);
			System.out.println("Successor has been updated - new Successor : "+idSuccesseur);
			break;
			
		case "getSucc":
			int hashToGet = Integer.valueOf(cmds[2]);
			if(idSuccesseur > hash && hashToGet < idSuccesseur)
				sendToIP(cmds[3], idSuccesseur+"-"+IPsuccesseur);
			else if(idSuccesseur < hash && (hashToGet > hash || (hashToGet > 0 && hashToGet < idSuccesseur)))
				sendToIP(cmds[3], idSuccesseur+"-"+IPsuccesseur);
			else if (idSuccesseur == hashToGet)
				sendToIP(cmds[3], idSuccesseur+"-"+IPsuccesseur);
			else
				passToSuccessor(message);
			break;

		case "transmit" :
			//transmit:[hashTo]:[myHash]:[myIP]:[message]
			sendToIP(cmds[3], "Whouhou");

			break;


		default:
			//System.out.println(message);
			break;
		}
	}


	private static boolean ClientJoining(String IPKnown) {
		boolean ret = true;
		System.out.println("Trying to join the server");
		//[command]:[hashTo]:[hashHrom]:[IPFrom]:[message]

		String[] result = (getResponseIP(IPKnown, "getSucc:"+":"+hash+":"+hash+":"+ip)).split("-");
		idSuccesseur = PeersUtility.safeParseInt(result[0]);
		IPsuccesseur = result[1];
		if(idSuccesseur == hash){
			System.err.println("hash already taken");
			ret = false;
		}
		
		
		result = null;
		//idSuccesseur ne sert à rien dans cette envois, mais c'est par soucis de respecter le format des messages
		result = (getResponseIP(IPsuccesseur, "addme:"+idSuccesseur+":"+hash+":"+ip)).split("-");
		idPredecesseur = PeersUtility.safeParseInt(result[0]);
		IPpredecesseur = result[1];

		sendToIP(IPpredecesseur, "newSucc:"+idSuccesseur+":"+hash+":"+ip);
		
		return ret;
	}






	private static void sendToIP(String IP, String message) {
		PeersUtility.sendToIP(IP, message);
	}
	private static void passToSuccessor(String message) {
		PeersUtility.sendToIP(IPsuccesseur, message);
	}




	private static String getResponse(String command, int hashTo, String message) {
		String send = command+":"+hashTo+":"+hash+":"+ip+":"+message.replace(":", " ");
		return PeersUtility.getResponseIP(IPsuccesseur, send);
	}

	private static String getResponseIP(String IP, String message) {
		return PeersUtility.getResponseIP(IP, message);
	}










	protected static void serverListener() {
		try(ServerSocket ss = new ServerSocket(2016);){

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
						System.out.println("un message est arrivé sur le serveur d'écoute");
						TreatMessage(message, os, d, cs);

					}
				}
				catch (IOException e) {
					System.err.println("Listening server IO Erreur");
				}
			}
		}catch (IOException e) {System.err.println("erreur starting listening server"); return;}

	}

	private static void scanCommandLine() {


		try (Scanner scan = new Scanner(System.in)) {
			while(true) {

				String mess = scan.nextLine();
				int hashTo = scan.nextInt();
				getResponse("transmit", hashTo, mess);


			}
		}
	}

}
