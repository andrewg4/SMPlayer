package ua.com.prologistic.musicplayerapp.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ua.com.prologistic.musicplayerapp.R;
import ua.com.prologistic.musicplayerapp.player.MediaFile;
import ua.com.prologistic.musicplayerapp.player.PlayerUtils;

/**
 * Created by Andriy Gulak on 17.03.2016.
 */

public class MediaFileAdapter extends ArrayAdapter<MediaFile> implements Filterable {

    List<MediaFile> arrayList;
    List<MediaFile> listOfSongs;
    Context context;
    LayoutInflater inflater;

    public MediaFileAdapter(Context context, int resource, List<MediaFile> listOfSongs) {
        super(context, resource, listOfSongs);

        this.listOfSongs = listOfSongs;
        this.context = context;
        inflater = LayoutInflater.from(context);
    }

    ViewHolder holder;

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.custom_list, parent, false);
            holder = new ViewHolder();
            holder.imageViewAlbumImage = (ImageView) convertView.findViewById(R.id.imageViewAlbumImage);
            holder.textViewSongName = (TextView) convertView.findViewById(R.id.textViewSongName);
            holder.textViewArtist = (TextView) convertView.findViewById(R.id.textViewArtist);
            holder.textViewDuration = (TextView) convertView.findViewById(R.id.textViewDuration);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Log.e("MEDIA_FILE_TAG", String.valueOf(listOfSongs.size()));
        Log.e("MEDIA_FILE_POS", String.valueOf(position));
        MediaFile mediaFile = listOfSongs.get(position);

        Bitmap albumArt = PlayerUtils.getInstance().getAlbumart(context, mediaFile.getAlbumId());
        if (albumArt != null) {
            holder.imageViewAlbumImage.setImageBitmap(albumArt);
        } else {
            holder.imageViewAlbumImage.setImageBitmap(PlayerUtils.getInstance().getDefaultAlbumArt(context));
        }
        holder.textViewSongName.setText(mediaFile.toString());
        String artistTitleText = mediaFile.getAlbum() + " - " + mediaFile.getArtist();
        holder.textViewArtist.setText(artistTitleText);
        holder.textViewDuration.setText(PlayerUtils.getDuration(mediaFile.getDuration()));

        return convertView;
    }

    private class ViewHolder {
        ImageView imageViewAlbumImage;
        TextView textViewSongName;
        TextView textViewArtist;
        TextView textViewDuration;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults result = new FilterResults();
                List<MediaFile> found = new ArrayList<>();
                if (arrayList == null) {
                    arrayList = new ArrayList<>(listOfSongs);
                }

                if (constraint == null || constraint.equals("") || constraint.length() == 0) {
                    result.values = arrayList;
                    result.count = arrayList.size();
                } else {
                    constraint = constraint.toString().toLowerCase();

                    // search songs by selected type
                    switch (PlayerUtils.getInstance().SEARCH_TYPE) {
                        case "TITLE":
                            for (MediaFile item : arrayList) {
                                if (item.getTitle().toLowerCase().startsWith(constraint.toString())) {
                                    found.add(item);
                                }
                            }
                            break;

                        case "ALBUM":
                            for (MediaFile item : arrayList) {
                                if (item.getAlbum().toLowerCase().startsWith(constraint.toString())) {
                                    found.add(item);
                                }
                            }
                            break;

                        case "ARTIST":
                            for (MediaFile item : arrayList) {
                                if (item.getArtist().toLowerCase().startsWith(constraint.toString())) {
                                    found.add(item);
                                }
                            }
                            break;
                    }
                    result.values = found;
                    result.count = found.size();
                }
                return result;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                clear();
                for (MediaFile item : (List<MediaFile>) results.values) {
                    add(item);
                }
                // notifies the data with new filtered values
                notifyDataSetChanged();
            }
        };
    }
}
