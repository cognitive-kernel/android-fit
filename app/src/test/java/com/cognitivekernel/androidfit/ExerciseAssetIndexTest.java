package com.cognitivekernel.androidfit;

import android.app.Activity;
import android.content.res.AssetManager;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.RobolectricTestRunner;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ExerciseAssetIndexTest {
    @Test public void catalogHasAllAssetsAndUniqueNumericIds() throws Exception {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            AssetManager assets = controller.get().getAssets();
            JSONObject catalog;
            try (InputStream input = assets.open("exercise_catalog.json")) {
                byte[] data = input.readAllBytes();
                catalog = new JSONObject(new String(data, StandardCharsets.UTF_8));
            }
            JSONArray rows = catalog.getJSONArray("exercises");
            assertEquals(6556, rows.length());
            assertEquals(6556, catalog.getInt("count"));
            Set<String> ids = new HashSet<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                String id = row.getString("id");
                assertTrue("Non-numeric exercise ID: " + id, id.matches("\\d{3,10}"));
                assertTrue("Duplicate exercise ID: " + id, ids.add(id));
                assertTrue("Missing gender metadata", row.has("gender"));
                assertTrue("Missing GIF filename", row.getString("file").endsWith(".gif"));
            }
        }
    }

    @Test public void numericIdResolvesToExactBundledGif() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            assertEquals("exercises/1 2 Stick Drill (male)_[596513].gif",
                ExerciseImages.findGifById(controller.get(), "596513"));
        }
    }

    @Test public void thumbnailUsesStaticPreviewView() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            assertTrue(ExerciseImages.thumbnail(controller.get(),
                "exercises/1 2 Stick Drill (male)_[596513].gif", "1 2 Stick Drill", 64)
                instanceof ExerciseThumbnailView);
        }
    }
}
