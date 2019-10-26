package utils;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import api.Server;

public abstract class Encryption {
	public static byte[] encrypt(byte[] source) {
		try {
			Cipher c = Cipher.getInstance("AES/OFB/NoPadding");
			SecretKeySpec key = new SecretKeySpec(Server.MSG_KEY.getBytes("UTF-8"),"AES");
			c.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(new byte[16]));
			return c.doFinal(source);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static byte[] decrypt(byte[] to_decrypt) {
		try {
			Cipher c = Cipher.getInstance("AES/OFB/NoPadding");
			SecretKeySpec key = new SecretKeySpec(Server.MSG_KEY.getBytes("UTF-8"),"AES");
			c.init(Cipher.DECRYPT_MODE, key,new IvParameterSpec(new byte[16]));
			return c.doFinal(to_decrypt);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
		return null;
	}
}
