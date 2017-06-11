package com.lunchareas.divertio.activities;


import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.lunchareas.divertio.models.SongDBHandler;
import com.lunchareas.divertio.models.SongData;

public class PlayMusicService extends Service {

    private static final String TAG = PlayMusicService.class.getName();

    public static final String MUSIC_RESULT = "REQUEST_PROCESSED";
    public static final String MUSIC_POSITION = "POSITION";
    public static final String MUSIC_DURATION = "DURATION";
    public static final String MUSIC_CURR = "CURR_SONG";

    public static final String MUSIC_CREATE = "CREATE";
    public static final String MUSIC_CHANGE = "CHANGE";
    public static final String MUSIC_PLAY = "START";
    public static final String MUSIC_PAUSE = "PAUSE";
    public static final String MUSIC_FINISH = "FINISH";
    public static final String PLAYLIST_CREATE = "PLAYLIST_CREATE";

    private Bundle intentCmd;
    private MediaPlayer mp = null;
    private int idx;

    // Set up broadcaster to activity to update progress bar
    private LocalBroadcastManager musicUpdater;
    private Thread musicUpdaterThread;
    private boolean musicReset;
    private String currSong;
    private SongData songData;

    // Media session stuff
    private MediaSession mediaSession;

    @Override
    public int onStartCommand(Intent workIntent, int flags, int startId) {

        // Get cmd
        if (workIntent != null) {
            intentCmd = workIntent.getExtras();
        }

        // Start media session if needed
        if (intentCmd.containsKey(MUSIC_CREATE)) {
            Log.d(TAG, "Creating new song.");
            initBroadcaster();
            initMedia();
        }

        // Start handler
        handleIntent();

        return START_STICKY;
    }

    private void initBroadcaster() {

        // Create the thread to update progress bar
        musicUpdater = LocalBroadcastManager.getInstance(this);
        musicUpdaterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int songPosition;
                int songDuration;
                do {

                    // Wait for reset to finish if necessary
                    while (musicReset) {
                        try {
                            Thread.sleep(200);
                            Log.d(TAG, "Pausing music updater!");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // Change duration if track changes
                    songPosition = mp.getCurrentPosition();
                    songDuration = mp.getDuration();
                    //Log.d(TAG, "Song service position: " + songPosition + "\nSong service duration: " + songDuration);

                    // Create and send intent with position and duration
                    Intent songIntent = new Intent(MUSIC_RESULT);
                    songIntent.putExtra(MUSIC_POSITION, songPosition);
                    songIntent.putExtra(MUSIC_DURATION, songDuration);
                    songIntent.putExtra(MUSIC_CURR, currSong);
                    musicUpdater.sendBroadcast(songIntent);

                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } while (mp != null && mp.getCurrentPosition() <= songDuration);
            }
        });
    }

    @SuppressLint("NewApi")
    private void handleIntent() {

        // Handle different events
        if (intentCmd != null) {
            if (intentCmd.containsKey(PlayMusicService.MUSIC_PLAY) && mp != null) {
                mediaSession.getController().getTransportControls().play();
            } else if (intentCmd.containsKey(PlayMusicService.MUSIC_PAUSE) && mp != null) {
                mediaSession.getController().getTransportControls().pause();
            } else if (intentCmd.containsKey(PlayMusicService.MUSIC_CHANGE) && mp != null) {
                mediaSession.getController().getTransportControls().seekTo(intentCmd.getInt(PlayMusicService.MUSIC_CHANGE));
            } else if (intentCmd.containsKey(PlayMusicService.PLAYLIST_CREATE)) {
                Log.d(TAG, "Beginning playlist queue!");
                String[] songPathList = intentCmd.getStringArray(PlayMusicService.PLAYLIST_CREATE);
                beginPlaylistQueue(songPathList);
            } else {
                //Log.e(TAG, "Command sent to PlayMusicService not found.");
                //System.out.println(intentCmd);
                if (intentCmd.isEmpty()) {
                    Log.e(TAG, "No command sent, bundle empty.");
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private void initMedia() {

        // Get song data
        currSong = intentCmd.getString(PlayMusicService.MUSIC_CREATE);
        songData = new SongDBHandler(this).getSongData(currSong);

        // Pause song if playing
        if (mp != null && mp.isPlaying()) {
            mp.pause();
        }
        initMusicPlayer();

        // Media session init
        mediaSession = new MediaSession(this, "MusicService");
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                mp.start();
                musicUpdaterThread.start();
                Log.d(TAG, "Play!");
            }

            @Override
            public void onPause() {
                if (mp.isPlaying()) {
                    mp.pause();
                }
                Log.d(TAG, "Pause!");
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
            }

            @Override
            public void onSeekTo(long pos) {
                super.onSeekTo(pos);
                mp.seekTo((int)pos);
                Log.d(TAG, "Seek!");
            }
        });

        // Start song
        mediaSession.getController().getTransportControls().play();
    }

    private void beginPlaylistQueue(final String[] songNameList) {

        // Get original song
        currSong = songNameList[0];
        songData = new SongDBHandler(this).getSongData(currSong);

        // Play the first song
        initMusicPlayer();
        mp.start();
        musicUpdaterThread.start();

        // Setup for completion
        idx = 1;
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if (idx < songNameList.length) {
                    try {

                        // Set new song
                        currSong = songNameList[idx];
                        songData = new SongDBHandler(getApplicationContext()).getSongData(currSong);

                        // Make sure the updater thread waits
                        musicReset = true;
                        mp.reset();
                        mp.setDataSource(songData.getSongPath());
                        mp.prepare();
                        mp.start();
                        musicReset = false;
                        musicUpdaterThread.start();

                        //musicUpdaterThread.start();
                        idx += 1;
                        Log.d(TAG, "Playing next song, number " + Integer.toString(idx));

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.d(TAG, "Finished playlist.");
                }
            }
        });
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "PlayMusicService destroyed...");
        mp.release();
        mp = null;
        musicUpdaterThread.interrupt();
    }

    public void initMusicPlayer() {
        mp = MediaPlayer.create(this, Uri.parse(songData.getSongPath()));
        if (mp != null) {
            mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }
    }
}