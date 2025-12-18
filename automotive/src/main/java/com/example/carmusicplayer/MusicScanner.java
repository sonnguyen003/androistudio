package com.example.carmusicplayer;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to get songs from database and scan device storage
 */
public class MusicScanner {

    private static final String TAG = "MusicScanner";

    /**
     * Initialize music - scan assets and return all songs from database
     */
    public static List<Song> getAllSongs(Context context) {
        // First, scan and register assets
        AssetMusicScanner.scanAndRegisterAssets(context);
        
        // Get database helper
        MusicDatabaseHelper dbHelper = MusicDatabaseHelper.getInstance(context);
        
        // Get songs from database
        List<Song> songs = dbHelper.getAllSongs();
        Log.d(TAG, "Database has " + songs.size() + " songs");
        
        // Also try to scan from MediaStore and add to database
        List<Song> deviceSongs = scanWithMediaStore(context);
        for (Song song : deviceSongs) {
            if (!dbHelper.songExists(song.getPath())) {
                dbHelper.insertSong(song, false);
                songs.add(song);
            }
        }
        
        // Return songs (even if empty - no demo songs)
        if (songs.isEmpty()) {
            Log.d(TAG, "No songs found");
        }
        
        return songs;
    }

    /**
     * Check if song is from assets folder
     */
    public static boolean isAssetSong(Song song) {
        return song.getPath().startsWith("music/");
    }
    
    private static List<Song> scanWithMediaStore(Context context) {
        List<Song> songs = new ArrayList<>();
        
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
        };
        
        try {
            Cursor cursor = contentResolver.query(uri, projection, null, null, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
                
                do {
                    long id = cursor.getLong(idColumn);
                    String title = cursor.getString(titleColumn);
                    String artist = cursor.getString(artistColumn);
                    String album = cursor.getString(albumColumn);
                    String path = cursor.getString(pathColumn);
                    long duration = cursor.getLong(durationColumn);
                    long albumId = cursor.getLong(albumIdColumn);
                    
                    if (duration > 5000) {
                        songs.add(new Song(id, title, artist, album, path, duration, albumId));
                    }
                } while (cursor.moveToNext());
                
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scanning with MediaStore", e);
        }
        
        return songs;
    }
    
    public static Uri getAlbumArtUri(long albumId) {
        return ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"), 
                albumId
        );
    }
}
