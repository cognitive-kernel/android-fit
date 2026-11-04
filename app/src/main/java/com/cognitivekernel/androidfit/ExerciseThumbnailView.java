package com.cognitivekernel.androidfit;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.drawable.Drawable;
import android.graphics.ImageDecoder;
import android.os.Build;
import android.util.AttributeSet;
import android.util.LruCache;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** First-frame-only, bounded thumbnail loader. It never decodes or animates a full GIF in a list row. */
final class ExerciseThumbnailView extends androidx.appcompat.widget.AppCompatImageView {
    private static final String TAG = "ExerciseThumbnail";
    private static final int EDGE_PX = 192;
    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(2);
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(6 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) {
            return Math.max(1, value.getByteCount() / 1024);
        }
    };
    private String boundPath;

    ExerciseThumbnailView(Context context) { super(context); init(); }
    ExerciseThumbnailView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        setScaleType(ScaleType.CENTER_CROP);
        setBackgroundColor(0xFFE8EEF5);
        setContentDescription("Exercise image");
    }

    void load(String path) {
        boundPath = path;
        setImageDrawable(null);
        Bitmap cached = CACHE.get(path);
        if (cached != null && !cached.isRecycled()) {
            setImageBitmap(cached);
            return;
        }
        WORKERS.execute(() -> {
            Bitmap decoded = null;
            try {
                decoded = decodeFirstFrame(getContext().getApplicationContext(), path);
            } catch (Exception error) {
                Log.e(TAG, "Thumbnail decode failed: " + path, error);
            }
            if (decoded == null) return;
            CACHE.put(path, decoded);
            Bitmap result = decoded;
            post(() -> {
                if (path.equals(boundPath) && !result.isRecycled()) setImageBitmap(result);
            });
        });
    }

    private static Bitmap decodeFirstFrame(Context context, String path) throws Exception {
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
