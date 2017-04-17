package com.lunchareas.divertio.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.lunchareas.divertio.fragments.CreatePlaylistDialog;
import com.lunchareas.divertio.fragments.DeletePlaylistDialog;
import com.lunchareas.divertio.adapters.PlaylistAdapter;
import com.lunchareas.divertio.models.PlaylistDBHandler;
import com.lunchareas.divertio.models.PlaylistData;
import com.lunchareas.divertio.R;

import java.util.ArrayList;
import java.util.List;

public class PlaylistActivity extends BaseActivity {

    private static final String TAG = PlaylistActivity.class.getName();

    public static final String PLAYLIST_NAME = "playlist_name";

    private int currentPosition;
    private List<PlaylistData> playlistInfoList;
    private ListView playlistView;

    public PlaylistActivity() {
        super(R.layout.activity_playlist);
    }

    @Override
    @SuppressLint("NewApi")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // playlist
        playlistInfoList = new ArrayList<>();
        playlistView = (ListView) findViewById(R.id.playlist_list);
        setPlaylistView();

        // current position is -1 because no playlist is playing
        currentPosition = -1;

        playlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Log.d(TAG, "Detected click in playlist item in list view, starting modifier.");
                Intent i = new Intent(view.getContext(), PlaylistManagerActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                i.putExtra(PLAYLIST_NAME, playlistInfoList.get(position).getPlaylistName());
                startActivity(i);
            }
        });
    }

    @Override
    protected void setDisplay() {
        setContentView(R.layout.activity_playlist);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.playlist_overflow_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "Detected that position " + item.getItemId() + " was selected.");
        switch (item.getItemId()) {
            case R.id.playlist_menu_create: {
                Log.d(TAG, "Starting new dialog - upload.");
                DialogFragment createPlaylistDialog = new CreatePlaylistDialog();
                createPlaylistDialog.show(getSupportFragmentManager(), "Upload");
                return true;
            }
            case R.id.playlist_menu_delete: {
                Log.d(TAG, "Starting new dialog - delete.");
                DialogFragment deletePlaylistDialog = new DeletePlaylistDialog();
                deletePlaylistDialog.show(getSupportFragmentManager(), "Delete");
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    // options for drawer menu
    @Override
    protected void selectMenuItem(int position) {
        Log.d(TAG, "Detected click on position " + position + ".");
        switch (position) {
            case 0: {
                Log.d(TAG, "Starting new activity - main.");
                Intent i = new Intent(this, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                break;
            }
            case 1: {
                Log.d(TAG, "No effect, on that activity.");
                break;
            }
            /*
            case 2: {
                Log.d(TAG, "Starting new activity - bluetooth.");
                Intent i = new Intent(this, BluetoothActivity.class);
                startActivity(i);
                break;
            }
            case 3: {
                Log.d(TAG, "Starting new activity - settings.");
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                break;
            }
            */
        }
    }

    public void setPlaylistView() {
        getPlaylistsForActivity();
        PlaylistAdapter playlistAdapter = new PlaylistAdapter(this, playlistInfoList);
        playlistView.setAdapter(playlistAdapter);
    }

    public List<PlaylistData> getPlaylistInfoList() {
        return this.playlistInfoList;
    }

    public void getPlaylistsForActivity() {

        // get database and playlist
        PlaylistDBHandler db = new PlaylistDBHandler(this);
        playlistInfoList = db.getPlaylistDataList();
    }
}
