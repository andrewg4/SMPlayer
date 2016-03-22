package ua.com.prologistic.musicplayerapp;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;

import ua.com.prologistic.musicplayerapp.adapter.MediaFileAdapter;
import ua.com.prologistic.musicplayerapp.player.MediaFile;
import ua.com.prologistic.musicplayerapp.player.PlayerUtils;
import ua.com.prologistic.musicplayerapp.service.MusicService;
import ua.com.prologistic.musicplayerapp.utils.MediaFileComparator;

/**
 * Created by Andriy Gulak on 17.03.2016.
 */

public class MainActivity extends AppCompatActivity {

    public static final int PICK_FOLDER_REQUEST = 8;
    private MediaFileAdapter mediaFileAdapter = null;
    private static TextView playingSong;
    private static Button btnPause, btnPlay, btnNext, btnPrevious;
    private Button btnStop;
    private static LinearLayout linearLayoutPlayingSong;
    private ListView mediaListView;
    private SeekBar seekbar;
    private TextView textBufferDuration, textDuration;
    private static ImageView imageViewAlbumArt;
    private static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = MainActivity.this;
        init();
        handleAudioFromOpenWithPopup();
    }

    private void init() {
        initViews();
        setListeners();
        playingSong.setSelected(true);
        seekbar.getProgressDrawable().setColorFilter(ContextCompat.getColor(context, R.color.colorText), PorterDuff.Mode.SRC_IN);
        if (PlayerUtils.SONGS_LIST.size() <= 0) {
            PlayerUtils.SONGS_LIST = PlayerUtils.listOfSongs(getApplicationContext());
        }
        initAdapter();
    }

    private void initAdapter() {
        mediaFileAdapter = new MediaFileAdapter(this, R.layout.custom_list, PlayerUtils.SONGS_LIST);
        mediaListView.setAdapter(mediaFileAdapter);
        mediaListView.setTextFilterEnabled(true);
        mediaListView.setFastScrollEnabled(true);
    }

    private void initViews() {
        playingSong = (TextView) findViewById(R.id.textNowPlaying);
        mediaListView = (ListView) findViewById(R.id.listViewMusic);
        btnPause = (Button) findViewById(R.id.btnPause);
        btnPlay = (Button) findViewById(R.id.btnPlay);
        linearLayoutPlayingSong = (LinearLayout) findViewById(R.id.linearLayoutPlayingSong);
        seekbar = (SeekBar) findViewById(R.id.seekBar);
        btnStop = (Button) findViewById(R.id.btnStop);
        textBufferDuration = (TextView) findViewById(R.id.textBufferDuration);
        textDuration = (TextView) findViewById(R.id.textDuration);
        imageViewAlbumArt = (ImageView) findViewById(R.id.imageViewAlbumArt);
        btnNext = (Button) findViewById(R.id.btnNext);
        btnPrevious = (Button) findViewById(R.id.btnPrevious);
    }

    private Animation setButtonAnimation() {
        return AnimationUtils.loadAnimation(this, R.anim.button_anim);
    }

    private void setListeners() {
        mediaListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View item, int position, long id) {
                PlayerUtils.SONG_PAUSED = false;
                PlayerUtils.SONG_NUMBER = position;
                boolean isServiceRunning = PlayerUtils.isServiceRunning(MusicService.class.getName(), getApplicationContext());
                if (!isServiceRunning) {
                    Intent i = new Intent(getApplicationContext(), MusicService.class);
                    startService(i);
                } else {
                    PlayerUtils.SONG_CHANGE_HANDLER.sendMessage(PlayerUtils.SONG_CHANGE_HANDLER.obtainMessage());
                }
                updateUI();
                changeButton();
            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayerUtils.playControl(getApplicationContext());
            }
        });
        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayerUtils.pauseControl(getApplicationContext());
            }
        });
        btnNext.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                v.startAnimation(setButtonAnimation());
                PlayerUtils.nextControl(getApplicationContext());
            }
        });
        btnPrevious.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                v.startAnimation(setButtonAnimation());
                PlayerUtils.previousControl(getApplicationContext());
            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), MusicService.class);
                stopService(i);
                linearLayoutPlayingSong.setVisibility(View.GONE);
            }
        });

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // clicked position on the seekBar
                    int currentSeekBarPosition = seekBar.getProgress();
                    // 1% of song duration = song duration / 100%
                    int onePercentOfSongDuration = (MusicService.getMp().getDuration() / 100);
                    // associate song duration with clicked position on the seekBar
                    int changedPosition = currentSeekBarPosition * onePercentOfSongDuration;

                    // seek to selected position
                    PlayerUtils.seekToAnyControl(getApplicationContext(), changedPosition);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean isServiceRunning = PlayerUtils.isServiceRunning(MusicService.class.getName(), getApplicationContext());
        if (isServiceRunning) {
            updateUI();
        } else {
            linearLayoutPlayingSong.setVisibility(View.GONE);
        }
        changeButton();
        PlayerUtils.SEEKBAR_HANDLER = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                Integer i[] = (Integer[]) msg.obj;
                textBufferDuration.setText(PlayerUtils.getDuration(i[0]));
                textDuration.setText(PlayerUtils.getDuration(i[1]));
                seekbar.setProgress(i[2]);
            }
        };
    }

    public static void updateUI() {
        MediaFile mediaFile = PlayerUtils.SONGS_LIST.get(PlayerUtils.SONG_NUMBER);
        String nowPlayingTitleText = mediaFile.getTitle() + " " + mediaFile.getArtist() + "-" + mediaFile.getAlbum();
        playingSong.setText(nowPlayingTitleText);
        Bitmap albumArt = PlayerUtils.getAlbumart(context, mediaFile.getAlbumId());
        if (albumArt != null) {
            imageViewAlbumArt.setBackgroundDrawable(new BitmapDrawable(Resources.getSystem(), albumArt));
        } else {
            Bitmap defaultAlbumArt = PlayerUtils.getDefaultAlbumArt(context);
            imageViewAlbumArt.setBackgroundDrawable(new BitmapDrawable(Resources.getSystem(), defaultAlbumArt));
        }
        linearLayoutPlayingSong.setVisibility(View.VISIBLE);
    }

    public static void changeButton() {
        if (PlayerUtils.SONG_PAUSED) {
            btnPause.setVisibility(View.GONE);
            btnPlay.setVisibility(View.VISIBLE);
        } else {
            btnPause.setVisibility(View.VISIBLE);
            btnPlay.setVisibility(View.GONE);
        }
    }

    public static void changeUI() {
        updateUI();
        changeButton();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        // init menu items for search
        MenuItem itemSearchTitle = menu.findItem(R.id.action_search_title);
        MenuItem itemSearchAlbum = menu.findItem(R.id.action_search_album);
        MenuItem itemSearchArtist = menu.findItem(R.id.action_search_artist);

        SearchView searchViewTitle = (SearchView) itemSearchTitle.getActionView();
        SearchView searchViewAlbum = (SearchView) itemSearchAlbum.getActionView();
        SearchView searchViewArtist = (SearchView) itemSearchArtist.getActionView();

        searchViewTitle.setFocusable(true);
        searchViewAlbum.setFocusable(true);
        searchViewArtist.setFocusable(true);

        // init searchable info for each menu item
        searchViewTitle.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchViewTitle.setQueryHint("title...");
        searchViewTitle.setIconified(true);

        searchViewAlbum.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchViewAlbum.setQueryHint("album...");
        searchViewAlbum.setIconified(true);

        searchViewArtist.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchViewArtist.setQueryHint("artist...");
        searchViewArtist.setIconified(true);

        // set textChangeListeners for each type of search
        final SearchView.OnQueryTextListener textChangeListenerTitle = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                if (!TextUtils.isEmpty(newText)) {
                    mediaFileAdapter.getFilter().filter(newText);
                }
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }
        };

        SearchView.OnQueryTextListener textChangeListenerAlbum = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                if (!TextUtils.isEmpty(newText)) {
                    mediaFileAdapter.getFilter().filter(newText);
                }
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }
        };

        SearchView.OnQueryTextListener textChangeListenerArtist = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                if (!TextUtils.isEmpty(newText)) {
                    mediaFileAdapter.getFilter().filter(newText);
                }
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }
        };

        // apply listeners to searchViews
        searchViewTitle.setOnQueryTextListener(textChangeListenerTitle);
        searchViewAlbum.setOnQueryTextListener(textChangeListenerAlbum);
        searchViewArtist.setOnQueryTextListener(textChangeListenerArtist);

        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        // get music from specific folder
        if (id == R.id.folder_music) {
            Intent intent = new Intent(this, FolderPickerActivity.class);
            startActivityForResult(intent, PICK_FOLDER_REQUEST);
            return true;
        }
        // fetch all music from device
        if (id == R.id.all_music) {
            PlayerUtils.SONGS_LIST = PlayerUtils.listOfSongs(getApplicationContext());
            initAdapter();

            return true;
        }

        // search songs by title
        if (id == R.id.action_search_title) {
            PlayerUtils.SEARCH_TYPE = "TITLE";

            return true;
        }
        // search songs by artist
        if (id == R.id.action_search_artist) {
            PlayerUtils.SEARCH_TYPE = "ARTIST";

            return true;
        }
        // search songs by album
        if (id == R.id.action_search_album) {
            PlayerUtils.SEARCH_TYPE = "ALBUM";

            return true;
        }

        if (id == R.id.action_sort_title) {
            MediaFileComparator.sortByTitle(PlayerUtils.SONGS_LIST);
            MediaFileAdapter mediaAdapter = (MediaFileAdapter) mediaListView.getAdapter();
            mediaAdapter.notifyDataSetChanged();

            return true;
        }
        if (id == R.id.action_sort_album) {
            MediaFileComparator.sortByAlbum(PlayerUtils.SONGS_LIST);
            MediaFileAdapter mediaAdapter = (MediaFileAdapter) mediaListView.getAdapter();
            mediaAdapter.notifyDataSetChanged();

            return true;
        }
        if (id == R.id.action_sort_artist) {
            MediaFileComparator.sortByArtist(PlayerUtils.SONGS_LIST);
            MediaFileAdapter mediaAdapter = (MediaFileAdapter) mediaListView.getAdapter();
            mediaAdapter.notifyDataSetChanged();

            return true;
        }
        if (id == R.id.action_sort_duration) {
            MediaFileComparator.sortByDuration(PlayerUtils.SONGS_LIST);
            MediaFileAdapter mediaAdapter = (MediaFileAdapter) mediaListView.getAdapter();
            mediaAdapter.notifyDataSetChanged();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {

            if (requestCode == PICK_FOLDER_REQUEST) {

                String pickedFolder = data.getStringExtra("pickedFolder");
                Log.i("PICKER_FOLDER_TAG", pickedFolder);

                // get last subfolder from path as WHERE arg
                pickedFolder = getSelectionArg(pickedFolder);
                Log.i("PICKER_FOLDER_WHERE_TAG", pickedFolder);

                // reinit music list and adapter with data from specific folder
                List<MediaFile> songsFromSpecificFolder = PlayerUtils.getSongsFromSpecificFolder(
                        getApplicationContext(), new String[]{pickedFolder}
                );
                if (songsFromSpecificFolder.size() != 0) {
                    PlayerUtils.SONGS_LIST = songsFromSpecificFolder;
                    PlayerUtils.SONG_NUMBER = 0;
                    initAdapter();
                }
            }
        }
    }

    // receive audio action from 'Complete action with' pop-up
    private void handleAudioFromOpenWithPopup() {
        // receive Intent
        Intent intent = getIntent();
        // check action
        String action = intent.getAction();
        // if it's audio action view
        if (action != null && action.equals("android.intent.action.VIEW")) {
            // play song
            PlayerUtils.playMusicFromActionPicker(this, intent.getData());
        }
    }

    // get last subfolder from path as WHERE arg
    private String getSelectionArg(String path) {
        String sTag = "%";
        // substring last directory in path
        String lastSubfolder = path.substring(path.lastIndexOf("/") + 1);
        return sTag + lastSubfolder + sTag;
    }

}