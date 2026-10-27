package com.cognitivekernel.androidfit;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Regression tests for resolving exercise names to bundled animated GIF assets. */
@RunWith(AndroidJUnit4.class)
public class ExerciseImagesTest {
    @Test public void resolvesStandardExerciseNames() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String squat = ExerciseImages.findGif(context, "squat");
        assertNotNull("A saved squat movement should show its bundled GIF", squat);
        assertTrue(squat.toLowerCase(java.util.Locale.ROOT).endsWith(".gif"));

        String stretch = ExerciseImages.findGif(context, "World Greatest Stretch");
        assertNotNull("Exercise names with catalog IDs should resolve", stretch);
        assertTrue(stretch.toLowerCase(java.util.Locale.ROOT).endsWith(".gif"));
    }

    @Test public void unknownCustomExerciseDoesNotShowAnUnrelatedGif() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue(ExerciseImages.findGif(context, "Custom Movement With No Catalog Match") == null);
    }
}
