package com.cognitivekernel.androidfit;

import android.content.Context;
import android.graphics.Movie;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.InputStream;
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
}
