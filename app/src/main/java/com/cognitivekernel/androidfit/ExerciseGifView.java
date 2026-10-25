package com.cognitivekernel.androidfit;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Animated GIF view for exercise demonstrations. Bundled core GIFs work offline;
 * GymVisual catalogue GIFs are downloaded only when selected and cached locally.
 */
public final class ExerciseGifView extends View {
    private static final String CATALOG_COMMIT = "c3f658cf3329ece6d4231044c3d4e3ed72c54805";
    private static final String RAW_BASE = "https://raw.githubusercontent.com/cognitive-kernel/android-fit/"
            + CATALOG_COMMIT + "/app/src/main/assets/exercises/";
    private static final ExecutorService DOWNLOADS = Executors.newFixedThreadPool(2);
    private static final ConcurrentHashMap<String, Boolean> IN_FLIGHT = new ConcurrentHashMap<>();

    private volatile Movie movie;
    private final long startedAt;
    private final String assetName;
    private volatile boolean loadFailed;

    public ExerciseGifView(Context context, String assetName) {
        super(context);
        this.assetName = assetName == null ? "" : assetName;
        Movie decoded = null;
        try (InputStream in = context.getAssets().open("core_exercises/" + this.assetName)) {
            decoded = Movie.decodeStream(in);
        } catch (Exception ignored) { }
        if (decoded == null) {
            try (InputStream in = context.getAssets().open("exercises/" + this.assetName)) {
                decoded = Movie.decodeStream(in);
            } catch (Exception ignored) { }
        }
        if (decoded == null) {
            File cached = cachedFile(context, this.assetName);
            if (cached.isFile()) {
                try (InputStream in = new FileInputStream(cached)) {
                    decoded = Movie.decodeStream(in);
                } catch (Exception ignored) { }
            }
        }
        movie = decoded;
        startedAt = SystemClock.uptimeMillis();
        setContentDescription(this.assetName.replace(".gif", "").replace('_', ' ')
                + (decoded == null ? " — loading GIF" : " — tap to enlarge"));
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setClickable(true);
        setFocusable(true);
        setOnClickListener(v -> showFullScreen());
        if (decoded == null && !this.assetName.isEmpty()) downloadIfNeeded(context.getApplicationContext());
    }

    private static File cachedFile(Context context, String name) {
        File root = new File(context.getCacheDir(), "exercise-gifs");
        File file = new File(root, name);
        try {
            String rootPath = root.getCanonicalPath() + File.separator;
            if (!file.getCanonicalPath().startsWith(rootPath)) return new File(root, "invalid.gif");
        } catch (Exception ignored) { return new File(root, "invalid.gif"); }
        return file;
    }

    private void downloadIfNeeded(Context context) {
        if (Boolean.TRUE.equals(IN_FLIGHT.putIfAbsent(assetName, Boolean.TRUE))) return;
        DOWNLOADS.execute(() -> {
            HttpURLConnection connection = null;
            File temporary = null;
            try {
                File target = cachedFile(context, assetName);
                if (target.isFile() && target.length() > 0) {
                    try (InputStream in = new FileInputStream(target)) { movie = Movie.decodeStream(in); }
                    if (movie != null) { postInvalidate(); return; }
                    target.delete();
                }
                File parent = target.getParentFile();
                if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) throw new IllegalStateException("Cache unavailable");
                String encodedPath = Uri.encode(assetName, "/");
                connection = (HttpURLConnection) new URL(RAW_BASE + encodedPath).openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(20000);
                connection.setRequestProperty("User-Agent", "FitYar-Exercise-Library");
                connection.connect();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) throw new IllegalStateException("GIF not found");
                temporary = File.createTempFile("exercise-", ".gif", parent);
                try (InputStream in = connection.getInputStream(); FileOutputStream out = new FileOutputStream(temporary)) {
                    byte[] buffer = new byte[16384];
                    int count;
                    while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                }
                try (InputStream in = new FileInputStream(temporary)) {
                    Movie validation = Movie.decodeStream(in);
                    if (validation == null || validation.width() <= 0 || validation.height() <= 0) {
                        throw new IllegalStateException("Invalid GIF");
                    }
                }
                if (target.exists() && !target.delete()) throw new IllegalStateException("Cannot replace cache");
                if (!temporary.renameTo(target)) throw new IllegalStateException("Cannot save GIF");
                try (InputStream in = new FileInputStream(target)) { movie = Movie.decodeStream(in); }
                loadFailed = movie == null;
            } catch (Exception ignored) {
                loadFailed = true;
            } finally {
                if (connection != null) connection.disconnect();
                if (temporary != null && temporary.exists()) temporary.delete();
                IN_FLIGHT.remove(assetName);
                post(() -> {
                    setContentDescription(assetName.replace(".gif", "").replace('_', ' ')
                            + (movie == null ? " — GIF unavailable" : " — tap to enlarge"));
                    invalidate();
                });
            }
        });
    }

    private void showFullScreen() {
        if (movie == null) return;
        Context context = getContext();
        Dialog dialog = new Dialog(context);
        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Color.BLACK);

        ExerciseGifView enlarged = new ExerciseGifView(context, assetName);
        enlarged.setContentDescription(getContentDescription());
        enlarged.setOnClickListener(v -> dialog.dismiss());
        FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        imageParams.gravity = Gravity.CENTER;
        root.addView(enlarged, imageParams);

        TextView close = new TextView(context);
        close.setText("×");
        close.setTextColor(Color.WHITE);
        close.setTextSize(32);
        close.setGravity(Gravity.CENTER);
        close.setContentDescription("Close enlarged GIF");
        close.setBackground(makeCloseBackground());
        close.setClickable(true);
        close.setFocusable(true);
        close.setOnClickListener(v -> dialog.dismiss());
        int buttonSize = dp(context, 48);
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(buttonSize, buttonSize,
                Gravity.TOP | Gravity.END);
        int margin = dp(context, 12);
        closeParams.setMargins(margin, margin, margin, margin);
        root.addView(close, closeParams);

        dialog.setContentView(root);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.black);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private static GradientDrawable makeCloseBackground() {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(0x99000000);
        bg.setStroke(1, 0x88FFFFFF);
        return bg;
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Movie current = movie;
        if (current == null) {
            canvas.drawColor(0xFFF1F1F1);
            Paint placeholder = new Paint(Paint.ANTI_ALIAS_FLAG);
            placeholder.setColor(0xFF444444);
            placeholder.setTextSize(14 * getResources().getDisplayMetrics().density);
            String language = getContext().getSharedPreferences("fit_settings", 0).getString("language", null);
            if (language == null) language = getContext().getSharedPreferences("fityar_preferences", 0).getString("language", "fa");
            boolean english = "en".equalsIgnoreCase(language);
            String message = loadFailed
                    ? (english ? "GIF unavailable — check internet" : "بارگذاری گیف ناموفق بود؛ اینترنت را بررسی کنید")
                    : (english ? "Loading GIF…" : "در حال بارگذاری گیف…");
            float x = Math.max(8, (getWidth() - placeholder.measureText(message)) / 2f);
            float y = getHeight() / 2f;
            canvas.drawText(message, x, y, placeholder);
            return;
        }
        int duration = current.duration();
        if (duration <= 0) duration = 1000;
        float sx = getWidth() / (float) Math.max(1, current.width());
        float sy = getHeight() / (float) Math.max(1, current.height());
        float scale = Math.min(sx, sy);
        canvas.save();
        canvas.translate((getWidth() - current.width() * scale) / 2f,
                (getHeight() - current.height() * scale) / 2f);
        canvas.scale(scale, scale);
        current.setTime((int) ((SystemClock.uptimeMillis() - startedAt) % duration));
        current.draw(canvas, 0, 0);
        canvas.restore();
        postInvalidateOnAnimation();
    }
}
