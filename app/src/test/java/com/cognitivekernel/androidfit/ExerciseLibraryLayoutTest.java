package com.cognitivekernel.androidfit;

import android.app.Activity;
import android.app.Dialog;
import android.view.ViewGroup;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ExerciseLibraryLayoutTest {
    @Test public void libraryDialogUsesTheEntireAvailableWindow() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            Dialog dialog = new Dialog(activity);
            dialog.setContentView(new android.view.View(activity));
            dialog.show();

            AdvancedFeatures.makeLibraryDialogFullscreen(dialog);

            assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, dialog.getWindow().getAttributes().width);
            assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, dialog.getWindow().getAttributes().height);
            dialog.dismiss();
        }
    }
}
