package ressources;

import java.io.IOException;

public class launchServers {

	public static void main(String[] args) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				String[] args = {"8000", "100"};
				WelcomeServer.main(args);
			}
		}).start();

		new Thread(new Runnable() {

			@Override
			public void run() {
				String[] args2 = {"8001", "100"};
				try {
					HashServer.main(args2);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();


		String[] args3 = {"localhost", "8000", "8002"};
		MonitorServer.main(args3);

	}

}
