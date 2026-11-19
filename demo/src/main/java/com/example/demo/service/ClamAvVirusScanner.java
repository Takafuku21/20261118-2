package com.example.demo.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClamAvVirusScanner implements VirusScanner {

	private static final int CHUNK_SIZE = 8192;
	private static final int MAX_RESPONSE_SIZE = 4096;

	private final String host;
	private final int port;
	private final int timeoutMillis;

	public ClamAvVirusScanner(
			@Value("${app.virus-scanner.clamav.host}") String host,
			@Value("${app.virus-scanner.clamav.port}") int port,
			@Value("${app.virus-scanner.clamav.timeout-millis}") int timeoutMillis) {
		this.host = host;
		this.port = port;
		this.timeoutMillis = timeoutMillis;
	}

	@Override
	public void scan(byte[] content) throws IOException {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(host, port), timeoutMillis);
			socket.setSoTimeout(timeoutMillis);
			OutputStream output = socket.getOutputStream();
			output.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));
			for (int offset = 0; offset < content.length; offset += CHUNK_SIZE) {
				int length = Math.min(CHUNK_SIZE, content.length - offset);
				output.write(ByteBuffer.allocate(Integer.BYTES).putInt(length).array());
				output.write(content, offset, length);
			}
			output.write(new byte[Integer.BYTES]);
			output.flush();

			String response = readResponse(socket.getInputStream());
			if (response.endsWith(" FOUND")) {
				throw new VirusDetectedException();
			}
			if (!response.endsWith(" OK")) {
				throw new IOException("ClamAV returned an unrecognized scan response");
			}
		}
	}

	private static String readResponse(InputStream input) throws IOException {
		ByteArrayOutputStream response = new ByteArrayOutputStream();
		int value;
		while ((value = input.read()) != -1 && value != 0 && value != '\n') {
			if (response.size() >= MAX_RESPONSE_SIZE) {
				throw new IOException("ClamAV response exceeded the size limit");
			}
			response.write(value);
		}
		if (response.size() == 0) {
			throw new IOException("ClamAV returned an empty scan response");
		}
		return response.toString(StandardCharsets.US_ASCII).trim();
	}
}
