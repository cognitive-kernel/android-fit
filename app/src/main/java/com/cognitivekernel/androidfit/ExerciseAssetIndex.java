package com.cognitivekernel.androidfit;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Loads the build-generated GIF path index; directory traversal is only a compatibility fallback. */
final class ExerciseAssetIndex {
    private static final String TAG = "ExerciseAssetIndex";
    private static volatile List<String> paths;

    private ExerciseAssetIndex() {}

    static List<String> get(Context context) {
        List<String> cached = paths;
        if (cached != null) return cached;
        synchronized (ExerciseAssetIndex.class) {
            if (paths != null) return paths;
            ArrayList<String> result = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    context.getApplicationContext().getAssets().open("exercise-index.txt"),
                    StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String path = line.trim();
                    if (path.startsWith("exercises/") && path.toLowerCase(Locale.ROOT).endsWith(".gif")) {
                        result.add(path);
                    }
                }
            } catch (IOException missingIndex) {
                Log.w(TAG, "Generated exercise index missing; using one-time asset scan", missingIndex);
                collect(context.getApplicationContext().getAssets(), "exercises", result);
            }
            Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
            paths = Collections.unmodifiableList(result);
            Log.i(TAG, "Loaded " + result.size() + " indexed exercise GIF assets");
            return paths;
        }
    }

    private static void collect(AssetManager assets, String directory, List<String> output) {
        try {
            String[] entries = assets.list(directory);
            if (entries == null) return;
            for (String entry : entries) {
                String path = directory + "/" + entry;
                if (entry.toLowerCase(Locale.ROOT).endsWith(".gif")) output.add(path);
                else collect(assets, path, output);
            }
        } catch (IOException error) {
            Log.e(TAG, "Cannot scan exercise asset directory: " + directory, error);
        }
    }
}
