package com.lichess.insights;

import java.io.File;
import java.io.IOException;

public class RunPythonScriptToConvertJsonToSql {

	public static void run(String playerName) {
		String fileName = playerName + ".json";
		String shellScript = "json_to_sql.py";

		try {
			// Build the command to run the shell script
			File outputSqlFile = new File(playerName + ".sql");
			outputSqlFile.createNewFile();

			ProcessBuilder processBuilder = new ProcessBuilder("python", shellScript, fileName);
			processBuilder.redirectErrorStream(true);  // Combine stderr and stdout
			processBuilder.redirectOutput(outputSqlFile);
			Process process = processBuilder.start();

			// Wait for the process to finish and get the exit code
			int exitCode = process.waitFor();
			System.out.println("Shell script exited with code: " + exitCode);

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

	}
}
