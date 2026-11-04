package com.cognitivekernel.androidfit;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
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
import java.util.ArrayList;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Resolves bundled exercise GIFs and displays them without dismissing the screen underneath. */
public final class ExerciseImages {
    private static volatile Map<String, String> indexedPaths;
    private static final WeakHashMap<Activity, Dialog> OPEN_FULLSCREEN = new WeakHashMap<>();
    private ExerciseImages() {}

    /** Warm the complete GIF alias index away from the UI thread during application startup. */
    public static void preload(Context context) {
        if (context == null || indexedPaths != null) return;
        INDEX_EXECUTOR.execute(() -> imageIndex(context.getApplicationContext()));
    }

    private static final java.util.concurrent.ExecutorService INDEX_EXECUTOR =
        java.util.concurrent.Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "exercise-image-index");
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        });

    public static String findGif(Context context, String exerciseName) {
        if (context == null || exerciseName == null || exerciseName.trim().isEmpty()) return null;
        Map<String, String> index = imageIndex(context);
        String wanted = normalize(exerciseName);
        String latin = normalize(transliteratePersian(exerciseName));
        String result = index.get(wanted);
        if (result == null && !latin.isEmpty()) result = index.get(latin);
        if (result == null) result = knownPersianAlias(index, wanted);
        return result;
    }

    private static String knownPersianAlias(Map<String, String> index, String normalizedName) {
        if (normalizedName.equals(normalize("شنا سوئدی"))
                || normalizedName.equals(normalize("شنا"))
                || normalizedName.equals(normalize("پوش آپ"))
                || normalizedName.equals(normalize("پوشاپ"))) {
            String path = index.get(normalize("Push-up"));
            if (path == null) path = index.get(normalize("Push up"));
            return path;
        }
        return null;
    }

    /** Build the lookup once per process; subsequent exercise lookups are constant-time. */
    private static Map<String, String> imageIndex(Context context) {
        Map<String, String> result = indexedPaths;
        if (result != null) return result;
        synchronized (ExerciseImages.class) {
            if (indexedPaths != null) return indexedPaths;
            Map<String, String> built = new HashMap<>();
            for (String file : files(context.getApplicationContext())) {
                String base = file.substring(file.lastIndexOf('/') + 1, file.length() - 4);
                String clean = stripAssetId(base);
                addAlias(built, clean, file);
            }
            indexedPaths = built;
            return built;
        }
    }

    private static void addAlias(Map<String, String> index, String alias, String path) {
        String key = normalize(alias);
        if (!key.isEmpty() && !index.containsKey(key)) index.put(key, path);
        String withoutVersion = removeVersionSuffix(alias);
        String simplified = normalize(withoutVersion);
        if (!simplified.isEmpty() && !index.containsKey(simplified)) index.put(simplified, path);
    }

    private static String transliteratePersian(String value) {
        String[][] pairs = {{"آ","a"},{"ا","a"},{"ب","b"},{"پ","p"},{"ت","t"},{"ث","s"},{"ج","j"},{"چ","ch"},{"ح","h"},{"خ","kh"},{"د","d"},{"ذ","z"},{"ر","r"},{"ز","z"},{"ژ","zh"},{"س","s"},{"ش","sh"},{"ص","s"},{"ض","z"},{"ط","t"},{"ظ","z"},{"ع",""},{"غ","gh"},{"ف","f"},{"ق","gh"},{"ک","k"},{"گ","g"},{"ل","l"},{"م","m"},{"ن","n"},{"و","w"},{"ه","h"},{"ی","y"},{"ئ","y"},{"ء",""}};
        String result = value == null ? "" : value;
        for (String[] pair : pairs) result = result.replace(pair[0], pair[1]);
        return result;
    }

    // Deliberately allocation-light: this method runs while ListView rows are bound.
    // Do not use String.replaceAll here; it compiles regular expressions on the UI thread.
    private static String normalize(String value) {
        if (value == null || value.isEmpty()) return "";
        String lower = value.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(lower.length());
        boolean pendingSpace = false;
        int limit = lower.length();
        if (limit >= 4 && lower.endsWith(".gif")) limit -= 4;
        for (int i = 0; i < limit; i++) {
            char ch = lower.charAt(i);
            if (ch == '_' || ch == '-') ch = ' ';
            boolean accepted = (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')
                    || (ch >= 0x0600 && ch <= 0x06FF) || ch == ' ';
            if (!accepted) ch = ' ';
            if (ch == ' ') {
                if (out.length() > 0) pendingSpace = true;
            } else {
                if (pendingSpace) out.append(' ');
                out.append(ch);
                pendingSpace = false;
            }
        }
        return out.toString();
    }

    private static String stripAssetId(String value) {
        int end = value.length();
        while (end > 0 && Character.isWhitespace(value.charAt(end - 1))) end--;
        if (end == 0 || value.charAt(end - 1) != ']') return value;
        int close = end - 1;
        int open = close - 1;
        while (open >= 0 && Character.isDigit(value.charAt(open))) open--;
        if (open == close - 1 || open < 0 || value.charAt(open) != '[') return value;
        int prefix = open - 1;
        if (prefix >= 0 && (value.charAt(prefix) == '_' || Character.isWhitespace(value.charAt(prefix)))) {
            while (prefix >= 0 && Character.isWhitespace(value.charAt(prefix))) prefix--;
            if (prefix >= 0 && value.charAt(prefix) == '_') prefix--;
            while (prefix >= 0 && Character.isWhitespace(value.charAt(prefix))) prefix--;
            return value.substring(0, prefix + 1);
        }
        return value;
    }

    private static String removeVersionSuffix(String value) {
        if (value == null) return "";
        String lower = value.toLowerCase(Locale.ROOT);
        int marker = lower.lastIndexOf("(version ");
        if (marker >= 0 && lower.endsWith(")")) return value.substring(0, marker).trim();
        marker = lower.lastIndexOf(" version ");
        if (marker >= 0) {
            int digit = marker + 9;
            if (digit < lower.length()) {
                boolean numbers = true;
                for (int i = digit; i < lower.length(); i++) numbers &= Character.isDigit(lower.charAt(i));
                if (numbers) return value.substring(0, marker).trim();
            }
        }
        return value.trim();
    }

    private static ArrayList<String> files(Context context) {
        return new ArrayList<>(ExerciseAssetIndex.get(context));
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
