package com.cognitivekernel.androidfit;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.widget.FrameLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Verifies that the production GIF view decodes and actually paints image pixels. */
@RunWith(AndroidJUnit4.class)
public class ExerciseGifRenderingTest {
    @Test public void bundledExerciseImageIsDecodedAndDrawnToCanvas() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            final ExerciseGifView[] image = new ExerciseGifView[1];
            final Bitmap[] rendered = new Bitmap[1];

            scenario.onActivity(activity -> {
                String asset = ExerciseImages.findGif(activity, "Air Squat");
                assertNotNull("Air Squat must resolve to a bundled GIF", asset);

                FrameLayout host = new FrameLayout(activity);
                activity.setContentView(host);
                ExerciseGifView view = new ExerciseGifView(activity, asset);
                host.addView(view, new FrameLayout.LayoutParams(480, 640));
                view.measure(
                    android.view.View.MeasureSpec.makeMeasureSpec(480, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(640, android.view.View.MeasureSpec.EXACTLY));
                view.layout(0, 0, 480, 640);
                image[0] = view;
            });

            long deadline = android.os.SystemClock.uptimeMillis() + 10000;
            while (!image[0].hasDecodedImage() &&
                android.os.SystemClock.uptimeMillis() < deadline) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                Thread.sleep(50);
            }
            assertTrue("ExerciseGifView did not decode a bundled image within 10 seconds",
                image[0].hasDecodedImage());

            scenario.onActivity(activity -> {
                Bitmap output = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(output);
                canvas.drawColor(Color.WHITE);
                image[0].draw(canvas);
                rendered[0] = output;
            });

            int visiblePixels = 0;
            for (int y = 0; y < rendered[0].getHeight(); y += 4) {
                for (int x = 0; x < rendered[0].getWidth(); x += 4) {
                    if (rendered[0].getPixel(x, y) != Color.WHITE) visiblePixels++;
                }
            }
            assertTrue("ExerciseGifView decoded an image but painted no visible pixels",
                visiblePixels > 100);
            rendered[0].recycle();
        }
    }
}
