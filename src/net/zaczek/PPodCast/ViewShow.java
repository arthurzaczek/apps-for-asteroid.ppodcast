package net.zaczek.PPodCast;

import net.zaczek.PPodCast.data.PuddleDbAdapter;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;

public class ViewShow extends Activity {

	private WakeLock wl;

	private static final int DLG_WAIT = 1;
	private long show = -1;
	private PuddleDbAdapter podcastDb;
	MediaPlayer mp;
	private AudioManager am;

	private TextView showTitle;
	private TextView showDescription;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "ViewShowAndStayAwake");

		setContentView(R.layout.view_show);

		podcastDb = new PuddleDbAdapter(this);
		podcastDb.open();

		Intent intent = getIntent();
		show = intent.getLongExtra("show", -1);

		showTitle = (TextView) findViewById(R.id.ViewShowTitle);
		showDescription = (TextView) findViewById(R.id.ViewShowDescription);

		mp = new MediaPlayer();
		am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

		fillData();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_DOWN:
			am.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
			return true;
		case KeyEvent.KEYCODE_DPAD_UP:
			am.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
			return true;
		default:
			return super.onKeyDown(keyCode, event);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		ProgressDialog dialog;
		switch (id) {
		case DLG_WAIT:
			dialog = new ProgressDialog(this);
			dialog.setMessage("Loading Podcast");
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	@Override
	protected void onResume() {
		super.onResume();
		wl.acquire();
	}

	@Override
	protected void onPause() {
		mp.stop();
		wl.release();
		super.onPause();
	}

	private void fillData() {
		Cursor showCursor = podcastDb.fetchShow(show);

		if (showCursor == null) {
			setResult(RESULT_CANCELED);
			finish();
		}

		startManagingCursor(showCursor);

		String title = showCursor.getString(showCursor.getColumnIndex(PuddleDbAdapter.SHOW_TITLE));
		showTitle.setText(title);

		String description = showCursor.getString(showCursor.getColumnIndex(PuddleDbAdapter.SHOW_DESCRIPTION));
		showDescription.setText(description);

		String url = showCursor.getString(showCursor.getColumnIndex(PuddleDbAdapter.SHOW_URL));
		play(url);
	}

	private void play(String url) {
		try {
			mp.setDataSource(url);
			mp.setOnPreparedListener(new OnPreparedListener() {
				public void onPrepared(MediaPlayer mp) {
					mp.start();
					dismissDialog(DLG_WAIT);
				}
			});
			showDialog(DLG_WAIT);
			mp.prepareAsync();
		} catch (Exception e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
}