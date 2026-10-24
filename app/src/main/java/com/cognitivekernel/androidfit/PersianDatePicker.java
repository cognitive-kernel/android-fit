package com.cognitivekernel.androidfit;

import android.app.AlertDialog;
import android.content.Context;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import java.util.Locale;

public final class PersianDatePicker {
    public interface Listener { void onDate(String date); }
    private PersianDatePicker() {}

    public static void show(Context context, String current, Listener listener) {
        int year = 1405, month = 1, day = 1;
        try {
            String[] p = current.split("/");
            year = Integer.parseInt(p[0]); month = Integer.parseInt(p[1]); day = Integer.parseInt(p[2]);
        } catch (Exception ignored) {}
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 8, 24, 8);
        TextView label = new TextView(context);
        label.setText("سال، ماه و روز شمسی را انتخاب کنید");
        label.setGravity(Gravity.CENTER);
        layout.addView(label);
        LinearLayout pickers = new LinearLayout(context);
        pickers.setOrientation(LinearLayout.HORIZONTAL);
        NumberPicker dp = new NumberPicker(context);
        dp.setMinValue(1); dp.setMaxValue(31); dp.setValue(Math.min(day,31));
        NumberPicker mp = new NumberPicker(context);
        mp.setMinValue(1); mp.setMaxValue(12); mp.setDisplayedValues(new String[]{"فروردین","اردیبهشت","خرداد","تیر","مرداد","شهریور","مهر","آبان","آذر","دی","بهمن","اسفند"});
        mp.setValue(month);
        NumberPicker yp = new NumberPicker(context);
        yp.setMinValue(1300); yp.setMaxValue(1500); yp.setValue(year);
        pickers.addView(dp, new LinearLayout.LayoutParams(0, -2, 1));
        pickers.addView(mp, new LinearLayout.LayoutParams(0, -2, 1));
        pickers.addView(yp, new LinearLayout.LayoutParams(0, -2, 1));
        layout.addView(pickers);
        Runnable adjustDay = () -> {
            int max = mp.getValue() <= 6 ? 31 : mp.getValue() <= 11 ? 30 : (isLeapJalali(yp.getValue()) ? 30 : 29);
            dp.setMaxValue(max);
            if (dp.getValue() > max) dp.setValue(max);
        };
        mp.setOnValueChangedListener((picker, oldValue, newValue) -> adjustDay.run());
        yp.setOnValueChangedListener((picker, oldValue, newValue) -> adjustDay.run());
        adjustDay.run();
        new AlertDialog.Builder(context).setTitle("انتخاب تاریخ شمسی").setView(layout)
            .setPositiveButton("تأیید", (dialog, which) -> listener.onDate(String.format(Locale.US, "%04d/%02d/%02d", yp.getValue(), mp.getValue(), dp.getValue())))
            .setNegativeButton("لغو", null).show();
    }

    private static boolean isLeapJalali(int year) {
        int[] breaks = {-61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178};
        int jp = breaks[0], jump = 0;
        for (int i = 1; i < breaks.length; i++) {
            int jm = breaks[i]; jump = jm - jp;
            if (year < jm) break;
            jp = jm;
        }
        int n = year - jp;
        if (jump - n < 6) n = n - jump + ((jump + 4) / 33) * 33;
        int leap = (((n + 1) % 33) - 1) % 4;
        if (leap == -1) leap = 4;
        return leap == 0;
    }
}