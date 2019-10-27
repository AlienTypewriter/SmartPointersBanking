package utils;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.json.Json;
import javax.json.JsonObject;

import api.Server;
import exceptions.SecurityException;

public class ClientMessage {
	private String json;
	private byte action = 0;
	private String pin = "";
	private double amount = -1;
	private String card_num = "";
	
	//create a message by decrypting an incoming message
	public ClientMessage(byte[] inc_message, InetAddress address) throws SecurityException {
		ByteBuffer wrapper = ByteBuffer.wrap(Encryption.decrypt(inc_message));
		byte[] src = new byte[wrapper.getInt()];
		byte[] newCheck = Server.getMac().doFinal(src);
		byte[] transCheck = new byte[wrapper.capacity()-src.length-4];
		wrapper.get(src).get(transCheck);
		String json = "";
		if (Arrays.equals(newCheck,transCheck)) {
			try {
				json = new String(src,"UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		} else {
			throw new SecurityException("Invalid MAC on response from address "+address);
		}
		JsonObject obj = Json.createReader(new StringReader(json)).readObject();
		if (obj.containsKey("amount")) {
			amount = obj.getJsonNumber("amount").doubleValue();		
		}
		if (obj.containsKey("card_num")) {
			card_num = obj.getString("card_num");	
		}
		if (obj.containsKey("pin")) {
			pin = obj.getString("pin");		
		}
		action = (byte) obj.getInt("action");
	}
	
	public byte getAction() {
		return action;
	}
	
	public double getAmount() {
		return amount;
	}
	
	public String getCardNumber() {
		return card_num;
	}
	
	public String getPin() {
		return pin;
	}
	
	@Override
	public String toString() {
		return json;
	}
}