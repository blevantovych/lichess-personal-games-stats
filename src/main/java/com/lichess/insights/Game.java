package com.lichess.insights;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "chess_games")
public class Game {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ID")
	private int id;

	@Column(name = "event", nullable = false, length = 100)
	private String event;

	@Column(name = "site", nullable = false, length = 32)
	private String site;

	@Column(name = "date", nullable = false)
	private Date date;

	@Column(name = "white", nullable = false, length = 32)
	private String white;

	@Column(name = "black", nullable = false, length = 32)
	private String black;

	@Column(name = "result", nullable = false, length = 10)
	private String result;

	@Column(name = "utcdate", nullable = false)
	private Date utcDate;

	@Column(name = "utctime", nullable = false)
	private Date utcTime;

	@Column(name = "whiteelo", nullable = false, length = 10)
	private String whiteElo;

	@Column(name = "blackelo", nullable = false, length = 10)
	private String blackElo;

	@Column(name = "whiteratingdiff", nullable = false, length = 10)
	private String whiteRatingDiff;

	@Column(name = "blackratingdiff", nullable = false, length = 10)
	private String blackRatingDiff;

	@Column(name = "whitetitle", nullable = false, length = 10)
	private String whitetitle;

	@Column(name = "blacktitle", nullable = false, length = 10)
	private String blacktitle;

	@Column(name = "variant", nullable = false, length = 32)
	private String variant;

	@Column(name = "timecontrol", nullable = false, length = 32)
	private String timeControl;

	@Column(name = "eco", nullable = false, length = 10)
	private String eco;

	@Column(name = "opening", nullable = false, length = 100)
	private String opening;

	@Column(name = "termination", nullable = false, length = 20)
	private String termination;

	@Lob
	@Column(name = "moves", nullable = false)
	private String moves;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getEvent() {
		return event;
	}

	public void setEvent(String event) {
		this.event = event;
	}

	public String getSite() {
		return site;
	}

	public void setSite(String site) {
		this.site = site;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getWhite() {
		return white;
	}

	public void setWhite(String white) {
		this.white = white;
	}

	public String getBlack() {
		return black;
	}

	public void setBlack(String black) {
		this.black = black;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public Date getUtcDate() {
		return utcDate;
	}

	public void setUtcDate(Date utcDate) {
		this.utcDate = utcDate;
	}

	public Date getUtcTime() {
		return utcTime;
	}

	public void setUtcTime(Date utcTime) {
		this.utcTime = utcTime;
	}

	public String getWhiteElo() {
		return whiteElo;
	}

	public void setWhiteElo(String whiteElo) {
		this.whiteElo = whiteElo;
	}

	public String getBlackElo() {
		return blackElo;
	}

	public void setBlackElo(String blackElo) {
		this.blackElo = blackElo;
	}

	public String getWhiteRatingDiff() {
		return whiteRatingDiff;
	}

	public void setWhiteRatingDiff(String whiteRatingDiff) {
		this.whiteRatingDiff = whiteRatingDiff;
	}

	public String getBlackRatingDiff() {
		return blackRatingDiff;
	}

	public void setBlackRatingDiff(String blackRatingDiff) {
		this.blackRatingDiff = blackRatingDiff;
	}

	public String getWhitetitle() {
		return whitetitle;
	}

	public void setWhitetitle(String whitetitle) {
		this.whitetitle = whitetitle;
	}

	public String getBlacktitle() {
		return blacktitle;
	}

	public void setBlacktitle(String blacktitle) {
		this.blacktitle = blacktitle;
	}

	public String getVariant() {
		return variant;
	}

	public void setVariant(String variant) {
		this.variant = variant;
	}

	public String getTimeControl() {
		return timeControl;
	}

	public void setTimeControl(String timeControl) {
		this.timeControl = timeControl;
	}

	public String getEco() {
		return eco;
	}

	public void setEco(String eco) {
		this.eco = eco;
	}

	public String getOpening() {
		return opening;
	}

	public void setOpening(String opening) {
		this.opening = opening;
	}

	public String getTermination() {
		return termination;
	}

	public void setTermination(String termination) {
		this.termination = termination;
	}

	public String getMoves() {
		return moves;
	}

	public void setMoves(String moves) {
		this.moves = moves;
	}
}
