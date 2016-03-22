package ua.com.prologistic.musicplayerapp.service;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import ua.com.prologistic.musicplayerapp.MainActivity;
import ua.com.prologistic.musicplayerapp.R;
import ua.com.prologistic.musicplayerapp.player.MediaFile;
import ua.com.prologistic.musicplayerapp.player.PlayerUtils;

/**
 * Created by Andriy Gulak on 17.03.2016.
 */

public class MusicService extends Service implements AudioManager.OnAudioFocusChangeListener {

    private static MediaPlayer mp;

    // intercepts incoming and outgoing calls
    AudioManager mAudioManager;

    private static Timer timer;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        mp = new MediaPlayer();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        timer = new Timer();
        mp.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                PlayerUtils.nextControl(getApplicationContext());
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
                if (PlayerUtils.SEEKBAR_HANDLER != null) {
                    PlayerUtils.SEEKBAR_HANDLER.sendMessage(PlayerUtils.SEEKBAR_HANDLER.obtainMessage(0, i));
                } else {
                    mp.stop();
                    mp = null;
                    stopSelf();
                }
            }
        }
    };

    public static MediaPlayer getMp() {
        return mp;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (PlayerUtils.SONGS_LIST.size() <= 0) {
                PlayerUtils.SONGS_LIST = PlayerUtils.listOfSongs(getApplicationContext());
            }
            MediaFile data = PlayerUtils.SONGS_LIST.get(PlayerUtils.SONG_NUMBER);

            String songPath = data.getPath();
            playSong(songPath);

            PlayerUtils.SONG_CHANGE_HANDLER = new Handler(new Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    MediaFile data = PlayerUtils.SONGS_LIST.get(PlayerUtils.SONG_NUMBER);
                    String songPath = data.getPath();
                    try {
                        playSong(songPath);
                        MainActivity.changeUI();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return false;
                }
            });

            PlayerUtils.PLAY_PAUSE_HANDLER = new Handler(new Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    String message = (String) msg.obj;
                    if (mp == null)
                        return false;
                    if (message.equalsIgnoreCase(getResources().getString(R.string.play))) {
                        PlayerUtils.SONG_PAUSED = false;
                        mp.start();
                    } else if (message.equalsIgnoreCase(getResources().getString(R.string.pause))) {
                        PlayerUtils.SONG_PAUSED = true;
                        mp.pause();
                    }

                    MainActivity.changeButton();

                    Log.d("TAG", "TAG Pressed: " + message);
                    return false;
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
        return START_STICKY;
    }


    public static void seekToAnyDirection(int position) {
        mp.seekTo(position);
    }

    public static void playMusicFromUri(Context context, Uri uri) {
        mp = MediaPlayer.create(context, uri);
        mp.setLooping(false);
        mp.start();
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
            PlayerUtils.pauseControl(getApplicationContext());
        } else {
            //GAIN -> PLAY
            PlayerUtils.playControl(getApplicationContext());
        }
    }
}