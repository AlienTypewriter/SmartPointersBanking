package tests;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.json.Json;
import javax.json.stream.JsonGenerator;

import org.junit.jupiter.api.Test;

import api.Server;
import exceptions.SecurityException;
import utils.ClientMessage;
import utils.Encryption;

class Connection_test {

	@Test
	void testConnection() throws IOException, InterruptedException, SecurityException, NoSuchAlgorithmException {
		String[] args = {"postgres","provided"};
		new Thread(()->{
			Socket s = new Socket();
			try {
				Thread.sleep(3000);
				s.connect(new InetSocketAddress(InetAddress.getLocalHost(),8000));
				InputStream in = s.getInputStream();
				DataOutputStream out = new DataOutputStream(s.getOutputStream());
				StringWriter s1 = new StringWriter();
				JsonGenerator gen = Json.createGenerator(s1);
				gen.writeStartObject();
				gen.write("card_num","5375414107745597");
				gen.write("action",0);
				gen.writeEnd();
				gen.close();
				byte[] to_enc = s1.toString().getBytes("UTF-8");
				Mac hash = Mac.getInstance("HmacSHA512");
				hash.init(new SecretKeySpec(Server.MAC_KEY.getBytes("UTF-8"),"AES"));
				Server.setMac(hash);
				byte[] check = hash.doFinal(to_enc);
				ByteBuffer wrap = ByteBuffer.allocate(to_enc.length+4+check.length);
				wrap.putInt(to_enc.length).put(to_enc).put(check);
				out.write(Encryption.encrypt(wrap.array()));
				byte[] data = new byte[256];
				in.read(data);
				data = Arrays.copyOf(data, ByteBuffer.wrap(Encryption.decrypt(data)).getInt()+68);
				ClientMessage m = new ClientMessage(data,InetAddress.getLocalHost());
				System.out.println(m.toString());
				s1.close();
				assert(("{\"success\":false,"
						+ "\"response\":\"This card is deactivated or does not belong to this bank\"}")
						.contentEquals(m.toString()));
				System.exit(0);
			}
			catch (Exception e) {
				e.printStackTrace();
			}}).start();
		Server.main(args);
	}

}
