package com.lichess.insights;

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
