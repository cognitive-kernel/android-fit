package com.cognitivekernel.androidfit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * GIF renderer based on Android's Movie API. It works on every supported API
 * level (minSdk 23) and avoids AnimatedImageDrawable rendering differences.
 */
public final class ExerciseGifView extends View {
    private static final String TAG = "ExerciseGifView";
    private static final ExecutorService DECODER = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final String assetPath;
    private final Paint placeholderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private volatile Movie movie;
    private volatile long startedAt;
    private volatile boolean loadFailed;

    public ExerciseGifView(Context context, String name) {
        super(context);
        String normalized = name == null ? "" : name.trim();
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        if (!normalized.startsWith("exercises/")) normalized = "exercises/" + normalized;
        assetPath = normalized;
        setContentDescription(normalized.substring(normalized.lastIndexOf('/') + 1).replace(".gif", "").replace('_', ' '));
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setWillNotDraw(false);
        DECODER.execute(() -> decode(context.getApplicationContext()));
    }

    private void decode(Context context) {
        Movie result = null;
        try (InputStream in = context.getAssets().open(assetPath)) {
            result = Movie.decodeStream(in);
            if (result == null || result.width() <= 0 || result.height() <= 0) {
                result = null;
                Log.w(TAG, "GIF could not be decoded: " + assetPath);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Unable to load bundled image: " + assetPath, ex);
        }
        final Movie decoded = result;
        MAIN.post(() -> {
            movie = decoded;
            loadFailed = decoded == null;
            startedAt = SystemClock.uptimeMillis();
            invalidate();
        });
    }

    /** Package-private test hook: true only after Movie decoded the bundled GIF. */
    boolean hasDecodedImage() {
        return movie != null;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Movie current = movie;
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        if (viewWidth <= 0 || viewHeight <= 0) return;

        if (current == null) {
            drawPlaceholder(canvas, viewWidth, viewHeight);
            return;
        }

        int sourceWidth = current.width();
        int sourceHeight = current.height();
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            drawPlaceholder(canvas, viewWidth, viewHeight);
            return;
        }
        int duration = Math.max(1, current.duration());
        float scale = Math.min(viewWidth / (float) sourceWidth, viewHeight / (float) sourceHeight);
        canvas.save();
        canvas.translate((viewWidth - sourceWidth * scale) / 2f, (viewHeight - sourceHeight * scale) / 2f);
        canvas.scale(scale, scale);
        current.setTime((int) ((SystemClock.uptimeMillis() - startedAt) % duration));
        current.draw(canvas, 0, 0);
        canvas.restore();
        postInvalidateOnAnimation();
    }

    private void drawPlaceholder(Canvas canvas, int width, int height) {
        placeholderPaint.setColor(0xFFE8EEF5);
        canvas.drawRoundRect(new RectF(0, 0, width, height), 12, 12, placeholderPaint);
        float cx = width / 2f;
        float cy = height / 2f;
        float unit = Math.min(width, height) / 7f;
        placeholderPaint.setColor(0xFF7890A8);
        placeholderPaint.setStrokeWidth(Math.max(2f, unit * 0.18f));
        placeholderPaint.setStrokeCap(Paint.Cap.ROUND);
        placeholderPaint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(cx, cy - unit * 1.3f, unit * 0.55f, placeholderPaint);
        canvas.drawLine(cx, cy - unit * 0.7f, cx, cy + unit * 0.8f, placeholderPaint);
        canvas.drawLine(cx, cy - unit * 0.1f, cx - unit, cy + unit * 0.45f, placeholderPaint);
        canvas.drawLine(cx, cy - unit * 0.1f, cx + unit, cy + unit * 0.45f, placeholderPaint);
        canvas.drawLine(cx, cy + unit * 0.8f, cx - unit * 0.8f, cy + unit * 1.8f, placeholderPaint);
        canvas.drawLine(cx, cy + unit * 0.8f, cx + unit * 0.8f, cy + unit * 1.8f, placeholderPaint);
        placeholderPaint.setStyle(Paint.Style.FILL);
    }
}
