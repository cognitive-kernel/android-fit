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

/** Resolves bundled exercise GIFs and displays them without dismissing the screen underneath. */
public final class ExerciseImages {
    private static volatile ArrayList<String> cachedFiles;
    private ExerciseImages() {}

    public static String findGif(Context context, String exerciseName) {
        if (exerciseName == null || exerciseName.trim().isEmpty()) return null;
        ArrayList<String> files = files(context.getAssets());
        String wanted = normalize(exerciseName);
        String best = null;
        int bestScore = 0;
        for (String file : files) {
            String base = file.substring(file.lastIndexOf('/') + 1, file.length() - 4);
            String candidate = normalize(base.replaceAll("[_\\s]+[\\[(]?\\d{3,8}[\\])]?$", ""));
            if (candidate.isEmpty()) continue;
            if (candidate.equals(wanted)) return file;
            int score = 0;
            if (wanted.contains(candidate) || candidate.contains(wanted)) {
                score = Math.min(wanted.length(), candidate.length()) * 3;
            } else {
                String[] a = wanted.split(" ");
                String[] b = candidate.split(" ");
                for (String token : a) if (token.length() > 2) {
                    for (String other : b) if (token.equals(other)) { score += token.length() * 2; break; }
                }
            }
            if (score > bestScore) { bestScore = score; best = file; }
        }
        int minimum = Math.max(9, wanted.replace(" ", "").length() / 2);
        return bestScore >= minimum ? best : null;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("\\.gif$", "")
            .replaceAll("[_\\-]+", " ").replaceAll("[^a-z0-9 ]", " ")
            .replaceAll("\\s+", " ").trim();
    }

    private static ArrayList<String> files(AssetManager assets) {
        ArrayList<String> result = cachedFiles;
        if (result != null) return result;
        synchronized (ExerciseImages.class) {
            if (cachedFiles != null) return cachedFiles;
            result = new ArrayList<>();
            collect(assets, "exercises", result);
            cachedFiles = result;
            return result;
        }
    }

    private static void collect(AssetManager assets, String directory, ArrayList<String> out) {
        try {
            String[] children = assets.list(directory);
            if (children == null) return;
            for (String child : children) {
                String path = directory + "/" + child;
                if (child.toLowerCase(Locale.ROOT).endsWith(".gif")) out.add(path);
                else collect(assets, path, out);
            }
        } catch (IOException ignored) { }
    }

    public static void showFullscreen(Activity activity, String assetPath, String title) {
        if (activity == null || activity.isFinishing() || assetPath == null || assetPath.trim().isEmpty()) return;
        Dialog dialog = new Dialog(activity);
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
        ExerciseGifView image = new ExerciseGifView(activity, assetPath);
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
