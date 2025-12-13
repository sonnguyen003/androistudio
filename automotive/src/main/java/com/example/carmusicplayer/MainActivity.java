package com.example.carmusicplayer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    // Repeat modes
    public static final int REPEAT_OFF = 0;
    public static final int REPEAT_ALL = 1;
    public static final int REPEAT_ONE = 2;

    private BottomNavigationView bottomNav;
    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private Handler handler = new Handler(Looper.getMainLooper());

    // Playback state
    private Song currentSong;
    private List<Song> currentPlaylist = new ArrayList<>();
    private int currentSongIndex = -1;
    private boolean isPlaying = false;
    private boolean isMuted = false;
    private int savedVolume = 70;
    private float currentVolume = 0.7f;
    private int repeatMode = REPEAT_OFF;

    // Fragments
    private HomeFragment homeFragment;
    private RadioFragment radioFragment;
    private FavoritesFragment favoritesFragment;
    private SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        
        initMiniPlayer();
        initFragments();
        setupBottomNav();
        checkPermissions();
    }

    // Mini player views
    private android.widget.LinearLayout globalMiniPlayer;
    private android.widget.ImageView ivMiniAlbum;
    private android.widget.TextView tvMiniTitle;
    private android.widget.TextView tvMiniArtist;
    private android.widget.ImageButton btnMiniPlayPause;
    private android.widget.ImageButton btnMiniShuffle;
    private android.widget.ImageButton btnMiniRepeat;
    private android.widget.ImageButton btnMiniMute;
    private android.widget.SeekBar seekBarMini;
    private android.widget.TextView tvMiniCurrentTime;
    private android.widget.TextView tvMiniTotalTime;
    private boolean isShuffleOn = false;

    private void initMiniPlayer() {
        globalMiniPlayer = findViewById(R.id.globalMiniPlayer);
        ivMiniAlbum = findViewById(R.id.ivMiniAlbum);
        tvMiniTitle = findViewById(R.id.tvMiniTitle);
        tvMiniArtist = findViewById(R.id.tvMiniArtist);
        btnMiniPlayPause = findViewById(R.id.btnMiniPlayPause);
        btnMiniShuffle = findViewById(R.id.btnMiniShuffle);
        btnMiniRepeat = findViewById(R.id.btnMiniRepeat);
        btnMiniMute = findViewById(R.id.btnMiniMute);
        seekBarMini = findViewById(R.id.seekBarMini);
        tvMiniCurrentTime = findViewById(R.id.tvMiniCurrentTime);
        tvMiniTotalTime = findViewById(R.id.tvMiniTotalTime);

        // Play/Pause
        btnMiniPlayPause.setOnClickListener(v -> togglePlayPause());

        // Shuffle
        btnMiniShuffle.setOnClickListener(v -> {
            isShuffleOn = !isShuffleOn;
            btnMiniShuffle.setColorFilter(isShuffleOn ? 0xFF4CAF50 : 0xFF808080);
            Toast.makeText(this, isShuffleOn ? "Shuffle ON" : "Shuffle OFF", Toast.LENGTH_SHORT).show();
        });

        // Repeat
        btnMiniRepeat.setOnClickListener(v -> {
            repeatMode = (repeatMode + 1) % 3;
            updateRepeatIcon();
        });

        // Mute
        btnMiniMute.setOnClickListener(v -> toggleMute());

        // Seek bar
        seekBarMini.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    seekTo(progress);
                    updateMiniTimeLabel(tvMiniCurrentTime, progress);
                }
            }
            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        });
    }

    private void updateRepeatIcon() {
        switch (repeatMode) {
            case REPEAT_OFF:
                btnMiniRepeat.setImageResource(R.drawable.ic_repeat);
                btnMiniRepeat.setColorFilter(0xFF808080);
                Toast.makeText(this, "Repeat OFF", Toast.LENGTH_SHORT).show();
                break;
            case REPEAT_ALL:
                btnMiniRepeat.setImageResource(R.drawable.ic_repeat);
                btnMiniRepeat.setColorFilter(0xFF4CAF50);
                Toast.makeText(this, "Repeat ALL", Toast.LENGTH_SHORT).show();
                break;
            case REPEAT_ONE:
                btnMiniRepeat.setImageResource(R.drawable.ic_repeat_one);
                btnMiniRepeat.setColorFilter(0xFF4CAF50);
                Toast.makeText(this, "Repeat ONE", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void initFragments() {
        homeFragment = new HomeFragment();
        radioFragment = new RadioFragment();
        favoritesFragment = new FavoritesFragment();
        settingsFragment = new SettingsFragment();
        
        // Add all fragments and hide all except home
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainer, settingsFragment, "settings").hide(settingsFragment)
                .add(R.id.fragmentContainer, favoritesFragment, "favorites").hide(favoritesFragment)
                .add(R.id.fragmentContainer, radioFragment, "radio").hide(radioFragment)
                .add(R.id.fragmentContainer, homeFragment, "home")
                .commit();
        
        activeFragment = homeFragment;
    }

    private Fragment activeFragment;

    private void setupBottomNav() {
        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();
            boolean showMiniPlayer = true;
            
            if (id == R.id.nav_home) {
                selectedFragment = homeFragment;
                showMiniPlayer = false; // Home has its own player
            } else if (id == R.id.nav_radio) {
                selectedFragment = radioFragment;
            } else if (id == R.id.nav_favorites) {
                selectedFragment = favoritesFragment;
            } else if (id == R.id.nav_settings) {
                selectedFragment = settingsFragment;
            }
            
            // Show/hide global mini player
            if (showMiniPlayer && currentSong != null) {
                globalMiniPlayer.setVisibility(android.view.View.VISIBLE);
                updateMiniPlayer();
            } else {
                globalMiniPlayer.setVisibility(android.view.View.GONE);
            }
            
            if (selectedFragment != null && selectedFragment != activeFragment) {
                getSupportFragmentManager().beginTransaction()
                        .hide(activeFragment)
                        .show(selectedFragment)
                        .commit();
                activeFragment = selectedFragment;
                return true;
            }
            return false;
        });
    }

    private void checkPermissions() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_AUDIO;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission denied. Some features may not work.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // ============= Playback Methods =============

    public void playSong(Song song, List<Song> playlist, int position) {
        currentSong = song;
        currentPlaylist = playlist;
        currentSongIndex = position;

        // Check if it's a demo song
        if (MusicScanner.isDemoSong(song)) {
            updateFragmentUI();
            Toast.makeText(this, "Demo mode - Add MP3 files to assets", Toast.LENGTH_SHORT).show();
            return;
        }

        // Release previous player
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        try {
            mediaPlayer = new MediaPlayer();
            
            // Check if song is from assets
            if (MusicScanner.isAssetSong(song)) {
                android.content.res.AssetFileDescriptor afd = getAssets().openFd(song.getPath());
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
            } else {
                mediaPlayer.setDataSource(this, Uri.parse(song.getPath()));
            }
            
            mediaPlayer.prepare();
            mediaPlayer.start();
            
            // Apply volume
            if (isMuted) {
                mediaPlayer.setVolume(0, 0);
            } else {
                mediaPlayer.setVolume(currentVolume, currentVolume);
            }
            
            isPlaying = true;
            updateFragmentUI();
            updateSeekBar();
            
            // Set completion listener
            mediaPlayer.setOnCompletionListener(mp -> onSongComplete());

        } catch (IOException e) {
            Toast.makeText(this, "Error playing song: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void playSongFromFragment(Song song) {
        // Create a single-song playlist
        List<Song> singleList = new ArrayList<>();
        singleList.add(song);
        playSong(song, singleList, 0);
    }

    public void playStreamUrl(String url) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            
            Toast.makeText(this, "Buffering...", Toast.LENGTH_SHORT).show();
            
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                if (isMuted) {
                    mp.setVolume(0, 0);
                } else {
                    mp.setVolume(currentVolume, currentVolume);
                }
                isPlaying = true;
                currentSong = new Song(0, "Online Stream", "Radio", "Stream", url, 0, 0);
                updateFragmentUI();
                Toast.makeText(this, "Playing stream", Toast.LENGTH_SHORT).show();
            });
            
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "Error playing stream", Toast.LENGTH_SHORT).show();
                return true;
            });

        } catch (IOException e) {
            Toast.makeText(this, "Invalid URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Play Deezer track with proper info
    public void playDeezerTrack(DeezerService.DeezerTrack track) {
        String url = track.getPreviewUrl();
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "No preview available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            
            Toast.makeText(this, "Buffering...", Toast.LENGTH_SHORT).show();
            
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                if (isMuted) {
                    mp.setVolume(0, 0);
                } else {
                    mp.setVolume(currentVolume, currentVolume);
                }
                isPlaying = true;
                // Create song with actual Deezer track info
                currentSong = new Song(
                    track.getId(), 
                    track.getTitle(), 
                    track.getArtist(), 
                    track.getAlbum(), 
                    url, 
                    track.getDuration() * 1000L, 
                    0
                );
                updateFragmentUI();
                updateSeekBar();
            });
            
            mediaPlayer.setOnCompletionListener(mp -> onSongComplete());
            
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "Error playing track", Toast.LENGTH_SHORT).show();
                return true;
            });

        } catch (IOException e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void onSongComplete() {
        switch (repeatMode) {
            case REPEAT_ONE:
                playSong(currentSong, currentPlaylist, currentSongIndex);
                break;
            case REPEAT_ALL:
                playNext();
                break;
            case REPEAT_OFF:
            default:
                if (currentSongIndex < currentPlaylist.size() - 1) {
                    playNext();
                } else {
                    isPlaying = false;
                    updateFragmentUI();
                }
                break;
        }
    }

    public void togglePlayPause() {
        if (mediaPlayer == null) {
            if (homeFragment != null && !homeFragment.getSongList().isEmpty()) {
                List<Song> songs = homeFragment.getSongList();
                playSong(songs.get(0), songs, 0);
            }
            return;
        }

        if (isPlaying) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
            updateSeekBar();
        }
        isPlaying = !isPlaying;
        updateFragmentUI();
    }

    public void playPrevious() {
        if (currentPlaylist.isEmpty()) return;
        
        int newIndex = currentSongIndex - 1;
        if (newIndex < 0) {
            newIndex = currentPlaylist.size() - 1;
        }
        playSong(currentPlaylist.get(newIndex), currentPlaylist, newIndex);
    }

    public void playNext() {
        if (currentPlaylist.isEmpty()) return;
        
        int newIndex = currentSongIndex + 1;
        if (newIndex >= currentPlaylist.size()) {
            newIndex = 0;
        }
        playSong(currentPlaylist.get(newIndex), currentPlaylist, newIndex);
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    public void setVolume(int percent) {
        savedVolume = percent;
        currentVolume = percent / 100f;
        isMuted = percent == 0;
        
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(currentVolume, currentVolume);
        }
        
        if (homeFragment != null) {
            homeFragment.updateMuteState(isMuted, percent);
        }
    }

    public void toggleMute() {
        isMuted = !isMuted;
        
        if (isMuted) {
            savedVolume = (int) (currentVolume * 100);
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(0, 0);
            }
            Toast.makeText(this, "Muted", Toast.LENGTH_SHORT).show();
        } else {
            if (savedVolume == 0) savedVolume = 70;
            currentVolume = savedVolume / 100f;
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(currentVolume, currentVolume);
            }
            Toast.makeText(this, "Volume: " + savedVolume + "%", Toast.LENGTH_SHORT).show();
        }
        
        if (homeFragment != null) {
            homeFragment.updateMuteState(isMuted, isMuted ? 0 : savedVolume);
        }
    }

    public void setRepeatMode(int mode) {
        this.repeatMode = mode;
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    private void updateSeekBar() {
        if (mediaPlayer != null && isPlaying) {
            int position = mediaPlayer.getCurrentPosition();
            if (homeFragment != null) {
                homeFragment.updateProgress(position);
            }
            // Also update mini player if visible
            if (globalMiniPlayer.getVisibility() == android.view.View.VISIBLE) {
                seekBarMini.setProgress(position);
                updateMiniTimeLabel(tvMiniCurrentTime, position);
            }
            handler.postDelayed(this::updateSeekBar, 1000);
        }
    }

    private void updateFragmentUI() {
        if (homeFragment != null && currentSong != null) {
            int duration = mediaPlayer != null ? mediaPlayer.getDuration() : (int) currentSong.getDuration();
            int position = mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
            homeFragment.updateNowPlaying(currentSong, isPlaying, position, duration);
            homeFragment.setCurrentPlaying(currentSongIndex);
        }
        // Also update mini player
        updateMiniPlayer();
    }

    private void updateMiniPlayer() {
        if (currentSong != null) {
            tvMiniTitle.setText(currentSong.getTitle());
            tvMiniArtist.setText(currentSong.getArtist());
            btnMiniPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
            btnMiniMute.setImageResource(isMuted ? R.drawable.ic_volume_off : R.drawable.ic_volume);
            
            int duration = mediaPlayer != null ? mediaPlayer.getDuration() : (int) currentSong.getDuration();
            int position = mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
            
            seekBarMini.setMax(duration);
            
            // Show mini player if not on Home tab
            if (activeFragment != homeFragment) {
                globalMiniPlayer.setVisibility(android.view.View.VISIBLE);
            }
            seekBarMini.setProgress(position);
            updateMiniTimeLabel(tvMiniCurrentTime, position);
            updateMiniTimeLabel(tvMiniTotalTime, duration);
        }
    }

    private void updateMiniTimeLabel(android.widget.TextView tv, int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / 1000) / 60;
        tv.setText(String.format("%d:%02d", minutes, seconds));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacksAndMessages(null);
    }
}
