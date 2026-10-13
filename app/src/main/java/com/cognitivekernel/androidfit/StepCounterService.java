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

    @Override public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("step_counter", MODE_PRIVATE);
        today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        createChannel();
        startForeground(41, notification(prefs.getInt("steps_" + today, 0)));
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (stepSensor != null) sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "قدم‌شمار", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("شمارش قدم‌ها در پس‌زمینه");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification notification(int steps) {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        Intent open = new Intent(this, MainActivity.class);
        android.app.PendingIntent pi = android.app.PendingIntent.getActivity(this, 0, open,
                (Build.VERSION.SDK_INT >= 23 ? android.app.PendingIntent.FLAG_IMMUTABLE : 0) | android.app.PendingIntent.FLAG_UPDATE_CURRENT);
        return b.setContentTitle("قدم‌شمار فعال است")
                .setContentText("قدم‌های امروز: " + steps)
                .setSmallIcon(android.R.drawable.ic_menu_directions)
                .setContentIntent(pi).setOngoing(true).build();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER || event.values.length == 0) return;
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        if (!date.equals(today)) today = date;
        float totalSinceBoot = event.values[0];
        String baseKey = "base_" + today;
        float base = prefs.getFloat(baseKey, -1f);
        if (base < 0 || totalSinceBoot < base) {
            base = totalSinceBoot;
            prefs.edit().putFloat(baseKey, base).apply();
        }
        int steps = Math.max(0, Math.round(totalSinceBoot - base));
        prefs.edit().putInt("steps_" + today, steps).apply();
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.notify(41, notification(steps));
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override public void onDestroy() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}