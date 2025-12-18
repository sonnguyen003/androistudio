package com.example.carmusicplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite Database Helper for managing songs
 */
public class MusicDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "music_player.db";
    private static final int DATABASE_VERSION = 2;

    // Table names
    public static final String TABLE_SONGS = "songs";
    public static final String TABLE_PLAYLISTS = "playlists";
    public static final String TABLE_PLAYLIST_SONGS = "playlist_songs";
    public static final String TABLE_FAVORITES = "favorites";

    // Songs table columns
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_ARTIST = "artist";
    public static final String COLUMN_ALBUM = "album";
    public static final String COLUMN_PATH = "path";
    public static final String COLUMN_DURATION = "duration";
    public static final String COLUMN_IS_ASSET = "is_asset";

    // Create songs table SQL
    private static final String CREATE_TABLE_SONGS = 
            "CREATE TABLE " + TABLE_SONGS + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_TITLE + " TEXT NOT NULL, " +
            COLUMN_ARTIST + " TEXT, " +
            COLUMN_ALBUM + " TEXT, " +
            COLUMN_PATH + " TEXT NOT NULL, " +
            COLUMN_DURATION + " INTEGER DEFAULT 0, " +
            COLUMN_IS_ASSET + " INTEGER DEFAULT 0)";

    // Create favorites table SQL
    private static final String CREATE_TABLE_FAVORITES =
            "CREATE TABLE " + TABLE_FAVORITES + " (" +
            "song_id INTEGER PRIMARY KEY, " +
            "added_at DATETIME DEFAULT CURRENT_TIMESTAMP)";

    private static MusicDatabaseHelper instance;

    public static synchronized MusicDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new MusicDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private MusicDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SONGS);
        db.execSQL(CREATE_TABLE_FAVORITES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SONGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
        onCreate(db);
    }

    // Insert a song
    public long insertSong(Song song, boolean isAsset) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, song.getTitle());
        values.put(COLUMN_ARTIST, song.getArtist());
        values.put(COLUMN_ALBUM, song.getAlbum());
        values.put(COLUMN_PATH, song.getPath());
        values.put(COLUMN_DURATION, song.getDuration());
        values.put(COLUMN_IS_ASSET, isAsset ? 1 : 0);
        
        return db.insert(TABLE_SONGS, null, values);
    }

    // Insert a song with specific ID (for Deezer tracks)
    public long insertSongWithId(Song song) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, song.getId());
        values.put(COLUMN_TITLE, song.getTitle());
        values.put(COLUMN_ARTIST, song.getArtist());
        values.put(COLUMN_ALBUM, song.getAlbum());
        values.put(COLUMN_PATH, song.getPath());
        values.put(COLUMN_DURATION, song.getDuration());
        values.put(COLUMN_IS_ASSET, 0);
        
        return db.insertWithOnConflict(TABLE_SONGS, null, values, 
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    // Get song by ID
    public Song getSongById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SONGS, null, 
                COLUMN_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
        
        Song song = null;
        if (cursor.moveToFirst()) {
            song = cursorToSong(cursor);
        }
        cursor.close();
        return song;
    }

    // Get all songs
    public List<Song> getAllSongs() {
        List<Song> songs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_SONGS, null, null, null, null, null, 
                COLUMN_TITLE + " ASC");
        
        if (cursor.moveToFirst()) {
            do {
                Song song = cursorToSong(cursor);
                songs.add(song);
            } while (cursor.moveToNext());
        }
        cursor.close();
        
        return songs;
    }

    // Get songs from assets only
    public List<Song> getAssetSongs() {
        List<Song> songs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_SONGS, null, 
                COLUMN_IS_ASSET + " = 1", null, null, null, 
                COLUMN_TITLE + " ASC");
        
        if (cursor.moveToFirst()) {
            do {
                Song song = cursorToSong(cursor);
                songs.add(song);
            } while (cursor.moveToNext());
        }
        cursor.close();
        
        return songs;
    }

    // Get local songs only (assets + imported, NOT Deezer online songs)
    public List<Song> getLocalSongs() {
        List<Song> songs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        // Filter out songs with http URLs (Deezer songs)
        Cursor cursor = db.query(TABLE_SONGS, null, 
                COLUMN_PATH + " NOT LIKE 'http%'", null, null, null, 
                COLUMN_TITLE + " ASC");
        
        if (cursor.moveToFirst()) {
            do {
                Song song = cursorToSong(cursor);
                songs.add(song);
            } while (cursor.moveToNext());
        }
        cursor.close();
        
        return songs;
    }

    // Check if song exists by path
    public boolean songExists(String path) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SONGS, new String[]{COLUMN_ID}, 
                COLUMN_PATH + " = ?", new String[]{path}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Add to favorites
    public void addToFavorites(long songId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("song_id", songId);
        db.insertWithOnConflict(TABLE_FAVORITES, null, values, 
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    // Remove from favorites
    public void removeFromFavorites(long songId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_FAVORITES, "song_id = ?", new String[]{String.valueOf(songId)});
    }

    // Check if song is favorite
    public boolean isFavorite(long songId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_FAVORITES, null, 
                "song_id = ?", new String[]{String.valueOf(songId)}, null, null, null);
        boolean isFav = cursor.getCount() > 0;
        cursor.close();
        return isFav;
    }

    // Get favorite songs
    public List<Song> getFavoriteSongs() {
        List<Song> songs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String query = "SELECT s.* FROM " + TABLE_SONGS + " s " +
                "INNER JOIN " + TABLE_FAVORITES + " f ON s." + COLUMN_ID + " = f.song_id " +
                "ORDER BY f.added_at DESC";
        
        Cursor cursor = db.rawQuery(query, null);
        
        if (cursor.moveToFirst()) {
            do {
                Song song = cursorToSong(cursor);
                songs.add(song);
            } while (cursor.moveToNext());
        }
        cursor.close();
        
        return songs;
    }

    // Delete all songs
    public void deleteAllSongs() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SONGS, null, null);
    }

    // Get song count
    public int getSongCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SONGS, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    private Song cursorToSong(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
        String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
        String artist = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ARTIST));
        String album = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ALBUM));
        String path = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PATH));
        long duration = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DURATION));
        
        return new Song(id, title, artist, album, path, duration, 0);
    }
}
