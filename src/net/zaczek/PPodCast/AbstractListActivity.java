package net.zaczek.PPodCast;

import net.zaczek.PPodCast.tts.ParrotTTSObserver;
import net.zaczek.PPodCast.tts.ParrotTTSPlayer;
import android.app.ListActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.AdapterView;
import android.widget.ListView;

public class AbstractListActivity extends ListActivity implements ParrotTTSObserver {
	protected ParrotTTSPlayer mTTSPlayer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mTTSPlayer = new ParrotTTSPlayer(this, this);
	}
	
	@Override
	protected void onResume() {
		if (mTTSPlayer != null)
			mTTSPlayer.destroy();
		mTTSPlayer = new ParrotTTSPlayer(this, this);
		super.onResume();
	}

	@Override
	protected void onPause() {
		mTTSPlayer.destroy();
		super.onPause();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		final ListView lst = getListView(); 
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_RIGHT:
		case KeyEvent.KEYCODE_MEDIA_NEXT:
			if(lst.getSelectedItemPosition()  == AdapterView.INVALID_POSITION) {
				lst.setSelection(0);
			} else {
				lst.setSelection(lst.getSelectedItemPosition() + 1);
			}
			return true;
		case KeyEvent.KEYCODE_DPAD_LEFT:
		case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
			if(lst.getSelectedItemPosition()  == AdapterView.INVALID_POSITION) {
				lst.setSelection(lst.getCount() - 1);
			} else {
				lst.setSelection(lst.getSelectedItemPosition() - 1);
			}
			return true;
		default:
			return super.onKeyDown(keyCode, event);
		}
	}
	
	@Override
	public void onTTSFinished() {

	}

	@Override
	public void onTTSAborted() {

	}
}
