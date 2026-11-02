package com.cognitivekernel.androidfit;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ExerciseImagesRegressionTest {
    @Test
    public void bundledGifsArePackagedAndContainAnimationFrames() throws Exception {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            String[] entries = controller.get().getAssets().list("exercises");
            assertNotNull("Exercise asset directory is missing", entries);
            Arrays.sort(entries);
            int checked = 0;
            for (String entry : entries) {
                if (!entry.toLowerCase(java.util.Locale.ROOT).endsWith(".gif")) continue;
                try (InputStream input = controller.get().getAssets().open("exercises/" + entry);
                     ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
                    assertNotNull("GIF stream could not be opened: " + entry, imageInput);
                    Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
                    assertTrue("No GIF decoder accepted " + entry, readers.hasNext());
                    ImageReader reader = readers.next();
                    try {
                        reader.setInput(imageInput, false, false);
                        assertTrue("Invalid GIF dimensions: " + entry,
                            reader.getWidth(0) > 0 && reader.getHeight(0) > 0);
                        assertTrue("GIF must contain multiple frames: " + entry,
                            reader.getNumImages(true) > 1);
                        assertNotNull("First GIF frame did not decode: " + entry, reader.read(0));
                    } finally {
                        reader.dispose();
                    }
                }
                if (++checked == 5) break;
            }
            assertTrue("Fewer than five exercise GIF assets were packaged", checked == 5);
        }
    }

    @Test
    public void resolverFindsExpectedExerciseGifAssets() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            assertNotNull("Air Squat should match a bundled GIF",
                ExerciseImages.findGif(activity, "Air Squat"));
            assertNotNull("World Greatest Stretch should match a bundled GIF",
                ExerciseImages.findGif(activity, "World Greatest Stretch"));
        }
    }

    @Test
    public void fullscreenCloseDismissesOnlyTheImageDialog() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            ExerciseImages.showFullscreenForTest(activity,
                "exercises/Air Squat (VERSION 2)_[471413].gif", "Air Squat");

            Dialog dialog = ShadowDialog.getLatestDialog();
            assertNotNull("Fullscreen image dialog was not created", dialog);
            assertTrue("Fullscreen image dialog should be visible", dialog.isShowing());
            Button close = findCloseButton(dialog.getWindow().getDecorView());
            assertNotNull("Fullscreen close button is missing", close);
            close.performClick();

            assertFalse("Close button should dismiss the fullscreen dialog", dialog.isShowing());
            assertFalse("Closing the image must not finish the parent activity", activity.isFinishing());
        }
    }

    private static Button findCloseButton(View view) {
        if (view instanceof Button && "Close image".contentEquals(view.getContentDescription())) {
            return (Button) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Button found = findCloseButton(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }
}
