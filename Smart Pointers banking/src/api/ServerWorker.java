package api;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

import utils.Encryption;

public class ServerWorker implements Runnable{
	private Socket client;
	private boolean active = true;
	public ServerWorker(Socket client) {
		this.client = client;
	}

	@Override
	public void run() {
		try {
			InputStream in = client.getInputStream();
			DataOutputStream out = new DataOutputStream(client.getOutputStream());
			boolean auth = checkAuth(client.getInetAddress());
			if (!auth) {
				//TODO implement authorization of new ATMs
			}
			while (active) {
				in.wait();
				ByteBuffer message = ByteBuffer.wrap(Encryption.decrypt(in.readAllBytes()));
				int length = message.getInt();
				byte[] infoAndCheck = new byte[message.capacity()-4];
				message.get(infoAndCheck);
				byte[] newCheck = Server.getMac().doFinal(Arrays.copyOf(message.array(), length+4));
				byte[] transCheck = Arrays.copyOfRange(message.array(), length+4, message.capacity());
				String info;
				if (Arrays.equals(newCheck,transCheck)) {
					info = new String(Arrays.copyOf(infoAndCheck, length));
				} else {
					throw new SecurityException("Invalid MAC on response from address "+client.getInetAddress());
				}
				//TODO implement actual command handling based on the info contained within the message
			}
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private boolean checkAuth(InetAddress inetAddress) {
		return true;
	}
}
