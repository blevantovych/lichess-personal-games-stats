package com.lichess.insights;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RunPythonScriptToConvertJsonToSql {

	public static void main(String[] args) {
		String fileName = "Openingmastery96.json";
		String shellScript = "json_to_sql.py";

		try {
			// Build the command to run the shell script
//			python3 json_to_sql.py lichess_Protagonist98_2024-08-31.json
			ProcessBuilder processBuilder = new ProcessBuilder("python", shellScript, fileName);

			processBuilder.redirectErrorStream(true);  // Combine stderr and stdout
			Process process = processBuilder.start();

			// Read output from the process (stdout)
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);  // Print the output of the shell script
			}

			// Wait for the process to finish and get the exit code
			int exitCode = process.waitFor();
			System.out.println("Shell script exited with code: " + exitCode);

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

	}
}
