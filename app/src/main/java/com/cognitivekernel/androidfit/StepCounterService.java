package com.cognitivekernel.androidfit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StepCounterService extends Service implements SensorEventListener {
    private static final String CHANNEL = "step_counter";
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private SharedPreferences prefs;
    private String today;
    private boolean listening = false;
    private boolean needsBaseline = true;
    private float lastRaw = -1f;

    @Override public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("step_counter", MODE_PRIVATE);
        today = dateNow();
        createChannel();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
    }

    private String dateNow() { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()); }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "قدم‌شمار", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("شمارش قدم‌ها در پس‌زمینه");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private int sessionSteps() { return prefs.getInt("session_steps", 0); }
    private int todaySteps() { return prefs.getInt("steps_" + dateNow(), 0); }

    private Notification notification(String title) {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        Intent open = new Intent(this, MainActivity.class);
        android.app.PendingIntent pi = android.app.PendingIntent.getActivity(this, 0, open,
                (Build.VERSION.SDK_INT >= 23 ? android.app.PendingIntent.FLAG_IMMUTABLE : 0) | android.app.PendingIntent.FLAG_UPDATE_CURRENT);
        String content = "این نوبت: " + sessionSteps() + " | امروز: " + todaySteps();
        return b.setContentTitle(title).setContentText(content)
                .setSmallIcon(android.R.drawable.ic_menu_directions).setContentIntent(pi).setOngoing(true).build();
    }

    private void updateNotification(String title) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.notify(41, notification(title));
    }

    private void beginForeground() {
        startForeground(41, notification(prefs.getBoolean("paused", false) ? "قدم‌شمار متوقف موقت است" : "قدم‌شمار فعال است"));
    }

    private boolean registerSensor() {
        today = dateNow();
        needsBaseline = true;
        lastRaw = -1f;
        if (stepSensor == null || sensorManager == null) return false;
        if (!listening) listening = sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        if (!listening) return false;
        prefs.edit().putBoolean("running", true).putBoolean("paused", false).apply();
        updateNotification("قدم‌شمار فعال است");
        return true;
    }

    private void pauseCounting() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
        listening = false;
        needsBaseline = true;
        lastRaw = -1f;
        prefs.edit().putBoolean("running", true).putBoolean("paused", true).apply();
        updateNotification("قدم‌شمار متوقف موقت است");
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? "RESUME" : intent.getAction();
        if ("PAUSE".equals(action)) { pauseCounting(); return START_STICKY; }
        // A sticky restart has no Intent. Preserve an explicit user pause instead of
        // silently restarting the sensor and changing the saved paused state.
        if (intent == null && prefs.getBoolean("paused", false)) {
            beginForeground();
            updateNotification("قدم‌شمار متوقف موقت است");
            return START_STICKY;
        }
        if ("FINISH".equals(action)) {
            if (sensorManager != null) sensorManager.unregisterListener(this);
            listening = false;
            prefs.edit().putBoolean("running", false).putBoolean("paused", false).apply();
            if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE); else stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        if ("START".equals(action)) {
            prefs.edit().putInt("session_steps", 0).putBoolean("paused", false).apply();
        }
        beginForeground();
        if (!registerSensor()) {
            prefs.edit().putBoolean("running", false).putBoolean("paused", false).apply();
            updateNotification("قدم‌شمار در این دستگاه در دسترس نیست");
            if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE); else stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER || event.values.length == 0) return;
        String date = dateNow();
        if (!date.equals(today)) {
            today = date;
            needsBaseline = true;
            lastRaw = -1f;
        }
        if (prefs.getBoolean("paused", false) || !prefs.getBoolean("running", false)) return;
        float raw = event.values[0];
        if (needsBaseline || lastRaw < 0 || raw < lastRaw) {
            lastRaw = raw;
            needsBaseline = false;
            updateNotification("قدم‌شمار فعال است");
            return;
        }
        int delta = Math.max(0, Math.round(raw - lastRaw));
        lastRaw = raw;
        if (delta > 0) {
            prefs.edit()
                    .putInt("session_steps", sessionSteps() + delta)
                    .putInt("steps_" + today, todaySteps() + delta)
                    .apply();
        }
        updateNotification("قدم‌شمار فعال است");
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    @Override public void onDestroy() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}