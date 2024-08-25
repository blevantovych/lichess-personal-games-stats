package com.lichess.insights;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
		Game game1 = new Game();
		game1.setWhitetitle("GM");
		game1.setBlacktitle("");

		Game game2 = new Game();
		game2.setWhitetitle("");
		game2.setBlacktitle("IM");

		Game game3 = new Game();
		game3.setWhitetitle("GM");
		game3.setBlacktitle("");

		Game game4 = new Game();
		game4.setWhitetitle("");
		game4.setBlacktitle("FM");

		Game game5 = new Game();
		game5.setWhitetitle("");
		game5.setBlacktitle("");  // Game without titled player

		when(gameRepository.findAll()).thenReturn(Arrays.asList(game1, game2, game3, game4, game5));

		// Act
		Map<GameController.Title, Long> result = gameController.getTitledStats();

		// Assert
		assertNotNull(result);
		assertEquals(3, result.size());

		// Check if the map is sorted by value in descending order
		assertTrue(isSortedByValueDescending(result));

		assertEquals(2L, result.get(GameController.Title.GM));
		assertEquals(1L, result.get(GameController.Title.IM));
		assertEquals(1L, result.get(GameController.Title.FM));
		assertNull(result.get(GameController.Title.CM));  // Title not present in the games

		verify(gameRepository, times(1)).findAll();
	}

	// Helper method to check if the map is sorted by value in descending order
	private boolean isSortedByValueDescending(Map<GameController.Title, Long> map) {
		Long previous = Long.MAX_VALUE;
		for (Long value : map.values()) {
			if (value > previous) {
				return false;
			}
			previous = value;
		}
		return true;
	}
}
