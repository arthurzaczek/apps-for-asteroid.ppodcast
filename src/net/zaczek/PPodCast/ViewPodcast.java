package net.zaczek.PPodCast;

import com.parrot.asteroid.tts.TTSManager;

import net.zaczek.PPodCast.data.PuddleDbAdapter;
import net.zaczek.PPodCast.util.PodcastUtil;
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
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class ViewPodcast extends AbstractListActivity implements OnItemSelectedListener {

	private static final String TAG = ViewPodcast.class.getName();

	private static final int MENU_UPDATE_PODCAST = 0;

	private static final int ACTIVITY_VIEW_SHOW = 0;

	private long podcastId = -1;
	private String title;
	private PuddleDbAdapter podcastDb;

	private TextView titleTextView;
	private ListView showList;
	private ProgressBar progBar;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.view_podcast);

		showList = getListView();
		titleTextView = (TextView) findViewById(R.id.text_header);
		progBar = (ProgressBar) findViewById(R.id.view_podcast_progress);
		getListView().setOnItemSelectedListener(this);

		progBar.setVisibility(View.INVISIBLE);

		podcastDb = new PuddleDbAdapter(this);
		podcastDb.open();


		Intent intent = getIntent();
		podcastId = intent.getLongExtra("podcast", -1);

		fillData();
		updatePodcast();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		viewShow(id);
	}

	@Override
	public void onItemSelected(AdapterView<?> adapterView, View view,
			int pos, long id) {
		try {
			Cursor c = (Cursor) adapterView.getAdapter().getItem(pos);
			mTTS.speak(c.getString(c
					.getColumnIndex(PuddleDbAdapter.SHOW_TITLE)),
					TTSManager.QUEUE_FLUSH,
					null);
		} catch (Exception ex) {
			Log.e(TAG, ex.toString());
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {

	}

	protected void viewShow(long id) {
		Intent i = new Intent(this, ViewShow.class);
		i.putExtra("show", id);
		startActivityForResult(i, ACTIVITY_VIEW_SHOW);
	}

	private void fillData() {
		Cursor c = podcastDb.fetchPodcast(podcastId);
		try {
			title = c
					.getString(c.getColumnIndex(PuddleDbAdapter.PODCAST_TITLE));
			titleTextView.setText(title);
		} finally {
			c.close();
		}

		Cursor showsCursor = podcastDb.fetchAllShowsForPodcast(podcastId);
		startManagingCursor(showsCursor);

		String[] from = new String[] { PuddleDbAdapter.SHOW_TITLE };
		int[] to = new int[] { android.R.id.text1 };

		SimpleCursorAdapter showsAdapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_1, showsCursor, from, to);

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
			progBar.setVisibility(View.VISIBLE);
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
			reportMsg(msg);
			fillData();
			progBar.setVisibility(View.INVISIBLE);
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
}