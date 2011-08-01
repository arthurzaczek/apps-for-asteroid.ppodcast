package net.zaczek.PPodCast.data;

import java.util.Date;


public class Show {

	private String title;
	private String description;
	private String url;
	private Date date;
	
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
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url.trim();
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("Show(");
		str.append("title='");
		str.append(title);
		str.append("',description='");
		str.append(description);
		str.append("',url='");
		str.append(url);
		str.append("')");
		return str.toString();
	}
}
