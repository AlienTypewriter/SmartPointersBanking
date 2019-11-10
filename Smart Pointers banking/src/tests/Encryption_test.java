package tests;

import static org.junit.Assume.assumeFalse;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import api.Server;
import utils.Encryption;

class Encryption_test {

	@Test
	void testDeencryption() throws UnsupportedEncodingException {
		byte[] abc123 = Encryption.encrypt("abc123".getBytes("UTF-8"),Server.MSG_KEY);
		byte[] decrypted = Encryption.decrypt(abc123,Server.MSG_KEY);
		byte[] original = "abc123".getBytes("UTF-8");
		assert(Arrays.equals(decrypted, original));
	}
	
	@Test
	void testFalseDeencryption() throws UnsupportedEncodingException {
		byte[] abc123 = Encryption.encrypt("abc123".getBytes("UTF-8"),Server.MSG_KEY);
		byte[] decrypted = Encryption.decrypt(abc123,Server.MSG_KEY);
		byte[] not_original = "abc1234".getBytes("UTF-8");
		assumeFalse(Arrays.equals(decrypted, not_original));
	}
	
	@RepeatedTest(10)
	void testMessagingSpeedwithMac() throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
		Mac hash = Mac.getInstance("HmacSHA512");
		hash.init(new SecretKeySpec(Server.MAC_KEY.getBytes(),"AES"));
		byte[] original = "abc123".getBytes("UTF-8");
		ByteBuffer bb = ByteBuffer.allocate(original.length+4).putInt(original.length).put(original);
		byte[] maccheck = hash.doFinal(bb.array());
		bb = ByteBuffer.allocate(original.length+4+maccheck.length).put(bb.array()).put(maccheck);
		byte[] encrypted = Encryption.encrypt(bb.array(),Server.MSG_KEY);
		ByteBuffer bb1 = ByteBuffer.wrap(Encryption.decrypt(encrypted,Server.MSG_KEY));
		int og_length = bb1.getInt();
		assert(og_length==original.length);
		byte[] retr = new byte[og_length];
		bb1.get(retr, 0, og_length);
		byte[] transcheck = Arrays.copyOfRange(bb1.array(), 10, bb1.array().length);
		assert(Arrays.equals(transcheck,maccheck));
	}
	
	@RepeatedTest(10)
	void testMessagingSpeedwithMD() throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
		MessageDigest hash = MessageDigest.getInstance("SHA-512");
		byte[] original = "abc123".getBytes("UTF-8");
		ByteBuffer bb = ByteBuffer.allocate(original.length+4).putInt(original.length).put(original);
		byte[] maccheck = hash.digest(bb.array());
		bb = ByteBuffer.allocate(original.length+4+maccheck.length).put(bb.array()).put(maccheck);
		byte[] encrypted = Encryption.encrypt(bb.array(),Server.MSG_KEY);
		ByteBuffer bb1 = ByteBuffer.wrap(Encryption.decrypt(encrypted,Server.MSG_KEY));
		int og_length = bb1.getInt();
		assert(og_length==original.length);
		byte[] retr = new byte[og_length];
		bb1.get(retr, 0, og_length);
		byte[] transcheck = Arrays.copyOfRange(bb1.array(), 10, bb1.array().length);
		assert(Arrays.equals(transcheck,maccheck));
	}

}
