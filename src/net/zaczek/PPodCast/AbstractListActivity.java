package net.zaczek.PPodCast;

import com.parrot.asteroid.Manager;
import com.parrot.asteroid.ManagerObserverInterface;
import com.parrot.asteroid.tts.TTSManager;
import com.parrot.asteroid.tts.TTSManagerFactory;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.AdapterView;
import android.widget.ListView;

public class AbstractListActivity extends ListActivity implements
		ManagerObserverInterface {
	protected TTSManager mTTS;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mTTS = TTSManagerFactory.getTTSManager(this);
		mTTS.addManagerObserver(this);

	}

	@Override
	protected void onDestroy() {
		if (mTTS != null) {
			mTTS.stop();
			mTTS.deleteManagerObserver(this);
		}
		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		final ListView lst = getListView();
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_RIGHT:
		case KeyEvent.KEYCODE_MEDIA_NEXT:
			if (lst.getSelectedItemPosition() == AdapterView.INVALID_POSITION) {
				lst.setSelection(0);
			} else {
				lst.setSelection(lst.getSelectedItemPosition() + 1);
			}
			return true;
		case KeyEvent.KEYCODE_DPAD_LEFT:
		case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
			if (lst.getSelectedItemPosition() == AdapterView.INVALID_POSITION) {
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
	public void onManagerReady(boolean arg0, Manager arg1) {

	}
}
