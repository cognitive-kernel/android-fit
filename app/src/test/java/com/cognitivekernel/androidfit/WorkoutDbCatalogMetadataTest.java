package com.cognitivekernel.androidfit;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.RobolectricTestRunner;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
public class WorkoutDbCatalogMetadataTest {
    private Context context;
    private WorkoutDb db;

    @Before public void setUp() {
        context=Robolectric.buildActivity(Activity.class).setup().get();
        context.deleteDatabase(WorkoutDb.DATABASE_NAME);
        db=new WorkoutDb(context);
    }

    @After public void tearDown() {
        if(db!=null)db.close();
        context.deleteDatabase(WorkoutDb.DATABASE_NAME);
    }

    @Test public void storesCatalogIdAndGenderWithoutChangingLegacyManualApi() {
        long plan=db.addPlan("Generated");
        db.addExercise(plan,"Air Squat",3,10,"394813","female");
        db.addExercise(plan,"My custom movement",2,12);
        java.util.List<String[]> rows=db.exercises(plan);
        assertEquals("394813",rows.get(0)[4]);
        assertEquals("female",rows.get(0)[5]);
        assertNull(rows.get(1)[4]);
        assertNull(rows.get(1)[5]);
    }

    @Test public void editingExerciseNameInvalidatesOnlyItsStaleCatalogReference() {
        long plan=db.addPlan("Generated");
        long exercise=db.addExercise(plan,"Air Squat",3,10,"394813","female");
        assertTrue(db.updateExercise(exercise,"Custom squat",4,8));
        String[] row=db.exercises(plan).get(0);
        assertEquals("Custom squat",row[1]);
        assertNull(row[4]);
        assertNull(row[5]);
    }

    @Test public void versionFourDatabaseMigratesAdditivelyAndPreservesExistingExercises() {
        db.close();db=null;
        context.deleteDatabase(WorkoutDb.DATABASE_NAME);
        SQLiteDatabase legacy=context.openOrCreateDatabase(WorkoutDb.DATABASE_NAME,Context.MODE_PRIVATE,null);
        legacy.execSQL("CREATE TABLE plans(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL)");
        legacy.execSQL("CREATE TABLE exercises(id INTEGER PRIMARY KEY AUTOINCREMENT,plan_id INTEGER NOT NULL,name TEXT NOT NULL,sets INTEGER NOT NULL,reps INTEGER NOT NULL)");
        legacy.execSQL("CREATE TABLE logs(id INTEGER PRIMARY KEY AUTOINCREMENT,date_jalali TEXT,plan_name TEXT,exercise_name TEXT,sets INTEGER,reps INTEGER,weight REAL,created_at INTEGER,rest_seconds INTEGER NOT NULL DEFAULT 60,effort INTEGER NOT NULL DEFAULT 7,notes TEXT NOT NULL DEFAULT '')");
        legacy.execSQL("CREATE TABLE sleep_logs(id INTEGER PRIMARY KEY AUTOINCREMENT,start_at INTEGER NOT NULL,end_at INTEGER NOT NULL,duration_minutes INTEGER NOT NULL)");
        legacy.execSQL("CREATE TABLE vital_logs(id INTEGER PRIMARY KEY AUTOINCREMENT,measured_at INTEGER NOT NULL,systolic INTEGER NOT NULL,diastolic INTEGER NOT NULL,pulse INTEGER NOT NULL)");
        legacy.execSQL("CREATE TABLE body_metrics(id INTEGER PRIMARY KEY AUTOINCREMENT,measured_at INTEGER NOT NULL,weight_kg REAL,waist_cm REAL)");
        legacy.execSQL("INSERT INTO plans(name) VALUES('Old plan')");
        legacy.execSQL("INSERT INTO exercises(plan_id,name,sets,reps) VALUES(1,'Old squat',3,10)");
        legacy.execSQL("INSERT INTO exercises(plan_id,name,sets,reps) VALUES(1,'Air Squat (female)',3,12)");
        legacy.execSQL("INSERT INTO exercises(plan_id,name,sets,reps) VALUES(1,'Air Squat (male)',3,8)");
        legacy.setVersion(4);legacy.close();
        db=new WorkoutDb(context);
        String[] migrated=db.exercises(1).get(0);
        assertEquals("Old squat",migrated[1]);
        assertEquals("3",migrated[2]);
        assertNull(migrated[4]);
        assertNull(migrated[5]);
        java.util.List<String[]> migratedRows=db.exercises(1);
        assertEquals("Air Squat",migratedRows.get(1)[1]);
        assertEquals("female",migratedRows.get(1)[5]);
        assertEquals("Air Squat",migratedRows.get(2)[1]);
        assertEquals("male",migratedRows.get(2)[5]);
        assertEquals(5,db.getReadableDatabase().getVersion());
    }
}
