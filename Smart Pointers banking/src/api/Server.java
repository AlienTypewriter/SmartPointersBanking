package api;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Server {
	private static Connection con;
	private static boolean keepConnections;
	private static ServerSocket server;
	public static void main(String[] args) {
		if (args.length<2) {
			System.out.println("This is a server application for the Smart Pointers banking project.\n"
					+ "Enter database credentials to proceed");
		}
		Properties dbsprops = new Properties();
		dbsprops.setProperty("user", args[0]);
		dbsprops.setProperty("password", args[1]);
		dbsprops.setProperty("ssl", "true");
		try {
			con = DriverManager.getConnection("jdbc:postgresql://localhost/banking", dbsprops);
			System.out.println("Connected to database. Establishing server");
			server = new ServerSocket();
			BlockingQueue<Runnable> workers = new SynchronousQueue<Runnable>();
			ThreadPoolExecutor exec = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
					Integer.MAX_VALUE, 5, TimeUnit.SECONDS, workers);
			while (keepConnections) {
				Socket client = server.accept();
				exec.execute(new ServerWorker(client));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("Couldn't establish a connection with the database. Check your credentials.");
			return;
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Server socket error");
		}
	}
	
	public static Connection getConnection() {
		return con;
	}
}
