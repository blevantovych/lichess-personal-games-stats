package com.lichess.insights;

import static java.util.Comparator.reverseOrder;
import static java.util.Map.Entry;

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
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller // This means that this class is a Controller
@RequestMapping(path = "/games") // This means URL's start with /demo (after Application path)
public class GameController {
	private final GameRepository gameRepository;

	public GameController(GameRepository gameRepository) {
		this.gameRepository = gameRepository;
	}

	@GetMapping()
	public String home(Model model) {
		model.addAttribute("message", "Hello Thymeleaf!");
		return "home";  // This refers to src/main/resources/templates/home.html
	}

	@GetMapping(path = "/all")
	public @ResponseBody Iterable<Game> getAllGames(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size
	) {
		// This returns a JSON or XML with the users
		return gameRepository.findAll(PageRequest.of(page, size, Sort.by("utcDate").ascending()));
	}

	@GetMapping(path = "/win-percentages")
	public @ResponseBody GamesStats getRecord() {
		List<Game> games = gameRepository.findAll();

		long gamesWithWhite = games.stream().filter(game -> game.getWhite().equals("bodya17")).count();
		long lostWithWhite = games.stream().filter(game -> game.getWhite().equals("bodya17") && game.getResult().equals("0-1")).count();
		double whiteLostPercentage = (double) lostWithWhite / gamesWithWhite;

		long gamesWithBlack = games.stream().filter(game -> game.getBlack().equals("bodya17")).count();
		long lostWithBlack = games.stream().filter(game -> game.getBlack().equals("bodya17") && game.getResult().equals("1-0")).count();
		double blackLostPercentage = (double) lostWithBlack / gamesWithBlack;
		return new GamesStats(gamesWithWhite, lostWithWhite, whiteLostPercentage, gamesWithBlack, lostWithBlack, blackLostPercentage);
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

		public String getName() {
			return name;
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
			} catch(IllegalArgumentException e) {
				return false;
			}
		}
	}

	record GameStat() {}

	@GetMapping(path = "/stats-with-titled-players")
	public @ResponseBody Map<Title, /*GamesStats*/ Long> getTitledStats() {
		List<Game> games = gameRepository.findAll();
		Map<Title, /*GamesStats*/Long> titleGamesStatsMap = new HashMap<>();


		for (var game : games) {
			if ((!game.getWhitetitle().isEmpty() || !game.getBlacktitle().isEmpty())) {
				try {
					Title title = Title.fromString(game.getWhitetitle().isEmpty() ? game.getBlacktitle() : game.getWhitetitle());
					titleGamesStatsMap.put(title, titleGamesStatsMap.getOrDefault(title, 0L) + 1);
				} catch(IllegalArgumentException ignored) { }

			}
		}

//		games.stream().filter(g -> !g.getWhitetitle().isEmpty()).collect(Collectors.groupingBy(Game::getWhitetitle, Collectors.counting()))

//		games.stream().filter(game -> game)
//		long gamesWithWhite = games.stream().filter(game -> game.getWhite().equals("bodya17")).count();
//		long lostWithWhite = games.stream().filter(game -> game.getWhite().equals("bodya17") && game.getResult().equals("0-1")).count();
//		double whiteLostPercentage = (double) lostWithWhite / gamesWithWhite;
//
//		long gamesWithBlack = games.stream().filter(game -> game.getBlack().equals("bodya17")).count();
//		long lostWithBlack = games.stream().filter(game -> game.getBlack().equals("bodya17") && game.getResult().equals("1-0")).count();
//		double blackLostPercentage = (double) lostWithBlack / gamesWithBlack;
//		return new GamesStats(gamesWithWhite, lostWithWhite, whiteLostPercentage, gamesWithBlack, lostWithBlack, blackLostPercentage);
		return titleGamesStatsMap
				.entrySet()
				.stream()
				.sorted(Entry.comparingByValue(Comparator.reverseOrder()))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
	}

	@CrossOrigin(origins = "*")
	@GetMapping(path = "/calendar")
	public @ResponseBody List<DataPoint> getCalendar() {
		List<Game> games = gameRepository.findAll();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

		return games.stream()
				.collect(Collectors.groupingBy(game -> dateFormat.format(game.getDate()), Collectors.counting()))
				.entrySet()
				.stream()
				.sorted(Entry.comparingByKey())
				.collect(Collectors.toMap(
						Entry::getKey,
						Entry::getValue,
						(oldValue, newValue) -> oldValue,
						LinkedHashMap::new)).entrySet().stream().map(e -> {
					try {
						return new DataPoint(
						e.getKey(),
						e.getValue().intValue(),
						String.format("https://lichess.org/games/search?players.a=bodya17&dateMin=%s&dateMax=%s&sort.field=d&sort.order=desc#results", e.getKey(), getNextDate(e.getKey()))
				);
					} catch (ParseException ex) {
						throw new RuntimeException(ex);
					}
				}).collect(Collectors.toList());
	}

	public static String getNextDate(String  curDate) throws ParseException {
		final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		final Date date = format.parse(curDate);
		final Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		return format.format(calendar.getTime());
	}

	public record GamesStats(
			long gamesWithWhite, long lostWithWhite, double whiteLostPercentage,
			long gamesWithBlack, long lostWithBlack, double blackLostPercentage
	) {
	}

	public record DataPoint(String x, int value, String link) {
	}
}
