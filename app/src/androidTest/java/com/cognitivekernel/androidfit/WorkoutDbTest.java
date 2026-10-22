package com.cognitivekernel.androidfit;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class WorkoutDbTest {
 private Context context;
 private WorkoutDb db;
 @Before public void setUp(){context=ApplicationProvider.getApplicationContext();context.deleteDatabase("fit_yar.db");db=new WorkoutDb(context);}
 @After public void tearDown(){db.close();context.deleteDatabase("fit_yar.db");}
 @Test public void createsListsAndDeletesPlansAndExercises(){
  long plan=db.addPlan("Upper body");assertTrue(plan>0);
  long exercise=db.addExercise(plan,"Bench press",3,10);assertTrue(exercise>0);
  List<String[]> plans=db.plans();assertEquals(1,plans.size());assertEquals("Upper body",plans.get(0)[1]);
  List<String[]> exercises=db.exercises(plan);assertEquals(1,exercises.size());assertEquals("Bench press",exercises.get(0)[1]);
  assertTrue(db.updateExercise(exercise,"Incline press",4,8));
  assertEquals("Incline press",db.exercises(plan).get(0)[1]);
  db.deleteExercise(exercise);assertTrue(db.exercises(plan).isEmpty());
  db.deletePlan(plan);assertTrue(db.plans().isEmpty());
 }
 @Test public void rejectsInvalidExerciseValues(){
  long plan=db.addPlan("Plan");
  try { db.addExercise(plan,"Squat",0,10);fail("Expected invalid sets to be rejected"); }
  catch(IllegalArgumentException expected) { }
 }
 @Test public void rejectsNegativeWeight(){
  try { db.log("1405/07/21","Plan","Squat",3,8,-1);fail("Expected negative weight to be rejected"); }
  catch(IllegalArgumentException expected) { }
 }
 @Test public void savesWorkoutLogForCsvExport(){
  db.log("1405/07/21","Plan A","Squat",3,8,42.5);
  try(android.database.Cursor c=db.allLogs()){
   assertTrue(c.moveToFirst());assertEquals("1405/07/21",c.getString(0));assertEquals("Plan A",c.getString(1));assertEquals("Squat",c.getString(2));assertEquals(3,c.getInt(3));assertEquals(8,c.getInt(4));assertEquals(42.5,c.getDouble(5),0.001);assertFalse(c.moveToNext());
  }
 }
 @Test public void savesDetailedWorkoutMetadataAndHealthRecords(){
  db.logDetailed("1405/07/21","Plan A","Squat",3,8,42.5,90,8,"Controlled tempo");
  try(android.database.Cursor c=db.allLogs()){
   assertTrue(c.moveToFirst());assertEquals(90,c.getInt(6));assertEquals(8,c.getInt(7));assertEquals("Controlled tempo",c.getString(8));
  }
  long start=1_800_000_000_000L;db.addSleep(start,start+8L*60*60*1000);
  try(android.database.Cursor c=db.sleepLogs()){assertTrue(c.moveToFirst());assertEquals(480,c.getInt(2));}
  db.addVitals(start,120,80,70);
  try(android.database.Cursor c=db.vitalLogs()){assertTrue(c.moveToFirst());assertEquals(120,c.getInt(1));assertEquals(80,c.getInt(2));assertEquals(70,c.getInt(3));}
  db.addBodyMetrics(start,72.5,84.0);
  try(android.database.Cursor c=db.bodyMetrics()){assertTrue(c.moveToFirst());assertEquals(72.5,c.getDouble(1),0.001);assertEquals(84.0,c.getDouble(2),0.001);}
 }
 @Test public void upgradesLegacyVersionOneWithoutLosingWorkoutRecords(){
  db.close();context.deleteDatabase(WorkoutDb.DATABASE_NAME);
  android.database.sqlite.SQLiteDatabase legacy=android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(WorkoutDb.DATABASE_NAME),null);
  legacy.execSQL("CREATE TABLE plans(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL)");
  legacy.execSQL("CREATE TABLE exercises(id INTEGER PRIMARY KEY AUTOINCREMENT,plan_id INTEGER NOT NULL,name TEXT NOT NULL,sets INTEGER NOT NULL,reps INTEGER NOT NULL)");
  legacy.execSQL("CREATE TABLE logs(id INTEGER PRIMARY KEY AUTOINCREMENT,date_jalali TEXT,plan_name TEXT,exercise_name TEXT,sets INTEGER,reps INTEGER,weight REAL,created_at INTEGER)");
  legacy.execSQL("INSERT INTO plans(name) VALUES('Legacy plan')");
  legacy.execSQL("INSERT INTO logs(date_jalali,plan_name,exercise_name,sets,reps,weight,created_at) VALUES('1405/07/21','Legacy plan','Squat',3,10,20,123456789)");
  legacy.setVersion(1);legacy.close();
  db=new WorkoutDb(context);
  assertEquals("Legacy plan",db.plans().get(0)[1]);
  try(android.database.Cursor c=db.allLogs()){
   assertTrue(c.moveToFirst());assertEquals("Squat",c.getString(2));assertEquals(60,c.getInt(6));assertEquals(7,c.getInt(7));assertEquals("",c.getString(8));
  }
  assertEquals(4,db.getReadableDatabase().getVersion());
  try(android.database.Cursor c=db.sleepLogs()){assertFalse(c.moveToFirst());}
 }
 @Test public void upgradesVersionTwoDetailedSchemaFromOtherBranch(){
  db.close();context.deleteDatabase(WorkoutDb.DATABASE_NAME);
  android.database.sqlite.SQLiteDatabase legacy=android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(WorkoutDb.DATABASE_NAME),null);
  legacy.execSQL("CREATE TABLE plans(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL)");
  legacy.execSQL("CREATE TABLE exercises(id INTEGER PRIMARY KEY AUTOINCREMENT,plan_id INTEGER NOT NULL,name TEXT NOT NULL,sets INTEGER NOT NULL,reps INTEGER NOT NULL)");
  legacy.execSQL("CREATE TABLE logs(id INTEGER PRIMARY KEY AUTOINCREMENT,date_jalali TEXT,plan_name TEXT,exercise_name TEXT,sets INTEGER,reps INTEGER,weight REAL,created_at INTEGER,rest_seconds INTEGER NOT NULL DEFAULT 60,effort INTEGER NOT NULL DEFAULT 7,notes TEXT NOT NULL DEFAULT '')");
  legacy.execSQL("INSERT INTO plans(name) VALUES('Version two plan')");
  legacy.execSQL("INSERT INTO logs(date_jalali,plan_name,exercise_name,sets,reps,weight,created_at,rest_seconds,effort,notes) VALUES('1405/07/21','Version two plan','Deadlift',4,6,70,123456789,120,9,'Kept from old schema')");
  legacy.setVersion(2);legacy.close();
  db=new WorkoutDb(context);
  assertEquals("Version two plan",db.plans().get(0)[1]);
  try(android.database.Cursor c=db.allLogs()){
   assertTrue(c.moveToFirst());assertEquals("Deadlift",c.getString(2));assertEquals(120,c.getInt(6));assertEquals(9,c.getInt(7));assertEquals("Kept from old schema",c.getString(8));
  }
  try(android.database.Cursor c=db.vitalLogs()){assertFalse(c.moveToFirst());}
  try(android.database.Cursor c=db.bodyMetrics()){assertFalse(c.moveToFirst());}
 }
}
