package net.zaczek.PPodCast.reader;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.zaczek.PPodCast.data.Podcast;
import net.zaczek.PPodCast.util.Base64;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

public class PodcastFeedReader {

	public static Podcast readPodcastFromUrl(String urlStr)
			throws RuntimeException {

		initHttps();

		URI url;
		try {
			url = new URI(urlStr);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Invalid Url '" + urlStr + "'", e);
		}

		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet get = new HttpGet(url);
		if (url.getUserInfo() != null) {
			get.setHeader("Authorization",
					"Basic " + Base64.encode(url.getUserInfo().getBytes()));
		}
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		String mytext;
		try {
			mytext = httpClient.execute(get, responseHandler);
		} catch (Exception e) {
			throw new RuntimeException("Could not download from url '" + urlStr
					+ "'", e);
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
		ByteArrayInputStream textBytes = new ByteArrayInputStream(
				mytext.getBytes());
		try {
			saxParser.parse(textBytes, rssHandler);
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse feed", e);
		}

		Podcast podcast = rssHandler.getPodcast();
		podcast.setUrl(urlStr);

		return podcast;
	}

	private static boolean httpsInitialized = false;

	private synchronized static void initHttps() {
		if (!httpsInitialized) {
			HttpsURLConnection
					.setDefaultHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			SSLSocketFactory socketFactory = SSLSocketFactory
					.getSocketFactory();
			socketFactory
					.setHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			httpsInitialized = true;
		}
	}
}
