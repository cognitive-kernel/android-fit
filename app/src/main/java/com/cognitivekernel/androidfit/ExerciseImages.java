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
    private static volatile ArrayList<String> cachedFiles;
    private static final java.util.Map<String, String> RESOLVED_GIFS = new java.util.HashMap<>();
    private static volatile ArrayList<GifCandidate> indexedFiles;

    private static final class GifCandidate {
        final String path, normalized, localized;
        GifCandidate(String path, String normalized, String localized) {
            this.path = path; this.normalized = normalized; this.localized = localized;
        }
    }
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

    private static String resolveGif(Context context, String wanted, String wantedLatin) {
        ArrayList<GifCandidate> candidates = indexedFiles(context);
        String best = null;
        int bestScore = 0;
        for (GifCandidate candidate : candidates) {
            if (candidate.normalized.isEmpty() && candidate.localized.isEmpty()) continue;
            if (candidate.normalized.equals(wanted) || candidate.localized.equals(wanted)
                    || (!wantedLatin.isEmpty() && candidate.normalized.equals(wantedLatin))
                    || (!wantedLatin.isEmpty() && candidate.localized.equals(wantedLatin))) {
                return candidate.path;
            }
            int score = Math.max(matchScore(wanted, candidate.normalized),
                Math.max(matchScore(wanted, candidate.localized),
                    Math.max(matchScore(wantedLatin, candidate.normalized),
                        matchScore(wantedLatin, candidate.localized))));
            if (score > bestScore) {
                bestScore = score;
                best = candidate.path;
            }
        }
        int minimum = Math.max(5, Math.max(wanted.replace(" ", "").length(),
            wantedLatin.replace(" ", "").length()) / 3);
        return bestScore >= minimum ? best : null;
    }

    private static ArrayList<GifCandidate> indexedFiles(Context context) {
        ArrayList<GifCandidate> result = indexedFiles;
        if (result != null) return result;
        synchronized (ExerciseImages.class) {
            if (indexedFiles != null) return indexedFiles;
            result = new ArrayList<>();
            for (String file : files(context.getAssets())) {
                String base = file.substring(file.lastIndexOf('/') + 1, file.length() - 4);
                String cleanBase = base.replaceAll("[_\\s]+[\\[(]?(\\d{3,8})[\\])]?$", "");
                result.add(new GifCandidate(file, normalize(cleanBase),
                    normalize(AdvancedFeatures.persianExerciseName(cleanBase))));
            }
            indexedFiles = result;
            return result;
        }
    }

    private static int matchScore(String wanted, String candidate) {
        if (wanted == null || wanted.isEmpty() || candidate == null || candidate.isEmpty()) return 0;
        if (wanted.equals(candidate)) return 10000;
        if (wanted.contains(candidate) || candidate.contains(wanted)) {
            return Math.min(wanted.length(), candidate.length()) * 3;
        }
        int score = 0;
        String[] a = wanted.split(" ");
        String[] b = candidate.split(" ");
        for (String token : a) {
            if (token.length() < 3) continue;
            for (String other : b) {
                if (token.equals(other)) {
                    score += token.length() * 2;
                    break;
                }
            }
        }
        return score;
    }

    private static String transliteratePersian(String value) {
        String[][] pairs = {{"آ","a"},{"ا","a"},{"ب","b"},{"پ","p"},{"ت","t"},{"ث","s"},{"ج","j"},{"چ","ch"},{"ح","h"},{"خ","kh"},{"د","d"},{"ذ","z"},{"ر","r"},{"ز","z"},{"ژ","zh"},{"س","s"},{"ش","sh"},{"ص","s"},{"ض","z"},{"ط","t"},{"ظ","z"},{"ع",""},{"غ","gh"},{"ف","f"},{"ق","gh"},{"ک","k"},{"گ","g"},{"ل","l"},{"م","m"},{"ن","n"},{"و","w"},{"ه","h"},{"ی","y"},{"ئ","y"},{"ء",""}};
        String result = value == null ? "" : value;
        for (String[] pair : pairs) result = result.replace(pair[0], pair[1]);
        return result;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("\\.gif$", "")
            .replaceAll("[_\\-]+", " ").replaceAll("[^a-z0-9\\u0600-\\u06FF ]", " ")
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
