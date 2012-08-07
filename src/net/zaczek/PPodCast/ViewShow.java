package net.zaczek.PPodCast;

import java.util.Timer;
import java.util.TimerTask;

import net.zaczek.PPodCast.data.PuddleDbAdapter;
import net.zaczek.PPodCast.util.DateUtil;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ViewShow extends Activity implements OnErrorListener, OnBufferingUpdateListener, OnCompletionListener, OnInfoListener, OnSeekCompleteListener {
	private static final String TAG = "PPodCast";

	private static final int STATUS_STOPPED = 1;
	private static final int STATUS_PLAY = 2;
	private static final int STATUS_BUFFERING = 3;
	private static final int STATUS_ERROR = 4;
	private static final int STATUS_EXITING = 5;

	private int status;

	private WakeLock wl;

	private static final int DLG_WAIT = 1;
	private long show = -1;
	private PuddleDbAdapter podcastDb;
	MediaPlayer mp;
	private AudioManager am;

	private TextView showTitle;
	private TextView showDescription;
	private TextView showStatus;
	private TextView showStatusCurrent;
	private ProgressBar progBar;
	private int lastPosition;
	private int total;
	private String url;

	private Timer _updateTimer;

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
		showStatus = (TextView) findViewById(R.id.ViewShowStatus);
		showStatusCurrent = (TextView) findViewById(R.id.ViewShowStatusCurrent);
		progBar = (ProgressBar) findViewById(R.id.progBar);

		lastPosition = 0;
		am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
		initPlayer();
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		progBar.setProgress(0);
		fillData();
		createUpdateTimer();
	}

	private void initPlayer() {
		Log.i(TAG, "initPlayer");
		if (mp != null) {
			try {
				mp.stop();
				mp.release();
			} catch(Exception ex) {
				// Don't care, recreating the player
			}
		}
		mp = new MediaPlayer();
		mp.setScreenOnWhilePlaying(true); // TODO: Remove wake lock

		mp.setOnErrorListener(this);
		mp.setOnBufferingUpdateListener(this);
		mp.setOnCompletionListener(this);
		mp.setOnInfoListener(this);
		mp.setOnSeekCompleteListener(this);
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume");
		super.onResume();
		updateStatus(STATUS_STOPPED);
		wl.acquire();
		createUpdateTimer();
		play(url);
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "onPause");
		mp.stop();
		wl.release();
		status = STATUS_EXITING;
		if (_updateTimer != null) {
			_updateTimer.cancel();
			_updateTimer = null;
		}
		super.onPause();
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
		case KeyEvent.KEYCODE_DPAD_RIGHT:
		case KeyEvent.KEYCODE_MEDIA_NEXT:
			mp.seekTo(mp.getCurrentPosition() + 5000);
			return true;
		case KeyEvent.KEYCODE_DPAD_LEFT:
		case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
			mp.seekTo(mp.getCurrentPosition() - 5000);
			return true;
		case KeyEvent.KEYCODE_DPAD_CENTER:
		case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
			if (status == STATUS_PLAY) {
				if (mp.isPlaying()) {
					pause();
				} else {
					start();
				}
			} else if (status == STATUS_ERROR || status == STATUS_STOPPED) {
				play(url);
			}
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

	private void fillData() {
		Cursor showCursor = podcastDb.fetchShow(show);

		if (showCursor == null) {
			setResult(RESULT_CANCELED);
			finish();
		}

		startManagingCursor(showCursor);

		String title = showCursor.getString(showCursor.getColumnIndex(PuddleDbAdapter.SHOW_TITLE));
		showTitle.setText(title);
		Log.i(TAG, "fill Data for show " + title);

		String description = showCursor.getString(showCursor.getColumnIndex(PuddleDbAdapter.SHOW_DESCRIPTION));
		showDescription.setText(description);

		url = showCursor.getString(showCursor.getColumnIndex(PuddleDbAdapter.SHOW_URL));
	}

	private void updateProgress() {
		int pos = mp.getCurrentPosition();
		if (pos >= lastPosition) {
			lastPosition = pos;
		} else {
			Log.d(TAG, "Position is less then last pos: " + pos  + " < " + lastPosition);
		}
		if (total > 0) {
			progBar.setProgress(100 * pos / total);
		}
		showStatusCurrent.setText(DateUtil.getTimeString(pos) + "/" + DateUtil.getTimeString(total));
	}

	private void updateStatus(String error) {
		updateStatus(STATUS_ERROR, error);
	}

	private void updateStatus(int newStatus) {
		updateStatus(newStatus, "Unknown Error");
	}

	private void updateStatus(int newStatus, String error) {
		Log.i(TAG, "updateStatus: " + newStatus);
		if (status == STATUS_ERROR && newStatus == STATUS_STOPPED)
		{
			Log.i(TAG, "  ignoring new Status Stopped because in error state");
			return;
		}
		status = newStatus;

		switch (status) {
		case STATUS_BUFFERING:
			Log.i(TAG, "  Buffering");
			showStatus.setText("Buffering");
			break;
		case STATUS_PLAY:
			if (mp.isPlaying()) {
				Log.i(TAG, "  Playing");
				showStatus.setText("Playing");
			} else {
				Log.i(TAG, "  Pause");
				showStatus.setText("Pause");
			}
			break;
		case STATUS_STOPPED:
			Log.i(TAG, "  Finished");
			showStatus.setText("Finished");
			break;
		case STATUS_ERROR:
			Log.w(TAG, "Error Status: " + error);
			showStatus.setText("Error: " + error);
			break;
		}
	}

	private void play(String url) {
		try {
			Log.i(TAG, "playing " + url);
			total = 0;
			mp.reset();
			mp.setDataSource(url);
			mp.setOnPreparedListener(new OnPreparedListener() {
				public void onPrepared(MediaPlayer mp) {
					start();
					total = mp.getDuration();
					dismissDialog(DLG_WAIT);
					updateProgress();
				}
			});
			updateProgress();
			updateStatus(STATUS_BUFFERING);
			showDialog(DLG_WAIT);
			mp.prepareAsync();
		} catch (Exception e) {
			Log.e(TAG, "Error during play(url)", e);
			Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	private void start() {
		Log.i(TAG, "start");
		mp.start();
		updateStatus(STATUS_PLAY);
		updateProgress();
		// restore
		if (lastPosition != 0) {
			Log.i(TAG, "seeking to last position " + lastPosition);
			mp.seekTo(lastPosition);
		}
	}

	private void pause() {
		mp.pause();
		updateStatus(STATUS_PLAY);
	}

	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		progBar.setSecondaryProgress(percent);
	}

	public boolean onError(MediaPlayer mp, int what, int extra) {
		String error;
		switch (what) {
		case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
			error = "Connection to server was broken";
			break;
		default:
			error = "Unknown error during playback";
			break;
		}
		Log.i(TAG, "onError: " + error);
		// Re-init player, stop/play etc. does not help
		initPlayer(); 
		updateStatus(error);
		try {
			dismissDialog(DLG_WAIT);
		} catch (Exception e) {
			// I don't want to track if I opened the dialog or not
		}
		Toast.makeText(this, error, Toast.LENGTH_LONG);
		return false;
	}

	@Override
	public void onSeekComplete(MediaPlayer mp) {
		Log.i(TAG, "onSeekComplete");
		updateProgress();
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		Log.i(TAG, "onCompletion");
		updateStatus(STATUS_STOPPED);
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		// nothing interesting
		// MEDIA_INFO_UNKNOWN
		// MEDIA_INFO_VIDEO_TRACK_LAGGING
		// MEDIA_INFO_BAD_INTERLEAVING
		// MEDIA_INFO_NOT_SEEKABLE
		// MEDIA_INFO_METADATA_UPDATE
		return false;
	}

	private Handler handler = new Handler();
	private Runnable handlerAction = new Runnable() {
		public void run() {
			updateProgress();
		}
	};
	
	class UpdateTimeTask extends TimerTask {
		public void run() {
			handler.post(handlerAction);
		}
	}

	private void createUpdateTimer() {
		if (_updateTimer == null) {
			_updateTimer = new Timer();
			_updateTimer.schedule(new UpdateTimeTask(), 1000, 1000);
		}
	}
}