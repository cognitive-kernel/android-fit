package com.cognitivekernel.androidfit;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SelectiveBackupTest {
 @Test public void exportsChosenSectionAndRestoresItWithoutTouchingOtherSections() throws Exception {
  try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
   scenario.onActivity(activity -> {
    try {
     WorkoutDb db = activity.db;
     SQLiteDatabase sql = db.getWritableDatabase();
     sql.beginTransaction();
     try {
      for (String table : new String[]{"exercises","plans","logs","sleep_logs","vital_logs","body_metrics"}) sql.delete(table,null,null);
      sql.setTransactionSuccessful();
     } finally { sql.endTransaction(); }

     long plan = db.addPlan("Backup plan");
     db.addExercise(plan,"Squat",3,10);
     db.log("1405/07/21","Backup plan","Squat",3,10,20);
     long now = System.currentTimeMillis();
     db.addSleep(now,now+8L*60*60*1000);
     db.addVitals(now,120,80,70);
     db.addBodyMetrics(now,72.5,84.0);

     Method create = AdvancedFeatures.class.getDeclaredMethod("createSelectiveBackup", boolean[].class);
     create.setAccessible(true);
     JSONObject backup = (JSONObject) create.invoke(activity.advanced, (Object)new boolean[]{true,false,false,false});
     assertEquals("fityar-selective-backup",backup.getString("format"));
     JSONArray sections = backup.getJSONArray("sections");
     assertEquals(1,sections.length());
     JSONObject workouts = sections.getJSONObject(0);
     assertEquals("workouts",workouts.getString("id"));
     assertEquals(1,workouts.getJSONArray("plans").length());
     assertEquals(1,workouts.getJSONArray("exercises").length());
     assertEquals(1,workouts.getJSONArray("logs").length());

     db.addPlan("Temporary plan");
     db.addSleep(now+1000000L,now+1000000L+60L*60*1000);
     Method restore = AdvancedFeatures.class.getDeclaredMethod("restoreSection",JSONObject.class,String.class);
     restore.setAccessible(true);
     sql.beginTransaction();
     try { restore.invoke(activity.advanced,workouts,"workouts"); sql.setTransactionSuccessful(); }
     finally { sql.endTransaction(); }

     assertEquals(1,db.plans().size());
     assertEquals("Backup plan",db.plans().get(0)[1]);
     try(Cursor sleeps=db.sleepLogs()){assertEquals(2,countRows(sleeps));}
     try(Cursor vitals=db.vitalLogs()){assertEquals(1,countRows(vitals));}
     try(Cursor metrics=db.bodyMetrics()){assertEquals(1,countRows(metrics));}
    } catch (Exception ex) { throw new AssertionError("Selective backup/restore regression",ex); }
   });
  }
 }
 private static int countRows(Cursor cursor) { int count=0;while(cursor.moveToNext())count++;return count; }

 @Test public void exportsHealthAndStepDataWithoutSavingLiveServiceState() throws Exception {
  try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
   scenario.onActivity(activity -> {
    try {
     SQLiteDatabase sql = activity.db.getWritableDatabase();
     sql.beginTransaction();
     try {
      for (String table : new String[]{"exercises","plans","logs","sleep_logs","vital_logs","body_metrics"}) sql.delete(table,null,null);
      sql.setTransactionSuccessful();
     } finally { sql.endTransaction(); }
     long now = System.currentTimeMillis();
     activity.db.addSleep(now, now + 7L*60*60*1000);
     activity.db.addVitals(now, 118, 78, 68);
     activity.db.addBodyMetrics(now, 71.0, 82.0);
     long preservedPlan = activity.db.addPlan("Keep workout data");
     activity.db.addExercise(preservedPlan,"Push-up",3,8);
     activity.getSharedPreferences("step_counter", android.content.Context.MODE_PRIVATE).edit()
       .putBoolean("running", true).putBoolean("paused", false)
       .putInt("session_steps", 125).putInt("steps_2026-10-23", 5400).apply();

     Method create = AdvancedFeatures.class.getDeclaredMethod("createSelectiveBackup", boolean[].class);
     create.setAccessible(true);
     JSONObject backup = (JSONObject) create.invoke(activity.advanced, (Object)new boolean[]{false,true,true,false});
     JSONArray sections = backup.getJSONArray("sections");
     assertEquals(2, sections.length());
     JSONObject health = null, steps = null;
     for (int i=0;i<sections.length();i++) {
      JSONObject section = sections.getJSONObject(i);
      if ("health".equals(section.getString("id"))) health=section;
      if ("steps".equals(section.getString("id"))) steps=section;
     }
     assertNotNull(health);
     assertEquals(1,health.getJSONArray("sleep_logs").length());
     assertEquals(1,health.getJSONArray("vital_logs").length());
     assertEquals(1,health.getJSONArray("body_metrics").length());
     assertNotNull(steps);
     JSONObject stepPrefs=steps.getJSONObject("step_counter");
     assertEquals(125,stepPrefs.getInt("session_steps"));
     assertEquals(5400,stepPrefs.getInt("steps_2026-10-23"));
     assertFalse(stepPrefs.has("running"));
     assertFalse(stepPrefs.has("paused"));
     android.content.SharedPreferences stepPreferences = activity.getSharedPreferences("step_counter", android.content.Context.MODE_PRIVATE);
     stepPreferences.edit().putInt("session_steps",999).putBoolean("running",true).putBoolean("paused",false).apply();
     Method restorePreferences = AdvancedFeatures.class.getDeclaredMethod("restorePreferences",String.class,JSONObject.class);
     restorePreferences.setAccessible(true);
     restorePreferences.invoke(activity.advanced,"step_counter",stepPrefs);
     assertEquals(125,stepPreferences.getInt("session_steps",0));
     assertFalse(stepPreferences.getBoolean("running",false));
     assertFalse(stepPreferences.getBoolean("paused",false));

     activity.db.addSleep(now + 2_000_000L, now + 2_000_000L + 60L*60*1000);
     Method restore = AdvancedFeatures.class.getDeclaredMethod("restoreSection",JSONObject.class,String.class);
     restore.setAccessible(true);
     SQLiteDatabase sql = activity.db.getWritableDatabase();
     sql.beginTransaction();
     try { restore.invoke(activity.advanced,health,"health"); sql.setTransactionSuccessful(); }
     finally { sql.endTransaction(); }
     try (Cursor sleeps = activity.db.sleepLogs()) { assertEquals(1,countRows(sleeps)); }
     assertEquals("Keep workout data",activity.db.plans().get(0)[1]);
    } catch (Exception ex) { throw new AssertionError("Selective health/step backup regression",ex); }
   });
  }
 }
}
