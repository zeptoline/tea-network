import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;


public class ClientLecture implements Runnable{

	private Socket sock;
	
	public ClientLecture(Socket sock) {
		super();
		this.sock = sock;
	}
	
	
	@Override
	public void run() {
		
		InputStream fluxEntree;
		try {
			fluxEntree = sock.getInputStream();
			BufferedReader entree = new BufferedReader(new InputStreamReader (fluxEntree));
		
			String entreeLue = "";
			while (true) {
				entreeLue = entree.readLine();
				System.out.println("Message reçu : " + entreeLue + "\n");
			}
		} catch(UnknownHostException e) {
			System.out.println(e.getMessage());
		} catch ( IOException e ) {
			System.out.println(e.getMessage());
		}
	}
}
