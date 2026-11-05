package com.cognitivekernel.androidfit;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.util.LruCache;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.HashSet;
import java.util.Set;

/** Static first-frame preview. The animated GIF is decoded only in the fullscreen viewer. */
public final class ExerciseThumbnailView extends View {
    private static final int MAX_EDGE = 360;
    private static final int CACHE_KB = 6 * 1024;
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(CACHE_KB * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount(); }
    };
    private static final ExecutorService DECODER = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Set<String> PENDING = new HashSet<>();

    private String assetPath;
    private String title;
    private Bitmap preview;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    public ExerciseThumbnailView(Context context, String path, String title) {
        super(context);
        setPreview(path, title);
    }

    public void setPreview(String path, String label) {
        assetPath = path;
        title = label == null ? "" : label;
        setContentDescription(title);
        preview = CACHE.get(path);
        invalidate();
        if (preview == null && path != null && !path.trim().isEmpty()) {
            final String requested = path;
            boolean shouldDecode;
            synchronized (PENDING) { shouldDecode = PENDING.add(requested); }
            if (shouldDecode) DECODER.execute(() -> {
                Bitmap decoded = decodeFirstFrame(getContext().getApplicationContext(), requested);
                if (decoded != null) CACHE.put(requested, decoded);
                synchronized (PENDING) { PENDING.remove(requested); }
                if (decoded != null) MAIN.post(() -> {
                    if (requested.equals(assetPath)) {
                        preview = decoded;
                        invalidate();
                    }
                });
            });
        }
        setOnClickListener(v -> {
            Context context = getContext();
            if (context instanceof Activity && assetPath != null) {
                ExerciseImages.showFullscreen((Activity) context, assetPath, title);
            }
        });
    }

    private static Bitmap decodeFirstFrame(Context context, String path) {
        try (InputStream input = context.getAssets().open(path)) {
            Movie movie = Movie.decodeStream(input);
            if (movie == null || movie.width() <= 0 || movie.height() <= 0) return null;
            float scale = Math.min(1f, MAX_EDGE / (float) Math.max(movie.width(), movie.height()));
            int width = Math.max(1, Math.round(movie.width() * scale));
            int height = Math.max(1, Math.round(movie.height() * scale));
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);
            canvas.save();
            canvas.scale(width / (float) movie.width(), height / (float) movie.height());
            movie.setTime(0);
            movie.draw(canvas, 0, 0);
            canvas.restore();
            return bitmap;
        } catch (Exception error) {
            android.util.Log.w("ExerciseThumbnail", "Cannot decode preview " + path, error);
            return null;
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth(), height = getHeight();
        if (width <= 0 || height <= 0) return;
        canvas.drawColor(0xFFE8EEF5);
        Bitmap image = preview;
        if (image == null || image.isRecycled()) return;
        float scale = Math.min(width / (float) image.getWidth(), height / (float) image.getHeight());
        int drawWidth = Math.max(1, Math.round(image.getWidth() * scale));
        int drawHeight = Math.max(1, Math.round(image.getHeight() * scale));
        int left = (width - drawWidth) / 2;
        int top = (height - drawHeight) / 2;
        canvas.drawBitmap(image, null, new Rect(left, top, left + drawWidth, top + drawHeight), paint);
    }
}
