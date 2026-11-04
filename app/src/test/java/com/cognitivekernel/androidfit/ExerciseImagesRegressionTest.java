package com.cognitivekernel.androidfit;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import java.io.InputStream;
import java.util.Arrays;
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
                try (InputStream input = controller.get().getAssets().open("exercises/" + entry)) {
                    byte[] gif = input.readAllBytes();
                    assertTrue("Invalid GIF structure or missing animation frames: " + entry,
                        hasAnimatedGifStructure(gif));
                }
                if (++checked == 5) break;
            }
            assertTrue("Fewer than five exercise GIF assets were packaged", checked == 5);
        }
    }

    private static boolean hasAnimatedGifStructure(byte[] data) {
        if (data == null || data.length < 14) return false;
        String header = new String(data, 0, 6, java.nio.charset.StandardCharsets.US_ASCII);
        if (!"GIF87a".equals(header) && !"GIF89a".equals(header)) return false;
        int width = (data[6] & 0xff) | ((data[7] & 0xff) << 8);
        int height = (data[8] & 0xff) | ((data[9] & 0xff) << 8);
        if (width <= 0 || height <= 0) return false;
        int[] position = {13};
        int packed = data[10] & 0xff;
        if ((packed & 0x80) != 0) {
            position[0] += 3 * (1 << ((packed & 0x07) + 1));
        }
        int frames = 0;
        try {
            while (position[0] < data.length) {
                int marker = data[position[0]++] & 0xff;
                if (marker == 0x3b) break;
                if (marker == 0x21) {
                    readByte(data, position); // extension label
                    skipGifSubBlocks(data, position);
                } else if (marker == 0x2c) {
                    position[0] += 8;
                    int imagePacked = readByte(data, position);
                    if ((imagePacked & 0x80) != 0) {
                        position[0] += 3 * (1 << ((imagePacked & 0x07) + 1));
                    }
                    readByte(data, position); // LZW minimum code size
                    skipGifSubBlocks(data, position);
                    frames++;
                } else {
                    return false;
                }
                if (position[0] > data.length) return false;
            }
        } catch (IndexOutOfBoundsException invalidGif) {
            return false;
        }
        return frames > 1;
    }

    private static int readByte(byte[] data, int[] position) {
        if (position[0] >= data.length) throw new IndexOutOfBoundsException();
        return data[position[0]++] & 0xff;
    }

    private static void skipGifSubBlocks(byte[] data, int[] position) {
        while (true) {
            int length = readByte(data, position);
            if (length == 0) return;
            position[0] += length;
            if (position[0] > data.length) throw new IndexOutOfBoundsException();
        }
    }

    @Test
    public void generatedIndexLoadsTheCompleteExerciseLibraryWithoutDirectoryWalk() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            java.util.List<String> paths = ExerciseAssetIndex.get(controller.get());
            assertTrue("Generated exercise index is missing or incomplete: " + paths.size(),
                paths.size() >= 6000);
            assertTrue("Expected exercise GIF is absent from generated index",
                paths.contains("exercises/Air Squat (VERSION 2)_[471413].gif"));
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
            String pushUp = ExerciseImages.findGif(activity, "شنا سوئدی");
            assertNotNull("Persian push-up name should resolve to a bundled GIF", pushUp);
            assertTrue("Persian push-up should resolve to the standard push-up asset: " + pushUp,
                pushUp.contains("Push-up_[288113].gif"));
            assertEquals("Common Persian push-up alias should resolve consistently",
                pushUp, ExerciseImages.findGif(activity, "پوش آپ"));
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
