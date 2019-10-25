package api;

import java.net.Socket;

public class ServerWorker implements Runnable{
	private Socket client;
	public ServerWorker(Socket client) {
		this.client = client;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
}
