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
import android.util.Log;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.List;

import ua.com.prologistic.musicplayerapp.R;
import ua.com.prologistic.musicplayerapp.service.MusicService;

/**
 * Created by Andriy Gulak on 17.03.2016.
 */

public class PlayerUtils {
    //List of Songs
    public static List<MediaFile> SONGS_LIST = new ArrayList<>();
    //song number which is playing right now from SONGS_LIST
    public static int SONG_NUMBER = 0;
    //song is playing or paused
    public static boolean SONG_PAUSED = true;

    // default value for search type
    public static String SEARCH_TYPE = "TITLE";

    //handler for song changed(next, previous) defined in service(SongService)
    public static Handler SONG_CHANGE_HANDLER;

    //handler for song play/pause defined in SongService
    public static Handler PLAY_PAUSE_HANDLER;

    //handler for showing song progress defined in MainActivity
    public static Handler SEEKBAR_HANDLER;

    public static void playControl(Context context) {
        sendMessage(context.getResources().getString(R.string.play));
    }

    public static void pauseControl(Context context) {
        sendMessage(context.getResources().getString(R.string.pause));
    }

    public static void playMusicFromActionPicker(Context context, Uri uri) {
        MusicService.playMusicFromUri(context, uri);
    }

    public static void nextControl(Context context) {
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

    public static void seekToAnyControl(Context context, int position) {
        boolean isRunning = isServiceRunning(MusicService.class.getName(), context);
        if (!isRunning)
            return;
        MusicService.seekToAnyDirection(position);
    }

    public static void previousControl(Context context) {
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

    public static void sendMessage(String message) {
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
    public static List<MediaFile> listOfSongs(Context context) {

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
    public static List<MediaFile> getSongsFromSpecificFolder(Context context, String[] where) {

        Cursor c = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                MediaStore.Audio.Media.DATA + " like ? ",
                where,
                null
        );

        return fetchDataFromCursor(c);
    }


    @NonNull
    private static List<MediaFile> fetchDataFromCursor(Cursor c) {
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
        Log.d(PlayerUtils.class.getName(), "list size: " + listOfSongs.size());
        return listOfSongs;
    }

    // get the album image by albumId
    public static Bitmap getAlbumart(Context context, Long album_id) {
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

    public static Bitmap getDefaultAlbumArt(Context context) {
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
