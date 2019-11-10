package api;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import exceptions.IncorrectUserDataException;
import exceptions.SecurityException;
import utils.ClientMessage;
import utils.Encryption;
import utils.ServerResponse;

public class ServerWorker implements Runnable{
	private Socket clientSocket;
	private boolean active = true;
	private Card currentCard = null;
	private String currentKey = null;
	public ServerWorker(Socket client) {
		clientSocket = client;
	}

	@SuppressWarnings("null")
	@Override
	public void run() {
		DataOutputStream out = null;
		try {
			InputStream in = clientSocket.getInputStream();
			out = new DataOutputStream(clientSocket.getOutputStream());
			boolean auth = checkRegistered(clientSocket.getInetAddress());
			while (!auth) {
				new ServerResponse(false,"Not authorized").writeToStream(out, Server.MSG_KEY);
				byte[] data = new byte[2116];
				in.read(data);
				data = Arrays.copyOf(data, ByteBuffer.wrap(Encryption.decrypt(data,Server.MSG_KEY)).getInt()+68);
				ClientMessage cred = new ClientMessage(data,clientSocket.getInetAddress(),Server.MSG_KEY);
				if (!(cred.getKeySpec()!=null)) {
					Cipher dec = Cipher.getInstance("RSA/ECB/PKCS1Padding");
					KeyFactory fac = KeyFactory.getInstance("RSA");
					PublicKey key = fac.generatePublic(cred.getKeySpec());
					dec.init(Cipher.ENCRYPT_MODE, key);
					SecureRandom rng = new SecureRandom();
					byte[] raw_server_key = new byte[128];
					rng.nextBytes(raw_server_key);
					byte[] enc_server_key = dec.doFinal(raw_server_key);
					currentKey = new String(raw_server_key,"UTF-8");
					Server.getConnection().beginRequest();
					Statement st = Server.getConnection().createStatement();
					st.execute("INSERT INTO ATMs (key,ip_address) VALUES ("
					+currentKey+clientSocket.getInetAddress()+")");
					Server.getConnection().endRequest();
					byte[] maccheck = Server.getMac().doFinal(enc_server_key);
					ByteBuffer wrapper = ByteBuffer.allocate(enc_server_key.length+maccheck.length+4);
					wrapper.putInt(enc_server_key.length).put(enc_server_key).put(maccheck);
					out.write(wrapper.array());
					auth = true;
				}
			}
			while (active) {
				byte[] data = new byte[256];
				in.read(data);
				data = Arrays.copyOf(data, ByteBuffer.wrap(Encryption.decrypt(data,currentKey)).getInt()+68);
				ClientMessage received = new ClientMessage(data,clientSocket.getInetAddress(),currentKey);
				ServerResponse s = null;
				if (currentCard==null) {
					currentCard = new Card(received.getCardNumber(),received.getPin());
					s = new ServerResponse(true,"Authorization successful.");					
				} else {
					switch (received.getAction()) {
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
				}
				s.writeToStream(out,currentKey);
			}
			clientSocket.close();
		} catch (IOException | SecurityException | SQLException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeySpecException | InvalidKeyException 
				| IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		} catch (IncorrectUserDataException e) {
			ServerResponse f = new ServerResponse(false,e.getMessage());
			try {
				f.writeToStream(out,currentKey);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	private boolean checkRegistered(InetAddress inetAddress) throws SQLException {
		Server.getConnection().beginRequest();
		Statement st = Server.getConnection().createStatement();
		st.execute("SELECT key FROM Cards WHERE card_number='"+inetAddress+"'");
		ResultSet res = st.getResultSet();
		String key = res.getString("key");
		if (key!=null) {
			currentKey = key;
			return true;
		}
		return false;
	}
}
