package com.lichess.insights;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping(path = "/games")
public class GameController {
	public static final String PLAYER_NAME = "bodya17";
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

	@GetMapping()
	public String home(Model model) {
		model.addAttribute("message", "Hello Thymeleaf!");
		return "home";
	}

	@GetMapping(path = "/all")
	public @ResponseBody Iterable<Game> getAllGames(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size
	) {
		return gameRepository.findAll(PageRequest.of(page, size, Sort.by("utcDate").ascending()));
	}

	@GetMapping(path = "/win-percentages")
	public @ResponseBody GamesStats getRecord() {
		List<Game> games = gameRepository.findAll();

		long gamesWithWhite = games.stream().filter(game -> game.getWhite().equals(PLAYER_NAME)).count();
		long lostWithWhite = games.stream().filter(game -> game.getWhite().equals(PLAYER_NAME) && game.getResult().equals("0-1")).count();
		double whiteLostPercentage = (double) lostWithWhite / gamesWithWhite;

		long gamesWithBlack = games.stream().filter(game -> game.getBlack().equals(PLAYER_NAME)).count();
		long lostWithBlack = games.stream().filter(game -> game.getBlack().equals(PLAYER_NAME) && game.getResult().equals("1-0")).count();
		double blackLostPercentage = (double) lostWithBlack / gamesWithBlack;
		return new GamesStats(gamesWithWhite, lostWithWhite, whiteLostPercentage, gamesWithBlack, lostWithBlack, blackLostPercentage);
	}

	@GetMapping(path = "/stats-with-titled-players")
	public String getTitledStats(Model model) {
		Map<Title, Map<String, Object>> stats = calculateTitledStats();
		model.addAttribute("titledStats", stats);
		return "titled-stats";  // This will be the name of your Thymeleaf template
	}

	public Map<Title, Map<String, Object>> calculateTitledStats() {
		List<Game> games = gameRepository.findAll();
		Map<Title, /*GamesStats*/Integer> titleGamesStatsMap = new HashMap<>();
		Map<Title, /*GamesStats*/Integer> lostGameWithTitledPlayer = new HashMap<>();
		Map<Title, /*GamesStats*/Integer> drawsWithTitledPlayer = new HashMap<>();
		Map<Title, /*GamesStats*/List<String>> lostGames = new HashMap<>();
		Map<Title, /*GamesStats*/List<String>> wonGames = new HashMap<>();
		Map<Title, /*GamesStats*/List<String>> drawGames = new HashMap<>();


		for (var game : games) {
			if ((!game.getWhitetitle().isEmpty() || !game.getBlacktitle().isEmpty())) {
				try {
					Title title = Title.fromString(game.getWhitetitle().isEmpty() ? game.getBlacktitle() : game.getWhitetitle());
					titleGamesStatsMap.put(title, titleGamesStatsMap.getOrDefault(title, 0) + 1);
					if (lostWithColor(game, true) || lostWithColor(game, false)) {
						lostGameWithTitledPlayer.put(title, lostGameWithTitledPlayer.getOrDefault(title, 0) + 1);
						List<String> lostGameWithTitle = lostGames.getOrDefault(title, new ArrayList<>());
						lostGameWithTitle.add(game.getSite());
						lostGames.put(title, lostGameWithTitle);
					} else if (game.getResult().equals("1/2-1/2")) {
						drawsWithTitledPlayer.put(title, drawsWithTitledPlayer.getOrDefault(title, 0) + 1);
						List<String> drawnGamesWithTitle = drawGames.getOrDefault(title, new ArrayList<>());
						drawnGamesWithTitle.add(game.getSite());
						drawGames.put(title, drawnGamesWithTitle);
					} else {
						List<String> wonGamesWithTitle = wonGames.getOrDefault(title, new ArrayList<>());
						wonGamesWithTitle.add(game.getSite());
						wonGames.put(title, wonGamesWithTitle);
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
	@GetMapping(path = "/calendar")
	public @ResponseBody List<DataPoint> getCalendar() {
		List<Game> games = gameRepository.findAll();
		Map<String, Long> gamesPerDay = countGamesPerDay(games);
		return createDataPoints(gamesPerDay);
	}

	private boolean lostWithColor(Game game, boolean isWhite) {
		String lossResult = isWhite ? "0-1" : "1-0";
		return (isWhite ? game.getWhite().equals(PLAYER_NAME) : game.getBlack().equals(PLAYER_NAME))
				&& game.getResult().equals(lossResult);
	}

	private Map<String, Long> countGamesPerDay(List<Game> games) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		return games.stream()
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

	private List<DataPoint> createDataPoints(Map<String, Long> gamesPerDay) {
		return gamesPerDay.entrySet().stream()
				.map(this::createDataPoint)
				.collect(Collectors.toList());
	}

	private DataPoint createDataPoint(Map.Entry<String, Long> entry) {
		String date = entry.getKey();
		int gameCount = entry.getValue().intValue();
		String link = createLichessLink(date);
		return new DataPoint(date, gameCount, link);
	}

	private String createLichessLink(String date) {
		try {
			String nextDate = getNextDate(date);
			return String.format("https://lichess.org/games/search?players.a=%s&dateMin=%s&dateMax=%s&sort.field=d&sort.order=desc#results",
					PLAYER_NAME, date, nextDate);
		} catch (ParseException ex) {
			throw new RuntimeException("Error creating Lichess link", ex);
		}
	}

	// https://lichess.org/faq#titles
	enum Title {
		GM("Grandmaster"),
		IM("International Master"),
		FM("FIDE Master"),
		CM("Candidate Master"),
		WGM("Woman Grandmaster"),
		WIM("Woman International Master"),
		WFM("Woman FIDE Master"),
		WCM("Woman Candidate Master"),
		NM("National Master"),
		LM("Lichess Master");

		private final String name;

		Title(String name) {
			this.name = name;
		}

		public static Title fromString(String t) {
			for (Title title : Title.values()) {
				if (title.name().equalsIgnoreCase(t)) {
					return title;
				}
			}
			throw new IllegalArgumentException("No title " + t + " found");
		}

		public static boolean isValid(String t) {
			try {
				fromString(t);
				return true;
			} catch (IllegalArgumentException e) {
				return false;
			}
		}

		public String getName() {
			return name;
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
