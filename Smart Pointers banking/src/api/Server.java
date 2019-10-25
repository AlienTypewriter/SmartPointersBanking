package api;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class Server {
	private static Connection con;
	private static boolean keepConnections = true;
	private static Mac hash;
	private static ServerSocket server;
	public static void main(String[] args) throws NoSuchAlgorithmException {
		if (args.length<2) {
			System.out.println("This is a server application for the Smart Pointers banking project.\n"
					+ "Enter database credentials to proceed");
			return;
		}
		Properties dbsprops = new Properties();
		dbsprops.setProperty("user", args[0]);
		dbsprops.setProperty("password", args[1]);
		dbsprops.setProperty("ssl", "false");
		try {
			con = DriverManager.getConnection("jdbc:postgresql://localhost/banking", dbsprops);
			System.out.println("Connected to database. Establishing server");
			SecretKeySpec keyfac = new SecretKeySpec("87h1287fhe8f7y01hcy1264dc184e6a4c6824e".getBytes(), "AES");
			hash = Mac.getInstance("HmacSHA512");
			hash.init(keyfac);
			server = new ServerSocket(8000);
			System.out.println("Server established. Awaiting connections.");
			BlockingQueue<Runnable> workers = new SynchronousQueue<Runnable>();
			ThreadPoolExecutor exec = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
					Integer.MAX_VALUE, 5, TimeUnit.SECONDS, workers);
			while (keepConnections) {
				Socket client = server.accept();
				exec.execute(new ServerWorker(client));
			}
		} catch (SQLException e) {
			System.out.println("Couldn't establish a connection with the database. Check your credentials.");
			e.printStackTrace();
			return;
		} catch (IOException e) {
			System.out.println("Server socket error");
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}
	}
	
	public static Connection getConnection() {
		return con;
	}
	
	public static Mac getMac() {
		return hash;
	}
}
