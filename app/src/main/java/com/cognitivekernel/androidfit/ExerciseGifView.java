package com.cognitivekernel.androidfit;

import android.annotation.TargetApi;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.ImageDecoder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import androidx.annotation.WorkerThread;
import java.io.InputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Offline animated-GIF view with asynchronous native decoding on Android 9+. */
@SuppressLint("WrongThread")
public final class ExerciseGifView extends View {
    private static final ExecutorService DECODER = Executors.newFixedThreadPool(2);
    private final String assetName;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile Movie movie;
    private volatile Drawable animatedDrawable;
    private volatile long startedAt;
    private volatile boolean attached;

    public ExerciseGifView(Context context, String name) {
        super(context);
        String normalized = name == null ? "" : name;
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        if (normalized.startsWith("exercises/")) normalized = normalized.substring("exercises/".length());
        assetName = normalized;
        setContentDescription(normalized.replace(".gif", "").replace('_', ' '));
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        Context appContext = context.getApplicationContext();
        DECODER.execute(() -> decode(appContext));
    }

    private void decode(Context context) {
        Drawable drawable = null;
        Movie decodedMovie = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                drawable = decodeNative(context);
            } catch (Exception ignored) {
                drawable = null;
            }
        }
        if (drawable == null) {
            try (InputStream in = context.getAssets().open("exercises/" + assetName)) {
                decodedMovie = Movie.decodeStream(in);
            } catch (Exception ignored) {
                decodedMovie = null;
            }
        }
        final Drawable resultDrawable = drawable;
        final Movie resultMovie = decodedMovie;
        mainHandler.post(() -> {
            animatedDrawable = resultDrawable;
            movie = resultMovie;
            startedAt = SystemClock.uptimeMillis();
            if (animatedDrawable != null) {
                animatedDrawable.setCallback(this);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && attached && animatedDrawable instanceof AnimatedImageDrawable) {
                    startNative((AnimatedImageDrawable) animatedDrawable);
                }
            }
            invalidate();
        });
    }

    @WorkerThread
    @TargetApi(Build.VERSION_CODES.P)
    private Drawable decodeNative(Context context) throws IOException {
        ImageDecoder.Source source = ImageDecoder.createSource(context.getAssets(), "exercises/" + assetName);
        Drawable candidate = ImageDecoder.decodeDrawable(source);
        if (candidate instanceof AnimatedImageDrawable) {
            ((AnimatedImageDrawable) candidate).setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE);
        }
        return candidate;
    }

    @TargetApi(Build.VERSION_CODES.P)
    private void startNative(AnimatedImageDrawable drawable) { drawable.start(); }

    @TargetApi(Build.VERSION_CODES.P)
    private void stopNative(AnimatedImageDrawable drawable) { drawable.stop(); }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        Drawable drawable = animatedDrawable;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && drawable instanceof AnimatedImageDrawable) {
            startNative((AnimatedImageDrawable) drawable);
        }
    }

    @Override protected void onDetachedFromWindow() {
        attached = false;
        Drawable drawable = animatedDrawable;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && drawable instanceof AnimatedImageDrawable) {
            stopNative((AnimatedImageDrawable) drawable);
        }
        super.onDetachedFromWindow();
    }

    @Override protected boolean verifyDrawable(Drawable who) {
        return who == animatedDrawable || super.verifyDrawable(who);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Drawable drawable = animatedDrawable;
        if (drawable != null) {
            int intrinsicWidth = Math.max(1, drawable.getIntrinsicWidth());
            int intrinsicHeight = Math.max(1, drawable.getIntrinsicHeight());
            float scale = Math.min(getWidth() / (float) intrinsicWidth, getHeight() / (float) intrinsicHeight);
            int width = Math.max(1, Math.round(intrinsicWidth * scale));
            int height = Math.max(1, Math.round(intrinsicHeight * scale));
            int left = (getWidth() - width) / 2;
            int top = (getHeight() - height) / 2;
            drawable.setBounds(left, top, left + width, top + height);
            drawable.draw(canvas);
            return;
        }
        Movie currentMovie = movie;
        if (currentMovie == null) return;
        int duration = currentMovie.duration();
        if (duration <= 0) duration = 1000;
        float sx = getWidth() / (float) Math.max(1, currentMovie.width());
        float sy = getHeight() / (float) Math.max(1, currentMovie.height());
        float scale = Math.min(sx, sy);
        canvas.save();
        canvas.translate((getWidth() - currentMovie.width() * scale) / 2f,
                         (getHeight() - currentMovie.height() * scale) / 2f);
        canvas.scale(scale, scale);
        currentMovie.setTime((int) ((SystemClock.uptimeMillis() - startedAt) % duration));
        currentMovie.draw(canvas, 0, 0);
        canvas.restore();
        postInvalidateOnAnimation();
    }
}
