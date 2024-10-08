package com.lichess.insights;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Controller
@RequestMapping()
public class GameController {
	private final GameRepository gameRepository;

	public GameController(GameRepository gameRepository) {
		this.gameRepository = gameRepository;
	}

	public static String getNextDate(String curDate) throws ParseException {
		final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		final Date date = format.parse(curDate);
		final Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		return format.format(calendar.getTime());
	}

	private static boolean checkIfOpponentIsTitled(Game game, String title, String playerName) {
		return (game.getWhitetitle().equalsIgnoreCase(title) && !game.getWhite().equals(playerName)) || (game.getBlacktitle().equalsIgnoreCase(title) && !game.getBlack().equals(playerName));
	}

	@GetMapping(path = "/ingest/{playerName}")
	public @ResponseBody boolean ingest(@PathVariable String playerName) throws IOException, InterruptedException {
		try {
			FileDownloadWithProgress.downloadFileWithProgress(playerName);
			RunPgnToJsonAwkScript.run(playerName);
			RunPythonScriptToConvertJsonToSql.run(playerName);
			RunSqlFile.run(playerName);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			cleanupTemporaryFiles(playerName);
		}
	}

	private void cleanupTemporaryFiles(String playerName) {
		for (var ext : List.of(".pgn", ".json", ".sql")) {
			String fileName = playerName + ext;
			Path path = Paths.get(fileName);

			try {
				Files.delete(path);
				System.out.printf("File %s deleted successfully%n", fileName);
			} catch (IOException e) {
				System.out.printf("Failed to delete the %s file", fileName);
				e.printStackTrace();
			}
		}
	}

	@GetMapping(path = "/home/{playerName}")
	public String home(@PathVariable String playerName, Model model) {
		return "home";
	}

	@GetMapping(path = "/{playerName}")
	public String calendar(@PathVariable String playerName, Model model) {
		Iterable<Game> games = gameRepository.findGamesByBlackOrWhite(playerName, playerName);
		model.addAttribute("playerName", playerName);

		if (!games.iterator().hasNext()) {
			return "not-found";
		}

		Calendar calendar = Calendar.getInstance();
		// games are sorted in descending order
		Game firstGame = StreamSupport.stream(games.spliterator(), false).min(Comparator.comparing(Game::getDate)).orElse(null);
		Game lastGame = StreamSupport.stream(games.spliterator(), false).max(Comparator.comparing(Game::getDate)).orElse(null);
		Date firstGameDate = firstGame.getDate();
		Date lastGameDate = lastGame.getDate();
		calendar.setTime(firstGameDate);

		model.addAttribute("startYear", calendar.get(Calendar.YEAR));
		calendar.setTime(lastGameDate);
		model.addAttribute("endYear", calendar.get(Calendar.YEAR));
		return "calendar";
	}

	@GetMapping(path = "/wins/{playerName}/{title}")
	public String getWinsWith(@PathVariable String title, @PathVariable String playerName, Model model) {
		List<String> games = StreamSupport.stream(gameRepository.findGamesByBlackOrWhite(playerName, playerName).spliterator(), false)
				.filter(game -> checkIfOpponentIsTitled(game, title, playerName))
				.filter(game -> wonWithColor(playerName, game, true) || wonWithColor(playerName, game, false))
				.map(Game::getSite)
				.collect(Collectors.toList());
		model.addAttribute("games", games);
		return "game-list";
	}

	@GetMapping(path = "/loses/{playerName}/{title}")
	public @ResponseBody Iterable<String> getLosesWith(@PathVariable String title, @PathVariable String playerName) {
		Iterable<Game> games = gameRepository.findGamesByBlackOrWhite(playerName, playerName);
		return StreamSupport.stream(games.spliterator(), false)
				.filter(game -> checkIfOpponentIsTitled(game, title, playerName))
				.filter(game -> lostWithColor(playerName, game, true) || lostWithColor(playerName, game, false))
				.map(Game::getSite)
				.collect(Collectors.toList());
	}

	@GetMapping(path = "/draws/{playerName}/{title}")
	public @ResponseBody Iterable<String> getDrawsWith(@PathVariable String title, @PathVariable String playerName) {
		Iterable<Game> games = gameRepository.findGamesByBlackOrWhite(playerName, playerName);

		return StreamSupport.stream(games.spliterator(), false)
				.filter(game -> checkIfOpponentIsTitled(game, title, playerName) && game.getResult().equals("1/2-1/2"))
				.map(Game::getSite)
				.collect(Collectors.toList());
	}

	@GetMapping(path = "/win-percentages/{playerName}")
	public @ResponseBody GamesStats getRecord(@PathVariable String playerName) {
		Iterable<Game> games = gameRepository.findGamesByBlackOrWhite(playerName, playerName);

		long gamesWithWhite = StreamSupport.stream(games.spliterator(), false).filter(game -> game.getWhite().equals(playerName)).count();
		long lostWithWhite = StreamSupport.stream(games.spliterator(), false).filter(game -> game.getWhite().equals(playerName) && game.getResult().equals("0-1")).count();
		double whiteLostPercentage = (double) lostWithWhite / gamesWithWhite;

		long gamesWithBlack = StreamSupport.stream(games.spliterator(), false).filter(game -> game.getBlack().equals(playerName)).count();
		long lostWithBlack = StreamSupport.stream(games.spliterator(), false).filter(game -> game.getBlack().equals(playerName) && game.getResult().equals("1-0")).count();
		double blackLostPercentage = (double) lostWithBlack / gamesWithBlack;
		return new GamesStats(gamesWithWhite, lostWithWhite, whiteLostPercentage, gamesWithBlack, lostWithBlack, blackLostPercentage);
	}

	@GetMapping(path = "/games-per-year/{playerName}")
	public @ResponseBody Map<Integer, Long> getGamesPerYear(@PathVariable String playerName) {
		Iterable<Game> games = gameRepository.findGamesByBlackOrWhite(playerName, playerName);
		return StreamSupport.stream(games.spliterator(), false)
				.collect(Collectors.groupingBy(game -> {
					final Calendar calendar = Calendar.getInstance();
					calendar.setTime(game.getDate());
					return calendar.get(Calendar.YEAR);
				}, Collectors.counting()));
	}

	@GetMapping(path = "/popular-openings/{playerName}")
	public String getPopularOpenings(@PathVariable String playerName, Model model) {
		Iterable<Game> games = gameRepository.findGamesByBlackOrWhite(playerName, playerName);
		var popularOpenings = StreamSupport.stream(games.spliterator(), false)
				.collect(Collectors.groupingBy(Game::getOpening, Collectors.counting()))
				.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						Map.Entry::getValue,
						(oldValue, newValue) -> oldValue,
						LinkedHashMap::new
				));
		model.addAttribute("openings", popularOpenings);
		return "popular-openings";
	}

	@GetMapping(path = "/opening/{playerName}/{opening}")
	public String getGamesInOpening(Model model, @PathVariable String playerName, @PathVariable String opening) {
		List<String> games = StreamSupport.stream(gameRepository.findGamesByBlackOrWhite(playerName, playerName).spliterator(), false)
				.filter(game -> (game.getWhite().equals(playerName) || game.getBlack().equals(playerName)) && game.getOpening().equalsIgnoreCase(opening))
				.map(Game::getSite)
				.collect(Collectors.toList());
		model.addAttribute("games", games);
		return "game-list";
	}

	@GetMapping(path = "/stats-with-titled-players/{playerName}")
	public String getTitledStats(@PathVariable String playerName, Model model) {
		Map<Title, Map<String, Object>> stats = calculateTitledStats(playerName);
		model.addAttribute("titledStats", stats);
		model.addAttribute("playerName", playerName);
		return "titled-stats";  // This will be the name of your Thymeleaf template
	}

	@GetMapping(path = "/players")
	public @ResponseBody Map<String, Long> getPlayers() {
		return gameRepository.findAll().stream()
				.flatMap(game -> Stream.of(game.getWhite(), game.getBlack()))
				.collect(Collectors.groupingBy(x -> x, Collectors.counting()))
				.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						Map.Entry::getValue,
						(oldValue, newValue) -> oldValue,
						LinkedHashMap::new
				));
	}

	public Map<Title, Map<String, Object>> calculateTitledStats(String playerName) {
		Iterable<Game> games = gameRepository.findGamesByBlackOrWhite(playerName, playerName);
		Map<Title, /*GamesStats*/Integer> titleGamesStatsMap = new HashMap<>();
		Map<Title, /*GamesStats*/Integer> lostGameWithTitledPlayer = new HashMap<>();
		Map<Title, /*GamesStats*/Integer> drawsWithTitledPlayer = new HashMap<>();
		Map<Title, /*GamesStats*/List<String>> lostGames = new HashMap<>();
		Map<Title, /*GamesStats*/List<String>> wonGames = new HashMap<>();
		Map<Title, /*GamesStats*/List<String>> drawGames = new HashMap<>();

		for (var game : games) {
			if ((!game.getWhitetitle().isEmpty() || !game.getBlacktitle().isEmpty())) {
				try {
					Title opponentTitle = game.getWhite().equals(playerName) ? Title.fromString(game.getBlacktitle()) : Title.fromString(game.getWhitetitle());
					titleGamesStatsMap.put(opponentTitle, titleGamesStatsMap.getOrDefault(opponentTitle, 0) + 1);
					if (lostWithColor(playerName, game, true) || lostWithColor(playerName, game, false)) {
						lostGameWithTitledPlayer.put(opponentTitle, lostGameWithTitledPlayer.getOrDefault(opponentTitle, 0) + 1);
						List<String> lostGameWithTitle = lostGames.getOrDefault(opponentTitle, new ArrayList<>());
						lostGameWithTitle.add(game.getSite());
						lostGames.put(opponentTitle, lostGameWithTitle);
					} else if (game.getResult().equals("1/2-1/2")) {
						drawsWithTitledPlayer.put(opponentTitle, drawsWithTitledPlayer.getOrDefault(opponentTitle, 0) + 1);
						List<String> drawnGamesWithTitle = drawGames.getOrDefault(opponentTitle, new ArrayList<>());
						drawnGamesWithTitle.add(game.getSite());
						drawGames.put(opponentTitle, drawnGamesWithTitle);
					} else {
						List<String> wonGamesWithTitle = wonGames.getOrDefault(opponentTitle, new ArrayList<>());
						wonGamesWithTitle.add(game.getSite());
						wonGames.put(opponentTitle, wonGamesWithTitle);
					}
				} catch (IllegalArgumentException ignored) {
				}
			}
		}

		Map<Title, Map<String, Object>> result = new HashMap<>();

		for (var title : titleGamesStatsMap.keySet()) {
			Map<String, Object> resultWithTitle = new HashMap<>();
			int wins = titleGamesStatsMap.get(title) - lostGameWithTitledPlayer.getOrDefault(title, 0) - drawsWithTitledPlayer.getOrDefault(title, 0);
			int loses = lostGameWithTitledPlayer.getOrDefault(title, 0);
			resultWithTitle.put("total", titleGamesStatsMap.get(title));
			resultWithTitle.put("wins", wins);
			resultWithTitle.put("draws", drawsWithTitledPlayer.getOrDefault(title, 0));
			resultWithTitle.put("loses", loses);
			resultWithTitle.put("diff", wins - loses);
			resultWithTitle.put("lostGames", lostGames.get(title));
			resultWithTitle.put("wonGames", wonGames.get(title));
			resultWithTitle.put("drawnGames", drawGames.get(title));
			result.put(title, resultWithTitle);
		}

		return result;
	}

	@CrossOrigin(origins = "*")
	@GetMapping(path = "/calendar/{playerName}")
	public @ResponseBody List<DataPoint> getCalendar(@PathVariable String playerName) {
		Iterable<Game> games = gameRepository.findGamesByBlackOrWhite(playerName, playerName);
		Map<String, Long> gamesPerDay = countGamesPerDay(games);
		return createDataPoints(gamesPerDay, playerName);
	}

	private boolean lostWithColor(String playerName, Game game, boolean isWhite) {
		String lossResult = isWhite ? "0-1" : "1-0";
		return (isWhite ? game.getWhite().equals(playerName) : game.getBlack().equals(playerName))
				&& game.getResult().equals(lossResult);
	}

	private boolean wonWithColor(String playerName, Game game, boolean isWhite) {
		String lossResult = isWhite ? "1-0" : "0-1";
		return (isWhite ? game.getWhite().equals(playerName) : game.getBlack().equals(playerName))
				&& game.getResult().equals(lossResult);
	}

	private Map<String, Long> countGamesPerDay(Iterable<Game> games) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		return StreamSupport.stream(games.spliterator(), false)
				.collect(Collectors.groupingBy(
						game -> dateFormat.format(game.getDate()),
						Collectors.counting()
				))
				.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByKey())
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						Map.Entry::getValue,
						(oldValue, newValue) -> oldValue,
						LinkedHashMap::new
				));
	}

	private List<DataPoint> createDataPoints(Map<String, Long> gamesPerDay, String playerName) {
		return gamesPerDay.entrySet().stream()
				.map(entry -> createDataPoint(entry, playerName))
				.collect(Collectors.toList());
	}

	private DataPoint createDataPoint(Map.Entry<String, Long> entry, String playerName) {
		String date = entry.getKey();
		int gameCount = entry.getValue().intValue();
		String link = createLichessLink(playerName, date);
		return new DataPoint(date, gameCount, link);
	}

	private String createLichessLink(String playerName, String date) {
		try {
			String nextDate = getNextDate(date);
			return String.format("https://lichess.org/games/search?players.a=%s&dateMin=%s&dateMax=%s&sort.field=d&sort.order=desc#results",
					playerName, date, nextDate);
		} catch (ParseException ex) {
			throw new RuntimeException("Error creating Lichess link", ex);
		}
	}

	public record GamesStats(
			long gamesWithWhite, long lostWithWhite, double whiteLostPercentage,
			long gamesWithBlack, long lostWithBlack, double blackLostPercentage
	) {
	}

	public record DataPoint(String x, int value, String link) {
	}
}
