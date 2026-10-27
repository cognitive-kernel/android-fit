package com.cognitivekernel.androidfit;

import android.app.AlertDialog;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Regression test: exercise detail is a child dialog; the library parent must remain open. */
@RunWith(AndroidJUnit4.class)
public class NestedExerciseDialogTest {
    @Test public void detailKeepsParentOpenAndRepeatedOpenDoesNotDuplicateChild() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                AlertDialog parent = new AlertDialog.Builder(activity)
                    .setTitle("Exercise library parent").create();
                parent.show();
                try {
                    String gif = ExerciseImages.findGif(activity, "Air Squat");
                    assertNotNull("Expected an actual bundled exercise GIF", gif);
                    Method openDetail = AdvancedFeatures.class.getDeclaredMethod(
                        "showExerciseDetail", String.class, String.class, String.class, String.class);
                    openDetail.setAccessible(true);
                    openDetail.invoke(activity.advanced, "Air Squat", "", gif, "");

                    Field openDialogs = AdvancedFeatures.class.getDeclaredField("OPEN_EXERCISE_DETAILS");
                    openDialogs.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Map<android.app.Activity, AlertDialog> dialogs =
                        (Map<android.app.Activity, AlertDialog>) openDialogs.get(null);
                    AlertDialog child = dialogs.get(activity);

                    assertNotNull("Detail dialog should open", child);
                    assertTrue("Parent dialog must remain visible under the child", parent.isShowing());
                    assertTrue("Child detail dialog should be visible", child.isShowing());

                    openDetail.invoke(activity.advanced, "Air Squat", "", gif, "");
                    assertSame("Repeated open must not create a second detail dialog", child, dialogs.get(activity));

                    child.dismiss();
                    assertTrue("Dismissing the child must leave its parent open", parent.isShowing());
                } catch (Exception error) {
                    throw new AssertionError("Nested exercise dialog regression", error);
                } finally {
                    parent.dismiss();
                }
            });
        }
    }
}
