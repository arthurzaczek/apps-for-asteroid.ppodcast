package net.zaczek.PPodCast;

import net.zaczek.PPodCast.data.PuddleDbAdapter;
import net.zaczek.PPodCast.util.PodcastUtil;
import android.app.Activity;
import android.app.Dialog;
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
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class ViewPodcast extends Activity {

	private static final String TAG = ViewPodcast.class.getName();

	private static final int MENU_UPDATE_PODCAST = 0;

	private static final int ACTIVITY_VIEW_SHOW = 0;
	private static final int DLG_WAIT = 1;
	private boolean waitDlgShown = false;

	private long podcastId = -1;
	private String title;
	private PuddleDbAdapter podcastDb;

	private TextView titleTextView;
	private ListView showList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.view_podcast);

		showList = (ListView) findViewById(R.id.view_podcast_list_shows);
		titleTextView = (TextView) findViewById(R.id.view_podcast_title);

		podcastDb = new PuddleDbAdapter(this);
		podcastDb.open();

		Intent intent = getIntent();
		podcastId = intent.getLongExtra("podcast", -1);

		fillData();
		updatePodcast();

		showList.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> adapterView, View view,
					int pos, long id) {
				viewShow(id);
			}
		});
	}

	protected void viewShow(long id) {
		Intent i = new Intent(this, ViewShow.class);
		i.putExtra("show", id);
		startActivityForResult(i, ACTIVITY_VIEW_SHOW);
	}

	private void fillData() {
		Cursor c = podcastDb.fetchPodcast(podcastId);
		try {
			title = c.getString(c
					.getColumnIndex(PuddleDbAdapter.PODCAST_TITLE));
			titleTextView.setText(title);
		} finally {
			c.close();
		}

		Cursor showsCursor = podcastDb.fetchAllShowsForPodcast(podcastId);
		startManagingCursor(showsCursor);

		String[] from = new String[] { PuddleDbAdapter.SHOW_TITLE,
				PuddleDbAdapter.SHOW_DATE };
		int[] to = new int[] { R.id.list_show_row_title,
				R.id.list_show_row_date };

		SimpleCursorAdapter showsAdapter = new SimpleCursorAdapter(this,
				R.layout.list_show_row, showsCursor, from, to);

		showList.setAdapter(showsAdapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		Log.d(TAG, "Creating options menu");
		menu.add(0, MENU_UPDATE_PODCAST, 0, "Update Podcast");
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int itemId = item.getItemId();
		Log.d(TAG, "Menu item selected: " + itemId);
		switch (itemId) {
		case MENU_UPDATE_PODCAST:
			showDialog(DLG_WAIT);
			updatePodcast();
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	private class UpdateTask extends AsyncTask<Void, Void, Void> {
		private String msg;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			titleTextView.setText("* " + title);
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				PodcastUtil.updatePodcast(podcastDb, podcastId);
			} catch (Exception ex) {
				msg = ex.toString();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if(waitDlgShown == true) {
				dismissDialog(DLG_WAIT);
				waitDlgShown = false;
			}
			reportMsg(msg);
			fillData();
			updateTask = null;
			super.onPostExecute(result);
		}
	}

	private void reportMsg(String msg) {
		if (!TextUtils.isEmpty(msg)) {
			Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
		}
	}

	private UpdateTask updateTask;

	private void updatePodcast() {
		if (updateTask == null) {
			updateTask = new UpdateTask();
			updateTask.execute();
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		ProgressDialog dialog;
		switch (id) {
		case DLG_WAIT:
			waitDlgShown = true;
			dialog = new ProgressDialog(this);
			dialog.setMessage("Updating Podcast");
			break;
		default:
			dialog = null;
		}
		return dialog;
	}
}