package com.cognitivekernel.androidfit;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.ImageDecoder;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import java.io.InputStream;

/** Offline animated-GIF view with native animated-image decoding on Android 9+. */
@TargetApi(Build.VERSION_CODES.P)
@SuppressLint("NewApi")
public final class ExerciseGifView extends View {
    private final Movie movie;
    private final Drawable animatedDrawable;
    private final long startedAt;

    public ExerciseGifView(Context context, String assetName) {
        super(context);
        Movie decodedMovie = null;
        Drawable decodedDrawable = null;
        String normalizedName = assetName == null ? "" : assetName;
        while (normalizedName.startsWith("/")) normalizedName = normalizedName.substring(1);
        if (normalizedName.startsWith("exercises/")) normalizedName = normalizedName.substring("exercises/".length());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                ImageDecoder.Source source = ImageDecoder.createSource(
                    context.getAssets(), "exercises/" + normalizedName);
                Drawable candidate = ImageDecoder.decodeDrawable(source);
                if (candidate instanceof AnimatedImageDrawable) {
                    ((AnimatedImageDrawable) candidate).setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE);
                    decodedDrawable = candidate;
                    decodedDrawable.setCallback(this);
                } else {
                    decodedDrawable = candidate;
                }
            } catch (Exception ignored) {
                decodedDrawable = null;
            }
        }

        if (decodedDrawable == null) {
            try (InputStream in = context.getAssets().open("exercises/" + normalizedName)) {
                decodedMovie = Movie.decodeStream(in);
            } catch (Exception ignored) {
                decodedMovie = null;
            }
        }

        movie = decodedMovie;
        animatedDrawable = decodedDrawable;
        startedAt = SystemClock.uptimeMillis();
        setContentDescription(normalizedName.replace(".gif", "").replace('_', ' '));
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (animatedDrawable instanceof AnimatedImageDrawable) {
            ((AnimatedImageDrawable) animatedDrawable).start();
        }
    }

    @Override protected void onDetachedFromWindow() {
        if (animatedDrawable instanceof AnimatedImageDrawable) {
            ((AnimatedImageDrawable) animatedDrawable).stop();
        }
        super.onDetachedFromWindow();
    }

    @Override protected boolean verifyDrawable(Drawable who) {
        return who == animatedDrawable || super.verifyDrawable(who);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (animatedDrawable != null) {
            int intrinsicWidth = Math.max(1, animatedDrawable.getIntrinsicWidth());
            int intrinsicHeight = Math.max(1, animatedDrawable.getIntrinsicHeight());
            float scale = Math.min(getWidth() / (float) intrinsicWidth,
                                   getHeight() / (float) intrinsicHeight);
            int width = Math.max(1, Math.round(intrinsicWidth * scale));
            int height = Math.max(1, Math.round(intrinsicHeight * scale));
            int left = (getWidth() - width) / 2;
            int top = (getHeight() - height) / 2;
            animatedDrawable.setBounds(left, top, left + width, top + height);
            animatedDrawable.draw(canvas);
            return;
        }
        if (movie == null) return;

        int duration = movie.duration();
        if (duration <= 0) duration = 1000;
        float sx = getWidth() / (float) Math.max(1, movie.width());
        float sy = getHeight() / (float) Math.max(1, movie.height());
        float scale = Math.min(sx, sy);
        canvas.save();
        canvas.translate((getWidth() - movie.width() * scale) / 2f,
                         (getHeight() - movie.height() * scale) / 2f);
        canvas.scale(scale, scale);
        movie.setTime((int) ((SystemClock.uptimeMillis() - startedAt) % duration));
        movie.draw(canvas, 0, 0);
        canvas.restore();
        postInvalidateOnAnimation();
    }
}
