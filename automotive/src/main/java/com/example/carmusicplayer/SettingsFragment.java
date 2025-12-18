package com.example.carmusicplayer;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Settings Fragment with Import Music feature
 */
public class SettingsFragment extends Fragment {

    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Setup file picker
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    // Handle single or multiple files
                    if (result.getData().getClipData() != null) {
                        // Multiple files
                        int count = result.getData().getClipData().getItemCount();
                        int imported = 0;
                        for (int i = 0; i < count; i++) {
                            Uri uri = result.getData().getClipData().getItemAt(i).getUri();
                            if (importMusicFile(uri)) {
                                imported++;
                            }
                        }
                        Toast.makeText(getContext(), "Imported " + imported + " songs!", Toast.LENGTH_SHORT).show();
                    } else if (result.getData().getData() != null) {
                        // Single file
                        Uri uri = result.getData().getData();
                        if (importMusicFile(uri)) {
                            Toast.makeText(getContext(), "Song imported!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        Button btnImportMusic = view.findViewById(R.id.btnImportMusic);
        if (btnImportMusic != null) {
            btnImportMusic.setOnClickListener(v -> openFilePicker());
        }
        
        return view;
    }
    
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }
    
    private boolean importMusicFile(Uri uri) {
        try {
            // Get file name
            String fileName = getFileName(uri);
            if (fileName == null) {
                fileName = "imported_" + System.currentTimeMillis() + ".mp3";
            }
            
            // Create music directory in internal storage
            File musicDir = new File(requireContext().getFilesDir(), "imported_music");
            if (!musicDir.exists()) {
                musicDir.mkdirs();
            }
            
            // Copy file
            File destFile = new File(musicDir, fileName);
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(destFile);
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            inputStream.close();
            outputStream.close();
            
            // Register in database
            registerImportedSong(destFile);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error importing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    result = cursor.getString(index);
                }
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }
    
    private void registerImportedSong(File file) {
        try {
            android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
            retriever.setDataSource(file.getAbsolutePath());
            
            String title = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String album = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM);
            String durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
            
            retriever.release();
            
            if (title == null || title.isEmpty()) {
                title = file.getName().replaceFirst("[.][^.]+$", "");
            }
            if (artist == null) artist = "Unknown Artist";
            if (album == null) album = "Imported";
            
            long duration = 0;
            if (durationStr != null) {
                duration = Long.parseLong(durationStr);
            }
            
            Song song = new Song(
                System.currentTimeMillis(),
                title,
                artist,
                album,
                file.getAbsolutePath(),
                duration,
                0
            );
            
            MusicDatabaseHelper dbHelper = MusicDatabaseHelper.getInstance(requireContext());
            dbHelper.insertSong(song, false);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
