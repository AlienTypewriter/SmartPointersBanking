package api;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class ServerWorker implements Runnable{
	private Socket client;
	private boolean active = true;
	private boolean auth = false;
	public ServerWorker(Socket client) {
		this.client = client;
	}

	@Override
	public void run() {
		try {
		InputStream in = client.getInputStream();
		DataOutputStream out = new DataOutputStream(client.getOutputStream());
		while (active) {
			if (auth) {
				
			}
			else {
				
			}
		}
		client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
