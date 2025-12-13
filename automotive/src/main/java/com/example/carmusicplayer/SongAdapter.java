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

import java.util.List;

/**
 * Adapter for displaying song list in RecyclerView
 */
public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<Song> songs;
    private Context context;
    private OnSongClickListener listener;
    private MusicDatabaseHelper dbHelper;
    private int currentPlayingPosition = -1;

    public interface OnSongClickListener {
        void onSongClick(Song song, int position);
    }

    public SongAdapter(Context context, List<Song> songs, OnSongClickListener listener) {
        this.context = context;
        this.songs = songs;
        this.listener = listener;
        this.dbHelper = MusicDatabaseHelper.getInstance(context);
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.tvTitle.setText(song.getTitle());
        holder.tvArtist.setText(song.getArtist());
        holder.tvDuration.setText(song.getFormattedDuration());
        
        // Check if favorited
        boolean isFavorite = dbHelper.isFavorite(song.getId());
        updateFavoriteIcon(holder.btnFavorite, isFavorite);
        
        // Favorite button click
        holder.btnFavorite.setOnClickListener(v -> {
            boolean currentFav = dbHelper.isFavorite(song.getId());
            if (currentFav) {
                dbHelper.removeFromFavorites(song.getId());
                updateFavoriteIcon(holder.btnFavorite, false);
                Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show();
            } else {
                dbHelper.addToFavorites(song.getId());
                updateFavoriteIcon(holder.btnFavorite, true);
                Toast.makeText(context, "Added to favorites ❤️", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Highlight current playing song
        if (position == currentPlayingPosition) {
            holder.itemView.setBackgroundColor(0x33E94560);
            holder.ivPlaying.setVisibility(View.VISIBLE);
        } else {
            holder.itemView.setBackgroundColor(0x00000000);
            holder.ivPlaying.setVisibility(View.GONE);
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSongClick(song, position);
            }
        });
    }

    private void updateFavoriteIcon(ImageButton btn, boolean isFavorite) {
        if (isFavorite) {
            btn.setImageResource(R.drawable.ic_favorite);
            btn.setColorFilter(0xFFE94560); // Pink/red color
        } else {
            btn.setImageResource(R.drawable.ic_favorite_border);
            btn.setColorFilter(0xFF808080); // Gray color
        }
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public void setCurrentPlaying(int position) {
        int oldPosition = currentPlayingPosition;
        currentPlayingPosition = position;
        if (oldPosition != -1) {
            notifyItemChanged(oldPosition);
        }
        if (position != -1) {
            notifyItemChanged(position);
        }
    }

    public void updateSongs(List<Song> newSongs) {
        this.songs = newSongs;
        notifyDataSetChanged();
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvArtist;
        TextView tvDuration;
        ImageView ivPlaying;
        ImageButton btnFavorite;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvSongItemTitle);
            tvArtist = itemView.findViewById(R.id.tvSongItemArtist);
            tvDuration = itemView.findViewById(R.id.tvSongItemDuration);
            ivPlaying = itemView.findViewById(R.id.ivPlaying);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }
    }
}
