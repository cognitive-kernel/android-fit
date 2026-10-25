package com.cognitivekernel.androidfit;

import android.content.Context;
import android.graphics.Movie;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Verifies bundled exercise GIFs are decodable offline Android assets. */
@RunWith(AndroidJUnit4.class)
public class ExerciseGifAssetsTest {
    @Test public void allBundledExerciseGifsDecodeAndAnimate() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String[] files = {"squat.gif", "pushup.gif", "lunge.gif", "plank.gif", "row.gif", "walk.gif"};
        for (String file : files) {
            try (InputStream stream = context.getAssets().open("core_exercises/" + file)) {
                Movie movie = Movie.decodeStream(stream);
                assertNotNull("Could not decode " + file, movie);
                assertTrue(file + " should have a valid width", movie.width() > 0);
                assertTrue(file + " should have a valid height", movie.height() > 0);
                assertTrue(file + " should contain animation frames", movie.duration() > 0);
            }
        }
    }
    @Test public void catalogueRowsHaveSafeGifNamesAndThreeColumns() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open("exercise_catalog.tsv"), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\t", -1);
                assertTrue("Catalogue row must have filename, name and source ID", fields.length == 3);
                assertTrue("GIF filename must not be empty", !fields[0].trim().isEmpty());
                assertTrue("GIF filename must be a basename", !fields[0].contains("/") && !fields[0].contains("\\\\"));
                assertTrue("Catalogue entry must reference a GIF", fields[0].toLowerCase(java.util.Locale.ROOT).endsWith(".gif"));
                assertTrue("English name must not be empty", !fields[1].trim().isEmpty());
                count++;
            }
        }
        assertTrue("Catalogue should include thousands of GIFs", count >= 6500);
    }

    @Test public void fullGymVisualCatalogueIndexIsBundled() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        int count = 0;
        boolean hasSquat = false, hasPushUp = false, hasShoulder = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open("exercise_catalog.tsv"), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                count++;
                hasSquat |= line.toLowerCase(java.util.Locale.ROOT).contains("squat");
                hasPushUp |= line.toLowerCase(java.util.Locale.ROOT).contains("push-up");
                hasShoulder |= line.toLowerCase(java.util.Locale.ROOT).contains("shoulder");
            }
        }
        assertTrue("Full GymVisual index should contain thousands of exercises", count >= 6500);
        assertTrue("Squat category examples should be indexed", hasSquat);
        assertTrue("Push-up examples should be indexed", hasPushUp);
        assertTrue("Shoulder examples should be indexed", hasShoulder);
    }

}
