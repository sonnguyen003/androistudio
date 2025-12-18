package com.example.carmusicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapter for displaying Deezer search results
 */
public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    private List<DeezerService.DeezerTrack> tracks;
    private Context context;
    private OnTrackClickListener listener;
    private Set<Long> favoriteIds = new HashSet<>(); // Track favorite status locally

    public interface OnTrackClickListener {
        void onTrackClick(DeezerService.DeezerTrack track);
    }

    public SearchResultAdapter(Context context, List<DeezerService.DeezerTrack> tracks, OnTrackClickListener listener) {
        this.context = context;
        this.tracks = tracks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeezerService.DeezerTrack track = tracks.get(position);
        
        holder.tvTitle.setText(track.getTitle());
        holder.tvArtist.setText(track.getArtist());
        holder.tvAlbum.setText(track.getAlbum());
        holder.tvDuration.setText(track.getFormattedDuration());
        
        holder.ivAlbumCover.setImageResource(R.drawable.ic_music_note);
        
        // Update favorite state
        boolean isFavorite = favoriteIds.contains(track.getId());
        updateFavoriteIcon(holder.btnFavorite, isFavorite);
        
        // Favorite button click - save to database as a new song
        holder.btnFavorite.setOnClickListener(v -> {
            MusicDatabaseHelper dbHelper = MusicDatabaseHelper.getInstance(context);
            boolean currentFav = favoriteIds.contains(track.getId());
            
            if (currentFav) {
                favoriteIds.remove(track.getId());
                updateFavoriteIcon(holder.btnFavorite, false);
                // Also remove from database
                dbHelper.removeFromFavorites(track.getId());
                Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show();
            } else {
                favoriteIds.add(track.getId());
                updateFavoriteIcon(holder.btnFavorite, true);
                
                // First check if song already exists
                Song existingSong = dbHelper.getSongById(track.getId());
                if (existingSong == null) {
                    // Insert new song with specific ID
                    Song song = new Song(
                        track.getId(),
                        track.getTitle(),
                        track.getArtist(),
                        track.getAlbum(),
                        track.getPreviewUrl(),
                        track.getDuration() * 1000L,
                        0
                    );
                    dbHelper.insertSongWithId(song);
                }
                // Add to favorites
                dbHelper.addToFavorites(track.getId());
                
                Toast.makeText(context, "Added to favorites ❤️", Toast.LENGTH_SHORT).show();
            }
        });
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTrackClick(track);
            }
        });
    }

    private void updateFavoriteIcon(ImageButton btn, boolean isFavorite) {
        if (isFavorite) {
            btn.setImageResource(R.drawable.ic_favorite);
            btn.setColorFilter(0xFFE94560);
        } else {
            btn.setImageResource(R.drawable.ic_favorite_border);
            btn.setColorFilter(0xFF808080);
        }
    }

    @Override
    public int getItemCount() {
        return tracks != null ? tracks.size() : 0;
    }

    public void updateTracks(List<DeezerService.DeezerTrack> newTracks) {
        this.tracks = newTracks;
        favoriteIds.clear(); // Reset favorites on new search
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAlbumCover;
        TextView tvTitle;
        TextView tvArtist;
        TextView tvAlbum;
        TextView tvDuration;
        ImageButton btnFavorite;

        ViewHolder(View itemView) {
            super(itemView);
            ivAlbumCover = itemView.findViewById(R.id.ivAlbumCover);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            tvAlbum = itemView.findViewById(R.id.tvAlbum);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }
    }
}
