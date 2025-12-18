package com.example.carmusicplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Fragment for displaying a list of songs (used in Favorites sub-tabs)
 */
public class ListFragment extends Fragment implements SongAdapter.OnSongClickListener {

    private static final String ARG_TYPE = "type";
    public static final int TYPE_FAVORITES = 0;
    public static final int TYPE_HISTORY = 1;

    private RecyclerView rvList;
    private TextView tvEmptyMessage;
    private SongAdapter songAdapter;
    private int listType;

    public static ListFragment newInstance(int type) {
        ListFragment fragment = new ListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            listType = getArguments().getInt(ARG_TYPE, TYPE_FAVORITES);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        
        rvList = view.findViewById(R.id.rvList);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        
        rvList.setLayoutManager(new LinearLayoutManager(getContext()));
        songAdapter = new SongAdapter(getContext(), new java.util.ArrayList<>(), this);
        rvList.setAdapter(songAdapter);
        
        loadData();
        
        return view;
    }

    private void loadData() {
        MusicDatabaseHelper dbHelper = MusicDatabaseHelper.getInstance(getContext());
        List<Song> songs;
        
        if (listType == TYPE_FAVORITES) {
            songs = dbHelper.getFavoriteSongs();
            tvEmptyMessage.setText("No favorite songs yet.\nTap ❤️ on a song to add it.");
        } else {
            // History - for now show all songs as placeholder
            songs = dbHelper.getAllSongs();
            tvEmptyMessage.setText("No listening history yet.");
        }
        
        if (songs.isEmpty()) {
            tvEmptyMessage.setVisibility(View.VISIBLE);
            rvList.setVisibility(View.GONE);
        } else {
            tvEmptyMessage.setVisibility(View.GONE);
            rvList.setVisibility(View.VISIBLE);
            songAdapter.updateSongs(songs);
        }
    }

    @Override
    public void onSongClick(Song song, int position) {
        // Play song via MainActivity
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).playSongFromFragment(song);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData(); // Refresh when returning to this fragment
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        // Refresh when tab becomes visible in ViewPager2
        if (menuVisible && isResumed()) {
            loadData();
        }
    }

    // Public method to allow parent fragments to trigger refresh
    public void refreshData() {
        if (isAdded()) {
            loadData();
        }
    }
}
