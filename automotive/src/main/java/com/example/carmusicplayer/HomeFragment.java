package com.example.carmusicplayer;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Home Fragment - Library + Now Playing
 */
public class HomeFragment extends Fragment implements SongAdapter.OnSongClickListener {

    private static final int REPEAT_OFF = 0;
    private static final int REPEAT_ALL = 1;
    private static final int REPEAT_ONE = 2;

    // Views
    private LinearLayout libraryPanel;
    private ImageButton btnToggleLibrary;
    private ImageButton btnSearch;
    private EditText etSearch;
    private TextView tvSongCount;
    private RecyclerView rvSongs;
    
    private ImageView ivAlbumArt;
    private TextView tvSongTitle;
    private TextView tvArtist;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private SeekBar seekBar;
    private ImageButton btnPrevious;
    private ImageButton btnPlayPause;
    private ImageButton btnNext;
    private ImageButton btnShuffle;
    private ImageButton btnRepeat;
    private ImageButton btnMute;
    private SeekBar seekBarVolume;
    private TextView tvVolumePercent;

    // Data
    private List<Song> allSongs = new ArrayList<>();
    private List<Song> songList = new ArrayList<>();
    private SongAdapter songAdapter;
    private int currentSongIndex = -1;
    
    private boolean isLibraryVisible = true;
    private boolean isSearchVisible = false;
    private boolean isShuffleOn = false;
    private int repeatMode = REPEAT_OFF;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        initViews(view);
        setupListeners();
        loadSongs();
        
        return view;
    }

    private void initViews(View view) {
        // Library Panel
        libraryPanel = view.findViewById(R.id.libraryPanel);
        btnToggleLibrary = view.findViewById(R.id.btnToggleLibrary);
        btnSearch = view.findViewById(R.id.btnSearch);
        etSearch = view.findViewById(R.id.etSearch);
        tvSongCount = view.findViewById(R.id.tvSongCount);
        rvSongs = view.findViewById(R.id.rvSongs);
        
        // Now Playing
        ivAlbumArt = view.findViewById(R.id.ivAlbumArt);
        tvSongTitle = view.findViewById(R.id.tvSongTitle);
        tvArtist = view.findViewById(R.id.tvArtist);
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime);
        tvTotalTime = view.findViewById(R.id.tvTotalTime);
        seekBar = view.findViewById(R.id.seekBar);
        btnPrevious = view.findViewById(R.id.btnPrevious);
        btnPlayPause = view.findViewById(R.id.btnPlayPause);
        btnNext = view.findViewById(R.id.btnNext);
        btnShuffle = view.findViewById(R.id.btnShuffle);
        btnRepeat = view.findViewById(R.id.btnRepeat);
        btnMute = view.findViewById(R.id.btnMute);
        seekBarVolume = view.findViewById(R.id.seekBarVolume);
        tvVolumePercent = view.findViewById(R.id.tvVolumePercent);
        
        // Setup RecyclerView
        songAdapter = new SongAdapter(getContext(), songList, this);
        rvSongs.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSongs.setAdapter(songAdapter);
    }

    private void setupListeners() {
        // Toggle library
        btnToggleLibrary.setOnClickListener(v -> toggleLibrary());
        
        // Search
        btnSearch.setOnClickListener(v -> toggleSearch());
        
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSongs(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Playback controls - delegate to MainActivity
        btnPlayPause.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).togglePlayPause();
            }
        });
        
        btnPrevious.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).playPrevious();
            }
        });
        
        btnNext.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).playNext();
            }
        });
        
        btnShuffle.setOnClickListener(v -> toggleShuffle());
        btnRepeat.setOnClickListener(v -> toggleRepeat());
        btnMute.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).toggleMute();
            }
        });
        
        // SeekBar - delegate to MainActivity
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Volume SeekBar
        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).setVolume(progress);
                    tvVolumePercent.setText(progress + "%");
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void toggleLibrary() {
        if (isLibraryVisible) {
            libraryPanel.setVisibility(View.GONE);
        } else {
            libraryPanel.setVisibility(View.VISIBLE);
        }
        isLibraryVisible = !isLibraryVisible;
    }

    private void toggleSearch() {
        isSearchVisible = !isSearchVisible;
        if (isSearchVisible) {
            etSearch.setVisibility(View.VISIBLE);
            etSearch.requestFocus();
        } else {
            etSearch.setVisibility(View.GONE);
            etSearch.setText("");
            songList = new ArrayList<>(allSongs);
            if (isShuffleOn) {
                Collections.shuffle(songList);
            }
            songAdapter.updateSongs(songList);
            tvSongCount.setText(songList.size() + " songs");
        }
    }

    private void filterSongs(String query) {
        if (query.isEmpty()) {
            songList = new ArrayList<>(allSongs);
        } else {
            songList = new ArrayList<>();
            String lowerQuery = query.toLowerCase();
            for (Song song : allSongs) {
                if (song.getTitle().toLowerCase().contains(lowerQuery) ||
                    song.getArtist().toLowerCase().contains(lowerQuery) ||
                    song.getAlbum().toLowerCase().contains(lowerQuery)) {
                    songList.add(song);
                }
            }
        }
        songAdapter.updateSongs(songList);
        tvSongCount.setText(songList.size() + " songs");
    }

    private void toggleShuffle() {
        isShuffleOn = !isShuffleOn;
        if (isShuffleOn) {
            btnShuffle.setColorFilter(getResources().getColor(android.R.color.holo_green_light));
            Collections.shuffle(songList);
            songAdapter.updateSongs(songList);
            Toast.makeText(getContext(), "Shuffle ON", Toast.LENGTH_SHORT).show();
        } else {
            btnShuffle.setColorFilter(0xFF808080);
            songList = new ArrayList<>(allSongs);
            songAdapter.updateSongs(songList);
            Toast.makeText(getContext(), "Shuffle OFF", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleRepeat() {
        repeatMode = (repeatMode + 1) % 3;
        switch (repeatMode) {
            case REPEAT_OFF:
                btnRepeat.setImageResource(R.drawable.ic_repeat);
                btnRepeat.setColorFilter(0xFF808080);
                Toast.makeText(getContext(), "Repeat OFF", Toast.LENGTH_SHORT).show();
                break;
            case REPEAT_ALL:
                btnRepeat.setImageResource(R.drawable.ic_repeat);
                btnRepeat.setColorFilter(getResources().getColor(android.R.color.holo_green_light));
                Toast.makeText(getContext(), "Repeat ALL", Toast.LENGTH_SHORT).show();
                break;
            case REPEAT_ONE:
                btnRepeat.setImageResource(R.drawable.ic_repeat_one);
                btnRepeat.setColorFilter(getResources().getColor(android.R.color.holo_green_light));
                Toast.makeText(getContext(), "Repeat ONE", Toast.LENGTH_SHORT).show();
                break;
        }
        
        // Update MainActivity
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setRepeatMode(repeatMode);
        }
    }

    private void loadSongs() {
        allSongs = MusicScanner.getAllSongs(getContext());
        songList = new ArrayList<>(allSongs);
        songAdapter.updateSongs(songList);
        tvSongCount.setText(songList.size() + " songs");
    }

    @Override
    public void onSongClick(Song song, int position) {
        currentSongIndex = position;
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).playSong(song, songList, position);
        }
    }

    // Called by MainActivity to update UI
    public void updateNowPlaying(Song song, boolean isPlaying, int currentPosition, int duration) {
        if (song != null) {
            tvSongTitle.setText(song.getTitle());
            tvArtist.setText(song.getArtist());
            seekBar.setMax(duration);
            seekBar.setProgress(currentPosition);
            updateTimeLabel(tvCurrentTime, currentPosition);
            updateTimeLabel(tvTotalTime, duration);
            
            btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        }
    }

    public void updateProgress(int position) {
        seekBar.setProgress(position);
        updateTimeLabel(tvCurrentTime, position);
    }

    public void updateMuteState(boolean isMuted, int volumePercent) {
        btnMute.setImageResource(isMuted ? R.drawable.ic_volume_off : R.drawable.ic_volume);
        seekBarVolume.setProgress(volumePercent);
        tvVolumePercent.setText(volumePercent + "%");
    }

    public void setCurrentPlaying(int position) {
        songAdapter.setCurrentPlaying(position);
    }

    private void updateTimeLabel(TextView textView, int milliseconds) {
        int minutes = (milliseconds / 1000) / 60;
        int seconds = (milliseconds / 1000) % 60;
        textView.setText(String.format("%d:%02d", minutes, seconds));
    }

    public List<Song> getSongList() {
        return songList;
    }

    public int getRepeatMode() {
        return repeatMode;
    }
}
