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
import java.util.Arrays;

import javax.crypto.Cipher;
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
				Thread.sleep(200);
				byte[] data = new byte[256];
				in.read(data);
				data = Arrays.copyOf(data, ByteBuffer.wrap(Encryption.decrypt(data,Server.MSG_KEY)).getInt()+68);
				ClientMessage m = new ClientMessage(data,InetAddress.getLocalHost(),Server.MSG_KEY);
				System.out.println(m);
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
				jgen.close();
				byte[] enc_key = Encryption.encrypt(str.toString().getBytes("UTF-8"), Server.MSG_KEY);
				Mac hash = Mac.getInstance("HmacSHA512");
				hash.init(new SecretKeySpec(Server.MAC_KEY.getBytes("UTF-8"),"AES"));
				Server.setMac(hash);
				byte[] check = hash.doFinal(enc_key);
				ByteBuffer wrap = ByteBuffer.allocate(enc_key.length+4+check.length);
				wrap.putInt(enc_key.length).put(enc_key).put(check);
				out.write(Encryption.encrypt(wrap.array(),Server.MSG_KEY));
				data = new byte[256];
				in.read(data);
				Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
				c.init(Cipher.DECRYPT_MODE, keys.getPrivate());
				byte[] dec_data = c.doFinal(data);
				dec_data = Arrays.copyOfRange(dec_data, 0, ByteBuffer.wrap(dec_data).getInt()+68);
				byte[] sim_key = Arrays.copyOfRange(dec_data, 4, dec_data.length-64);
				String spec = new String(sim_key,"UTF-8");
				str = new StringWriter();
				jgen = Json.createGenerator(str);
				jgen.writeStartObject();
				jgen.write("card_num","5375414107745597");
				jgen.write("action",0);
				jgen.writeEnd();
				jgen.close();
				byte[] to_enc = str.toString().getBytes("UTF-8");
				check = hash.doFinal(to_enc);
				wrap = ByteBuffer.allocate(to_enc.length+4+check.length);
				wrap.putInt(to_enc.length).put(to_enc).put(check);
				out.write(Encryption.encrypt(wrap.array(),spec));
				data = new byte[256];
				in.read(data);
				data = Arrays.copyOf(data, ByteBuffer.wrap(Encryption.decrypt(data,spec)).getInt()+68);
				m = new ClientMessage(data,InetAddress.getLocalHost(),spec);
				System.out.println(m);
				s.close();
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
