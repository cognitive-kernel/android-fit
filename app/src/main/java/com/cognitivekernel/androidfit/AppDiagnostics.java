package com.cognitivekernel.androidfit;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Persistent, privacy-conscious app diagnostics stored only in the app's private files directory. */
public final class AppDiagnostics {
    private static final String TAG = "FitYarDiagnostics";
    private static final String FILE_NAME = "diagnostics.log";
    private static final long MAX_BYTES = 1024L * 1024L;
    private static volatile Context appContext;
    private static Thread.UncaughtExceptionHandler previousHandler;

    private AppDiagnostics() {}

    public static synchronized void initialize(Context context) {
        if (appContext != null) return;
        appContext = context.getApplicationContext();
        previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            record("CRASH", "Uncaught exception on thread " + thread.getName(), error);
            if (previousHandler != null) previousHandler.uncaughtException(thread, error);
        });
        record("APP", "Diagnostics initialized; Android " + Build.VERSION.RELEASE
            + " (API " + Build.VERSION.SDK_INT + "), device " + Build.MANUFACTURER + " " + Build.MODEL);
    }

    public static void record(String area, String message) {
        write(area, message, null);
    }

    public static void record(String area, String message, Throwable error) {
        write(area, message, error);
    }

    private static synchronized void write(String area, String message, Throwable error) {
        Context context = appContext;
        if (context == null) return;
        StringBuilder entry = new StringBuilder();
        entry.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date()))
            .append(" | ").append(area == null ? "APP" : area)
            .append(" | ").append(message == null ? "" : message);
        if (error != null) {
            StringWriter stack = new StringWriter();
            error.printStackTrace(new PrintWriter(stack));
            entry.append("\n").append(stack);
            Log.e(TAG, String.valueOf(message), error);
        }
        entry.append("\n");
        try {
            File file = new File(context.getFilesDir(), FILE_NAME);
            if (file.exists() && file.length() + entry.length() > MAX_BYTES) {
                File backup = new File(context.getFilesDir(), FILE_NAME + ".1");
                if (backup.exists()) backup.delete();
                if (!file.renameTo(backup)) file.delete();
            }
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(file, true), StandardCharsets.UTF_8)) {
                writer.write(entry.toString());
            }
        } catch (Exception writeError) {
            Log.e(TAG, "Could not persist diagnostic log", writeError);
        }
    }

    public static synchronized String readAll(Context context) {
        StringBuilder result = new StringBuilder();
        File backup = new File(context.getFilesDir(), FILE_NAME + ".1");
        File current = new File(context.getFilesDir(), FILE_NAME);
        appendFile(backup, result);
        appendFile(current, result);
        return result.length() == 0 ? "هنوز گزارشی ثبت نشده است." : result.toString();
    }

    private static void appendFile(File file, StringBuilder output) {
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) output.append(line).append('\n');
        } catch (Exception error) {
            output.append("Could not read ").append(file.getName()).append(": ")
                .append(error.getClass().getSimpleName()).append(": ").append(error.getMessage()).append('\n');
        }
    }

    public static synchronized void clear(Context context) {
        new File(context.getFilesDir(), FILE_NAME).delete();
        new File(context.getFilesDir(), FILE_NAME + ".1").delete();
        record("APP", "Diagnostic log cleared by user");
    }
}
