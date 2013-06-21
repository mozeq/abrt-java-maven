package com.redhat.abrt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.Channels;
import java.util.Map.Entry;

import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;

public class ProblemDataServer {

	//TODO: read this from a config file?
	private final String abrtSocketPath = "/var/run/abrt/abrt.socket";
	private UnixSocketAddress address = null;
	UnixSocketChannel channel = null;
	OutputStreamWriter writer = null;

	public ProblemDataServer() {
		System.out.println("Opening file");
		File socketFile = new File(abrtSocketPath);
		System.out.println("File opened");
		try {
			address = new UnixSocketAddress(socketFile);
		} catch (Exception e) {
			System.out.println("Can't open socket: " + e.getMessage());
		}
		System.out.println("Opening socket");
		try {
			channel = UnixSocketChannel.open(address);
			System.out.println("Socket connected");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/* unbuffered writer */
		writer = new OutputStreamWriter(Channels.newOutputStream(channel));

		if (channel != null) {
			System.out.println("connected to " + channel.getRemoteSocketAddress());
		}
	}

	public void send(ProblemData problemData) throws IOException {
		writer.write("PUT / HTTP/1.1\r\n\r\n");
		for (Entry<String, String> item: problemData.entrySet()) {
			System.err.println("Sending: " + item.getKey() + "|" + item.getValue());

			String key = null;

			/* use just the last part of the path as key */
			int lastSeparator = item.getKey().lastIndexOf("/");
			if (lastSeparator < 0)
				lastSeparator = 0;
			key = item.getKey().toUpperCase().substring(lastSeparator);
			writer.write(String.format("%s=%s\0", key, item.getValue()));
		}
		writer.flush();
	}

	public void close() {
		try {
			channel.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	/* just for testing */
	public static void main(String[] args) {
		ProblemData pd = new ProblemDataAbrt();
		pd.add("BACKTRACE", "backtrace content");
		pd.add("TYPE", "java");
		pd.add("ANALYZER", "java");
		pd.add("PID", "12345");
		pd.add("EXECUTABLE", "/bin/eclipse");
		pd.add("REASON", "tesing java problem data");
		String filename = "/etc/hosts";
		try {
			pd.addFile(filename);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.err.println("Can't add file: " + filename);
		}
		ProblemDataServer ps = new ProblemDataServer();
		try {
			ps.send(pd);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println("Can't send data to ABRT: " + e.getMessage());
		}
	}

}
