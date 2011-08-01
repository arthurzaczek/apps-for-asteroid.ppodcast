package net.zaczek.PPodCast;

import net.zaczek.PPodCast.data.PuddleDbAdapter;
import net.zaczek.PPodCast.util.PodcastUtil;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class ListPodcasts extends ListActivity {
	private static final String TAG = ListPodcasts.class.getName();

	private static final int ACTIVITY_VIEW_PODCAST = 0;
	private static final int DLG_WAIT = 1;

	private static final int SYNC_PODCAST_ID = Menu.FIRST;
	private static final int EXIT_ID = Menu.FIRST + 1;

	private PuddleDbAdapter podcastDb;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "Setting up List Podcasts activity");

		podcastDb = new PuddleDbAdapter(this);
		podcastDb.open();

		fillData();
		syncPodcast();
	}

	private void fillData() {
		Log.d(TAG, "Filling podcast list with data");
		Cursor podcastsCursor = podcastDb.fetchAllPodcasts();
		startManagingCursor(podcastsCursor);

		String[] from = new String[] { PuddleDbAdapter.PODCAST_TITLE,
				PuddleDbAdapter.PODCAST_LAST_CHECKED,
				PuddleDbAdapter.PODCAST_LATEST_SHOW_TITLE };
		int[] to = new int[] { R.id.list_podcasts_row_title,
				R.id.list_podcasts_row_last_checked,
				R.id.list_podcasts_row_latest_show };

		SimpleCursorAdapter podcastsAdapter = new SimpleCursorAdapter(this,
				R.layout.list_podcasts_row, podcastsCursor, from, to);
		setListAdapter(podcastsAdapter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Log.d(TAG, "Opening podcast at position " + position + " with id " + id);
		Intent i = new Intent(this, ViewPodcast.class);
		i.putExtra("podcast", id);
		startActivityForResult(i, ACTIVITY_VIEW_PODCAST);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		Log.d(TAG, "Creating options menu");
		menu.add(0, SYNC_PODCAST_ID, 0, "Sync");
		menu.add(0, EXIT_ID, 0, "Exit");
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int itemId = item.getItemId();
		Log.d(TAG, "Menu item selected: " + itemId);
		switch (itemId) {
		case SYNC_PODCAST_ID:
			syncPodcast();
			return true;
		case EXIT_ID:
			finish();
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	private void reportMsg(String msg) {
		if (!TextUtils.isEmpty(msg)) {
			Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
		}
	}

	private class SyncTask extends AsyncTask<Void, Void, Void> {
		private String msg;

		@Override
		protected void onPreExecute() {
			showDialog(DLG_WAIT);
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				PodcastUtil.syncPodcasts(podcastDb);
			} catch (Exception ex) {
				msg = ex.toString();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			dismissDialog(DLG_WAIT);
			reportMsg(msg);
			fillData();
			syncTask = null;
			super.onPostExecute(result);
		}
	}

	private SyncTask syncTask;

	private void syncPodcast() {
		Log.d(TAG, "Syncing podcasts");
		if (syncTask == null) {
			syncTask = new SyncTask();
			syncTask.execute();
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		ProgressDialog dialog;
		switch (id) {
		case DLG_WAIT:
			dialog = new ProgressDialog(this);
			dialog.setMessage("Syncing Podcasts");
			break;
		default:
			dialog = null;
		}
		return dialog;
	}
}