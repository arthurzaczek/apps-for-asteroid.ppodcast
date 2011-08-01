package net.zaczek.PPodCast.reader;

import java.util.Date;

import net.zaczek.PPodCast.data.Podcast;
import net.zaczek.PPodCast.data.Show;
import net.zaczek.PPodCast.util.DateUtil;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;



public class PodcastFeedHandler extends DefaultHandler {

	Podcast podcast = new Podcast();

	boolean inPodcastTitle = false;
	boolean inPodcastDescription = false;
	
	boolean inItem = false;
	boolean inItemTitle = false;
	boolean inItemDescription = false;
	boolean inItemSummary = false;
	boolean inItemPubDate = false;
	
	String currentItemTitle = null;
	StringBuilder currentItemDescription = new StringBuilder();
	StringBuilder currentItemSummary = new StringBuilder();
	String currentItemEnclosure = null;
	String currentItemPubDate = null;
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (inItemTitle) {
			currentItemTitle = String.valueOf(ch, start, length);
		} else if (inItemDescription) {
			currentItemDescription.append(ch, start, length);
		} else if (inItemSummary) {
			currentItemSummary.append(ch, start, length);
		} else if (inPodcastTitle) {
			podcast.setTitle(String.valueOf(ch, start, length));
		} else if (inPodcastDescription) {
			podcast.setDescription(String.valueOf(ch, start, length));
		} else if (inItemPubDate) {
			currentItemPubDate = String.valueOf(ch, start, length);
		}
	}
	
	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes)
			throws SAXException {
		if ("item".equals(localName)) {
			inItem = true;
		} else if (inItem && "title".equals(localName)) {
			inItemTitle = true;
		} else if (inItem && "enclosure".equals(localName)) {
			currentItemEnclosure = attributes.getValue("url");
		} else if (inItem && "description".equals(localName)) {
			inItemDescription = true;
		} else if (inItem && "summary".equals(localName)) {
			inItemSummary = true;
		} else if (!inItem && "title".equals(localName)) {
			inPodcastTitle = true;
		} else if (!inItem && "description".equals(localName)) {
			inPodcastDescription = true;
		} else if (inItem && "pubDate".equals(localName)) {
			inItemPubDate = true;
		}
	}

	@Override
	public void endElement(String uri, String localName, String name) throws SAXException {
		if ("item".equals(localName)) {
			inItem = false;
			Show show = new Show();
			show.setTitle(currentItemTitle);
			show.setUrl(currentItemEnclosure);
			
			if (currentItemPubDate != null && currentItemPubDate.length() > 0) {
				Date pubDate = DateUtil.parseRfc822DateString(currentItemPubDate);
				show.setDate(pubDate);
			}
			
			if (currentItemSummary.length() > 0) {
				show.setDescription(currentItemSummary.toString());
			} else if (currentItemDescription.length() > 0) {
				show.setDescription(currentItemDescription.toString());
			}

			podcast.addShow(show);
			
			currentItemTitle = null;
			currentItemEnclosure = null;
			currentItemDescription = new StringBuilder();
			currentItemSummary = new StringBuilder();
		} else if (inItem && "title".equals(localName)) {
			inItemTitle = false;
		} else if (inItem && "description".equals(localName)) {
			inItemDescription = false;
		} else if (inItem && "summary".equals(localName)) {
			inItemSummary = false;
		} else if (!inItem && "title".equals(localName)) {
			inPodcastTitle = false;
		} else if (!inItem && "description".equals(localName)) {
			inPodcastDescription = false;
		} else if (inItem && "pubDate".equals(localName)) {
			inItemPubDate = false;
		} 
	}

	public Podcast getPodcast() {
		return podcast;
	}
	
}
