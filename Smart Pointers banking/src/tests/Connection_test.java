package tests;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.RSAPublicKeySpec;
import java.sql.PreparedStatement;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.json.Json;
import javax.json.stream.JsonGenerator;

import org.junit.Ignore;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import api.Server;
import exceptions.SecurityException;
import utils.ClientMessage;
import utils.Encryption;

@TestMethodOrder(OrderAnnotation.class)
class Connection_test {
	public String spec = "e64f646f-9fc0-4e";

	@Test
	@Order(2)
	void testFirstConnection() throws IOException, InterruptedException, SecurityException, NoSuchAlgorithmException {
		String[] args = {"postgres","provided"};
		new Thread(()->{
			Socket s = new Socket();
			try {
				Thread.sleep(3000);
				Server.getConnection().beginRequest();
				String delQuery = "DELETE FROM ATMS WHERE ip_address=(?)";
				PreparedStatement st = Server.getConnection().prepareStatement(delQuery);
				st.setString(1, InetAddress.getLocalHost().toString().substring(8));
				st.execute();
				Server.getConnection().endRequest();
				s.connect(new InetSocketAddress(InetAddress.getLocalHost(),8000));
				InputStream in = s.getInputStream();
				DataOutputStream out = new DataOutputStream(s.getOutputStream());
				byte[] data = new byte[256];
				in.read(data);
				data = Arrays.copyOf(data, ByteBuffer.wrap(Encryption.decrypt(data,Server.MSG_KEY)).getInt()+68);
				ClientMessage m = new ClientMessage(data,InetAddress.getLocalHost(),Server.MSG_KEY);
				System.out.println("M1"+m);
				KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
				gen.initialize(2048);
				KeyPair keys = gen.generateKeyPair();
				RSAPublicKeySpec pks = KeyFactory.getInstance("RSA").getKeySpec(keys.getPublic(), 
						RSAPublicKeySpec.class);
				StringWriter str = new StringWriter();
				JsonGenerator jgen = Json.createGenerator(str);
				jgen.writeStartObject();
				jgen.writeStartObject("key");
				jgen.write("modulus",pks.getModulus());
				jgen.write("public_exponent",pks.getPublicExponent());
				jgen.writeEnd();
				jgen.writeEnd();
				jgen.close();
				byte[] enc_key = str.toString().getBytes("UTF-8");
				Mac hash = Server.getMac();
				byte[] check = hash.doFinal(enc_key);
				ByteBuffer wrap = ByteBuffer.allocate(enc_key.length+4+check.length);
				wrap.putInt(enc_key.length).put(enc_key).put(check);
				out.write(Encryption.encrypt(wrap.array(),Server.MSG_KEY));
				data = new byte[256];
				in.read(data);
				Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
				c.init(Cipher.DECRYPT_MODE, keys.getPrivate());
				byte[] dec_data = c.doFinal(data);
				byte[] sim_key = Arrays.copyOfRange(dec_data, 4, dec_data.length-64);
				spec = new String(sim_key);
				str = new StringWriter();
				jgen = Json.createGenerator(str);
				jgen.writeStartObject();
				jgen.write("card_num","5375414107745597");
				jgen.write("pin","1111");
				jgen.writeEnd();
				jgen.close();
				byte[] to_enc = str.toString().getBytes("UTF-8");
				check = Server.getMac().doFinal(to_enc);
				wrap = ByteBuffer.allocate(to_enc.length+4+check.length);
				wrap.putInt(to_enc.length).put(to_enc).put(check);
				out.write(Encryption.encrypt(wrap.array(),spec));
				data = new byte[256];
				in.read(data);
				data = Arrays.copyOf(data, ByteBuffer.wrap(Encryption.decrypt(data,spec)).getInt()+68);
				m = new ClientMessage(data,InetAddress.getLocalHost(),spec);
				System.out.println("M2"+m);
				s.close();
				assert(("M2{\"success\":true,\"response\":\"Authorization successful.\"}")
						.contentEquals(m.toString()));
				Server.stop();
				System.exit(0);
			}
			catch (Exception e) {
				e.printStackTrace();
			}}).start();
		Server.main(args);
	}
	
	@Test
	@Order(1)
	void testViewBalance() throws NoSuchAlgorithmException, IOException {
		String[] args = {"postgres","provided"};
		new Thread(()->{
			Socket s = new Socket();
			try {
				Thread.sleep(1000);
				s.connect(new InetSocketAddress(InetAddress.getLocalHost(),8000));
				InputStream in = s.getInputStream();
				DataOutputStream out = new DataOutputStream(s.getOutputStream());
				byte[] data = new byte[256];
				in.read(data);
				ClientMessage m = new ClientMessage(data,InetAddress.getLocalHost(),Server.MSG_KEY);
				System.out.println("M0"+m);
				StringWriter str = new StringWriter();
				JsonGenerator jgen = Json.createGenerator(str);
				jgen.writeStartObject();
				jgen.write("card_num","4111111111111111");
				jgen.write("pin","1111");
				jgen.writeEnd();
				jgen.close();
				byte[] to_enc = str.toString().getBytes("UTF-8");
				byte[] check = Server.getMac().doFinal(to_enc);
				ByteBuffer wrap = ByteBuffer.allocate(to_enc.length+4+check.length);
				wrap.putInt(to_enc.length).put(to_enc).put(check);
				out.write(Encryption.encrypt(wrap.array(),spec));
				data = new byte[256];
				in.read(data);
				data = Arrays.copyOf(data, ByteBuffer.wrap(Encryption.decrypt(data,spec)).getInt()+68);
				m = new ClientMessage(data,InetAddress.getLocalHost(),spec);
				System.out.println("M1"+m);
				assert(("{\"success\":true,\"response\":\"Authorization successful.\"}")
						.contentEquals(m.toString()));
				str = new StringWriter();
				jgen = Json.createGenerator(str);
				jgen.writeStartObject();
				jgen.write("action",0);
				//jgen.write("card_num","5375414107745597");
				//jgen.write("amount","1250");
				jgen.writeEnd();
				jgen.close();
				to_enc = str.toString().getBytes("UTF-8");
				check = Server.getMac().doFinal(to_enc);
				wrap = ByteBuffer.allocate(to_enc.length+4+check.length);
				wrap.putInt(to_enc.length).put(to_enc).put(check);
				out.write(Encryption.encrypt(wrap.array(),spec));
				data = new byte[256];
				in.read(data);
				data = Arrays.copyOf(data, ByteBuffer.wrap(Encryption.decrypt(data,spec)).getInt()+68);
				m = new ClientMessage(data,InetAddress.getLocalHost(),spec);
				System.out.println("M2"+m);
				s.close();
				System.exit(0);
			}
			catch (Exception e) {
				e.printStackTrace();
			}}).start();
		Server.main(args);
	}
}
