package com.cognitivekernel.androidfit;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Movie;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Verifies representative GIFs from the real bundled exercise catalogue decode offline. */
@RunWith(AndroidJUnit4.class)
public class ExerciseGifAssetsTest {
    @Test public void representativeCatalogGifsDecode() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AssetManager assets = context.getAssets();
        String[] entries = assets.list("exercises");
        assertNotNull("Exercise asset directory is missing", entries);

        List<String> gifs = new ArrayList<>();
        for (String entry : entries) {
            if (entry != null && entry.toLowerCase(java.util.Locale.ROOT).endsWith(".gif")) {
                gifs.add(entry);
            }
        }
        assertTrue("No GIFs were bundled in the exercise catalogue", gifs.size() >= 5);

        // Sample across the full catalogue rather than relying on generic filenames
        // that do not exist in this repository's real asset collection.
        int[] positions = {0, gifs.size() / 4, gifs.size() / 2,
                (gifs.size() * 3) / 4, gifs.size() - 1};
        for (int position : positions) {
            String file = gifs.get(position);
            try (InputStream stream = assets.open("exercises/" + file)) {
                Movie movie = Movie.decodeStream(stream);
                assertNotNull("Could not decode bundled GIF: " + file, movie);
                assertTrue(file + " should have a valid width", movie.width() > 0);
                assertTrue(file + " should have a valid height", movie.height() > 0);
            }
        }
    }
}
