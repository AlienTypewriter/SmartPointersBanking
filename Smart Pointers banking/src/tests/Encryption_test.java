package tests;

import static org.junit.Assume.assumeFalse;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
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
	void testPEncryption() throws GeneralSecurityException {
		KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
		KeyPair keys = keygen.generateKeyPair();
		RSAPublicKeySpec pks = KeyFactory.getInstance("RSA").getKeySpec(keys.getPublic(), 
				RSAPublicKeySpec.class);
		KeyFactory fac = KeyFactory.getInstance("RSA");
		PublicKey key = fac.generatePublic(new RSAPublicKeySpec(pks.getModulus(),pks.getPublicExponent()));
		byte[] abc123 = "abc123".getBytes();
		Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		c.init(Cipher.ENCRYPT_MODE, key);
		byte[] enc = c.doFinal(abc123);
		c.init(Cipher.DECRYPT_MODE, keys.getPrivate());
		assert(Arrays.equals(abc123,c.doFinal(enc)));
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
