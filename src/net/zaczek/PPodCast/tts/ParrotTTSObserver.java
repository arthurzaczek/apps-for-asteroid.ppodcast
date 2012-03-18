package net.zaczek.PPodCast.tts;

public interface ParrotTTSObserver
{
    void onTTSFinished();
    void onTTSAborted();
}
