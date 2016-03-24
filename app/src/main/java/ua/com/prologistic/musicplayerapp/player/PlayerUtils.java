package ua.com.prologistic.musicplayerapp.player;


import android.app.ActivityManager;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.List;

import ua.com.prologistic.musicplayerapp.R;
import ua.com.prologistic.musicplayerapp.service.MusicService;

/**
 * Created by Andriy Gulak on 17.03.2016.
 */

public class PlayerUtils {

    private static PlayerUtils instance;

    private PlayerUtils() {}

    // PlayerUtils Singleton
    public static PlayerUtils getInstance() {
        if (instance == null) {
            instance = new PlayerUtils();
        }
        return instance;
    }

    MediaPlayerListener mediaPlayerListener;

    public interface MediaPlayerListener {
        void onSeekbarPositionChanged(int position);
        int getDuration();
        void playMusicFromUri(Context context, Uri uri);
    }
    public void setListener(MediaPlayerListener listener) {
        mediaPlayerListener = listener;
    }

    //List of Songs
    public List<MediaFile> SONGS_LIST = new ArrayList<>();
    //song number which is playing right now from SONGS_LIST
    public int SONG_NUMBER = 0;
    //song is playing or paused
    public boolean SONG_PAUSED = true;

    // default value for search type
    public String SEARCH_TYPE = "TITLE";

    //handler for song changed(next, previous) defined in service(MusicService)
    public Handler SONG_CHANGE_HANDLER;

    //handler for song play/pause defined in MusicService
    public Handler PLAY_PAUSE_HANDLER;

    //handler for showing song progress defined in MainActivity
    public Handler SEEKBAR_HANDLER;

    public void playControl(Context context) {
        sendMessage(context.getResources().getString(R.string.play));
    }

    public void pauseControl(Context context) {
        sendMessage(context.getResources().getString(R.string.pause));
    }

    public void playMusicFromActionPicker(Context context, Uri uri) {
        mediaPlayerListener.playMusicFromUri(context, uri);
    }

    public void nextControl(Context context) {
        boolean isRunning = isServiceRunning(MusicService.class.getName(), context);
        if (!isRunning)
            return;
        if (SONGS_LIST.size() > 0) {
            if (SONG_NUMBER < (SONGS_LIST.size() - 1)) {
                SONG_NUMBER++;
                SONG_CHANGE_HANDLER.sendMessage(SONG_CHANGE_HANDLER.obtainMessage());
            } else {
                SONG_NUMBER = 0;
                SONG_CHANGE_HANDLER.sendMessage(SONG_CHANGE_HANDLER.obtainMessage());
            }
        }
        SONG_PAUSED = false;
    }

    public void seekToAnyControl(Context context, int position) {
        boolean isRunning = isServiceRunning(MusicService.class.getName(), context);
        if (!isRunning)
            return;
        mediaPlayerListener.onSeekbarPositionChanged(position);
    }

    public void previousControl(Context context) {
        boolean isRunning = isServiceRunning(MusicService.class.getName(), context);
        if (!isRunning)
            return;
        if (SONGS_LIST.size() > 0) {
            if (SONG_NUMBER > 0) {
                SONG_NUMBER--;
                SONG_CHANGE_HANDLER.sendMessage(SONG_CHANGE_HANDLER.obtainMessage());
            } else {
                SONG_NUMBER = SONGS_LIST.size() - 1;
                SONG_CHANGE_HANDLER.sendMessage(SONG_CHANGE_HANDLER.obtainMessage());
            }
        }
        SONG_PAUSED = false;
    }

    public void sendMessage(String message) {
        PLAY_PAUSE_HANDLER.sendMessage(PLAY_PAUSE_HANDLER.obtainMessage(0, message));

    }

    // check if service is running or not
    public static boolean isServiceRunning(String serviceName, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    // get all songs from device
    public List<MediaFile> listOfSongs(Context context) {

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor c = context.getContentResolver().query(uri,
                null,
                MediaStore.Audio.Media.IS_MUSIC + " != 0",
                null,
                null
        );

        return fetchDataFromCursor(c);
    }


    // read all music from specific folder
    public List<MediaFile> getSongsFromSpecificFolder(Context context, String[] where) {

        Cursor c = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                MediaStore.Audio.Media.DATA + " like ? ",
                where,
                null
        );

        return fetchDataFromCursor(c);
    }


    @NonNull
    private List<MediaFile> fetchDataFromCursor(Cursor c) {
        List<MediaFile> listOfSongs = new ArrayList<>();
        c.moveToFirst();
        while (c.moveToNext()) {
            MediaFile mediaFile = new MediaFile();

            String title = c.getString(c.getColumnIndex(MediaStore.Audio.Media.TITLE));
            String artist = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            String album = c.getString(c.getColumnIndex(MediaStore.Audio.Media.ALBUM));
            long duration = c.getLong(c.getColumnIndex(MediaStore.Audio.Media.DURATION));
            String data = c.getString(c.getColumnIndex(MediaStore.Audio.Media.DATA));
            long albumId = c.getLong(c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
            String composer = c.getString(c.getColumnIndex(MediaStore.Audio.Media.COMPOSER));

            mediaFile.setTitle(title);
            mediaFile.setAlbum(album);
            mediaFile.setArtist(artist);
            mediaFile.setDuration(duration);
            mediaFile.setPath(data);
            mediaFile.setAlbumId(albumId);
            mediaFile.setComposer(composer);

            listOfSongs.add(mediaFile);
        }
        c.close();
        return listOfSongs;
    }

    // get the album image by albumId
    public Bitmap getAlbumart(Context context, Long album_id) {
        Bitmap bm = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        try {
            final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
            Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
            if (pfd != null) {
                FileDescriptor fd = pfd.getFileDescriptor();
                bm = BitmapFactory.decodeFileDescriptor(fd, null, options);
            }
        } catch (Exception ignored) {
        }
        return bm;
    }

    public Bitmap getDefaultAlbumArt(Context context) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        return BitmapFactory.decodeResource(context.getResources(), R.drawable.default_pic, options);
    }

    // millis to hh:mm:ss converter
    public static String getDuration(long milliseconds) {
        long sec = (milliseconds / 1000) % 60;
        long min = (milliseconds / (60 * 1000)) % 60;
        long hour = milliseconds / (60 * 60 * 1000);

        String s = (sec < 10) ? "0" + sec : "" + sec;
        String m = (min < 10) ? "0" + min : "" + min;
        String h = "" + hour;

        String time;
        if (hour > 0) {
            time = h + ":" + m + ":" + s;
        } else {
            time = m + ":" + s;
        }
        return time;
    }

}
