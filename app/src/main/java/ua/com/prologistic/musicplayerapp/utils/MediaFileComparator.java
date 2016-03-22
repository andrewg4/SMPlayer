package ua.com.prologistic.musicplayerapp.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ua.com.prologistic.musicplayerapp.player.MediaFile;

/**
 * Created by Andrew on 20.03.2016.
 */
public class MediaFileComparator {

    private static TitleComparator titleComparator;
    private static ArtistComparator artistComparator;
    private static AlbumComparator albumComparator;
    private static DurationComparator durationComparator;

    public static void sortByTitle(List<MediaFile> songsList) {
        Collections.sort(songsList, getTitleComparator());
    }

    public static void sortByArtist(List<MediaFile> songsList) {
        Collections.sort(songsList, getArtistComparator());
    }

    public static void sortByAlbum(List<MediaFile> songsList) {
        Collections.sort(songsList, getAlbumComparator());
    }

    public static void sortByDuration(List<MediaFile> songsList) {
        Collections.sort(songsList, getDurationComparator());
    }

    public static Comparator<MediaFile> getTitleComparator() {
        if (titleComparator == null) {
            titleComparator = new TitleComparator();
        }
        return titleComparator;
    }

    public static Comparator<MediaFile> getArtistComparator() {
        if (artistComparator == null) {
            artistComparator = new ArtistComparator();
        }
        return artistComparator;
    }

    public static Comparator<MediaFile> getAlbumComparator() {
        if (albumComparator == null) {
            albumComparator = new AlbumComparator();
        }
        return albumComparator;
    }

    public static Comparator<MediaFile> getDurationComparator() {
        if (durationComparator == null) {
            durationComparator = new DurationComparator();
        }
        return durationComparator;
    }

    private static class TitleComparator implements Comparator<MediaFile> {
        @Override
        public int compare(MediaFile lhs, MediaFile rhs) {
            return lhs.getTitle().compareTo(rhs.getTitle());
        }
    }

    private static class ArtistComparator implements Comparator<MediaFile> {
        @Override
        public int compare(MediaFile lhs, MediaFile rhs) {
            return lhs.getArtist().compareTo(rhs.getArtist());
        }
    }

    private static class AlbumComparator implements Comparator<MediaFile> {
        @Override
        public int compare(MediaFile lhs, MediaFile rhs) {
            return lhs.getAlbum().compareTo(rhs.getAlbum());
        }
    }

    private static class DurationComparator implements Comparator<MediaFile> {
        @Override
        public int compare(MediaFile lhs, MediaFile rhs) {
            return (int) (rhs.getDuration() - lhs.getDuration());
        }
    }
}
