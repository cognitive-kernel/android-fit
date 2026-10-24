package com.cognitivekernel.androidfit;

import android.content.Context;
import android.database.Cursor;

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

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("fit_yar.db");
        db = new WorkoutDb(context);
    }

    @After
    public void tearDown() {
        db.close();
        context.deleteDatabase("fit_yar.db");
    }

    @Test
    public void createsListsAndDeletesPlansAndExercises() {
        long plan = db.addPlan("Upper body");
        assertTrue(plan > 0);
        long exercise = db.addExercise(plan, "Bench press", 3, 10);
        assertTrue(exercise > 0);

        List<String[]> plans = db.plans();
        assertEquals(1, plans.size());
        assertEquals("Upper body", plans.get(0)[1]);

        List<String[]> exercises = db.exercises(plan);
        assertEquals(1, exercises.size());
        assertEquals("Bench press", exercises.get(0)[1]);
        assertTrue(db.updateExercise(exercise, "Incline press", 4, 8));
        assertEquals("Incline press", db.exercises(plan).get(0)[1]);

        db.deleteExercise(exercise);
        assertTrue(db.exercises(plan).isEmpty());
        db.deletePlan(plan);
        assertTrue(db.plans().isEmpty());
    }

    @Test
    public void updatesPlanNameAndTrimsWhitespace() {
        long plan = db.addPlan("  Upper body  ");
        assertEquals("Upper body", db.plans().get(0)[1]);
        assertTrue(db.updatePlan(plan, "  Push day  "));
        assertEquals("Push day", db.plans().get(0)[1]);
        assertFalse(db.updatePlan(-1, "Missing plan"));
    }

    @Test
    public void rejectsBlankPlanAndExerciseNames() {
        assertThrows(IllegalArgumentException.class, () -> db.addPlan("  "));
        long plan = db.addPlan("Plan");
        assertThrows(IllegalArgumentException.class, () -> db.addExercise(plan, " ", 3, 8));
        assertThrows(IllegalArgumentException.class, () -> db.updateExercise(1, null, 3, 8));
    }

    @Test
    public void rejectsInvalidExerciseValues() {
        long plan = db.addPlan("Plan");
        assertThrows(IllegalArgumentException.class, () -> db.addExercise(plan, "Squat", 0, 10));
        assertThrows(IllegalArgumentException.class, () -> db.addExercise(plan, "Squat", 3, -1));
    }

    @Test
    public void rejectsInvalidWeightValues() {
        assertThrows(IllegalArgumentException.class, () -> db.log("1405/07/21", "Plan", "Squat", 3, 8, -1));
        assertThrows(IllegalArgumentException.class, () -> db.log("1405/07/21", "Plan", "Squat", 3, 8, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> db.log("1405/07/21", "Plan", "Squat", 3, 8, Double.POSITIVE_INFINITY));
    }

    @Test
    public void savesWorkoutLogForCsvExport() {
        db.log("1405/07/21", "Plan A", "Squat", 3, 8, 42.5);
        try (Cursor cursor = db.allLogs()) {
            assertTrue(cursor.moveToFirst());
            assertEquals("1405/07/21", cursor.getString(0));
            assertEquals("Plan A", cursor.getString(1));
            assertEquals("Squat", cursor.getString(2));
            assertEquals(3, cursor.getInt(3));
            assertEquals(8, cursor.getInt(4));
            assertEquals(42.5, cursor.getDouble(5), 0.001);
            assertFalse(cursor.moveToNext());
        }
    }

    @Test
    public void preservesWorkoutHistoryWhenPlanIsDeleted() {
        long plan = db.addPlan("Plan A");
        db.addExercise(plan, "Squat", 3, 8);
        db.log("1405/07/21", "Plan A", "Squat", 3, 8, 20);
        db.deletePlan(plan);

        assertTrue(db.plans().isEmpty());
        try (Cursor cursor = db.allLogs()) {
            assertTrue(cursor.moveToFirst());
            assertEquals("Plan A", cursor.getString(1));
            assertEquals("Squat", cursor.getString(2));
        }
    }
}
