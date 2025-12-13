package com.example.carmusicplayer;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Deezer API Service for searching and playing music
 * API Documentation: https://developers.deezer.com/api
 */
public class DeezerService {

    private static final String BASE_URL = "https://api.deezer.com";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Search for tracks on Deezer
     */
    public static void searchTracks(String query, SearchCallback callback) {
        executor.execute(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, "UTF-8");
                String urlString = BASE_URL + "/search?q=" + encodedQuery + "&limit=25";
                
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    List<DeezerTrack> tracks = parseSearchResults(response.toString());
                    mainHandler.post(() -> callback.onSuccess(tracks));
                } else {
                    mainHandler.post(() -> callback.onError("HTTP Error: " + responseCode));
                }
                connection.disconnect();
                
            } catch (IOException | JSONException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    private static List<DeezerTrack> parseSearchResults(String json) throws JSONException {
        List<DeezerTrack> tracks = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(json);
        JSONArray data = jsonObject.getJSONArray("data");
        
        for (int i = 0; i < data.length(); i++) {
            JSONObject trackJson = data.getJSONObject(i);
            
            long id = trackJson.getLong("id");
            String title = trackJson.getString("title");
            int duration = trackJson.getInt("duration");
            String preview = trackJson.optString("preview", "");
            
            JSONObject artistJson = trackJson.getJSONObject("artist");
            String artistName = artistJson.getString("name");
            
            JSONObject albumJson = trackJson.getJSONObject("album");
            String albumTitle = albumJson.getString("title");
            String albumCover = albumJson.optString("cover_medium", "");
            
            DeezerTrack track = new DeezerTrack(id, title, artistName, albumTitle, 
                    duration, preview, albumCover);
            tracks.add(track);
        }
        
        return tracks;
    }

    /**
     * Callback interface for search results
     */
    public interface SearchCallback {
        void onSuccess(List<DeezerTrack> tracks);
        void onError(String message);
    }

    /**
     * Data class for Deezer track
     */
    public static class DeezerTrack {
        private long id;
        private String title;
        private String artist;
        private String album;
        private int duration; // in seconds
        private String previewUrl; // 30s preview MP3
        private String albumCoverUrl;

        public DeezerTrack(long id, String title, String artist, String album, 
                          int duration, String previewUrl, String albumCoverUrl) {
            this.id = id;
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.duration = duration;
            this.previewUrl = previewUrl;
            this.albumCoverUrl = albumCoverUrl;
        }

        public long getId() { return id; }
        public String getTitle() { return title; }
        public String getArtist() { return artist; }
        public String getAlbum() { return album; }
        public int getDuration() { return duration; }
        public String getPreviewUrl() { return previewUrl; }
        public String getAlbumCoverUrl() { return albumCoverUrl; }

        public String getFormattedDuration() {
            int minutes = duration / 60;
            int seconds = duration % 60;
            return String.format("%d:%02d", minutes, seconds);
        }
    }
}
