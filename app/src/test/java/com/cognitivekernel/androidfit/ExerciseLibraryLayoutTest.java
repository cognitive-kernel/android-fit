package com.cognitivekernel.androidfit;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ExerciseLibraryLayoutTest {
    @Test public void libraryDialogFillsWindowAndLeavesScrollableListViewport() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            LinearLayout content = new LinearLayout(activity);
            content.setOrientation(LinearLayout.VERTICAL);
            ListView list = new ListView(activity);
            List<String> rows = new ArrayList<>();
            for (int i = 0; i < 50; i++) rows.add("Exercise " + i);
            list.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, rows));
            content.addView(list, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

            Dialog dialog = AdvancedFeatures.showFullscreenLibraryDialog(
                    activity, content, "Exercise library");

            int width = activity.getResources().getDisplayMetrics().widthPixels;
            int height = activity.getResources().getDisplayMetrics().heightPixels;
            View decor = dialog.getWindow().getDecorView();
            decor.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            decor.layout(0, 0, width, height);

            assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, dialog.getWindow().getAttributes().width);
            assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, dialog.getWindow().getAttributes().height);
            assertTrue("The list must receive a real viewport, not collapse to one row",
                    list.getHeight() > 64 * activity.getResources().getDisplayMetrics().density);
            assertTrue("The list must scroll when more rows exist than fit in the viewport",
                    list.canScrollVertically(1));
            dialog.dismiss();
        }
    }
}
