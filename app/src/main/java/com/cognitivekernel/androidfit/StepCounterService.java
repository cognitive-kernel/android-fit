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
    private Notification notification(int steps, String title) {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        Intent open = new Intent(this, MainActivity.class);
        android.app.PendingIntent pi = android.app.PendingIntent.getActivity(this, 0, open,
                (Build.VERSION.SDK_INT >= 23 ? android.app.PendingIntent.FLAG_IMMUTABLE : 0) | android.app.PendingIntent.FLAG_UPDATE_CURRENT);
        return b.setContentTitle(title).setContentText("قدم‌های امروز: " + steps)
                .setSmallIcon(android.R.drawable.ic_menu_directions).setContentIntent(pi).setOngoing(true).build();
    }
    private void updateNotification(String title) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        int n = prefs.getInt("steps_" + dateNow(), 0);
        if (manager != null) manager.notify(41, notification(n, title));
    }
    private void beginForeground() {
        startForeground(41, notification(prefs.getInt("steps_" + dateNow(), 0), "قدم‌شمار فعال است"));
    }
    private void resumeCounting() {
        today = dateNow();
        if (stepSensor != null && !listening) listening = sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        prefs.edit().putBoolean("running", true).putBoolean("paused", false).apply();
        updateNotification("قدم‌شمار فعال است");
    }
    private void pauseCounting() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
        listening = false;
        prefs.edit().putBoolean("running", true).putBoolean("paused", true).apply();
        updateNotification("قدم‌شمار متوقف موقت است");
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? "RESUME" : intent.getAction();
        if ("PAUSE".equals(action)) { pauseCounting(); return START_STICKY; }
        if ("FINISH".equals(action)) {
            if (sensorManager != null) sensorManager.unregisterListener(this);
            prefs.edit().putBoolean("running", false).putBoolean("paused", false).apply();
            if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE); else stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        beginForeground();
        resumeCounting();
        return START_STICKY;
    }
    @Override public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER || event.values.length == 0) return;
        String date = dateNow();
        if (!date.equals(today)) today = date;
        float total = event.values[0];
        String key = "base_" + today;
        float base = prefs.getFloat(key, -1f);
        if (base < 0 || total < base) { base = total; prefs.edit().putFloat(key, base).apply(); }
        int steps = Math.max(0, Math.round(total - base));
        prefs.edit().putInt("steps_" + today, steps).apply();
        updateNotification(prefs.getBoolean("paused", false) ? "قدم‌شمار متوقف موقت است" : "قدم‌شمار فعال است");
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    @Override public void onDestroy() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}