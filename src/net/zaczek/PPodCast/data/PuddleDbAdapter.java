package net.zaczek.PPodCast.data;

import java.util.Date;

import net.zaczek.PPodCast.util.DateUtil;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.format.Time;
import android.util.Log;

public class PuddleDbAdapter {

	private static final int DATABASE_VERSION = 3;
	private static final String TAG = PuddleDbAdapter.class.getName();

	public static final String PODCAST_ROWID = "_id";
	public static final String PODCAST_TITLE = "title";
	public static final String PODCAST_DESCRIPTION = "description";
	public static final String PODCAST_URL = "url";
	public static final String PODCAST_LAST_CHECKED = "last_checked";
	public static final String PODCAST_LATEST_SHOW_TITLE = "latest_show_title";
	public static final String PODCAST_LATEST_SHOW_ID = "latest_show_id";

	public static final String SHOW_ROWID = "_id";
	public static final String SHOW_TITLE = "title";
	public static final String SHOW_DESCRIPTION = "description";
	public static final String SHOW_URL = "url";
	public static final String SHOW_PODCAST = "podcast";
	public static final String SHOW_DATE = "show_date";

	private static final String DATABASE_NAME = "data";
	private static final String DATABASE_PODCASTS_TABLE = "podcasts";
	private static final String DATABASE_SHOWS_TABLE = "shows";

	private DatabaseHelper dbHelper;
	private SQLiteDatabase db;
	private final Context context;

	public PuddleDbAdapter(Context ctx) {
		this.context = ctx;
	}

	public void open() throws SQLException {
		dbHelper = new DatabaseHelper(context);
		db = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public long createPodcast(String title, String description, String url) {
		Log.d(TAG, String.format("Creating podcast '%s', '%s', '%s'", title, description, url));
		ContentValues initialValues = new ContentValues();
		initialValues.put(PODCAST_TITLE, title);
		initialValues.put(PODCAST_DESCRIPTION, description);
		initialValues.put(PODCAST_URL, url);
		return db.insert(DATABASE_PODCASTS_TABLE, null, initialValues);
	}

	public boolean deletePodcast(long rowId) {
		Log.d(TAG, "Deleting podcast " + rowId);
		return db.delete(DATABASE_PODCASTS_TABLE, PODCAST_ROWID + "=" + rowId, null) > 0;
	}

	public Cursor fetchAllPodcasts() {
		Log.d(TAG, "Fetching all podcasts");
		return db.query(DATABASE_PODCASTS_TABLE, new String[] { PODCAST_ROWID, PODCAST_TITLE, PODCAST_DESCRIPTION,
				PODCAST_URL, PODCAST_LAST_CHECKED, PODCAST_LATEST_SHOW_TITLE, PODCAST_LATEST_SHOW_ID }, null, null, null, null, null);
	}

	public Cursor fetchPodcast(long rowId) throws SQLException {
		Log.d(TAG, "Fetching podcast " + rowId);
		Cursor cursor = db.query(true, DATABASE_PODCASTS_TABLE, new String[] { PODCAST_ROWID, PODCAST_TITLE,
				PODCAST_DESCRIPTION, PODCAST_URL, PODCAST_LAST_CHECKED }, PODCAST_ROWID + "=" + rowId, null, null,
				null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}
	
	public Cursor fetchPodcast(String url) {
		Log.d(TAG, "Fetching podcast " + url);
		Cursor cursor = db.query(true, DATABASE_PODCASTS_TABLE, new String[] { PODCAST_ROWID, PODCAST_TITLE,
				PODCAST_DESCRIPTION, PODCAST_URL, PODCAST_LAST_CHECKED }, PODCAST_URL + "='" + url + "'", null, null,
				null, null, null);
		return cursor;
	}

	public boolean updatePodcast(long rowId, String title, String description, String url) {
		Log.d(TAG, String.format("Updating podcast %d with '%s', '%s', '%s'", rowId, title, description, url));
		Time now = new Time();
		now.setToNow();

		ContentValues args = new ContentValues();
		args.put(PODCAST_TITLE, title);
		args.put(PODCAST_DESCRIPTION, description);
		args.put(PODCAST_URL, url);
		args.put(PODCAST_LAST_CHECKED, now.format("%x %X"));
		return db.update(DATABASE_PODCASTS_TABLE, args, PODCAST_ROWID + "=" + rowId, null) > 0;
	}

	public long addShow(long podcast, String title, String description, String url, Date date) {
		Log.d(TAG, String.format("Adding show for podcast %d: '%s', '%s'", podcast, title, url));
		ContentValues initialValues = new ContentValues();
		initialValues.put(SHOW_PODCAST, podcast);
		initialValues.put(SHOW_TITLE, title);
		initialValues.put(SHOW_DESCRIPTION, description);
		initialValues.put(SHOW_URL, url);
		if (date != null) {
			initialValues.put(SHOW_DATE, DateUtil.dateToSqliteDateString(date));
		}
		long showId = db.insert(DATABASE_SHOWS_TABLE, null, initialValues);

		updateLatestShow(podcast);
		return showId;
	}

	private void updateLatestShow(long podcast) {
		Log.d(TAG, "Updating latest show for podcast " + podcast);
		Cursor c = db.query(DATABASE_SHOWS_TABLE, new String[] { SHOW_ROWID, SHOW_TITLE }, SHOW_PODCAST + "="
				+ podcast, null, null, null, SHOW_DATE + " desc");
		try {
			boolean firstExists = c.moveToFirst();
			if (firstExists) {
				String title = c.getString(c.getColumnIndex(SHOW_TITLE));
				long showId = c.getLong(c.getColumnIndex(SHOW_ROWID));
				Log.d(TAG, "Found show " + showId + " with title '" + title + "'");
				ContentValues v = new ContentValues();
				v.put(PODCAST_LATEST_SHOW_ID, showId);
				v.put(PODCAST_LATEST_SHOW_TITLE, title);
				db.update(DATABASE_PODCASTS_TABLE, v, PODCAST_ROWID + "=" + podcast, null);
			}
		} finally {
			c.close();
		}
	}

	public boolean deleteShow(long rowId) {
		Log.d(TAG, "Deleting show " + rowId);
		return db.delete(DATABASE_SHOWS_TABLE, SHOW_ROWID + "=" + rowId, null) > 0;
	}

	public Cursor fetchAllShowsForPodcast(long podcast) {
		Log.d(TAG, "Fetching all shows for podcast " + podcast);
		return db.query(true, DATABASE_SHOWS_TABLE, new String[] { SHOW_ROWID, SHOW_PODCAST, SHOW_TITLE,
				SHOW_DESCRIPTION, SHOW_URL, SHOW_DATE }, SHOW_PODCAST + "=" + podcast, null, null, null, SHOW_DATE + " DESC", null);
	}

	public Cursor fetchShow(long rowId) throws SQLException {
		Log.d(TAG, "Fetching show " + rowId);
		Cursor cursor = db.query(true, DATABASE_SHOWS_TABLE, new String[] { SHOW_ROWID, SHOW_PODCAST, SHOW_TITLE,
				SHOW_DESCRIPTION, SHOW_URL, SHOW_DATE }, SHOW_ROWID + "=" + rowId, null, null, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}
	
	public Cursor fetchShow(String url) {
		Log.d(TAG, "Fetching show " + url);
		Cursor cursor = db.query(true, DATABASE_SHOWS_TABLE, new String[] { SHOW_ROWID, SHOW_PODCAST, SHOW_TITLE,
				SHOW_DESCRIPTION, SHOW_URL, SHOW_DATE }, SHOW_URL + "='" + url + "'", null, null, null, null, null);		
		return cursor;
	}


	public boolean updateShow(long rowId, String title, String description, String url) {
		Log.d(TAG, String.format("Updating show %d with '%s', '%s'", rowId, title, url));
		ContentValues args = new ContentValues();
		args.put(SHOW_TITLE, title);
		args.put(SHOW_DESCRIPTION, description);
		args.put(SHOW_URL, url);
		return db.update(DATABASE_SHOWS_TABLE, args, SHOW_ROWID + "=" + rowId, null) > 0;
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		private static final String TAG = DatabaseHelper.class.getName();

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			Log.d(TAG, "Connected to database " + DATABASE_NAME + " with version " + DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d(TAG, "Creating table " + DATABASE_PODCASTS_TABLE);
			db.execSQL("create table " 
					+ DATABASE_PODCASTS_TABLE + " (_id integer primary key autoincrement, "
					+ PODCAST_TITLE + " text not null, " 
					+ PODCAST_DESCRIPTION + " text, " 
					+ PODCAST_URL + " text not null, " 
					+ PODCAST_LAST_CHECKED + " text not null default CURRENT_TIMESTAMP, "
					+ PODCAST_LATEST_SHOW_ID + " integer, " 
					+ PODCAST_LATEST_SHOW_TITLE + " text " 
					+ ");");

			Log.d(TAG, "Creating table " + DATABASE_SHOWS_TABLE);
			db.execSQL("create table " 
					+ DATABASE_SHOWS_TABLE + " (_id integer primary key autoincrement, "
					+ SHOW_PODCAST + " integer not null, " 
					+ SHOW_TITLE + " text not null, " 
					+ SHOW_DESCRIPTION + " text, " 
					+ SHOW_URL + " text not null, " 
					+ SHOW_DATE + " text not null default CURRENT_TIMESTAMP " 
					+ ");");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.d(TAG, "Dropping tables");
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_PODCASTS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_SHOWS_TABLE);
			onCreate(db);
		}
	}
}
