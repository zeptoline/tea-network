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

public class Peers {
	private static final String IP_SERVEUR = "172.21.65.13";
	private static final int SERVER_SIZE = 100;

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
				System.out.println("Your successor is at " + entreeLue);
				ClientJoining(entreeLue);
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
								TreatMessage(message, os, d, cs);

							}
								}
						catch (IOException e) {
							System.err.println("Listening server IO Erreur");
						}
					}

				} catch (IOException e) {}
			}
		});
		te.start();

		try (Scanner scan = new Scanner(System.in)) {
			while(true) {
				String mess = scan.nextLine();
				switch (mess) {
				case "sendTo":
					System.out.println("send to Who ?");
					mess += ":"+scan.nextLine();
					mess += ":"+hash;
					System.out.println("What'd you wanna send ?");
					mess += ":"+(scan.nextLine().replaceAll(":", " "));
					sendToSuccessor("transfert:"+mess);
					break;

				case "who":
					mess += ":"+hash;
					mess += ":"+hash+";";
					sendToSuccessor(mess);
					break;

				case "refresh":
					mess += ":"+hash+":";
					sendToSuccessor(mess);
					break;


				case "bye":
					mess += ":"+idPredecesseur;
					mess += ":"+IPpredecesseur;
					sendToSuccessor(mess);
					break;

				default:
					if(mess.matches("sendTo:(\\d)+:"+hash+":[^:]*"))
						sendToSuccessor("transfert:"+mess);
					else {
						System.out.println("usage :");
						System.out.println("sendTo:[HashTo]:[YourHash]:[message]");
						System.out.println("or just sendTo, and follow the instructions");
					}
					break;
				}

			}
		}


	}


	private static void TreatMessage(String message, PrintStream os, BufferedReader d, Socket cs) {
		String[] cmds = message.split(":");
		switch (cmds[0]) {
		case "init" :
			switch (cmds[1]) {
			case "addme_pls" :
				AddToNetwork(os, d, cs);
				break;

			case "Hey Im new" :
				setSuccessor(os, d, cs);
				break;
			default:
				break;
			}
			break;

			//transfert ::  transfert:[typeTransfert]:[hashTo]:[hashFrom]:[message]
		case "transfert" :
			System.out.println(message);
			int hashTo = Integer.valueOf(cmds[2]);
			if(hashTo != hash) {
				if(idSuccesseur > hash) {
					if(idSuccesseur > hashTo) {
						sendToSuccessor("transfert:ERRORsendTo:"+cmds[3]+":"+hash+":the peers "+cmds[2]+" was not found");
					} else {
						sendToSuccessor(message);
					}
				} else {
					if(hashTo > hash) {
						sendToSuccessor("transfert:ERRORsendTo:"+cmds[3]+":"+hash+":the peers "+cmds[2]+" was not found");
					}else if (hashTo > 0 && hashTo < idSuccesseur) {
						sendToSuccessor("transfert:ERRORsendTo:"+cmds[3]+":"+hash+":the peers "+cmds[2]+" was not found");
					} else {
						sendToSuccessor(message);
					}
				}
			} else {
				if(cmds[1].equals("sendTo")) 
				{
					System.out.println("Received message from "+cmds[3]+" : "+cmds[4]);
					sendToSuccessor("transfert:sendToWellReceived:"+cmds[3]+":"+hash);
				} 
				if(cmds[1].equals("sendToWellReceived")) 
				{
					System.out.println("The message to "+cmds[3]+" was well received.");
				} 
				if(cmds[1].equals("ERRORsendTo")) 
				{
					System.err.println(cmds[4]);
				}
			}

			break;

		case "who":
			int hashfrom = Integer.valueOf(cmds[1]);
			if(hashfrom != hash) {
				sendToSuccessor(message+hash+";");
			} else {
				System.out.println("list of hashes  :"+cmds[2]);
			}
			break;

		case "refresh":
			System.out.println(message);
			int hashfrom2 = Integer.valueOf(cmds[1]);
			if(hashfrom2 != hash) {
				sendToSuccessor(message+hash+"-"+ip+";");
			} else {
				refreshFinger(cmds[2]);
			}
			break;
		case "bye":
			idPredecesseur = Integer.valueOf(cmds[1]);
			IPpredecesseur = cmds[2];
			sendToPredecessor("newSuccessor:"+hash+":"+ip);
			break;


		case "newSuccessor":
			idSuccesseur = Integer.valueOf(cmds[1]);
			IPsuccesseur = cmds[2];
			finger.put(idSuccesseur, IPsuccesseur);
			break;


		default:
			//System.out.println(message);
			break;
		}
	}


	private static void AddToNetwork(PrintStream os, BufferedReader d, Socket cs) {
		System.out.println("Adding a peer to the network");
		os.println("K.");
		os.println(hash);
		os.println(IPpredecesseur);
		os.println(idPredecesseur);



		IPpredecesseur = cs.getInetAddress().getHostAddress();
		try {
			idPredecesseur = Integer.valueOf(d.readLine());
		} catch (IOException e) {
			System.err.println("fuck he's ded");
		}
		System.out.println("Peer added to the network");
	}

	private static void setSuccessor(PrintStream os, BufferedReader d, Socket cs) {
		System.out.println("Setting new successor");
		IPsuccesseur = cs.getInetAddress().getHostAddress();
		os.println("hash pls?");
		try {
			idSuccesseur = Integer.valueOf(d.readLine());
		} catch (IOException e) {
			System.err.println("fuck he's ded");
		}
		finger.put(idSuccesseur, IPsuccesseur);
		System.out.println("Successor added");
	}


	private static void ClientJoining(String IPKnown) {
		System.out.println("Trying to join the server");

		IPsuccesseur = IPKnown;
		
		try 
		(		Socket clientPresent = new Socket(IPKnown, 2016);
				BufferedReader in = new BufferedReader(new InputStreamReader (clientPresent.getInputStream()));
				PrintStream out = new PrintStream (clientPresent.getOutputStream(), true);)
				{

			out.println("init:addme_pls");
			if(in.readLine().equals("K.")) {
				idSuccesseur = Integer.valueOf(in.readLine());
				IPpredecesseur = in.readLine();
				idPredecesseur = Integer.valueOf(in.readLine());
				finger.put(idSuccesseur, IPsuccesseur);
				out.println(hash);
			} else{
				System.out.println("wut ?!");
			}

				} 	
		catch(UnknownHostException e) {System.err.println("unknown host"); return;}
		catch ( IOException e ) {System.err.println("I/O error joining known host"); return;}


		try 
		(		Socket pred = new Socket(IPpredecesseur, 2016);
				BufferedReader in = new BufferedReader(new InputStreamReader (pred.getInputStream()));
				PrintStream out = new PrintStream (pred.getOutputStream(), true);)
				{

			out.println("init:Hey Im new");
			if(in.readLine().equals("hash pls?")) {
				out.println(hash);	
			} else{
				System.out.println("wut ?!");
			}
				} 	
		catch(UnknownHostException e) {System.err.println("unknown host"); return;}
		catch ( IOException e ) {System.err.println("I/O error joining predecessor host"); return;}
	}


	public static void sendToSuccessor(String message) {
		try(Socket sc = new Socket(IPsuccesseur, 2016);
				PrintStream os = new PrintStream (sc.getOutputStream(), true);) {
			os.println(message);
		}
		catch (IOException e) {
			System.err.println("Sending server IO Erreur");
		}
	}
	public static void sendToPredecessor(String message) {
		try(Socket sc = new Socket(IPpredecesseur, 2016);
				PrintStream os = new PrintStream (sc.getOutputStream(), true);) {
			os.println(message);
		}
		catch (IOException e) {
			System.err.println("Sending server IO Erreur");
		}
	}
	
	
	public static void refreshFinger(String message) {
		System.out.println("Refreshing Routing Table");
		finger.clear();
		
		String[] hashIP = message.split(";");
		
		String[] allHashes = {};
		String[] allIP = {};
		for (String  string: hashIP) {
			String[] splitted = string.split("-");
			allHashes[allHashes.length]  = splitted[0];
			allIP[allIP.length] = splitted[1];
		}
		
		
		
		int max = (int) Math.log(SERVER_SIZE);
		int puissance = 0;
		for (int i = 1; i < max; i++) {
			boolean endLoop = false;
			puissance = (hash + (int)Math.pow(2, i));
			endLoop = puissance > SERVER_SIZE;
			int hashI = 0;
			int index = -1;
			for (String hashS : allHashes) {
				index++;
				hashI = Integer.valueOf(hashS);
				if(!endLoop) {
					if (hashI >= puissance) {
						
						
						if(!finger.containsKey(hashI))
							finger.put(hashI, allIP[index]);
						break;
					}
				} else {
					if(hashI < hash) {
						if (hashI + SERVER_SIZE >= puissance) {
							
							
							if(!finger.containsKey(hashI))
								finger.put(hashI, allIP[index]);
							break;
						}
					}
				}
			}
			
			for (int key : finger.keySet()) {
				System.out.print(key+" : ");
				System.out.println(finger.get(key));
			}
			
		}
	}



}
