package utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import javax.json.Json;
import javax.json.stream.JsonGenerator;

import api.Server;

public class ServerResponse {
	private String json;
	
	//create a message by writing down a server response
	public ServerResponse(boolean success, String response) {
		StringWriter s = new StringWriter();
		JsonGenerator gen = Json.createGenerator(s);
		gen.writeStartObject();
		gen.write("success", success);
		gen.write("response",response);
		gen.writeEnd();
		gen.close();
		json = s.toString();
	}
	
	//encode the contents of the message and write them into a stream
	public void writeToStream(OutputStream out,String spec) throws IOException {
		byte[] data = null;
		try {
			data = json.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		byte[] maccheck = Server.getMac().doFinal(data);
		ByteBuffer wrapper = ByteBuffer.allocate(data.length+maccheck.length+4);
		wrapper.putInt(data.length).put(data).put(maccheck);
		out.write(Encryption.encrypt(wrapper.array(),spec));
	}
	
	@Override
	public String toString() {
		return json;
	}
	
}
