package com.cognitivekernel.androidfit;

import android.content.Context;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Persistent application error log, independent from Android's restricted system logcat. */
public final class AppErrorLog {
    private static final String FILE_NAME = "application-errors.log";
    private static final Object LOCK = new Object();
    private static volatile Thread.UncaughtExceptionHandler previousHandler;

    private AppErrorLog() {}

    public static void initialize(Context context) {
        synchronized (LOCK) {
            if (previousHandler != null) return;
            previousHandler = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
                record(context, "UNCAUGHT", "Uncaught exception on thread " + thread.getName(), error);
                Thread.UncaughtExceptionHandler previous = previousHandler;
                if (previous != null) previous.uncaughtException(thread, error);
            });
        }
    }

    public static void record(Context context, String type, String message, Throwable error) {
        if (context == null) return;
        StringBuilder entry = new StringBuilder();
        entry.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date()))
            .append(" | ").append(type == null ? "ERROR" : type)
            .append(" | ").append(message == null ? "" : message).append('\n');
        if (error != null) {
            java.io.StringWriter trace = new java.io.StringWriter();
            error.printStackTrace(new java.io.PrintWriter(trace));
            entry.append(trace).append('\n');
        }
        synchronized (LOCK) {
            File file = new File(context.getApplicationContext().getFilesDir(), FILE_NAME);
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
                writer.write(entry.toString());
                writer.flush();
            } catch (IOException ignored) {
                android.util.Log.e("AppErrorLog", "Unable to persist application error", error);
            }
        }
    }

    public static String readAll(Context context) throws IOException {
        File file = new File(context.getApplicationContext().getFilesDir(), FILE_NAME);
        if (!file.exists()) return "";
        if (file.length() > 5L * 1024L * 1024L) {
            throw new IOException("Application error log exceeds the 5 MB display limit");
        }
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream input = new FileInputStream(file)) {
            int offset = 0;
            while (offset < bytes.length) {
                int count = input.read(bytes, offset, bytes.length - offset);
                if (count < 0) break;
                offset += count;
            }
            return new String(bytes, 0, offset, StandardCharsets.UTF_8);
        }
    }
}
