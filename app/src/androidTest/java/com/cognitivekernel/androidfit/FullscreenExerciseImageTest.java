package com.cognitivekernel.androidfit;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Verifies immersive fullscreen image display and close behavior over an open parent page. */
@RunWith(AndroidJUnit4.class)
public class FullscreenExerciseImageTest {
    @Test public void imageOpensImmersiveFullscreenAndCloseKeepsParentOpen() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Dialog parent = new Dialog(activity);
                parent.setContentView(new View(activity));
                parent.show();
                try {
                    String gif = ExerciseImages.findGif(activity, "Air Squat");
                    assertNotNull("Expected a bundled exercise GIF", gif);

                    ExerciseImages.showFullscreen(activity, gif, "Air Squat");

                    Field field = ExerciseImages.class.getDeclaredField("OPEN_FULLSCREEN");
                    field.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Map<Activity, Dialog> dialogs = (Map<Activity, Dialog>) field.get(null);
                    Dialog fullscreen = dialogs.get(activity);

                    assertNotNull("Fullscreen image dialog should open", fullscreen);
                    assertTrue("Fullscreen dialog should be visible", fullscreen.isShowing());
                    WindowManager.LayoutParams attributes = fullscreen.getWindow().getAttributes();
                    assertTrue("Fullscreen width must fill the screen",
                        attributes.width == WindowManager.LayoutParams.MATCH_PARENT);
                    assertTrue("Fullscreen height must fill the screen",
                        attributes.height == WindowManager.LayoutParams.MATCH_PARENT);
                    assertTrue("Status bar must be hidden",
                        (fullscreen.getWindow().getDecorView().getSystemUiVisibility()
                            & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0);
                    assertTrue("Navigation bar must be hidden",
                        (fullscreen.getWindow().getDecorView().getSystemUiVisibility()
                            & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0);

                    View close = findByDescription(fullscreen.getWindow().getDecorView(), "Close image");
                    assertNotNull("Fullscreen viewer must expose its close button", close);
                    close.performClick();

                    assertTrue("Closing fullscreen must leave the previous page open", parent.isShowing());
                    assertTrue("Fullscreen dialog must be dismissed", !fullscreen.isShowing());
                } catch (Exception error) {
                    throw new AssertionError("Fullscreen exercise image regression", error);
                } finally {
                    parent.dismiss();
                }
            });
        }
    }

    private static View findByDescription(View view, String description) {
        if (description.contentEquals(view.getContentDescription())) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findByDescription(group.getChildAt(i), description);
                if (found != null) return found;
            }
        }
        return null;
    }
}
