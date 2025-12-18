package com.example.carmusicplayer;

/**
 * Model class representing a song
 */
public class Song {
    
    private long id;
    private String title;
    private String artist;
    private String album;
    private String path;
    private long duration;
    private long albumId;

    public Song(long id, String title, String artist, String album, String path, long duration, long albumId) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.path = path;
        this.duration = duration;
        this.albumId = albumId;
    }

    // Getters
    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getPath() { return path; }
    public long getDuration() { return duration; }
    public long getAlbumId() { return albumId; }

    // Setters
    public void setId(long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setArtist(String artist) { this.artist = artist; }
    public void setAlbum(String album) { this.album = album; }
    public void setPath(String path) { this.path = path; }
    public void setDuration(long duration) { this.duration = duration; }
    public void setAlbumId(long albumId) { this.albumId = albumId; }

    public String getFormattedDuration() {
        long minutes = (duration / 1000) / 60;
        long seconds = (duration / 1000) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
