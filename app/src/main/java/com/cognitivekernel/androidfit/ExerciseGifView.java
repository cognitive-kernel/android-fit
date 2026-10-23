package com.cognitivekernel.androidfit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.os.SystemClock;
import android.view.View;
import java.io.InputStream;

/** Small offline animated-GIF view for bundled exercise demonstrations. */
public final class ExerciseGifView extends View {
    private final Movie movie;
    private final long startedAt;
    public ExerciseGifView(Context context, String assetName) {
        super(context);
        Movie decoded = null;
        try (InputStream in = context.getAssets().open("exercises/" + assetName)) {
            decoded = Movie.decodeStream(in);
        } catch (Exception ignored) { }
        movie = decoded;
        startedAt = SystemClock.uptimeMillis();
        setContentDescription(assetName.replace(".gif", "").replace('_', ' '));
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (movie == null) return;
        int duration = movie.duration();
        if (duration <= 0) duration = 1000;
        float sx = getWidth() / (float) Math.max(1, movie.width());
        float sy = getHeight() / (float) Math.max(1, movie.height());
        float scale = Math.min(sx, sy);
        canvas.save();
        canvas.translate((getWidth() - movie.width() * scale) / 2f, (getHeight() - movie.height() * scale) / 2f);
        canvas.scale(scale, scale);
        movie.setTime((int) ((SystemClock.uptimeMillis() - startedAt) % duration));
        movie.draw(canvas, 0, 0);
        canvas.restore();
        postInvalidateOnAnimation();
    }
}
