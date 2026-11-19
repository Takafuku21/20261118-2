package com.example.demo.service;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClamAvVirusScannerTests {

	@Test
	void streamsContentUsingClamAvInstreamProtocol() throws Exception {
		String content = "scan-content-".repeat(1000);
		assertEquals("zINSTREAM|" + content.length(),
				sendToScanner("stream: OK", content.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void rejectsContentFlaggedByClamAv() {
		assertThrows(VirusDetectedException.class,
				() -> sendToScanner("stream: Eicar-Test-Signature FOUND",
						"infected".getBytes(StandardCharsets.UTF_8)));
	}

	private static String sendToScanner(String response, byte[] content) throws Exception {
		try (ServerSocket server = new ServerSocket(0)) {
			ExecutorService executor = Executors.newSingleThreadExecutor();
			try {
				Future<String> request = executor.submit(() -> receiveRequest(server, response));
				ClamAvVirusScanner scanner = new ClamAvVirusScanner(
						"127.0.0.1", server.getLocalPort(), 3000);
				scanner.scan(content);
				return request.get(3, TimeUnit.SECONDS);
			} finally {
				executor.shutdownNow();
			}
		}
	}

	private static String receiveRequest(ServerSocket server, String response) throws IOException {
		try (Socket client = server.accept()) {
			client.setSoTimeout(3000);
			DataInputStream input = new DataInputStream(client.getInputStream());
			ByteArrayOutputStream command = new ByteArrayOutputStream();
			int value;
			while ((value = input.readUnsignedByte()) != 0) {
				command.write(value);
			}

			int totalBytes = 0;
			while (true) {
				int chunkLength = input.readInt();
				if (chunkLength == 0) {
					break;
				}
				byte[] chunk = input.readNBytes(chunkLength);
				if (chunk.length != chunkLength) {
					throw new IOException("Incomplete INSTREAM chunk");
				}
				totalBytes += chunk.length;
			}

			client.getOutputStream().write((response + "\0").getBytes(StandardCharsets.US_ASCII));
			client.getOutputStream().flush();
			return command.toString(StandardCharsets.US_ASCII) + "|" + totalBytes;
		}
	}
}
