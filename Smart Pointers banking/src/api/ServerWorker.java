package api;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import exceptions.IncorrectUserDataException;
import exceptions.SecurityException;
import utils.ClientMessage;
import utils.Encryption;
import utils.ServerResponse;

public class ServerWorker implements Runnable{
	private Socket client;
	private boolean active = true;
	public ServerWorker(Socket client) {
		this.client = client;
	}

	@SuppressWarnings("null")
	@Override
	public void run() {
		DataOutputStream out = null;
		try {
			InputStream in = client.getInputStream();
			out = new DataOutputStream(client.getOutputStream());
			boolean auth = checkAuth(client.getInetAddress());
			if (!auth) {
				Server.getConnection().beginRequest();
				Statement st = Server.getConnection().createStatement();
				st.execute("INSERT INTO ATMs (ip_address) VALUES ("+client.getInetAddress()+")");
			}
			while (active) {
				byte[] data = new byte[256];
				in.read(data);
				data = Arrays.copyOf(data, ByteBuffer.wrap(Encryption.decrypt(data)).getInt()+68);
				ClientMessage received = new ClientMessage(data,client.getInetAddress());
				Card currentCard = null;
				ServerResponse s = null;
				switch (received.getAction()) {
				case 0:
					currentCard = new Card(received.getCardNumber(),received.getPin());
					s = new ServerResponse(true,"Authorization successful.");
					break;
				case 1:
					currentCard.transfer(received.getCardNumber(),received.getAmount());
					s = new ServerResponse(true,"Transfer complete.");
					break;
				case 2:
					currentCard.changeAmount(-received.getAmount());
					s = new ServerResponse(true,"Withdrawal complete. Call our supports if you did not receive the money.");
					break;
				case 3:
					currentCard.changeAmount(received.getAmount());
					s = new ServerResponse(true,"Deposit complete. Your money will appear in your account soon.");
					break;
				default:
					throw new IncorrectUserDataException("Data sent does not follow protocol."
							+ " Check the machine's internet connection.");
				}
				s.writeToStream(out);
			}
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IncorrectUserDataException e) {
			ServerResponse f = new ServerResponse(false,e.getMessage());
			try {
				f.writeToStream(out);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private boolean checkAuth(InetAddress inetAddress) {
		return true;
	}
}
