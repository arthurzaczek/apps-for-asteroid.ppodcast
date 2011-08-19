package net.zaczek.PPodCast.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import net.zaczek.PPodCast.data.Podcast;
import net.zaczek.PPodCast.data.PuddleDbAdapter;
import net.zaczek.PPodCast.data.Show;
import net.zaczek.PPodCast.reader.PodcastFeedReader;

import android.database.Cursor;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;


public class PodcastUtil {
	private static final String TAG = PodcastUtil.class.getName();

	public static void addPodcast(PuddleDbAdapter podcastDb, String url) {
		Log.i(TAG, "Adding podcast at URL: " + url);
		Podcast podcast = PodcastFeedReader.readPodcastFromUrl(url);

		long podcastId = podcastDb.createPodcast(podcast.getTitle(),
				podcast.getDescription(), podcast.getUrl());
		for (Show s : podcast.getShows()) {
			podcastDb.addShow(podcastId, s.getTitle(), s.getDescription(),
					s.getUrl(), s.getDate());
		}
	}

	public static void updatePodcast(PuddleDbAdapter podcastDb, long podcastId) {
		Cursor c = podcastDb.fetchPodcast(podcastId);
		try {
			String url = c.getString(c
					.getColumnIndex(PuddleDbAdapter.PODCAST_URL));
			Podcast podcast = PodcastFeedReader.readPodcastFromUrl(url);

			// Add new
			for (Show s : podcast.getShows()) {
				Cursor cShow = podcastDb.fetchShow(s.getUrl());
				try {
					if (!cShow.moveToFirst()) {
						podcastDb.addShow(podcastId, s.getTitle(),
								s.getDescription(), s.getUrl(), s.getDate());
					}
				} finally {
					cShow.close();
				}
			}

			// Delete
			Cursor cShows = podcastDb.fetchAllShowsForPodcast(podcastId);
			try {
				while (cShows.moveToNext()) {
					final String showUrl = cShows.getString(cShows
							.getColumnIndex(PuddleDbAdapter.SHOW_URL));
					if (podcast.findShow(showUrl) == null) {
						final long showId = cShows.getLong(cShows
								.getColumnIndex(PuddleDbAdapter.SHOW_ROWID));
						podcastDb.deleteShow(showId);
					}
				}
			} finally {
				cShows.close();
			}

			podcastDb.updatePodcast(podcastId, podcast.getTitle(),
					podcast.getDescription(), url);
		} finally {
			c.close();
		}
	}

	public static void syncPodcasts(PuddleDbAdapter podcastDb)
			throws IOException {
		File root = Environment.getExternalStorageDirectory();
		File dir = new File(root, "PPodCast");
		dir.mkdir();
		File file = new File(dir, "podcasts.txt");
		FileReader reader = new FileReader(file);
		try {
			BufferedReader in = new BufferedReader(reader);
			while (true) {
				String url = in.readLine();
				if (url == null)
					break;
				url = url.trim();
				if (TextUtils.isEmpty(url))
					continue;
				Cursor c = podcastDb.fetchPodcast(url);
				try {
					if (!c.moveToFirst()) {
						// Add
						addPodcast(podcastDb, url);
					}
				} finally {
					c.close();
				}
			}
		} finally {
			reader.close();
		}
	}
}
