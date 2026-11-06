package com.cognitivekernel.androidfit;

import android.app.Activity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PlansExercisesLocalizationTest {
    @Test public void englishCatalogNamesAreLocalizedOnlyForPersianDisplay() {
        assertEquals("Barbell Squat",
                MainActivity.exerciseDisplayName("Barbell Squat", true));
        assertEquals("هالتر اسکوات",
                MainActivity.exerciseDisplayName("Barbell Squat", false));
        assertEquals("اسکوات",
                MainActivity.exerciseDisplayName("اسکوات", false));
    }

    @Test public void displayingGeneratedPlanNamesDoesNotRewriteStoredExerciseNames() {
        try (ActivityController<Activity> controller =
                     Robolectric.buildActivity(Activity.class).setup()) {
            WorkoutDb database = new WorkoutDb(controller.get());
            long planId = database.addPlan("برنامه شخصی");
            database.addExercise(planId, "Barbell Squat", 3, 10);

            String[] storedBeforeDisplay = database.exercises(planId).get(0);
            String displayed = MainActivity.exerciseDisplayName(storedBeforeDisplay[1], false);
            String[] storedAfterDisplay = database.exercises(planId).get(0);

            assertEquals("هالتر اسکوات", displayed);
            assertEquals("Barbell Squat", storedAfterDisplay[1]);
            assertEquals(storedBeforeDisplay[1], storedAfterDisplay[1]);
        }
    }
}
