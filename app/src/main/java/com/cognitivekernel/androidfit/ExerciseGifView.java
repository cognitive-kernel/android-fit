package com.cognitivekernel.androidfit;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.io.InputStream;

/**
 * Animated GIF view for bundled exercise demonstrations.
 * Tapping the preview opens the original GIF in a full-screen viewer.
 */
public final class ExerciseGifView extends View {
    private final Movie movie;
    private final long startedAt;
    private final String assetName;

    public ExerciseGifView(Context context, String assetName) {
        super(context);
        this.assetName = assetName == null ? "" : assetName;
        Movie decoded = null;
        try (InputStream in = context.getAssets().open("exercises/" + this.assetName)) {
            decoded = Movie.decodeStream(in);
        } catch (Exception ignored) { }
        movie = decoded;
        startedAt = SystemClock.uptimeMillis();
        setContentDescription(this.assetName.replace(".gif", "").replace('_', ' ') + " — tap to enlarge");
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setClickable(true);
        setFocusable(true);
        setOnClickListener(v -> showFullScreen());
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
