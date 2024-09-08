package com.lichess.insights;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class FileDownloadWithProgress {
	public static boolean downloadFileWithProgress(String playerName) throws IOException, InterruptedException {
		String savePath = playerName + ".pgn"; // Path to save the file
		String fileURL = String.format("https://lichess.org/api/games/user/%s?tags=true&clocks=true&evals=true&opening=true", playerName);
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(fileURL))
				.build();

		// Send request and receive response as InputStream
		HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

		// Check if the request was successful
		if (response.statusCode() == 200) {
			// Get the file size from the headers
			long fileSize = response.headers()
					.firstValueAsLong("Content-Length")
					.orElse(-1L); // Handle cases where the file size is unknown

			InputStream inputStream = response.body();
			FileOutputStream outputStream = new FileOutputStream(savePath);

			// Buffer for reading data
			byte[] buffer = new byte[4096];
			int bytesRead = -1;

			// File size in KB
			long fileSizeInKB = fileSize / 1024;

			System.out.println("File size: " + fileSizeInKB + " KB");

			// Loop to read the file
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
				System.out.println("Read: " + bytesRead + " bytes");
			}

			// Close streams
			outputStream.close();
			inputStream.close();

			System.out.println("\nDownload completed.");
			return true;
		} else {
			System.out.println("No file to download. Server replied with code: " + response.statusCode());
			return false;
		}
	}
}
