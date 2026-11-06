package com.cognitivekernel.androidfit;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.WeakHashMap;

/** Resolves bundled exercise GIFs and displays them without dismissing the screen underneath. */
public final class ExerciseImages {
    private static final WeakHashMap<Activity, Dialog> OPEN_FULLSCREEN = new WeakHashMap<>();
    private ExerciseImages() {}

    /** Gender-constrained lookup for generated catalog-backed plans. */
    public static String findGif(Context context, String exerciseName, String gender) {
        return ExerciseAssetIndex.findFile(context, exerciseName, gender);
    }

    public static String findGifById(Context context, String catalogId) {
        return ExerciseAssetIndex.findFileById(context, catalogId, null);
    }

    public static String findGifById(Context context, String catalogId, String gender) {
        return ExerciseAssetIndex.findFileById(context, catalogId, gender);
    }

    /** Unrestricted manual/library lookup, resolved through the same authoritative catalog. */
    public static String findGif(Context context, String exerciseName) {
        return ExerciseAssetIndex.findFile(context, exerciseName, null);
    }

    public static void showFullscreen(Activity activity, String assetPath, String title) {
        showFullscreenInternal(activity, assetPath, title, true);
    }

    static void showFullscreenForTest(Activity activity, String assetPath, String title) {
        showFullscreenInternal(activity, assetPath, title, false);
    }

    private static void showFullscreenInternal(Activity activity, String assetPath, String title, boolean decodeImage) {
        if (activity == null || activity.isFinishing() || assetPath == null || assetPath.trim().isEmpty()) return;
        synchronized (OPEN_FULLSCREEN) {
            Dialog existing = OPEN_FULLSCREEN.get(activity);
            if (existing != null && existing.isShowing()) return;
        }
        Dialog dialog = new Dialog(activity);
        synchronized (OPEN_FULLSCREEN) { OPEN_FULLSCREEN.put(activity, dialog); }
        dialog.setOnDismissListener(ignored -> {
            synchronized (OPEN_FULLSCREEN) {
                if (OPEN_FULLSCREEN.get(activity) == dialog) OPEN_FULLSCREEN.remove(activity);
            }
        });
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        LinearLayout toolbar = new LinearLayout(activity);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(16, 8, 12, 8);
        TextView heading = new TextView(activity);
        heading.setText(title == null ? "" : title);
        heading.setTextColor(Color.WHITE);
        heading.setTextSize(17);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setMaxLines(2);
        toolbar.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        Button close = new Button(activity);
        close.setText("×");
        close.setTextSize(28);
        close.setTextColor(Color.WHITE);
        close.setBackgroundColor(Color.TRANSPARENT);
        close.setContentDescription("Close image");
        toolbar.addView(close, new LinearLayout.LayoutParams(
            (int)(52 * activity.getResources().getDisplayMetrics().density), -2));
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, -2));
        ExerciseGifView image = new ExerciseGifView(activity, assetPath, decodeImage);
        image.setBackgroundColor(Color.BLACK);
        root.addView(image, new LinearLayout.LayoutParams(-1, 0, 1));
        close.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.black);
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.black);
            // Keep the image viewer immersive while retaining its own visible close control.
            dialog.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    public static ExerciseGifView thumbnail(Context context, String assetPath, String title, int heightDp) {
        ExerciseGifView image = new ExerciseGifView(context, assetPath);
        image.setContentDescription(title);
        image.setOnClickListener(v -> {
            if (context instanceof Activity) showFullscreen((Activity) context, assetPath, title);
        });
        image.setLayoutParams(new LinearLayout.LayoutParams(-1,
            (int)(heightDp * context.getResources().getDisplayMetrics().density)));
        return image;
    }
}
