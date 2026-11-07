package com.cognitivekernel.androidfit;

import android.app.Activity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.RobolectricTestRunner;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
public class FitnessProfileStoreTest {
    @Test public void savesAndRestoresTheFullProfile() {
        try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()) {
            Activity a=controller.get();
            FitnessPlanEngine.Profile p=new FitnessPlanEngine.Profile();
            p.age=41;p.heightCm=181.5;p.weightKg=83.2;p.sexIndex=1;p.goalIndex=3;
            p.activityIndex=2;p.levelIndex=1;p.equipmentIndex=2;p.trainingDays=4;
            p.sessionMinutes=55;p.sleepHours=6.5;p.conditions="controlled asthma";
            p.injuries="none";p.medications="inhaler";p.allergies="peanuts";
            p.dietaryPreference="vegetarian";p.dislikedFoods="mushrooms";p.notes="night shift";p.medicalClearance=true;
            FitnessProfileStore.save(a,p);
            FitnessPlanEngine.Profile restored=FitnessProfileStore.load(a);
            assertTrue(FitnessProfileStore.hasProfile(a));
            assertEquals(41,restored.age);
            assertEquals(181.5,restored.heightCm,0.001);
            assertEquals(83.2,restored.weightKg,0.001);
            assertEquals(3,restored.goalIndex);
            assertEquals(4,restored.trainingDays);
            assertEquals(6.5,restored.sleepHours,0.001);
            assertEquals("controlled asthma",restored.conditions);
            assertEquals("peanuts",restored.allergies);
            assertEquals("vegetarian",restored.dietaryPreference);
            assertEquals("night shift",restored.notes);
            assertTrue(restored.medicalClearance);
        }
    }

    @Test public void savingProfileDoesNotModifyExistingWorkoutRecords() {
        try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()) {
            Activity a=controller.get();
            WorkoutDb db=new WorkoutDb(a);
            try {
                long plan=db.addPlan("Keep existing");
                long exercise=db.addExercise(plan,"Manual exercise",3,10);
                db.log("1405/08/16","Keep existing","Manual exercise",3,10,20);
                FitnessPlanEngine.Profile p=new FitnessPlanEngine.Profile();
                FitnessProfileStore.save(a,p);
                assertEquals(1,db.plans().size());
                assertEquals(String.valueOf(plan),db.plans().get(0)[0]);
                assertEquals(String.valueOf(exercise),db.exercises(plan).get(0)[0]);
                try(android.database.Cursor logs=db.allLogs()) {
                    assertTrue(logs.moveToFirst());
                    assertEquals("Manual exercise",logs.getString(2));
                }
            } finally {
                db.close();
            }
        }
    }
}
