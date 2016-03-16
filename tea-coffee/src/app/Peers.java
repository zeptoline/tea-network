package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;


public class Peers {
	private static String IP_SERVEUR = "localhost";
	private static int SERVER_SIZE = 50;

	private static int hash = -1;

	private static HashMap<Integer, String> finger = new HashMap<Integer, String>();
	private static ArrayList<String> finger_moniteur = new ArrayList<String>();
	private static String IPsuccesseur = "";
	private static int idSuccesseur;
	private static String IPpredecesseur = "";
	private static int idPredecesseur;


	private static String ip;

	public static void main(String[] args) {
	
		if(args.length >= 1) {
			if (args[0].matches("\\d+.\\d+.\\d+.\\d+")) {
				IP_SERVEUR = args[0];
			}
		}
		if(args.length >= 2) {
			SERVER_SIZE = PeersUtility.safeParseInt(args[1]);
		}
		
		
		
		
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
				System.out.println("You're first on the network");
				IPsuccesseur = ip;
				idSuccesseur = hash;
				finger.put(hash, ip);
				finger_moniteur.add(hash+":"+hash+":"+ip);
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

		Thread monitor = new Thread(new Runnable() {
			@Override
			public void run() {
				monitorListener();
			}
		});
		monitor.start();

		Thread refresher = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						Thread.sleep(300000);
						refreshFinger();
					} catch (InterruptedException e) {
						System.err.println("error stopping thread");
					}
				}

			}
		});
		refresher.start();

		Thread expiredTest = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						Thread.sleep(30000);

						try (Socket sc = new Socket (IPsuccesseur, 2016);
								BufferedReader entree = new BufferedReader(new InputStreamReader (sc.getInputStream()));
								PrintStream sortie = new PrintStream (sc.getOutputStream(), true);
							){
							sortie.println("ruhere");							
							if(!entree.readLine().equals("yes")) {
								System.err.println("howdidIgotHere");
							}
						}
						catch (IOException e) {
							catchSuccessorFail();
						}
					} 
					catch (InterruptedException e) {System.err.println("error time refresh thread");}
				}
			}
		});
		expiredTest.start();
		
		scanCommandLine();

	}



	private static void catchSuccessorFail() {
		System.out.println("The successor is not joignable");

		leaveWelcomeServer(idSuccesseur);
		
//		finger.remove(idSuccesseur);
//		finger.remove(hash);
//		System.out.println("removed successor from routing table");
//		int next = -1;
//		for (int key : finger.keySet()) {
//			next = key;
//			break;
//		}
//		if (next != -1) {
//			int old = idSuccesseur;
//			idSuccesseur = next;
//			IPsuccesseur = finger.get(next);
//
//			passToSuccessor("exit:"+old+":"+hash);
//		} else {
//			idSuccesseur = hash;
//			idPredecesseur = hash;
//			IPpredecesseur = ip;
//			IPsuccesseur = ip;
//		}
		
		// si seulement 2 personnes
		if(idPredecesseur == idSuccesseur){
			finger.remove(idSuccesseur);

			idSuccesseur = hash;
			idPredecesseur = hash;
			IPpredecesseur = ip;
			IPsuccesseur = ip;
			
			refreshFinger();
		}
		else {
			String result = getResponseIP(IPpredecesseur, "getNewSucc:"+idSuccesseur+":"+ip+":"+hash);
			int old = idSuccesseur;
			
			String[] results = result.split("-");
			idSuccesseur = PeersUtility.safeParseInt(results[0]);
			IPsuccesseur = results[1];

			passToSuccessor("exit:"+old+":"+hash);
		}
		
	}


	//message format :
	//[command]:[hashTo]:[hashFrom]:[IPFrom]:[message]
	private static void TreatMessage(String message, PrintStream os, BufferedReader d, Socket cs) {
		String[] cmds = message.split(":");
		switch (cmds[0]) {
		/*
		 * Init messages
		 */
		//addme:[hash]:[new idPred]:[new IPpred]
		case "addme":			
			sendResponse(cmds[3], idPredecesseur+"-"+IPpredecesseur);

			IPpredecesseur=cmds[3];
			idPredecesseur = PeersUtility.safeParseInt(cmds[2]);

			System.out.println("New Peer added to the network");
			break;
		//newPred:[hash]:[newIDpred]:[newIPpred]
		case "newPred" :
			IPpredecesseur = cmds[3];
			idPredecesseur = PeersUtility.safeParseInt(cmds[2]);
			System.out.println("Predecessor has been updated - new Predecessor : "+idPredecesseur);
			break;

		//newSucc:[hash]:[newIDsucc]:[newIPsucc]
		case "newSucc" :
			IPsuccesseur = cmds[3];
			idSuccesseur = PeersUtility.safeParseInt(cmds[2]);
			finger.put(idSuccesseur, IPsuccesseur);
			finger_moniteur.add(hash+":"+idSuccesseur+":"+IPsuccesseur);
			System.out.println("Successor has been updated - new Successor : "+idSuccesseur);
			break;

		case "getSucc":
			int hashToGet = Integer.valueOf(cmds[1]);
			if(idSuccesseur > hash && hashToGet < idSuccesseur && hashToGet > hash)
				sendResponse(cmds[3], idSuccesseur+"-"+IPsuccesseur);
			else if(idSuccesseur < hash && (hashToGet > hash || (hashToGet <= idSuccesseur)))
				sendResponse(cmds[3], idSuccesseur+"-"+IPsuccesseur);
			else if (idSuccesseur == hash)
				sendResponse(cmds[3], hash+"-"+ip);
			else if (hashToGet == hash)
				sendResponse(cmds[3], hash+"-"+ip);
			else
				passToSuccessor(message);
			break;


			/*
			 * Clients messages 
			 */
		case "transmit" :
			System.out.println(message);
			int hashTo = PeersUtility.safeParseInt(cmds[1]);
			if(hashTo != hash) {
				if(idSuccesseur > hash && idSuccesseur < hashTo)
					passToNearest(message, hashTo);
				else if (idSuccesseur > hash && idSuccesseur >hashTo && hashTo > hash) 
					sendResponse(cmds[3], "error, no hash corresponding");
				else if (idSuccesseur == hash && hashTo != hash)
					sendResponse(cmds[3], "error, no hash corresponding");
				else if (idSuccesseur < hash && !(hashTo > hash || hashTo < idSuccesseur))
					sendResponse(cmds[3], "error : no hash corresponding");
				else if (idSuccesseur == hashTo)
					passToSuccessor(message);
				else
					passToNearest(message, hashTo);
			} else 
			{
				System.out.println("You received a message from "+cmds[2]+" : ");
				System.out.println("\t"+cmds[4]);
				sendResponse(cmds[3], "Message well received");
			}

			break;

		case "who" :
			int hashfrom = Integer.valueOf(cmds[1]);
			if(hashfrom != hash) 
				passToSuccessor(message+" - "+hash);
			else 
				System.out.println("list of hashes  : "+cmds[2]);

			break;			
		//exit:hash
		case "exit" :
			finger.remove(Integer.valueOf(cmds[1]));
			if(Integer.valueOf(cmds[2]) != hash)
				passToSuccessor(message);
			System.out.println(cmds[1] + " has left");
			break;
			
		case "ruhere":
			os.println("yes");
			break;

		//getNewSucc:hashToRemove:ipFrom:hashfrom
		case "getNewSucc":
			int hashToRemove = PeersUtility.safeParseInt(cmds[1]);
			if(idPredecesseur == hashToRemove) {
				idPredecesseur = PeersUtility.safeParseInt(cmds[2]);
				IPpredecesseur = cmds[3];
				sendResponse(cmds[2], hash+"-"+ip);
			} else {
				sendToIP(IPpredecesseur, message);
			}
			break;
			
			
		default:
			System.err.println("Received message '"+message+"', cannot treat");
			break;
		}
	}


	private static boolean ClientJoining(String IPKnown) {
		boolean ret = true;

		System.out.println("Looking for Successor...");
		String[] result = (getResponseIP(IPKnown, "getSucc:"+hash+":"+hash+":"+ip)).split("-");
		idSuccesseur = PeersUtility.safeParseInt(result[0]);
		IPsuccesseur = result[1];
		finger.put(idSuccesseur, IPsuccesseur);
		finger_moniteur.add(hash+":"+idSuccesseur+":"+IPsuccesseur);
		if(idSuccesseur == hash){
			System.err.println("hash already taken");
			ret = false;
		}


		System.out.println("Getting Predecessor...");
		result = null;

		//idSuccesseur ne sert � rien dans cette envois, mais c'est par soucis de respecter le format des messages
		result = (getResponseIP(IPsuccesseur, "addme:"+idSuccesseur+":"+hash+":"+ip)).split("-");
		idPredecesseur = PeersUtility.safeParseInt(result[0]);
		IPpredecesseur = result[1];

		System.out.println("Updating Predecessor...");
		sendToIP(IPpredecesseur, "newSucc:"+idSuccesseur+":"+hash+":"+ip);

		return ret;
	}





	private static void sendResponse(String IP, String message) {
		try {
			PeersUtility.sendToIP(IP, message, 2017);
		} catch (IOException e) {e.printStackTrace();}
	}
	private static void sendToIP(String IP, String message) {
		try {
			PeersUtility.sendToIP(IP, message, 2016);
		} catch (IOException e) {e.printStackTrace();}
	}
	private static void passToSuccessor(String message) {
		try {
			PeersUtility.sendToIP(IPsuccesseur, message, 2016);
		} catch (IOException e) {
			catchSuccessorFail();
			passToSuccessor(message);
			}
	}
	private static void passToNearest(String message, int hashTo) {
		/*
		 * Regarder la finger table
		 * 
		 */	
		int lastHash;
		boolean trouver = false;
		
		//Si c'est dans ta finger, envoyé direct
		if(finger.containsKey(hashTo)) {
			lastHash = hashTo;
		} else {
			
			int hashTest1 = -1, hashTest2 = -1;
			for (int hashTest : finger.keySet()) {
				if(hashTest1 == -1){
					hashTest1 = hash;
				} else {
					hashTest1 = hashTest2;
				}
				hashTest2 = hashTest;
				
				if(hashTest2 < hashTest1) {
					//passage au 0
					if( hashTo > hashTest1 || hashTo < hashTest2) {
						trouver = true;
						break;
					}
				} else {
					if(hashTo < hashTest2 && hashTo > hashTest1) {
						trouver = true;
						break;
					}
				}
			}
			if(trouver){
				lastHash = hashTest1;
			} else {
				lastHash = hashTest2;
			}
		}
		try {
			PeersUtility.sendToIP(finger.get(lastHash), message, 2016);
		} catch (IOException e) {e.printStackTrace();}
	}



	private static String getResponse(String command, int hashTo, String message) {
		String send = command+":"+hashTo+":"+hash+":"+ip+":"+message;
		try {
			return PeersUtility.getResponseIP(IPsuccesseur, send);
		} catch (IOException e) {e.printStackTrace();}
		return null;
	}

	private static String getResponseIP(String IP, String message) {
		try {
			return PeersUtility.getResponseIP(IP, message);
		} catch (IOException e) {e.printStackTrace();}
		return null;
	}






	public static void refreshFinger() {
		System.out.println("Refreshing Routing Table");
		finger.clear();
		finger_moniteur.clear();
		
		int max = (int) (Math.log(SERVER_SIZE) / Math.log(2));
		int puissance = 0;
		for (int i = 0; i <= max; i++) {
			puissance = (hash + (int)Math.pow(2, i)) % SERVER_SIZE;
			String[] result = (getResponseIP("localhost", "getSucc:"+puissance+":"+hash+":"+ip)).split("-");
			int hp = PeersUtility.safeParseInt(result[0]);

			finger.put(hp, result[1]);
			finger_moniteur.add(puissance+":"+hp+":"+result[1]);
			//System.out.println(puissance+" : "+result[1]);

		}
		System.out.println("Finished refreshing routing table");
		
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
						TreatMessage(message, os, d, cs);

					}
				}
				catch (IOException e) {
					System.err.println("Listening server IO Erreur");
				}
			}
		}catch (IOException e) {System.err.println("erreur starting listening server"); return;}

	}

	protected static void monitorListener() {
		try(ServerSocket ss = new ServerSocket(8002);){
			while(true) {
				try(Socket cs = ss.accept();
						BufferedReader d = new BufferedReader(new InputStreamReader(cs.getInputStream()));
						PrintStream os = new PrintStream (cs.getOutputStream(), true);
						) 
				{
					System.out.println("Starting sending routing table");
					d.readLine();
					for (String key : finger_moniteur) {
						os.println(key);
					}
					os.println("end");
				}
				catch (IOException e) {
					System.err.println("Monitor server IO Erreur");
				}
			}
		}catch (IOException e) {System.err.println("erreur starting monitor server"); return;}
	}

	
	
	protected static void leaveWelcomeServer(int hashToLeave) {
		try (Socket WSsock = new Socket (IP_SERVEUR, 8000);
				PrintStream sortie = new PrintStream (WSsock.getOutputStream(), true);
				)
		{
			String connec = "a+:"+hashToLeave;
			sortie.println(connec);			
		} 	
		catch(UnknownHostException e) {System.err.println("unknown host"); return;}
		catch ( IOException e ) {System.err.println("erreur I/O Welcome Server"); return;}
	}
	
	
	
	
	
	
	
	
	//message format :
	//[command]:[hashTo]:[hashHrom]:[IPFrom]:[message]
	private static void scanCommandLine() {
		try (Scanner scan = new Scanner(System.in)) {
			boolean terminate = false;
			while(!terminate) {
				String mess = scan.nextLine();
				switch (mess) {
				case "info":
					System.out.println("IP Successeur : " +IPsuccesseur);
					System.out.println("Id Successeur : " +idSuccesseur);
					System.out.println("IP Predecesseur : " +IPpredecesseur);
					System.out.println("Id Predecesseur : " +idPredecesseur);
					for (String key : finger_moniteur) {
						System.out.println(key);
					}
					break;

				case "transmit" :
					System.out.println("Hash to send to : ");
					int hashTo = scan.nextInt();
					scan.nextLine();
					System.out.println("What to send : ");
					String message = scan.nextLine();
					System.out.println(getResponse("transmit", hashTo, message));
					break;
				case "who" :
					passToSuccessor("who:"+hash+":"+hash);
					break;
				case "refresh" :
					refreshFinger();
					break;
				case "exit" :
					//newSucc:[hash]:[newIDsucc]:[newIPsucc]
					sendToIP(IPpredecesseur, "newSucc:"+idPredecesseur+":"+idSuccesseur+":"+IPsuccesseur);
					//newPred:[hash]:[newIDpred]:[newIPpred]
					passToSuccessor("newPred:"+idSuccesseur+":"+idPredecesseur+":"+IPpredecesseur);
					
					leaveWelcomeServer(hash);
					
					//Pour retirer de la table
					passToSuccessor("exit:"+hash+":"+idPredecesseur);
					terminate = true;
					break;
				default:
					System.out.println("Utilisation :");
					System.out.println("\tinfo : donne le successeurs et le pr�d�c�sseur, avec la table de routage");
					System.out.println("\ttransmit : Envois un message � un autre membre du network (le hash doit exister)");
					System.out.println("\twho : Liste tous les hashes du network");
					System.out.println("\trefresh : rafraichis la table de routage manuellement");
					System.out.println("\texit : quites le network");
					
					break;
				}


			}
			
			System.exit(0);
		}
	}

}
