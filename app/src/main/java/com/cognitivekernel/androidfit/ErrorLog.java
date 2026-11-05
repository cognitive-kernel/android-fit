package com.cognitivekernel.androidfit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Persistent on-device application error log and viewer. */
public final class ErrorLog {
    private static final String PREFS = "fit_error_log";
    private static final String KEY_LOG = "entries";
    private static final int MAX_CHARS = 100000;
    private ErrorLog() {}

    public static void install(Context context) {
        final Context app = context.getApplicationContext();
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            record(app, "UNCAUGHT", thread == null ? "unknown" : thread.getName(), error);
            if (previous != null) previous.uncaughtException(thread, error);
        });
    }

    public static void record(Context context, String source, String detail, Throwable error) {
        if (context == null) return;
        StringWriter stack = new StringWriter();
        if (error != null) error.printStackTrace(new PrintWriter(stack));
        String entry = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date())
            + " [" + safe(source) + "] " + safe(detail) + "\n" + stack + "\n";
        SharedPreferences prefs = context.getApplicationContext()
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String old = prefs.getString(KEY_LOG, "");
        String combined = entry + old;
        if (combined.length() > MAX_CHARS) combined = combined.substring(0, MAX_CHARS);
        prefs.edit().putString(KEY_LOG, combined).apply();
    }

    public static void show(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String entries = prefs.getString(KEY_LOG, "");
        TextView text = new TextView(context);
        text.setTextSize(12);
        text.setText(TextUtils.isEmpty(entries) ? "No application errors have been recorded." : entries);
        int pad = (int) (12 * context.getResources().getDisplayMetrics().density);
        text.setPadding(pad, pad, pad, pad);
        ScrollView scroll = new ScrollView(context);
        scroll.addView(text, new ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        new AlertDialog.Builder(context)
            .setTitle("Application error log")
            .setView(scroll)
            .setNeutralButton("Clear", (dialog, which) -> prefs.edit().remove(KEY_LOG).apply())
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace("\\n", " ").replace("\\r", " ");
    }
}
