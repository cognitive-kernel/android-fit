package com.cognitivekernel.androidfit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.LruCache;
import android.view.View;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Small offline animated-GIF view; asset decoding is kept off the UI thread. */
public final class ExerciseGifView extends View {
    private static final ExecutorService DECODER = Executors.newFixedThreadPool(2);
    private static final LruCache<String, Movie> CACHE = new LruCache<>(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger requestGeneration = new AtomicInteger();
    private volatile Movie movie;
    private volatile long startedAt = SystemClock.uptimeMillis();

    public ExerciseGifView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public ExerciseGifView(Context context, String assetName) {
        this(context);
        setAssetName(assetName);
    }

    /** Changes the GIF without replacing the view or closing its parent dialog. */
    public void setAssetName(String assetName) {
        final int generation = requestGeneration.incrementAndGet();
        if (assetName == null || assetName.trim().isEmpty()) {
            movie = null;
            invalidate();
            return;
        }

        Movie cached;
        synchronized (CACHE) {
            cached = CACHE.get(assetName);
        }
        if (cached != null) {
            movie = cached;
            startedAt = SystemClock.uptimeMillis();
            invalidate();
            return;
        }

        movie = null;
        invalidate();
        final String requestedAsset = assetName;
        DECODER.execute(() -> {
            Movie decoded = null;
            try (InputStream in = getContext().getApplicationContext().getAssets()
                    .open("exercises/" + requestedAsset)) {
                decoded = Movie.decodeStream(in);
            } catch (Exception ignored) {
                // A missing/corrupt GIF should not crash the library.
            }

            if (decoded != null) {
                synchronized (CACHE) {
                    CACHE.put(requestedAsset, decoded);
                }
            }
            final Movie result = decoded;
            mainHandler.post(() -> {
                if (requestGeneration.get() != generation) return;
                movie = result;
                startedAt = SystemClock.uptimeMillis();
                invalidate();
            });
        });
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Movie current = movie;
        if (current == null) return;

        int duration = current.duration();
        if (duration <= 0) duration = 1000;
        float sx = getWidth() / (float) Math.max(1, current.width());
        float sy = getHeight() / (float) Math.max(1, current.height());
        float scale = Math.min(sx, sy);
        canvas.save();
        canvas.translate(
                (getWidth() - current.width() * scale) / 2f,
                (getHeight() - current.height() * scale) / 2f);
        canvas.scale(scale, scale);
        current.setTime((int) ((SystemClock.uptimeMillis() - startedAt) % duration));
        current.draw(canvas, 0, 0);
        canvas.restore();
        postInvalidateOnAnimation();
    }
}
