package com.example.carmusicplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Radio Fragment with Deezer search integration
 * Uses global mini player from MainActivity
 */
public class RadioFragment extends Fragment implements SearchResultAdapter.OnTrackClickListener {

    private EditText etSearchQuery;
    private ImageButton btnSearch;
    private ProgressBar progressBar;
    private TextView tvResultsHeader;
    private RecyclerView rvSearchResults;

    private SearchResultAdapter adapter;
    private List<DeezerService.DeezerTrack> searchResults = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_radio, container, false);
        
        initViews(view);
        setupListeners();
        
        return view;
    }

    private void initViews(View view) {
        etSearchQuery = view.findViewById(R.id.etSearchQuery);
        btnSearch = view.findViewById(R.id.btnSearch);
        progressBar = view.findViewById(R.id.progressBar);
        tvResultsHeader = view.findViewById(R.id.tvResultsHeader);
        rvSearchResults = view.findViewById(R.id.rvSearchResults);
        
        adapter = new SearchResultAdapter(getContext(), searchResults, this);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSearchResults.setAdapter(adapter);
    }

    private void setupListeners() {
        btnSearch.setOnClickListener(v -> performSearch());
        
        etSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
        
        // Hide mini player and bottom nav when search field is focused
        etSearchQuery.setOnFocusChangeListener((v, hasFocus) -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                if (hasFocus) {
                    mainActivity.hideMiniPlayer();
                    mainActivity.hideBottomNav();
                } else {
                    mainActivity.showMiniPlayer();
                    mainActivity.showBottomNav();
                }
            }
        });
    }

    private void performSearch() {
        String query = etSearchQuery.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(getContext(), "Enter a search term", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvResultsHeader.setVisibility(View.GONE);
        
        DeezerService.searchTracks(query, new DeezerService.SearchCallback() {
            @Override
            public void onSuccess(List<DeezerService.DeezerTrack> tracks) {
                progressBar.setVisibility(View.GONE);
                searchResults = tracks;
                adapter.updateTracks(tracks);
                
                if (tracks.isEmpty()) {
                    tvResultsHeader.setText("No results found");
                } else {
                    tvResultsHeader.setText(tracks.size() + " results");
                }
                tvResultsHeader.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Search failed: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onTrackClick(DeezerService.DeezerTrack track) {
        String previewUrl = track.getPreviewUrl();
        if (previewUrl == null || previewUrl.isEmpty()) {
            Toast.makeText(getContext(), "No preview available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Clear focus from search field and show bottom nav
        if (etSearchQuery != null) {
            etSearchQuery.clearFocus();
        }
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.showBottomNav();
            mainActivity.playDeezerTrack(track);
        }
        
        Toast.makeText(getContext(), "Playing: " + track.getTitle(), Toast.LENGTH_SHORT).show();
    }
}
