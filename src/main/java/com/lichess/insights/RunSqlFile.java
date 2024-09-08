package com.lichess.insights;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class RunSqlFile {

	// Database credentials
	static final String JDBC_URL = "jdbc:postgresql://localhost:5433/chess_games";
	// TODO: get these from properties file
	static final String USER = "postgres";
	static final String PASS = "mysecretpassword";

	public static void run(String playerName) {
		Connection connection = null;
		Statement statement = null;

		try {
			// Load PostgreSQL JDBC Driver
			Class.forName("org.postgresql.Driver");

			// Connect to the PostgreSQL database
			connection = DriverManager.getConnection(JDBC_URL, USER, PASS);

			// Create a statement
			statement = connection.createStatement();

			// Read the SQL file
			String sqlFilePath = playerName + ".sql";  // Full path to the SQL file
			String sql = readSqlFile(sqlFilePath);

			// Execute SQL statements
			statement.execute(sql);

			System.out.println("SQL file executed successfully.");

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (statement != null) statement.close();
				if (connection != null) connection.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	// Function to read the contents of the SQL file
	private static String readSqlFile(String filePath) throws IOException {
		StringBuilder sqlBuilder = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				sqlBuilder.append(line).append("\n");
			}
		}
		return sqlBuilder.toString();
	}
}
