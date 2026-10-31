package com.cognitivekernel.androidfit;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.ImageDecoder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import androidx.annotation.WorkerThread;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Offline exercise image renderer.
 *
 * Prefer Android's native animated GIF decoder, fall back to Movie on older or
 * incompatible devices, and finally retain a decoded first frame so an image
 * is still visible if animation decoding is unavailable.
 */
public final class ExerciseGifView extends View {
    private static final String TAG = "ExerciseGifView";
    private static final ExecutorService DECODER = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final String assetPath;
    private volatile Drawable drawable;
    private volatile Movie movie;
    private volatile Bitmap bitmap;
    private volatile long startedAt;
    private volatile boolean attached;

    public ExerciseGifView(Context context, String name) {
        super(context);
        String normalized = name == null ? "" : name.trim();
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        if (!normalized.startsWith("exercises/")) normalized = "exercises/" + normalized;
        assetPath = normalized;
        setContentDescription(normalized.substring(normalized.lastIndexOf('/') + 1)
            .replace(".gif", "").replace('_', ' '));
        setWillNotDraw(false);
        DECODER.execute(() -> decode(context.getApplicationContext()));
    }

    private void decode(Context context) {
        Drawable decodedDrawable = null;
        Movie decodedMovie = null;
        Bitmap decodedBitmap = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                decodedDrawable = decodeNative(context);
            } catch (Exception error) {
                Log.w(TAG, "Native GIF decoder failed for " + assetPath, error);
            }
        }

        if (decodedDrawable == null) {
            try (InputStream input = context.getAssets().open(assetPath)) {
                decodedMovie = Movie.decodeStream(input);
                if (decodedMovie != null &&
                    (decodedMovie.width() <= 0 || decodedMovie.height() <= 0)) {
                    decodedMovie = null;
                }
            } catch (Exception error) {
                Log.w(TAG, "Movie GIF decoder failed for " + assetPath, error);
            }
        }

        if (decodedDrawable == null && decodedMovie == null) {
            try (InputStream input = context.getAssets().open(assetPath)) {
                decodedBitmap = BitmapFactory.decodeStream(input);
            } catch (Exception error) {
                Log.e(TAG, "Static image fallback failed for " + assetPath, error);
            }
        }

        final Drawable resultDrawable = decodedDrawable;
        final Movie resultMovie = decodedMovie;
        final Bitmap resultBitmap = decodedBitmap;
        MAIN.post(() -> {
            drawable = resultDrawable;
            movie = resultMovie;
            bitmap = resultBitmap;
            startedAt = SystemClock.uptimeMillis();
            if (drawable != null) {
                drawable.setCallback(this);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    configureNativeLayer(drawable);
                } else {
                    setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                    attached && drawable instanceof AnimatedImageDrawable) {
                    startAnimation((AnimatedImageDrawable) drawable);
                }
            } else {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
            invalidate();
        });
    }

    @TargetApi(Build.VERSION_CODES.P)
    private void configureNativeLayer(Drawable image) {
        setLayerType(image instanceof AnimatedImageDrawable
            ? View.LAYER_TYPE_HARDWARE : View.LAYER_TYPE_SOFTWARE, null);
    }

    @WorkerThread
    @TargetApi(Build.VERSION_CODES.P)
    private Drawable decodeNative(Context context) throws Exception {
        ImageDecoder.Source source = ImageDecoder.createSource(context.getAssets(), assetPath);
        Drawable result = ImageDecoder.decodeDrawable(source);
        if (result instanceof AnimatedImageDrawable) {
            ((AnimatedImageDrawable) result)
                .setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE);
        }
        return result;
    }

    @TargetApi(Build.VERSION_CODES.P)
    private void startAnimation(AnimatedImageDrawable image) {
        image.start();
    }

    @TargetApi(Build.VERSION_CODES.P)
    private void stopAnimation(AnimatedImageDrawable image) {
        image.stop();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        Drawable current = drawable;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            current instanceof AnimatedImageDrawable) {
            startAnimation((AnimatedImageDrawable) current);
        }
    }

    @Override protected void onDetachedFromWindow() {
        attached = false;
        Drawable current = drawable;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            current instanceof AnimatedImageDrawable) {
            stopAnimation((AnimatedImageDrawable) current);
        }
        super.onDetachedFromWindow();
    }

    @Override protected boolean verifyDrawable(Drawable who) {
        return who == drawable || super.verifyDrawable(who);
    }

    /** Package-private test hook: true only after a real image decoder produced pixels. */
    boolean hasDecodedImage() {
        return drawable != null || movie != null || (bitmap != null && !bitmap.isRecycled());
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        Drawable currentDrawable = drawable;
        if (currentDrawable != null) {
            int intrinsicWidth = Math.max(1, currentDrawable.getIntrinsicWidth());
            int intrinsicHeight = Math.max(1, currentDrawable.getIntrinsicHeight());
            float scale = Math.min(width / (float) intrinsicWidth,
                height / (float) intrinsicHeight);
            int drawWidth = Math.max(1, Math.round(intrinsicWidth * scale));
            int drawHeight = Math.max(1, Math.round(intrinsicHeight * scale));
            int left = (width - drawWidth) / 2;
            int top = (height - drawHeight) / 2;
            currentDrawable.setBounds(left, top, left + drawWidth, top + drawHeight);
            currentDrawable.draw(canvas);
            return;
        }

        Movie currentMovie = movie;
        if (currentMovie != null) {
            int sourceWidth = currentMovie.width();
            int sourceHeight = currentMovie.height();
            int duration = Math.max(1, currentMovie.duration());
            float scale = Math.min(width / (float) sourceWidth, height / (float) sourceHeight);
            canvas.save();
            canvas.translate((width - sourceWidth * scale) / 2f,
                (height - sourceHeight * scale) / 2f);
            canvas.scale(scale, scale);
            currentMovie.setTime((int) ((SystemClock.uptimeMillis() - startedAt) % duration));
            currentMovie.draw(canvas, 0, 0);
            canvas.restore();
            postInvalidateOnAnimation();
            return;
        }

        Bitmap currentBitmap = bitmap;
        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            Rect destination = fitRect(currentBitmap.getWidth(), currentBitmap.getHeight(), width, height);
            canvas.drawBitmap(currentBitmap, null, destination, null);
            return;
        }

        drawPlaceholder(canvas, width, height);
    }

    private static Rect fitRect(int sourceWidth, int sourceHeight, int width, int height) {
        float scale = Math.min(width / (float) sourceWidth, height / (float) sourceHeight);
        int drawWidth = Math.max(1, Math.round(sourceWidth * scale));
        int drawHeight = Math.max(1, Math.round(sourceHeight * scale));
        int left = (width - drawWidth) / 2;
        int top = (height - drawHeight) / 2;
        return new Rect(left, top, left + drawWidth, top + drawHeight);
    }

    private void drawPlaceholder(Canvas canvas, int width, int height) {
        android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xFFE8EEF5);
        canvas.drawRoundRect(0, 0, width, height, 12, 12, paint);
        float cx = width / 2f;
        float cy = height / 2f;
        float unit = Math.min(width, height) / 7f;
        paint.setColor(0xFF7890A8);
        paint.setStrokeWidth(Math.max(2f, unit * 0.18f));
        paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
        paint.setStyle(android.graphics.Paint.Style.STROKE);
        canvas.drawCircle(cx, cy - unit * 1.3f, unit * 0.55f, paint);
        canvas.drawLine(cx, cy - unit * 0.7f, cx, cy + unit * 0.8f, paint);
        canvas.drawLine(cx, cy - unit * 0.1f, cx - unit, cy + unit * 0.45f, paint);
        canvas.drawLine(cx, cy - unit * 0.1f, cx + unit, cy + unit * 0.45f, paint);
        canvas.drawLine(cx, cy + unit * 0.8f, cx - unit * 0.8f, cy + unit * 1.8f, paint);
        canvas.drawLine(cx, cy + unit * 0.8f, cx + unit * 0.8f, cy + unit * 1.8f, paint);
    }
}
