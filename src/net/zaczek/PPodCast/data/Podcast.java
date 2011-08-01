package net.zaczek.PPodCast.data;

import java.util.ArrayList;
import java.util.List;


public class Podcast {

	private String title;
	private String description;
	private String url;
	
	private List<Show> shows = new ArrayList<Show>();

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title.trim();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description.trim();
	}

	public List<Show> getShows() {
		return shows;
	}

	public void setShows(List<Show> shows) {
		this.shows = shows;
	}

	public void addShow(Show show) {
		shows.add(show);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("Podcast(");
		str.append("title='");
		str.append(title);
		str.append("',description='");
		str.append(description);
		str.append("',url='");
		str.append(url);
		str.append("',shows=[");
		boolean first = true;
		for (Show show : shows) {
			if (!first) str.append(",");
			str.append(show.toString());
			first = false;
		}
		str.append("]");
		str.append(")");
		return str.toString();
	}

	public Show findShow(String showUrl) {
		for(Show s : shows) {
			if(s.getUrl().equals(showUrl)) return s;
		}
		return null;
	}	
}