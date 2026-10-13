package com.cognitivekernel.androidfit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class ReportChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<String> labels = new ArrayList<>();
    private List<Float> values = new ArrayList<>();
    private final int ink = 0xFF53617A;
    private final int blue = 0xFF365CF5;
    private final int grid = 0xFFE3E8F2;

    public ReportChartView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setData(List<String> labels, List<Float> values) {
        this.labels = new ArrayList<>(labels);
        this.values = new ArrayList<>(values);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        float left = 42, right = w - 10, top = 18, bottom = h - 38;
        paint.setTextSize(11 * getResources().getDisplayMetrics().scaledDensity);
        paint.setColor(ink); paint.setStrokeWidth(1);
        if (values.isEmpty()) {
            paint.setTextSize(14 * getResources().getDisplayMetrics().scaledDensity);
            canvas.drawText("هنوز داده‌ای برای نمودار وجود ندارد", 12, h / 2, paint);
            return;
        }
        float max = 1;
        for (Float v : values) if (v != null && v > max) max = v;
        for (int i = 0; i <= 4; i++) {
            float y = bottom - (bottom - top) * i / 4f;
            paint.setColor(grid);
            canvas.drawLine(left, y, right, y, paint);
            paint.setColor(ink);
            canvas.drawText(String.valueOf(Math.round(max * i / 4f)), 3, y + 4, paint);
        }
        int n = values.size();
        float slot = (right - left) / Math.max(1, n);
        float barWidth = Math.max(3, Math.min(24, slot * .58f));
        for (int i = 0; i < n; i++) {
            float value = values.get(i) == null ? 0 : values.get(i);
            float bh = (bottom - top) * value / max;
            float x = left + slot * i + (slot - barWidth) / 2;
            paint.setColor(blue);
            canvas.drawRoundRect(new RectF(x, bottom - bh, x + barWidth, bottom), 5, 5, paint);
            if (n <= 10 || i == 0 || i == n - 1 || i % Math.max(1, n / 5) == 0) {
                paint.setColor(ink);
                String label = labels.get(i);
                float tw = paint.measureText(label);
                canvas.drawText(label, x + barWidth / 2 - tw / 2, h - 10, paint);
            }
        }
    }
}
