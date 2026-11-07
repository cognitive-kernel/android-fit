package com.cognitivekernel.androidfit;

import android.app.Activity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PersonalPlanGeneratorTest {
    @Test public void generatesRequestedExerciseCountForEveryPreferenceCombination() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            List<ExerciseAssetIndex.ExerciseAsset> catalog = ExerciseAssetIndex.all(controller.get());
            int[] durations = {10, 20, 21, 35, 36, 50, 51, 180};
            for (int goal = 0; goal < 4; goal++) {
                for (int level = 0; level < 3; level++) {
                    for (int equipment = 0; equipment < 3; equipment++) {
                        for (String gender : new String[]{"male", "female"}) {
                            for (int minutes : durations) {
                                List<PersonalPlanGenerator.PlannedExercise> plan =
                                        PersonalPlanGenerator.generate(catalog, goal, level, equipment, gender, minutes);
                                int expectedCount = minutes <= 20 ? 3 : minutes <= 35 ? 4 : minutes <= 50 ? 6 : 8;
                                assertEquals("goal=" + goal + ", level=" + level + ", equipment=" + equipment
                                        + ", gender=" + gender + ", minutes=" + minutes, expectedCount, plan.size());
                                Set<String> ids = new HashSet<>();
                                for (PersonalPlanGenerator.PlannedExercise item : plan) {
                                    assertNotNull(item.asset.id);
                                    assertTrue(gender.equals(item.asset.gender) || "unspecified".equals(item.asset.gender));
                                    assertTrue(ids.add(item.asset.id));
                                    assertTrue(item.sets >= 1);
                                    assertTrue(item.reps >= 1);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test public void weeklyPlanAssignsDistinctMovementsToTheCorrectTrainingDays() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            List<ExerciseAssetIndex.ExerciseAsset> catalog=ExerciseAssetIndex.all(controller.get());
            List<PersonalPlanGenerator.PlannedExercise> plan=PersonalPlanGenerator.generate(
                    catalog,1,1,2,"all",45,"none",4);
            int[] counts=new int[5];
            Set<String> ids=new HashSet<>();
            for(PersonalPlanGenerator.PlannedExercise item:plan) {
                assertTrue(item.day>=1&&item.day<=4);
                assertTrue(ids.add(item.asset.id));
                counts[item.day]++;
                String category=PersonalPlanGenerator.category(item.asset.name);
                if(item.day%2==1) {
                    assertTrue("upper day contains lower-body movement: "+item.asset.name,
                            "push".equals(category)||"pull".equals(category)||"other".equals(category));
                } else {
                    assertTrue("lower day contains upper-body movement: "+item.asset.name,
                            "legs".equals(category)||"glutes".equals(category)||"core".equals(category)||"other".equals(category));
                }
            }
            for(int day=1;day<=4;day++)assertTrue("day "+day+" should have at least three movements",counts[day]>=3);
            FitnessPlanEngine.Profile profile=new FitnessPlanEngine.Profile();
            profile.english=true;profile.trainingDays=4;profile.equipmentIndex=2;
            profile.conditions="none";profile.injuries="none";profile.medications="none";profile.allergies="none";
            String report=FitnessPlanEngine.trainingReport(profile,FitnessPlanEngine.analyze(profile),plan);
            for(PersonalPlanGenerator.PlannedExercise item:plan)assertTrue("report omitted "+item.asset.name,report.contains(item.asset.name));
        }
    }

    @Test public void complexAndPostSurgicalRestrictionsRequireClinicianReviewedExerciseSelection() {
        assertTrue(PersonalPlanGenerator.hasUnmappedLimitation("knee pain after surgery"));
        assertTrue(PersonalPlanGenerator.hasUnmappedLimitation("shoulder tendon tear"));
        assertTrue(PersonalPlanGenerator.hasUnmappedLimitation("شکستگی زانو"));
        assertFalse(PersonalPlanGenerator.hasUnmappedLimitation("knee"));
        assertFalse(PersonalPlanGenerator.hasUnmappedLimitation("درد زانو"));
        assertFalse(PersonalPlanGenerator.hasUnmappedLimitation("none"));
    }

    @Test public void equipmentFreePlanDoesNotSelectEquipmentDependentMovements() {
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
                assertFalse(name.contains("medicine ball"));
                assertFalse(name.contains("battle rope"));
                assertFalse(name.contains("weighted"));
            }
        }
    }

    @Test public void generatedPlanPersistsExercisesAndLeavesExistingManualDataUntouched() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            WorkoutDb db = new WorkoutDb(activity);
            try {
                long existingPlan = db.addPlan("Existing manual plan");
                long manualExercise = db.addExercise(existingPlan, "My custom movement", 2, 12);
                List<PersonalPlanGenerator.PlannedExercise> generated = PersonalPlanGenerator.generate(
                        ExerciseAssetIndex.all(activity), 2, 0, 0, "female", 35);
                long generatedPlan = db.addPlanWithExercises("Generated test plan", generated);

                List<String[]> saved = db.exercises(generatedPlan);
                assertEquals(generated.size(), saved.size());
                for (int i = 0; i < saved.size(); i++) {
                    assertEquals(generated.get(i).asset.id, saved.get(i)[4]);
                    assertEquals(generated.get(i).asset.gender, saved.get(i)[5]);
                }

                List<String[]> oldRows = db.exercises(existingPlan);
                assertEquals(1, oldRows.size());
                assertEquals(String.valueOf(manualExercise), oldRows.get(0)[0]);
                assertEquals("My custom movement", oldRows.get(0)[1]);
                assertNull(oldRows.get(0)[4]);
                assertNull(oldRows.get(0)[5]);
            } finally {
                db.close();
            }
        }
    }

    @Test public void fiveAndSixDayPlansUseTheExpectedWeeklySplit() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            List<ExerciseAssetIndex.ExerciseAsset> catalog=ExerciseAssetIndex.all(controller.get());
            List<PersonalPlanGenerator.PlannedExercise> five=PersonalPlanGenerator.generate(
                    catalog,1,1,2,"all",45,"none",5);
            int[] fiveCounts=new int[6];
            for(PersonalPlanGenerator.PlannedExercise item:five)fiveCounts[item.day]++;
            for(int day=1;day<=5;day++)assertTrue("five-day split missing day "+day,fiveCounts[day]>=3);
            List<PersonalPlanGenerator.PlannedExercise> six=PersonalPlanGenerator.generate(
                    catalog,1,1,2,"all",45,"none",6);
            int[] sixCounts=new int[7];
            for(PersonalPlanGenerator.PlannedExercise item:six)sixCounts[item.day]++;
            for(int day=1;day<=6;day++)assertTrue("six-day split missing day "+day,sixCounts[day]>=3);
        }
    }

    @Test public void weeklyDayAssignmentsSurviveDatabasePersistence() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity=controller.get();
            WorkoutDb db=new WorkoutDb(activity);
            try {
                List<PersonalPlanGenerator.PlannedExercise> generated=PersonalPlanGenerator.generate(
                        ExerciseAssetIndex.all(activity),1,1,2,"all",45,"none",4);
                long planId=db.addPlanWithExercises("Four day plan",generated);
                List<String[]> rows=db.exercises(planId);
                assertEquals(generated.size(),rows.size());
                for(int i=0;i<generated.size();i++) {
                    assertEquals(String.valueOf(generated.get(i).day),rows.get(i)[6]);
                    assertEquals(generated.get(i).asset.id,rows.get(i)[4]);
                }
            } finally { db.close(); }
        }
    }

    @Test public void failedGeneratedPlanTransactionRollsBackOnlyItsOwnWrites() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup()) {
            WorkoutDb db = new WorkoutDb(controller.get());
            try {
                long existingPlan = db.addPlan("Keep this plan");
                db.addExercise(existingPlan, "Manual movement", 3, 10);
                List<PersonalPlanGenerator.PlannedExercise> generated = PersonalPlanGenerator.generate(
                        ExerciseAssetIndex.all(controller.get()), 1, 0, 0, "female", 35);
                try {
                    db.addPlanWithExercises("Must roll back", Arrays.asList(generated.get(0), null));
                    fail("Expected invalid generated movement to abort the transaction");
                } catch (IllegalArgumentException expected) {
                    assertEquals(1, db.plans().size());
                    assertEquals("Keep this plan", db.plans().get(0)[1]);
                    assertEquals("Manual movement", db.exercises(existingPlan).get(0)[1]);
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
