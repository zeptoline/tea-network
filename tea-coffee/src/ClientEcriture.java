import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;


public class ClientEcriture {

	public static void main(String[] args) {

		//Faire un hashmap pour la finger table
		//+ variable pour connaître son hash
		//+ variable pour connaître son prédécesseur

		String hash = "";
		String IPsuccesseur = "";
		String IPpredecesseur = "";

		String ip = "";
		try (Socket hashSocket = new Socket("localhost", 8001);
				BufferedReader hashEntree = new BufferedReader(new InputStreamReader (hashSocket.getInputStream()));
				PrintWriter hashSortie = new PrintWriter (hashSocket.getOutputStream(), true);) {
			ip = InetAddress.getLocalHost().getHostAddress();
			hashSortie.println(ip);
			hash = hashEntree.readLine();
		} catch(UnknownHostException e) {
			System.out.println(e.getMessage());
		} catch ( IOException e ) {
			System.out.println(e.getMessage());
		}


		Socket WSsock = null;
		boolean estPremier = false;
		try {
			WSsock = new Socket("localhost" , 8000);

			InputStream WSfluxEntree = WSsock.getInputStream();
			OutputStream WSfluxSortie = WSsock.getOutputStream ( );

			BufferedReader entree = new BufferedReader(new InputStreamReader (WSfluxEntree));
			PrintWriter sortie = new PrintWriter (WSfluxSortie, true);

			String connec = "yo:" + hash + ":" + ip;
			sortie.println(connec);

			String entreeLue = entree.readLine();
			if(entreeLue.equals("yaf")) {
				System.out.println("Vous êtes le premier client sur le réseau");
				IPsuccesseur = hash;
				IPpredecesseur = hash;
				estPremier = true;
			}
			else if(!entreeLue.equals("wrq")) {
				System.out.println("votre successeur a l'addresse " + entreeLue);
				IPsuccesseur = entreeLue;
			}
			else
				System.out.println("La connection est un échec");

		} catch(UnknownHostException e) {
			System.out.println(e.getMessage());
		} catch ( IOException e ) {
			System.out.println(e.getMessage());
		}

	}


	//Le client lecteur va écouter tout le monde sur un port prédéfinis commun
	//(par exemple le port 2016)
	//Il devra différencier les messages du successeur
	//Et les messages des nouveaux arrivants
/*
	Thread myThreadLecture = new Thread(new ClientLecture(WSsock));
	myThreadLecture.start();

	Scanner sc = new Scanner(System.in);
	String str = "";
	while (true) {
		System.out.println("Message a envoyer : ");
		str = sc.nextLine();
		sortie.println(str);
	}
*/



}
