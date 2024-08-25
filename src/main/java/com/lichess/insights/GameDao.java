package com.lichess.insights;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class GameDao {

	private final JdbcTemplate jdbcTemplate;

	@Autowired
	public GameDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

//	public int saveUser(String name, String email) {
//		String sql = "INSERT INTO users (name, email) VALUES (?, ?)";
//		return jdbcTemplate.update(sql, name, email);
//	}

	public List<String> findAllGames() {
		String sql = "SELECT * FROM chess_games";
		return jdbcTemplate.query(sql, (rs, rowNum) ->
			 rs.getString(1)
		);
	}

//	public Game findGameById(int id) {
//		String sql = "SELECT * FROM games WHERE id = ?";
//		return jdbcTemplate.queryForObject(sql, new Object[]{id}, (rs, rowNum) ->
//				new Game(
//						rs.getInt("id"),
//						rs.getString("name"),
//						rs.getString("email")
//				)
//		);
//	}
//
//	public int updateUser(int id, String name, String email) {
//		String sql = "UPDATE users SET name = ?, email = ? WHERE id = ?";
//		return jdbcTemplate.update(sql, name, email, id);
//	}
//
//	public int deleteUser(int id) {
//		String sql = "DELETE FROM users WHERE id = ?";
//		return jdbcTemplate.update(sql, id);
//	}
}
