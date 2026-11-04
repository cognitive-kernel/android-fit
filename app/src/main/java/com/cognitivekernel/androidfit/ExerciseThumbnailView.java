package com.cognitivekernel.androidfit;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.ImageDecoder;
import android.os.Build;
import android.util.LruCache;
import android.util.Log;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Bounded first-frame-only GIF thumbnail loader for scrolling lists. */
final class ExerciseThumbnailView extends android.widget.ImageView {
    private static final String TAG = "ExerciseThumbnail";
    private static final int EDGE_PX = 160;
    private static final Object PENDING_LOCK = new Object();
    private static final Set<String> PENDING = Collections.synchronizedSet(new HashSet<>());
    // One decoder and a small queue prevent fast scrolling through 6,500+ assets from
    // creating an unbounded backlog or competing with the UI thread for CPU and memory.
    private static final ThreadPoolExecutor WORKERS = new ThreadPoolExecutor(
            1, 1, 30L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(12),
            runnable -> {
                Thread thread = new Thread(runnable, "exercise-thumbnail-decoder");
                thread.setPriority(Thread.NORM_PRIORITY - 1);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(4 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) {
            return Math.max(1, value.getByteCount() / 1024);
        }
    };
    private String boundPath;

    ExerciseThumbnailView(Context context) { super(context); init(); }

    private void init() {
        setScaleType(ScaleType.CENTER_CROP);
        setBackgroundColor(0xFFE8EEF5);
        setContentDescription("Exercise image");
    }

    void load(String path) {
        if (path == null || path.isEmpty()) {
            boundPath = null;
            setImageDrawable(null);
            return;
        }
        if (path.equals(boundPath) && getDrawable() != null) return;
        boundPath = path;
        setImageDrawable(null);
        Bitmap cached = CACHE.get(path);
        if (cached != null && !cached.isRecycled()) {
            setImageBitmap(cached);
            return;
        }
        synchronized (PENDING_LOCK) {
            if (!PENDING.add(path)) return;
        }
        try {
            WORKERS.execute(() -> {
                Bitmap decoded = null;
                try {
                    decoded = decodeFirstFrame(getContext().getApplicationContext(), path);
                    if (decoded != null) CACHE.put(path, decoded);
                } catch (Exception error) {
                    Log.w(TAG, "Thumbnail decode failed: " + path, error);
                } finally {
                    synchronized (PENDING_LOCK) { PENDING.remove(path); }
                }
                if (decoded == null) return;
                Bitmap result = decoded;
                post(() -> {
                    if (path.equals(boundPath) && !result.isRecycled()) setImageBitmap(result);
                });
            });
        } catch (RejectedExecutionException queueFull) {
            synchronized (PENDING_LOCK) { PENDING.remove(path); }
            // Keep the neutral placeholder. A later bind can retry after the queue drains.
        }
    }

    static Bitmap decodeFirstFrame(Context context, String path) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) return decodeWithImageDecoder(context, path);
        try (InputStream input = context.getAssets().open(path)) {
            Movie movie = Movie.decodeStream(input);
            if (movie == null || movie.width() <= 0 || movie.height() <= 0) return null;
            float scale = Math.min(1f, EDGE_PX / (float) Math.max(movie.width(), movie.height()));
            Bitmap bitmap = Bitmap.createBitmap(Math.max(1, Math.round(movie.width() * scale)),
                    Math.max(1, Math.round(movie.height() * scale)), Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            canvas.scale(scale, scale);
            movie.setTime(0);
            movie.draw(canvas, 0, 0);
            return bitmap;
        }
    }

    @TargetApi(Build.VERSION_CODES.P)
    private static Bitmap decodeWithImageDecoder(Context context, String path) throws Exception {
        ImageDecoder.Source source = ImageDecoder.createSource(context.getAssets(), path);
        return ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
            int width = info.getSize().getWidth();
            int height = info.getSize().getHeight();
            float scale = Math.min(1f, EDGE_PX / (float) Math.max(width, height));
            decoder.setTargetSize(Math.max(1, Math.round(width * scale)),
                    Math.max(1, Math.round(height * scale)));
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
        });
    }
}
