package com.cognitivekernel.androidfit;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Single shared, immutable in-memory view of the bundled exercise catalog.
 * Loading is performed on a worker thread; callers never parse the catalog on the UI thread.
 */
public final class ExerciseCatalog {
    private static final String ASSET = "exercise_catalog.json";
    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor();
    private static volatile Future<List<Exercise>> load;
    private ExerciseCatalog() {}

    public static synchronized Future<List<Exercise>> preload(Context context) {
        if (load == null) {
            Context app = context.getApplicationContext();
            load = LOADER.submit(() -> read(app));
        }
        return load;
    }

    public static List<Exercise> await(Context context) throws Exception {
        return preload(context).get();
    }

    private static List<Exercise> read(Context context) throws Exception {
        try (InputStream input = context.getAssets().open(ASSET);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[16384];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            JSONObject root = new JSONObject(new String(output.toByteArray(), StandardCharsets.UTF_8));
            JSONArray rows = root.getJSONArray("exercises");
            ArrayList<Exercise> result = new ArrayList<>(rows.length());
            java.util.HashSet<String> ids = new java.util.HashSet<>();
            java.util.HashSet<String> files = new java.util.HashSet<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                String id = row.getString("id").trim();
                String gender = row.optString("gender", "unspecified").toLowerCase(Locale.ROOT);
                String file = row.getString("file").trim();
                String en = row.getString("name_en").trim();
                String fa = row.optString("name_fa", "").trim();
                if (!id.matches("\\d{3,10}") || !ids.add(id))
                    throw new IllegalStateException("Invalid or duplicate exercise ID: " + id);
                if (!file.toLowerCase(Locale.ROOT).endsWith(".gif") || !files.add(file))
                    throw new IllegalStateException("Invalid or duplicate exercise GIF: " + file);
                if (!"male".equals(gender) && !"female".equals(gender) && !"unspecified".equals(gender))
                    throw new IllegalStateException("Invalid exercise gender: " + gender);
                if (en.isEmpty()) throw new IllegalStateException("Empty exercise name: " + id);
                result.add(new Exercise(id, gender, file, en, fa));
            }
            if (root.has("count") && root.getInt("count") != result.size())
                throw new IllegalStateException("Exercise catalog count mismatch");
            return Collections.unmodifiableList(result);
        }
    }

    public static final class Exercise {
        public final String id, gender, file, nameEn, nameFa;
        private Exercise(String id, String gender, String file, String nameEn, String nameFa) {
            this.id = id; this.gender = gender; this.file = file;
            this.nameEn = nameEn; this.nameFa = nameFa;
        }
        public boolean isEligibleFor(String requestedGender) {
            return requestedGender != null && requestedGender.equalsIgnoreCase(gender)
                && ("male".equals(gender) || "female".equals(gender));
        }
    }
}
