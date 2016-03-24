package ua.com.prologistic.musicplayerapp;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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

public class MainActivity extends AppCompatActivity implements MusicService.MediaFileListener {

    public static final PlayerUtils PlayerController = PlayerUtils.getInstance();

    public static final int PICK_FOLDER_REQUEST = 8;
    private MediaFileAdapter mediaFileAdapter = null;
    private TextView playingSong;
    private Button btnPause, btnPlay, btnNext, btnPrevious;
    private Button btnStop;
    private LinearLayout linearLayoutPlayingSong;
    private ListView mediaListView;
    private SeekBar seekbar;
    private TextView textBufferDuration, textDuration;
    private ImageView imageViewAlbumArt;
    private Context context;

    MusicService mService;
    boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = MainActivity.this;
        init();

        if (!PlayerUtils.isServiceRunning(MusicService.class.getName(), getApplicationContext())) {
            Intent i = new Intent(getApplicationContext(), MusicService.class);
            startService(i);
        }
    }

    private void init() {
        initViews();
        setListeners();
        playingSong.setSelected(true);
        seekbar.getProgressDrawable().setColorFilter(ContextCompat.getColor(context, R.color.colorText), PorterDuff.Mode.SRC_IN);
        if (PlayerController.SONGS_LIST.size() <= 0) {
            PlayerController.SONGS_LIST = PlayerController.listOfSongs(getApplicationContext());
        }
        initAdapter();
    }

    private void initAdapter() {
        mediaFileAdapter = new MediaFileAdapter(this, R.layout.custom_list, PlayerController.SONGS_LIST);
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

    // defines callbacks for service binding, passed to bindService()
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            mService = binder.getService();
            mService.setListener(MainActivity.this);
            mBound = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            mService = null;

        }
    };


    private void setListeners() {
        mediaListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View item, int position, long id) {
                PlayerController.SONG_PAUSED = false;
                PlayerController.SONG_NUMBER = position;
                boolean isServiceRunning = PlayerUtils.isServiceRunning(MusicService.class.getName(), getApplicationContext());
                if (!isServiceRunning) {
                    Intent i = new Intent(getApplicationContext(), MusicService.class);
                    startService(i);
                } else {
                    PlayerController.SONG_CHANGE_HANDLER.sendMessage(
                            PlayerController.SONG_CHANGE_HANDLER.obtainMessage());
                }
                updateUI();
                changeButton();
            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayerController.playControl(getApplicationContext());
            }
        });
        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayerController.pauseControl(getApplicationContext());
            }
        });
        btnNext.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                v.startAnimation(setButtonAnimation());
                PlayerController.nextControl(getApplicationContext());
            }
        });
        btnPrevious.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                v.startAnimation(setButtonAnimation());
                PlayerController.previousControl(getApplicationContext());
            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PlayerUtils.isServiceRunning(MusicService.class.getName(), getApplicationContext())) {
                    PlayerController.pauseControl(getApplicationContext());
                }
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
                    int onePercentOfSongDuration = (mService.getDuration() / 100);
                    // associate song duration with clicked position on the seekBar
                    int changedPosition = currentSeekBarPosition * onePercentOfSongDuration;

                    // seek to selected position
                    PlayerController.seekToAnyControl(getApplicationContext(), changedPosition);
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
    protected void onPause() {
        if (mBound) {
            unbindService(mConnection);
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent i = new Intent(getApplicationContext(), MusicService.class);
        bindService(i, mConnection, Context.BIND_AUTO_CREATE);

        handleAudioFromOpenWithPopup();

        boolean isServiceRunning = PlayerUtils.isServiceRunning(MusicService.class.getName(), getApplicationContext());
        if (isServiceRunning) {
            updateUI();
        } else {
            linearLayoutPlayingSong.setVisibility(View.GONE);
        }
        changeButton();
        PlayerController.SEEKBAR_HANDLER = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                Integer i[] = (Integer[]) msg.obj;
                textBufferDuration.setText(PlayerUtils.getDuration(i[0]));
                textDuration.setText(PlayerUtils.getDuration(i[1]));
                seekbar.setProgress(i[2]);
            }
        };
    }

    public void updateUI() {
        MediaFile mediaFile = PlayerController.SONGS_LIST.get(PlayerController.SONG_NUMBER);
        String nowPlayingTitleText = mediaFile.getTitle() + " " + mediaFile.getArtist() + "-" + mediaFile.getAlbum();
        playingSong.setText(nowPlayingTitleText);
        Bitmap albumArt = PlayerController.getAlbumart(context, mediaFile.getAlbumId());
        if (albumArt != null) {
            imageViewAlbumArt.setBackgroundDrawable(new BitmapDrawable(Resources.getSystem(), albumArt));
        } else {
            Bitmap defaultAlbumArt = PlayerController.getDefaultAlbumArt(context);
            imageViewAlbumArt.setBackgroundDrawable(new BitmapDrawable(Resources.getSystem(), defaultAlbumArt));
        }
        linearLayoutPlayingSong.setVisibility(View.VISIBLE);
    }

    public void changeButton() {
        if (PlayerController.SONG_PAUSED) {
            btnPause.setVisibility(View.GONE);
            btnPlay.setVisibility(View.VISIBLE);
        } else {
            btnPause.setVisibility(View.VISIBLE);
            btnPlay.setVisibility(View.GONE);
        }
    }

    public void changeUI() {
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
            PlayerController.SONGS_LIST = PlayerController.listOfSongs(getApplicationContext());
            initAdapter();

            return true;
        }

        // search songs by title
        if (id == R.id.action_search_title) {
            PlayerController.SEARCH_TYPE = "TITLE";

            return true;
        }
        // search songs by artist
        if (id == R.id.action_search_artist) {
            PlayerController.SEARCH_TYPE = "ARTIST";

            return true;
        }
        // search songs by album
        if (id == R.id.action_search_album) {
            PlayerController.SEARCH_TYPE = "ALBUM";

            return true;
        }

        if (id == R.id.action_sort_title) {
            MediaFileComparator.sortByTitle(PlayerController.SONGS_LIST);
            MediaFileAdapter mediaAdapter = (MediaFileAdapter) mediaListView.getAdapter();
            mediaAdapter.notifyDataSetChanged();

            return true;
        }
        if (id == R.id.action_sort_album) {
            MediaFileComparator.sortByAlbum(PlayerController.SONGS_LIST);
            MediaFileAdapter mediaAdapter = (MediaFileAdapter) mediaListView.getAdapter();
            mediaAdapter.notifyDataSetChanged();

            return true;
        }
        if (id == R.id.action_sort_artist) {
            MediaFileComparator.sortByArtist(PlayerController.SONGS_LIST);
            MediaFileAdapter mediaAdapter = (MediaFileAdapter) mediaListView.getAdapter();
            mediaAdapter.notifyDataSetChanged();

            return true;
        }
        if (id == R.id.action_sort_duration) {
            MediaFileComparator.sortByDuration(PlayerController.SONGS_LIST);
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
                List<MediaFile> songsFromSpecificFolder = PlayerController.getSongsFromSpecificFolder(
                        getApplicationContext(), new String[]{pickedFolder}
                );
                if (songsFromSpecificFolder.size() != 0) {
                    PlayerController.SONGS_LIST = songsFromSpecificFolder;
                    PlayerController.SONG_NUMBER = 0;
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
            PlayerController.playMusicFromActionPicker(this, intent.getData());
        }
    }

    // get last subfolder from path as WHERE arg
    private String getSelectionArg(String path) {
        String sTag = "%";
        // substring last directory in path
        String lastSubfolder = path.substring(path.lastIndexOf("/") + 1);
        return sTag + lastSubfolder + sTag;
    }

    @Override
    public void onMediaFileChanged() {
        changeUI();
    }

    @Override
    public void onPlayPauseActionChanged() {
        changeButton();
    }
}