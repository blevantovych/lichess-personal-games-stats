package com.lichess.insights;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class GameControllerTest {

	@Mock
	private GameRepository gameRepository;

	@InjectMocks
	private GameController gameController;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testGetCalendar() throws ParseException {
		// Arrange
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Game game1 = new Game();
		game1.setDate(dateFormat.parse("2024-08-01"));
		Game game2 = new Game();
		game2.setDate(dateFormat.parse("2024-08-01"));
		Game game3 = new Game();
		game3.setDate(dateFormat.parse("2024-08-02"));

		when(gameRepository.findAll()).thenReturn(Arrays.asList(game1, game2, game3));

		// Act
		List<GameController.DataPoint> result = gameController.getCalendar();

		// Assert
		assertNotNull(result);
		assertEquals(2, result.size());

		GameController.DataPoint dataPoint1 = result.get(0);
		assertEquals("2024-08-01", dataPoint1.x());
		assertEquals(2, dataPoint1.value());
		assertTrue(dataPoint1.link().contains("2024-08-01"));
		assertTrue(dataPoint1.link().contains("2024-08-02"));

		GameController.DataPoint dataPoint2 = result.get(1);
		assertEquals("2024-08-02", dataPoint2.x());
		assertEquals(1, dataPoint2.value());
		assertTrue(dataPoint2.link().contains("2024-08-02"));
		assertTrue(dataPoint2.link().contains("2024-08-03"));

		verify(gameRepository, times(1)).findAll();
	}

	@Test
	void testGetRecord() {
		// Arrange
		Game game1 = new Game();
		game1.setWhite(GameController.PLAYER_NAME);
		game1.setBlack("Magnus Carlsen");
		game1.setResult("1-0");

		Game game2 = new Game();
		game2.setWhite(GameController.PLAYER_NAME);
		game2.setBlack("Fabiano Caruana");
		game2.setResult("0-1");

		Game game3 = new Game();
		game3.setWhite("Hikaru Nakamura");
		game3.setBlack(GameController.PLAYER_NAME);
		game3.setResult("1-0");

		Game game4 = new Game();
		game4.setWhite("Maxime Vachier-Lagrave");
		game4.setBlack(GameController.PLAYER_NAME);
		game4.setResult("0-1");

		when(gameRepository.findAll()).thenReturn(Arrays.asList(game1, game2, game3, game4));

		// Act
		GameController.GamesStats result = gameController.getRecord();

		// Assert
		assertNotNull(result);
		assertEquals(2, result.gamesWithWhite());
		assertEquals(1, result.lostWithWhite());
		assertEquals(0.5, result.whiteLostPercentage(), 0.001);
		assertEquals(2, result.gamesWithBlack());
		assertEquals(1, result.lostWithBlack());
		assertEquals(0.5, result.blackLostPercentage(), 0.001);

		verify(gameRepository, times(1)).findAll();
	}

	@Test
	void testGetCalendarExactLink() throws ParseException {
		// Arrange
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Game game1 = new Game();
		game1.setDate(dateFormat.parse("2024-08-15"));
		Game game2 = new Game();
		game2.setDate(dateFormat.parse("2024-08-15"));
		Game game3 = new Game();
		game3.setDate(dateFormat.parse("2024-08-16"));

		when(gameRepository.findAll()).thenReturn(Arrays.asList(game1, game2, game3));

		// Act
		List<GameController.DataPoint> result = gameController.getCalendar();

		// Assert
		assertNotNull(result);
		assertEquals(2, result.size());

		GameController.DataPoint dataPoint1 = result.get(0);
		assertEquals("2024-08-15", dataPoint1.x());
		assertEquals(2, dataPoint1.value());
		assertEquals("https://lichess.org/games/search?players.a=bodya17&dateMin=2024-08-15&dateMax=2024-08-16&sort.field=d&sort.order=desc#results", dataPoint1.link());

		GameController.DataPoint dataPoint2 = result.get(1);
		assertEquals("2024-08-16", dataPoint2.x());
		assertEquals(1, dataPoint2.value());
		assertEquals("https://lichess.org/games/search?players.a=bodya17&dateMin=2024-08-16&dateMax=2024-08-17&sort.field=d&sort.order=desc#results", dataPoint2.link());

		verify(gameRepository, times(1)).findAll();
	}

	@Test
	void testGetTitledStats() {
		// Arrange
		Game game1 = createGame("GM", "", "1-0", "https://lichess.org/game1", GameController.PLAYER_NAME, "Opponent1");
		Game game2 = createGame("", "IM", "0-1", "https://lichess.org/game2", GameController.PLAYER_NAME, "Opponent2");
		Game game3 = createGame("GM", "", "1/2-1/2", "https://lichess.org/game3", "Opponent3", GameController.PLAYER_NAME);
		Game game4 = createGame("", "FM", "1-0", "https://lichess.org/game4", GameController.PLAYER_NAME, "Opponent4");
		Game game5 = createGame("", "", "1-0", "https://lichess.org/game5", GameController.PLAYER_NAME, "Opponent5");  // Game without titled player

		when(gameRepository.findAll()).thenReturn(Arrays.asList(game1, game2, game3, game4, game5));

		// Act
		Map<GameController.Title, Map<String, Object>> result = gameController.calculateTitledStats();

		// Assert
		assertNotNull(result);
		assertEquals(3, result.size());

		// Check GM stats
		assertTitleStats(result, GameController.Title.GM, 2, 1, 1, 0,
				Arrays.asList("https://lichess.org/game1"),
				Arrays.asList("https://lichess.org/game3"),
				Collections.emptyList());

		// Check IM stats
		assertTitleStats(result, GameController.Title.IM, 1, 0, 0, 1,
				Collections.emptyList(),
				Collections.emptyList(),
				Arrays.asList("https://lichess.org/game2"));

		// Check FM stats
		assertTitleStats(result, GameController.Title.FM, 1, 1, 0, 0,
				Arrays.asList("https://lichess.org/game4"),
				Collections.emptyList(),
				Collections.emptyList());

		verify(gameRepository, times(1)).findAll();
	}

	private void assertTitleStats(Map<GameController.Title, Map<String, Object>> result,
								  GameController.Title title,
								  int expectedTotal, int expectedWins, int expectedDraws, int expectedLosses,
								  List<String> expectedWonGames, List<String> expectedDrawnGames, List<String> expectedLostGames) {
		Map<String, Object> stats = result.get(title);
		assertNotNull(stats, "Stats for " + title + " should not be null");
		assertEquals(expectedTotal, stats.get("total"), "Incorrect total for " + title);
		assertEquals(expectedWins, stats.get("wins"), "Incorrect wins for " + title);
		assertEquals(expectedDraws, stats.get("draws"), "Incorrect draws for " + title);
		assertEquals(expectedLosses, stats.get("loses"), "Incorrect losses for " + title);
		assertEquals(expectedWins - expectedLosses, stats.get("diff"), "Incorrect diff for " + title);
		assertListEquals(expectedWonGames, (List<?>) stats.get("wonGames"), "Incorrect won games for " + title);
		assertListEquals(expectedDrawnGames, (List<?>) stats.get("drawnGames"), "Incorrect drawn games for " + title);
		assertListEquals(expectedLostGames, (List<?>) stats.get("lostGames"), "Incorrect lost games for " + title);
	}

	private void assertListEquals(List<?> expected, List<?> actual, String message) {
		if (expected.isEmpty()) {
			assertTrue(actual == null || actual.isEmpty(), message + " (expected empty or null list)");
		} else {
			assertEquals(expected, actual, message);
		}
	}

	private Game createGame(String whiteTitle, String blackTitle, String result, String site, String white, String black) {
		Game game = new Game();
		game.setWhitetitle(whiteTitle);
		game.setBlacktitle(blackTitle);
		game.setResult(result);
		game.setSite(site);
		game.setWhite(white);
		game.setBlack(black);
		return game;
	}

}
