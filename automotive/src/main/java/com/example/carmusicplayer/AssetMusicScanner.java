package com.example.carmusicplayer;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to scan music files from assets folder
 */
public class AssetMusicScanner {

    private static final String TAG = "AssetMusicScanner";
    private static final String MUSIC_FOLDER = "music";

    /**
     * Scan and register all music files from assets/music folder
     */
    public static void scanAndRegisterAssets(Context context) {
        MusicDatabaseHelper dbHelper = MusicDatabaseHelper.getInstance(context);
        AssetManager assetManager = context.getAssets();
        
        try {
            String[] files = assetManager.list(MUSIC_FOLDER);
            
            if (files != null) {
                for (String fileName : files) {
                    if (isAudioFile(fileName)) {
                        String assetPath = MUSIC_FOLDER + "/" + fileName;
                        
                        // Check if already in database
                        if (!dbHelper.songExists(assetPath)) {
                            Song song = createSongFromAsset(context, assetPath, fileName);
                            if (song != null) {
                                dbHelper.insertSong(song, true);
                                Log.d(TAG, "Registered asset song: " + song.getTitle());
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error scanning assets", e);
        }
    }

    /**
     * Create a Song object from an asset file
     */
    private static Song createSongFromAsset(Context context, String assetPath, String fileName) {
        AssetManager assetManager = context.getAssets();
        
        try {
            AssetFileDescriptor afd = assetManager.openFd(assetPath);
            
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            
            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            
            // Use filename if no title in metadata
            if (title == null || title.isEmpty()) {
                title = fileName.replaceFirst("[.][^.]+$", "")  // Remove extension
                        .replace("_", " ");  // Replace underscores with spaces
            }
            if (artist == null || artist.isEmpty()) artist = "Unknown Artist";
            if (album == null || album.isEmpty()) album = "Assets";
            
            long duration = 0;
            if (durationStr != null) {
                duration = Long.parseLong(durationStr);
            }
            
            retriever.release();
            afd.close();
            
            return new Song(0, title, artist, album, assetPath, duration, 0);
            
        } catch (Exception e) {
            Log.e(TAG, "Error reading asset: " + assetPath, e);
            // Create with basic info from filename
            String title = fileName.replaceFirst("[.][^.]+$", "").replace("_", " ");
            return new Song(0, title, "Unknown Artist", "Assets", assetPath, 0, 0);
        }
    }

    /**
     * Get list of asset song paths
     */
    public static List<String> getAssetMusicFiles(Context context) {
        List<String> files = new ArrayList<>();
        AssetManager assetManager = context.getAssets();
        
        try {
            String[] assetFiles = assetManager.list(MUSIC_FOLDER);
            if (assetFiles != null) {
                for (String fileName : assetFiles) {
                    if (isAudioFile(fileName)) {
                        files.add(MUSIC_FOLDER + "/" + fileName);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error listing asset music files", e);
        }
        
        return files;
    }

    private static boolean isAudioFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".mp3") || lower.endsWith(".wav") || 
               lower.endsWith(".m4a") || lower.endsWith(".ogg") ||
               lower.endsWith(".flac") || lower.endsWith(".aac");
    }
}
