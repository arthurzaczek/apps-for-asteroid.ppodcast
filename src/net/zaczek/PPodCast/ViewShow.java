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
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.KeyEvent;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ViewShow extends Activity implements OnErrorListener,
		OnBufferingUpdateListener, OnCompletionListener, OnInfoListener {

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
	private TextView showStatusBuffer;
	private ProgressBar progBar;
	private int bufferPercent;
	private Runnable _progressUpdater;

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
		showStatusBuffer = (TextView) findViewById(R.id.ViewShowStatusBuffer);
		progBar = (ProgressBar)findViewById(R.id.progBar);

		mp = new MediaPlayer();
		mp.setScreenOnWhilePlaying(true); // TODO: Remove wake lock
		am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

		mp.setOnErrorListener(this);
		mp.setOnBufferingUpdateListener(this);
		mp.setOnCompletionListener(this);
		mp.setOnInfoListener(this);
		
		progBar.setProgress(0);
		createProgressThread();
		updateStatus(STATUS_STOPPED);
		fillData();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_DOWN:
			am.adjustVolume(AudioManager.ADJUST_RAISE,
					AudioManager.FLAG_SHOW_UI);
			return true;
		case KeyEvent.KEYCODE_DPAD_UP:
			am.adjustVolume(AudioManager.ADJUST_LOWER,
					AudioManager.FLAG_SHOW_UI);
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
			if (status != STATUS_BUFFERING) {
				if (mp.isPlaying()) {
					pause();
				} else {
					start();
				}
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

	@Override
	protected void onResume() {
		super.onResume();
		wl.acquire();
	}

	@Override
	protected void onPause() {
		mp.stop();
		wl.release();
		status = STATUS_EXITING;
		super.onPause();
	}

	private void fillData() {
		Cursor showCursor = podcastDb.fetchShow(show);

		if (showCursor == null) {
			setResult(RESULT_CANCELED);
			finish();
		}

		startManagingCursor(showCursor);

		String title = showCursor.getString(showCursor
				.getColumnIndex(PuddleDbAdapter.SHOW_TITLE));
		showTitle.setText(title);

		String description = showCursor.getString(showCursor
				.getColumnIndex(PuddleDbAdapter.SHOW_DESCRIPTION));
		showDescription.setText(description);

		String url = showCursor.getString(showCursor
				.getColumnIndex(PuddleDbAdapter.SHOW_URL));
		play(url);
	}

	private void updateStatus(String error) {
		updateStatus(STATUS_ERROR, error);
	}

	private void updateStatus(int newStatus) {
		updateStatus(newStatus, "Unknown Error");
	}

	private void updateStatus(int newStatus, String error) {
		if (status == STATUS_ERROR && newStatus == STATUS_STOPPED)
			return;
		status = newStatus;

		switch (status) {
		case STATUS_BUFFERING:
			showStatus.setText("Buffering");
			break;
		case STATUS_PLAY:
			showStatus.setText("Playing");
			break;
		case STATUS_STOPPED:
			showStatus.setText("Stopped");
			break;
		case STATUS_ERROR:
			showStatus.setText(error);
			break;
		}
	}

	private void play(String url) {
		try {
			mp.setDataSource(url);
			mp.setOnPreparedListener(new OnPreparedListener() {
				public void onPrepared(MediaPlayer mp) {
					start();
					dismissDialog(DLG_WAIT);
				}
			});
			updateStatus(STATUS_BUFFERING);
			showDialog(DLG_WAIT);
			mp.prepareAsync();
		} catch (Exception e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	private void start() {
		mp.start();
		updateStatus(STATUS_PLAY);
	}

	private void pause() {
		mp.pause();
		updateStatus(STATUS_STOPPED);
	}

	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		bufferPercent = percent;
		progBar.setSecondaryProgress(percent);
		showStatusBuffer.setText(percent + "% Buf.");
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
		updateStatus(error);
		Toast.makeText(this, error, Toast.LENGTH_LONG);
		return false;
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
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
	
	private void createProgressThread() {

	    _progressUpdater = new Runnable() {

			@Override
	        public void run() {
	            while(status != STATUS_EXITING) {
	                if(mp.isPlaying()) {
	                    try
	                    {
	                        int current = 0;
	                        int total = mp.getDuration();

	                        while(mp.isPlaying() && current<total){
	                            try {
	                                Thread.sleep(1000);
	                                current = mp.getCurrentPosition();
	                                // Is this legal?
	                                progBar.setProgress(100 * current / total); 
	                            } catch (Exception e){

	                            }            
	                        }
	                    }
	                    catch(Exception e) {
	                    }
	                }
	            }
	        }
	    };
	    Thread thread = new Thread(_progressUpdater);
	    thread.start();
	}
}