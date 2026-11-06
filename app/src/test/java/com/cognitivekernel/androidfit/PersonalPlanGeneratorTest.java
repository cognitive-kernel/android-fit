package com.cognitivekernel.androidfit;

import android.app.Activity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PersonalPlanGeneratorTest {
    @Test public void generatesExercisesForEveryGoalAndGenderFromCatalog() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            List<ExerciseAssetIndex.ExerciseAsset> catalog = ExerciseAssetIndex.all(controller.get());
            for (int goal = 0; goal < 4; goal++) {
                for (String gender : new String[]{"male", "female"}) {
                    List<PersonalPlanGenerator.PlannedExercise> plan =
                            PersonalPlanGenerator.generate(catalog, goal, 1, 0, gender, 35);
                    assertEquals(4, plan.size());
                    for (PersonalPlanGenerator.PlannedExercise item : plan) {
                        assertNotNull(item.asset.id);
                        assertTrue(gender.equals(item.asset.gender) || "unspecified".equals(item.asset.gender));
                        assertTrue(item.sets >= 2);
                        assertTrue(item.reps >= 1);
                    }
                }
            }
        }
    }

    @Test public void equipmentFreePlanDoesNotSelectMachineOrFreeWeightMovements() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            List<PersonalPlanGenerator.PlannedExercise> plan = PersonalPlanGenerator.generate(
                    ExerciseAssetIndex.all(controller.get()), 2, 0, 0, "male", 50);
            assertEquals(6, plan.size());
            for (PersonalPlanGenerator.PlannedExercise item : plan) {
                String name = item.asset.name.toLowerCase(java.util.Locale.ROOT);
                assertFalse(name.contains("machine"));
                assertFalse(name.contains("cable"));
                assertFalse(name.contains("barbell"));
                assertFalse(name.contains("dumbbell"));
                assertFalse(name.contains("kettlebell"));
                assertFalse(name.contains("resistance band"));
            }
        }
    }

    @Test public void generatedPlanPersistsExercisesIncludingNeutralGenderAssetsAtomically() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            WorkoutDb db = new WorkoutDb(activity);
            try {
                List<PersonalPlanGenerator.PlannedExercise> generated = PersonalPlanGenerator.generate(
                        ExerciseAssetIndex.all(activity), 2, 0, 0, "female", 35);
                long planId = db.addPlanWithExercises("Generated test plan", generated);
                List<String[]> saved = db.exercises(planId);
                assertEquals(generated.size(), saved.size());
                for (String[] row : saved) {
                    assertNotNull(row[4]);
                    assertTrue("female".equals(row[5]) || "unspecified".equals(row[5]));
                }
            } finally {
                db.close();
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyGeneratedPlanIsRejectedBeforeCreatingAnEmptyPlan() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            WorkoutDb db = new WorkoutDb(controller.get());
            try {
                db.addPlanWithExercises("Must not be empty", java.util.Collections.emptyList());
            } finally {
                db.close();
            }
        }
    }
}
