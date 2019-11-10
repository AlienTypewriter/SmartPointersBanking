package tests;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.json.Json;
import javax.json.stream.JsonGenerator;

import org.junit.jupiter.api.Test;

import api.Server;
import exceptions.SecurityException;
import utils.ClientMessage;
import utils.Encryption;

class Message_test {

	@Test
	void MessageDeciphering() throws InvalidKeyException, NoSuchAlgorithmException, SecurityException, IOException {
		StringWriter s = new StringWriter();
		JsonGenerator gen = Json.createGenerator(s);
		gen.writeStartObject();
		gen.write("card_num","5375414107745597");
		gen.write("action",0);
		gen.writeEnd();
		gen.close();
		byte[] to_enc = s.toString().getBytes("UTF-8");
		Mac hash = Mac.getInstance("HmacSHA512");
		hash.init(new SecretKeySpec(Server.MAC_KEY.getBytes("UTF-8"),"AES"));
		Server.setMac(hash);
		byte[] check = hash.doFinal(to_enc);
		ByteBuffer wrap = ByteBuffer.allocate(to_enc.length+4+check.length);
		wrap.putInt(to_enc.length).put(to_enc).put(check);
		ClientMessage msg = new ClientMessage(Encryption.encrypt(wrap.array(),Server.MSG_KEY),
				InetAddress.getLocalHost(),Server.MSG_KEY);
		System.out.println(msg.toString());
		assert(msg.getCardNumber().contentEquals("5375414107745597"));
		assert(msg.getAction()==0);
	}

}
