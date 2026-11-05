package com.cognitivekernel.androidfit;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Persistent numeric index for bundled exercise GIF assets. */
public final class ExerciseAssetIndex extends SQLiteOpenHelper {
    private static final String DB_NAME = "exercise_assets.db";
    private static final int DB_VERSION = 1;
    private static final ExecutorService BUILDER = Executors.newSingleThreadExecutor();
    private static volatile boolean ready;
    private static volatile boolean building;
    private static volatile ExerciseAssetIndex instance;

    public static synchronized void preload(Context context) {
        if (ready || building) return;
        building = true;
        Context app = context.getApplicationContext();
        BUILDER.execute(() -> {
            try {
                index(app).getWritableDatabase();
                ready = true;
            } catch (Exception error) {
                android.util.Log.e("ExerciseAssetIndex", "Could not build exercise index", error);
            } finally {
                building = false;
            }
        });
    }

    private static synchronized ExerciseAssetIndex index(Context context) {
        if (instance == null) instance = new ExerciseAssetIndex(context.getApplicationContext());
        return instance;
    }

    private final Context context;

    private ExerciseAssetIndex(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context.getApplicationContext();
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE exercise_assets (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, normalized_name TEXT NOT NULL, normalized_fa TEXT NOT NULL, gender TEXT NOT NULL, file TEXT NOT NULL)");
        db.execSQL("CREATE INDEX idx_exercise_assets_name ON exercise_assets(normalized_name)");
        db.execSQL("CREATE INDEX idx_exercise_assets_fa ON exercise_assets(normalized_fa)");
        db.execSQL("CREATE INDEX idx_exercise_assets_gender ON exercise_assets(gender)");
        seed(db);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS exercise_assets");
        onCreate(db);
    }

    private void seed(SQLiteDatabase db) {
        try (InputStream input = context.getAssets().open("exercise_catalog.json");
             ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[16384];
            int count;
            while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
            JSONObject root = new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
            JSONArray rows = root.getJSONArray("exercises");
            db.beginTransaction();
            try {
                for (int i = 0; i < rows.length(); i++) {
                    JSONObject row = rows.getJSONObject(i);
                    String name = row.getString("name");
                    ContentValues values = new ContentValues();
                    values.put("id", row.getString("id"));
                    values.put("name", name);
                    values.put("normalized_name", normalize(name));
                    values.put("normalized_fa", normalize(AdvancedFeatures.persianExerciseName(name)));
                    values.put("gender", row.optString("gender", "unspecified"));
                    values.put("file", row.getString("file"));
                    db.insertOrThrow("exercise_assets", null, values);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Exception error) {
            throw new IllegalStateException("Invalid exercise_catalog.json", error);
        }
    }

    private static void ensureReady(Context context) {
        if (ready) return;
        try {
            index(context).getReadableDatabase();
            ready = true;
        } catch (Exception error) {
            android.util.Log.e("ExerciseAssetIndex", "Exercise index is unavailable", error);
        }
    }

    public static String findFile(Context context, String exerciseName) {
        if (exerciseName == null || exerciseName.trim().isEmpty()) return null;
        if (!ready && building) return null;
        ensureReady(context);
        if (!ready) return null;
        String query = normalize(exerciseName);
        try (Cursor cursor = index(context).getReadableDatabase().rawQuery(
            "SELECT file FROM exercise_assets WHERE normalized_name=? OR normalized_fa=? OR normalized_name LIKE ? OR normalized_fa LIKE ? ORDER BY length(normalized_name),id LIMIT 1",
            new String[]{query, query, query + "%", query + "%"})) {
            return cursor.moveToFirst() ? "exercises/" + cursor.getString(0) : null;
        }
    }

    public static String findFileById(Context context, String id) {
        if (id == null || !id.matches("\\d{3,10}")) return null;
        if (!ready && building) return null;
        ensureReady(context);
        if (!ready) return null;
        try (Cursor cursor = index(context).getReadableDatabase().rawQuery(
            "SELECT file FROM exercise_assets WHERE id=?", new String[]{id})) {
            return cursor.moveToFirst() ? "exercises/" + cursor.getString(0) : null;
        }
    }

    public static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
            .replaceAll("\\.gif$", "").replaceAll("[_\\-]+", " ")
            .replaceAll("[^a-z0-9\\u0600-\\u06FF ]", " ")
            .replaceAll("\\s+", " ").trim();
    }

    static boolean isReady() { return ready; }
}
