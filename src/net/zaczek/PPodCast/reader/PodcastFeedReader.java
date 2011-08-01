package net.zaczek.PPodCast.reader;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.zaczek.PPodCast.data.Podcast;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;



public class PodcastFeedReader {

	public static Podcast readPodcastFromUrl(String url) throws RuntimeException {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet get = new HttpGet(url);
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		String mytext;
		try {
			mytext = httpClient.execute(get, responseHandler);
		} catch (Exception e) {
			throw new RuntimeException("Could not download from url '" + url + "'", e);
		}
		
		PodcastFeedHandler rssHandler = new PodcastFeedHandler();
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		saxParserFactory.setValidating(false);
		SAXParser saxParser;
		try {
			saxParser = saxParserFactory.newSAXParser();
		} catch (Exception e) {
			throw new RuntimeException("Could not create XML parser", e);
		}
		ByteArrayInputStream textBytes = new ByteArrayInputStream(mytext.getBytes());
		try {
			saxParser.parse(textBytes, rssHandler);
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse feed", e);
		}
		
		Podcast podcast = rssHandler.getPodcast();
		podcast.setUrl(url);
		
		return podcast;
	}
	
}
