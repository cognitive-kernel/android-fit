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
     sql.beginTransaction();
     try { for(String table:new String[]{"exercises","plans","logs","sleep_logs","vital_logs","body_metrics"})sql.delete(table,null,null);sql.setTransactionSuccessful(); }
     finally { sql.endTransaction(); }
    } catch (Exception ex) { throw new AssertionError("Selective backup/restore regression",ex); }
   });
  }
 }
 @Test public void persistsSelectedBackupSectionsForDocumentPickerReturn() throws Exception {
  try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
   scenario.onActivity(activity -> {
    AdvancedFeatures features=activity.advanced;
    features.savePendingBackupSections(new boolean[]{true,false,true,false});
    boolean[] restored=features.loadPendingBackupSections();
    assertArrayEquals(new boolean[]{true,false,true,false},restored);
    features.clearPendingBackupSections();
    assertArrayEquals(new boolean[]{false,false,false,false},features.loadPendingBackupSections());
   });
  }
 }

 @Test public void preferencesBackupRestoresOnlyTheSelectedPreferenceSection() throws Exception {
  try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
   scenario.onActivity(activity -> {
    try {
     android.content.SharedPreferences appPrefs=activity.getSharedPreferences("fityar_preferences",0);
     android.content.SharedPreferences languagePrefs=activity.getSharedPreferences("fit_settings",0);
     java.util.Map<String,?> originalApp=new java.util.HashMap<>(appPrefs.getAll());
     java.util.Map<String,?> originalLanguage=new java.util.HashMap<>(languagePrefs.getAll());
     try {
     appPrefs.edit().clear().putBoolean("reminder_enabled",true).putInt("weekly_goal",4).apply();
     languagePrefs.edit().clear().putString("language","en").putInt("font_scale",2).apply();
     Method create=AdvancedFeatures.class.getDeclaredMethod("createSelectiveBackup",boolean[].class);
     create.setAccessible(true);
     JSONObject backup=(JSONObject)create.invoke(activity.advanced,(Object)new boolean[]{false,false,false,true});
     JSONArray sections=backup.getJSONArray("sections");
     assertEquals(1,sections.length());
     JSONObject section=sections.getJSONObject(0);
     assertEquals("preferences",section.getString("id"));
     assertEquals(4,section.getJSONObject("fityar_preferences").getInt("weekly_goal"));
     assertEquals("en",section.getJSONObject("fit_settings").getString("language"));
     appPrefs.edit().putInt("weekly_goal",1).apply();
     languagePrefs.edit().putString("language","fa").apply();
     Method restore=AdvancedFeatures.class.getDeclaredMethod("restorePreferences",String.class,JSONObject.class);
     restore.setAccessible(true);
     restore.invoke(activity.advanced,"fityar_preferences",section.getJSONObject("fityar_preferences"));
     restore.invoke(activity.advanced,"fit_settings",section.getJSONObject("fit_settings"));
     assertEquals(4,appPrefs.getInt("weekly_goal",0));
     assertTrue(appPrefs.getBoolean("reminder_enabled",false));
     assertEquals("en",languagePrefs.getString("language",""));
     assertEquals(2,languagePrefs.getInt("font_scale",0));
     } finally {
      restorePrefs(appPrefs,originalApp);
      restorePrefs(languagePrefs,originalLanguage);
     }
    } catch(Exception ex) { throw new AssertionError("Preference backup/restore regression",ex); }
   });
  }
 }
 @Test public void refusesToCreateBackupWithNoSelectedSections() throws Exception {
  try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
   scenario.onActivity(activity -> {
    try {
     Method create=AdvancedFeatures.class.getDeclaredMethod("createSelectiveBackup",boolean[].class);
     create.setAccessible(true);
     try { create.invoke(activity.advanced,(Object)new boolean[]{false,false,false,false}); fail("Expected empty selection to be rejected"); }
     catch(java.lang.reflect.InvocationTargetException expected) { assertTrue(expected.getCause() instanceof java.io.IOException); }
    } catch(Exception ex) { throw new AssertionError("Empty backup selection regression",ex); }
   });
  }
 }


 @Test public void legacyDatabaseRestoreValidationRejectsForeignAndFutureDatabases() throws Exception {
  try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
   scenario.onActivity(activity -> {
    java.io.File valid = new java.io.File(activity.getCacheDir(), "restore-validation-valid.db");
    java.io.File foreign = new java.io.File(activity.getCacheDir(), "restore-validation-foreign.db");
    java.io.File future = new java.io.File(activity.getCacheDir(), "restore-validation-future.db");
    try {
     for (java.io.File file : new java.io.File[]{valid, foreign, future}) if (file.exists()) assertTrue(file.delete());
     createValidationDb(valid, 4, true);
     createValidationDb(foreign, 4, false);
     createValidationDb(future, 99, true);
     Method validate = AdvancedFeatures.class.getDeclaredMethod("validateDatabase", java.io.File.class);
     validate.setAccessible(true);
     validate.invoke(activity.advanced, valid);
     assertValidationRejected(validate, activity.advanced, foreign);
     assertValidationRejected(validate, activity.advanced, future);
     java.io.File corrupt = new java.io.File(activity.getCacheDir(), "restore-validation-corrupt.db");
     try (java.io.FileOutputStream out = new java.io.FileOutputStream(corrupt)) { out.write("not sqlite".getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
     assertValidationRejected(validate, activity.advanced, corrupt);
     assertTrue(corrupt.delete());
    } catch (Exception ex) {
     throw new AssertionError("Legacy SQLite restore validation regression", ex);
    } finally {
     for (java.io.File file : new java.io.File[]{valid, foreign, future}) if (file.exists()) file.delete();
    }
   });
  }
 }
 private static void createValidationDb(java.io.File file, int version, boolean fitYar) {
  SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(file, null);
  try {
   database.setVersion(version);
   if (fitYar) {
    database.execSQL("CREATE TABLE plans(id INTEGER PRIMARY KEY, name TEXT NOT NULL)");
    database.execSQL("CREATE TABLE exercises(id INTEGER PRIMARY KEY, plan_id INTEGER NOT NULL, name TEXT NOT NULL, sets INTEGER NOT NULL, reps INTEGER NOT NULL)");
    database.execSQL("CREATE TABLE logs(id INTEGER PRIMARY KEY, date_jalali TEXT, plan_name TEXT, exercise_name TEXT, sets INTEGER, reps INTEGER, weight REAL, created_at INTEGER)");
   } else {
    database.execSQL("CREATE TABLE unrelated(id INTEGER PRIMARY KEY)");
   }
  } finally { database.close(); }
 }
 private static void assertValidationRejected(Method validate, AdvancedFeatures features, java.io.File file) throws Exception {
  try {
   validate.invoke(features, file);
   fail("Expected invalid or unsupported SQLite backup to be rejected: " + file.getName());
  } catch (java.lang.reflect.InvocationTargetException expected) {
   assertTrue("Expected IOException, got " + expected.getCause(), expected.getCause() instanceof java.io.IOException);
  }
 }
 private static void restorePrefs(android.content.SharedPreferences prefs,java.util.Map<String,?> values) {
  android.content.SharedPreferences.Editor editor=prefs.edit().clear();
  for(java.util.Map.Entry<String,?> entry:values.entrySet()){
   Object value=entry.getValue();String key=entry.getKey();
   if(value instanceof Boolean)editor.putBoolean(key,(Boolean)value);
   else if(value instanceof Integer)editor.putInt(key,(Integer)value);
   else if(value instanceof Long)editor.putLong(key,(Long)value);
   else if(value instanceof Float)editor.putFloat(key,(Float)value);
   else if(value instanceof String)editor.putString(key,(String)value);
   else if(value instanceof java.util.Set){java.util.HashSet<String> set=new java.util.HashSet<>();for(Object item:(java.util.Set<?>)value)set.add(String.valueOf(item));editor.putStringSet(key,set);}
  }
  editor.commit();
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
     String todayKey = "steps_" + new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
     activity.getSharedPreferences("step_counter", android.content.Context.MODE_PRIVATE).edit()
       .putBoolean("running", true).putBoolean("paused", false)
       .putInt("session_steps", 125).putInt(todayKey, 5400).apply();

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
     assertEquals(5400,stepPrefs.getInt(todayKey));
     assertFalse(stepPrefs.has("running"));
     assertFalse(stepPrefs.has("paused"));
     android.content.SharedPreferences stepPreferences = activity.getSharedPreferences("step_counter", android.content.Context.MODE_PRIVATE);
     stepPreferences.edit().putInt("session_steps",999).putBoolean("running",true).putBoolean("paused",false).apply();
     Method restoreSteps = AdvancedFeatures.class.getDeclaredMethod("restoreSection",JSONObject.class,String.class);
     restoreSteps.setAccessible(true);
     restoreSteps.invoke(activity.advanced,steps,"steps");
     assertEquals(125,stepPreferences.getInt("session_steps",0));
     assertFalse(stepPreferences.getBoolean("running",false));
     assertFalse(stepPreferences.getBoolean("paused",false));
     activity.refreshStepCounterAfterRestore();
     assertTrue(((android.widget.TextView)activity.findViewById(R.id.stepsCountText)).getText().toString().contains("5,400"));

     activity.db.addSleep(now + 2_000_000L, now + 2_000_000L + 60L*60*1000);
     Method restore = AdvancedFeatures.class.getDeclaredMethod("restoreSection",JSONObject.class,String.class);
     restore.setAccessible(true);
     sql.beginTransaction();
     try { restore.invoke(activity.advanced,health,"health"); sql.setTransactionSuccessful(); }
     finally { sql.endTransaction(); }
     try (Cursor sleeps = activity.db.sleepLogs()) { assertEquals(1,countRows(sleeps)); }
     assertEquals("Keep workout data",activity.db.plans().get(0)[1]);
     sql.beginTransaction();
     try { for(String table:new String[]{"exercises","plans","logs","sleep_logs","vital_logs","body_metrics"})sql.delete(table,null,null);sql.setTransactionSuccessful(); }
     finally { sql.endTransaction(); }
     stepPreferences.edit().clear().apply();
    } catch (Exception ex) { throw new AssertionError("Selective health/step backup regression",ex); }
   });
  }
 }

 @Test public void advancedFeaturesPreferTheMainActivityLanguageSetting() throws Exception {
  try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
   scenario.onActivity(activity -> {
    android.content.SharedPreferences primary=activity.getSharedPreferences("fityar_preferences",android.content.Context.MODE_PRIVATE);
    android.content.SharedPreferences legacy=activity.getSharedPreferences("fit_settings",android.content.Context.MODE_PRIVATE);
    String oldPrimary=primary.getString("language",null),oldLegacy=legacy.getString("language",null);
    try {
     primary.edit().putString("language","fa").commit();
     legacy.edit().putString("language","en").commit();
     Method selected=AdvancedFeatures.class.getDeclaredMethod("selectedLanguage");
     selected.setAccessible(true);
     assertEquals("en",selected.invoke(activity.advanced));
     legacy.edit().remove("language").commit();
     assertEquals("fa",selected.invoke(activity.advanced));
    } catch(Exception ex) { throw new AssertionError("Language preference regression",ex); }
    finally {
     android.content.SharedPreferences.Editor p=primary.edit(),l=legacy.edit();
     if(oldPrimary==null)p.remove("language");else p.putString("language",oldPrimary);
     if(oldLegacy==null)l.remove("language");else l.putString("language",oldLegacy);
     p.commit();l.commit();
    }
   });
  }
 }
}
