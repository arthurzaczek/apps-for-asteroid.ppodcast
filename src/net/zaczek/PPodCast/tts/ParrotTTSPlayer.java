package net.zaczek.PPodCast.tts;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.parrot.hsti.EvtListener;
import com.parrot.hsti.HSTIInterface;
import com.parrot.hsti.SysListener;
import com.parrot.hsti.generated.ATParams.SIVR_RSTS_EVT_STATE;
import com.parrot.hsti.generated.Command;
import com.parrot.hsti.generated.Event;
import com.parrot.hsti.generated.Evt;
import com.parrot.hsti.generated.ATParams.SIVR_RPMT_PARAM_ACTION;
import com.parrot.hsti.generated.ATParams.SIVR_RPMT_PARAM_TYPE;
import com.parrot.hsti.router.GenericEvent;
import com.parrot.hsti.router.GenericSystemEvt;
import com.parrot.hsti.router.SystemEvt;

/**
 * Class providing a TTS player interface.
 * At the moment, it handles string length as Parrot TTS only properly deals with 150-character strings. This feature may be removed after further TTS' enhancements.
 * @author jlepocher
 */
public class ParrotTTSPlayer implements SysListener, EvtListener
{
	private static final String TAG = ParrotTTSPlayer.class.getSimpleName();
	private static final String TTS_FILE_DIRECTORY_PATH = "/tmp/ppodcast/tts";
	private static final String TTS_FILE_PATH = TTS_FILE_DIRECTORY_PATH + "/file.tts";
	private static final int	MSG_PLAY_MESSAGE	 = 0;
	private static final int	BEEP_NO				 = 0;

	private HSTIInterface		mHSTI				= null;
	private boolean 			mHSTIReady			= false;
	private boolean				mIsTTSProcessing 	= false;
	
	private ParrotTTSObserver mTTSObserver = null;
	
	private boolean mEnable = true;
    private boolean mAbort;
	
	protected Handler mHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
				case MSG_PLAY_MESSAGE:
					playMessage((String)msg.obj);
					break;
				default:
					break;
			}
		}
	};

	public ParrotTTSPlayer(Context ctx, ParrotTTSObserver observer)
	{
		mTTSObserver = observer;
		mHSTI = new HSTIInterface(TAG, ctx);
		mHSTI.startInternalInterface();
		registerEvents();
	}

	private void registerEvents()
	{
		if(mHSTI != null)
		{
			mHSTI.registerSystemEvt(SystemEvt.HSTISERVICE_HOSTCONNECTED, this);
			mHSTI.registerSystemEvt(SystemEvt.HSTISERVICE_HOSTDISCONNECTED, this);
			mHSTI.registerEvent(Evt.RSTS, this);
		}
	}
	
	public void destroy()
	{
		stop();
		unregisterEvents();
	}
	
	private void unregisterEvents()
	{
		if(mHSTI != null)
		{
			mHSTI.unregisterSystemEvt(SystemEvt.HSTISERVICE_HOSTCONNECTED, this);
			mHSTI.unregisterSystemEvt(SystemEvt.HSTISERVICE_HOSTDISCONNECTED, this);
			mHSTI.unregisterEvent(Evt.RSTS, this);
		}
	}

	/**
	 * Plays message in TTS. If other message already in play, it is stopped.
	 * @param message string message to be played
	 * @return true if message was played; false if HSTI is not ready.
	 */
	public boolean play(String message)
	{
		if(mHSTIReady)
		{
			if(mIsTTSProcessing) 
				stop();
			Message.obtain(mHandler, MSG_PLAY_MESSAGE, message).sendToTarget();
			mIsTTSProcessing = true;
			return true;
		}
		return false;
	}
	
	private void playMessage(String message)
	{
	    writeMessageInTmpFile(message);
	    sendAtCommand();
	}
	
	private void writeMessageInTmpFile(String message){
	    if ((message != null) && !"".equals(message))
        {
            File f = null;
            RandomAccessFile file = null;
            try 
            {
                f = new File(TTS_FILE_DIRECTORY_PATH);
                f.mkdirs();
                
                file = new RandomAccessFile(TTS_FILE_PATH, "rws");
                file.setLength(0);
                file.write(message.getBytes());
            } catch (FileNotFoundException e) 
            {
                e.printStackTrace();
            } catch (IOException e) 
            {
                e.printStackTrace();
            }
            
            if (null != file)
            {
                try 
                {
                    file.close();
                } catch (IOException e) 
                {
                    e.printStackTrace();
                }
            }
        }   
	}
	
	private void sendAtCommand(){
	    Command.RPMT_STR cmdRPMT = new Command.RPMT_STR(SIVR_RPMT_PARAM_ACTION.START, BEEP_NO, SIVR_RPMT_PARAM_TYPE.TTS_READ_FILE, TTS_FILE_PATH);
	    mHSTI.send(cmdRPMT);
	}

	/**
	 * Stops playing if playing anything at all.s
	 */
	public void stop()
	{
		if(mIsTTSProcessing)
		{
			Command.RPMT_STR cmdRPMT = new Command.RPMT_STR(SIVR_RPMT_PARAM_ACTION.STOP, BEEP_NO, SIVR_RPMT_PARAM_TYPE.TTS_READ_FILE, "");
			mHSTI.send(cmdRPMT);
		}
	}

	public void onSysEvt(GenericSystemEvt evt)
	{
		switch(evt.getId())
		{
		case SystemEvt.HSTISERVICE_HOSTCONNECTED:
			mHSTIReady = true;
			break;

		case SystemEvt.HSTISERVICE_HOSTDISCONNECTED:
			mHSTIReady = false;
			break;
		}
	}

	public void onEvt(GenericEvent evt)
	{
		if(evt.getId() == Evt.RSTS && ((Event.RSTS)evt).Status.equals(SIVR_RSTS_EVT_STATE.PROMPT_IDLE))
		{
			mIsTTSProcessing = false;
			if(mTTSObserver != null && mEnable)
			{
			    if(mAbort)
			    {
			        mTTSObserver.onTTSAborted();
			    }
			    else
			    {
			        mTTSObserver.onTTSFinished(); 
			    }
			}
			
	         mAbort = false;


		}
	}
}
