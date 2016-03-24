package ua.com.prologistic.musicplayerapp.service;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import ua.com.prologistic.musicplayerapp.R;
import ua.com.prologistic.musicplayerapp.player.MediaFile;
import ua.com.prologistic.musicplayerapp.player.PlayerUtils;

/**
 * Created by Andriy Gulak on 17.03.2016.
 */

public class MusicService extends Service implements AudioManager.OnAudioFocusChangeListener, PlayerUtils.MediaPlayerListener {

    public static final PlayerUtils PlayerController = PlayerUtils.getInstance();

    MediaFileListener mListener;

    public interface MediaFileListener {
        void onMediaFileChanged();
        void onPlayPauseActionChanged();
    }

    public void setListener(MediaFileListener listener) {
        mListener = listener;
    }

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private MediaPlayer mp;

    // intercepts incoming and outgoing calls
    AudioManager mAudioManager;

    private Timer timer;
    private boolean isOncreate = true;

    @Override
    public void onCreate() {
        mp = new MediaPlayer();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        PlayerController.setListener(this);

        timer = new Timer();
        mp.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                PlayerController.nextControl(getApplicationContext());
            }
        });
        super.onCreate();
    }

    private class MainTask extends TimerTask {
        public void run() {
            handler.sendEmptyMessage(0);
        }
    }

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mp != null) {
                int progress = (mp.getCurrentPosition() * 100) / mp.getDuration();
                Integer i[] = new Integer[3];
                i[0] = mp.getCurrentPosition();
                i[1] = mp.getDuration();
                i[2] = progress;
                if (PlayerController.SEEKBAR_HANDLER != null) {
                    PlayerController.SEEKBAR_HANDLER.sendMessage(PlayerController.SEEKBAR_HANDLER.obtainMessage(0, i));
                } else {
                    mp.stop();
                    mp = null;
                    stopSelf();
                }
            }
        }
    };

    @Override
    public void onSeekbarPositionChanged(int position) {
        mp.seekTo(position);
    }

    @Override
    public int getDuration() {
        return mp.getDuration();
    }

    @Override
    public void playMusicFromUri(Context context, Uri uri) {
        mp = MediaPlayer.create(context, uri);
        mp.setLooping(false);
        mp.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (PlayerController.SONGS_LIST.size() <= 0) {
                PlayerController.SONGS_LIST = PlayerController.listOfSongs(getApplicationContext());
            }
            MediaFile data = PlayerController.SONGS_LIST.get(PlayerController.SONG_NUMBER);

            String songPath = data.getPath();
            if (!isOncreate) {
                playSong(songPath);
                isOncreate = false;
            }

            PlayerController.SONG_CHANGE_HANDLER = new Handler(new Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    MediaFile data = PlayerController.SONGS_LIST.get(PlayerController.SONG_NUMBER);
                    String songPath = data.getPath();
                    try {
                        playSong(songPath);
                        mListener.onMediaFileChanged();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return false;
                }
            });

            PlayerController.PLAY_PAUSE_HANDLER = new Handler(new Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    String message = (String) msg.obj;
                    if (mp == null)
                        return false;
                    if (message.equalsIgnoreCase(getResources().getString(R.string.play))) {
                        PlayerController.SONG_PAUSED = false;
                        mp.start();
                    } else if (message.equalsIgnoreCase(getResources().getString(R.string.pause))) {
                        PlayerController.SONG_PAUSED = true;
                        mp.pause();
                    }
                    mListener.onPlayPauseActionChanged();
                    return false;
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mp != null) {
            mp.stop();
            mp = null;
        }
        mAudioManager.abandonAudioFocus(this);
        super.onDestroy();
    }

    private void playSong(String songPath) {
        try {
            mp.reset();
            mp.setDataSource(songPath);
            mp.prepare();
            mp.start();
            timer.scheduleAtFixedRate(new MainTask(), 0, 100);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange <= 0) {
            //LOSS -> PAUSE
            PlayerController.pauseControl(getApplicationContext());
        } else {
            //GAIN -> PLAY
            PlayerController.playControl(getApplicationContext());
        }
    }
}