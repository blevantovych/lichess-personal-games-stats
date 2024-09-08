package com.lichess.insights;

import java.io.File;
import java.io.IOException;

public class RunPgnToJsonAwkScript {

	public static boolean run(String playerName) {
		String shellScript = "convert_pgn_to_json.sh";

		try {
			File outputJsonFile = new File(playerName + ".json");
			outputJsonFile.createNewFile();
			// Build the command to run the shell script
			ProcessBuilder processBuilder = new ProcessBuilder("bash", shellScript, playerName);
			processBuilder.redirectErrorStream(true);  // Combine stderr and stdout
			processBuilder.redirectOutput(outputJsonFile);
			Process process = processBuilder.start();

			// Wait for the process to finish and get the exit code
			int exitCode = process.waitFor();
			System.out.println("Shell script exited with code: " + exitCode);
			return exitCode == 0;
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return false;
		}
	}
}
