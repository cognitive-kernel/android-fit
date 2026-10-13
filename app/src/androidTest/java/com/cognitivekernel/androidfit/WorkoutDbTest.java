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
 @Test public void rejectsNegativeWeight(){\n  try { db.log("1405/07/21","Plan","Squat",3,8,-1);fail("Expected negative weight to be rejected"); }\n  catch(IllegalArgumentException expected) { }\n }\n @Test public void savesWorkoutLogForCsvExport(){
  db.log("1405/07/21","Plan A","Squat",3,8,42.5);
  try(android.database.Cursor c=db.allLogs()){
   assertTrue(c.moveToFirst());assertEquals("1405/07/21",c.getString(0));assertEquals("Plan A",c.getString(1));assertEquals("Squat",c.getString(2));assertEquals(3,c.getInt(3));assertEquals(8,c.getInt(4));assertEquals(42.5,c.getDouble(5),0.001);assertFalse(c.moveToNext());
  }
 }
}
