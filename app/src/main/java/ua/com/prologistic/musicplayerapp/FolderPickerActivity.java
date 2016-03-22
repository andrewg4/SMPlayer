package ua.com.prologistic.musicplayerapp;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Andrew on 19.03.2016.
 */
public class FolderPickerActivity extends ListActivity implements View.OnClickListener {

    private List<String> item = null;
    private List<String> path = null;
    private String root;
    private TextView myPath;
    private Button btnPick;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pickfolder);

        myPath = (TextView) findViewById(R.id.path);
        btnPick = (Button) findViewById(R.id.pick_btn);
        btnPick.setOnClickListener(this);
        root = Environment.getExternalStorageDirectory().getPath();

        getApprovedFolders(root);
    }

    // get all folders with music (audio files)
    private void getApprovedFolders(String dirPath) {
        myPath.setText(dirPath);
        item = new ArrayList<>();
        path = new ArrayList<>();
        File f = new File(dirPath);
        File[] files = f.listFiles();

        if (!dirPath.equals(root)) {
            item.add(root);
            path.add(root);
            item.add("../");
            path.add(f.getParent());
        }

        // get first folder
        for (File file : files) {
            // is it accessible?
            if (!file.isHidden() && file.canRead()) {
                if (file.isDirectory()) {
                    // if current folder contains any audio file
                    File[] okFiles = file.listFiles(new AudioFilesFilter());
                    // add it to the path
                    if (okFiles.length != 0) {
                        item.add(file.getName() + "/");
                        path.add(file.getPath());
                    }
                }
            }
        }

        ArrayAdapter<String> fileList = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, item);
        setListAdapter(fileList);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        File file = new File(path.get(position));

        // get sub folders
        if (file.isDirectory()) {
            if (file.canRead()) {
                getApprovedFolders(path.get(position));
            }
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        intent.putExtra("pickedFolder", myPath.getText().toString());
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    private class AudioFilesFilter implements FileFilter {
        // approved audio file extensions
        private final String[] okExtensions = new String[]{
                "mp3", "flac", "3ga", "avr", "aa", "amr", "m4a", "wav", "aac", "au", "aac"
        };

        public boolean accept(File file) {
            for (String extension : okExtensions) {
                if (file.getName().toLowerCase().endsWith(extension)) {
                    return true;
                }
            }
            return false;
        }
    }
}
