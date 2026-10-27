package com.cognitivekernel.androidfit;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Movie;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Verifies real bundled GIF assets decode and the exercise resolver finds them. */
@RunWith(AndroidJUnit4.class)
public class ExerciseGifAssetsTest {
    @Test public void bundledExerciseGifsDecodeAndAnimate() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String[] entries = context.getAssets().list("exercises");
        assertNotNull("Exercise asset directory is missing", entries);
        List<String> gifs = new ArrayList<>();
        for (String entry : entries) if (entry.toLowerCase(java.util.Locale.ROOT).endsWith(".gif")) gifs.add(entry);
        Collections.sort(gifs);
        assertTrue("No exercise GIFs were packaged", gifs.size() >= 5);
        for (int i = 0; i < Math.min(5, gifs.size()); i++) {
            String file = gifs.get(i);
            try (InputStream stream = context.getAssets().open("exercises/" + file)) {
                Movie movie = Movie.decodeStream(stream);
                assertNotNull("Could not decode " + file, movie);
                assertTrue(file + " should have a valid width", movie.width() > 0);
                assertTrue(file + " should have a valid height", movie.height() > 0);
                assertTrue(file + " should contain animation frames", movie.duration() > 0);
            }
        }
    }

    @Test public void resolverFindsRealAssetNames() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertNotNull("Air Squat should match a bundled GIF", ExerciseImages.findGif(context, "Air Squat"));
        assertNotNull("World Greatest Stretch should match a bundled GIF",
            ExerciseImages.findGif(context, "World Greatest Stretch"));
    }
}
