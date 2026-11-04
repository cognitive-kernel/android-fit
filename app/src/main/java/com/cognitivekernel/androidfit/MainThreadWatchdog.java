package com.cognitivekernel.androidfit;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Records Android UI-thread stalls (including hangs that never reach the uncaught handler). */
final class MainThreadWatchdog {
    private static final long PING_INTERVAL_MS = 500L;
    private static final long STALL_THRESHOLD_MS = 2500L;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final HandlerThread thread = new HandlerThread("fityar-main-watchdog", Process.THREAD_PRIORITY_BACKGROUND);
    private final AtomicLong lastResponse = new AtomicLong(SystemClock.uptimeMillis());
    private final AtomicBoolean stallRecorded = new AtomicBoolean(false);
    private final Thread mainThread = Looper.getMainLooper().getThread();
    private final android.content.Context context;

    MainThreadWatchdog(android.content.Context context) {
        this.context = context.getApplicationContext();
        thread.start();
        Handler monitor = new Handler(thread.getLooper());
        Runnable ping = new Runnable() {
            @Override public void run() {
                main.post(() -> lastResponse.set(SystemClock.uptimeMillis()));
                monitor.postDelayed(this, PING_INTERVAL_MS);
            }
        };
        Runnable check = new Runnable() {
            @Override public void run() {
                long delay = SystemClock.uptimeMillis() - lastResponse.get();
                if (delay >= STALL_THRESHOLD_MS && stallRecorded.compareAndSet(false, true)) {
                    RuntimeException trace = new RuntimeException(
                            "Main thread unresponsive for at least " + delay + " ms");
                    trace.setStackTrace(mainThread.getStackTrace());
                    AppErrorLog.record(context, "MAIN_THREAD_STALL",
                            "UI thread did not process watchdog heartbeat", trace);
                } else if (delay < STALL_THRESHOLD_MS) {
                    stallRecorded.set(false);
                }
                monitor.postDelayed(this, PING_INTERVAL_MS);
            }
        };
        main.post(() -> lastResponse.set(SystemClock.uptimeMillis()));
        monitor.post(ping);
        monitor.post(check);
    }
}
