package com.cognitivekernel.androidfit;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExerciseGifMatcherTest {
    @Test public void exactMovementNameIsPreferred() {
        assertEquals(0, ExerciseGifMatcher.score("Push-Up", "Push-up"));
        assertEquals(0, ExerciseGifMatcher.score("Dead Bug", "Dead Bug"));
    }

    @Test public void relevantEquipmentAndGenderVariantsAreAllowed() {
        assertTrue(ExerciseGifMatcher.score("Overhead Press", "Barbell Seated Overhead Press (male)") > 0);
        assertTrue(ExerciseGifMatcher.score("Biceps Curl", "Band Biceps Curl (female)") > 0);
        assertTrue(ExerciseGifMatcher.score("Squat", "Air Squat (VERSION 2)") > 0);
        assertTrue(ExerciseGifMatcher.score("Reverse Lunge", "Bodyweight Reverse Lunge") > 0);
        assertTrue(ExerciseGifMatcher.score("Reverse Lunge", "Bodyweight Reverse Lunge with Overhead...") > 0);
        assertTrue(ExerciseGifMatcher.score("Incline Bench Press", "Dumbbell Incline Bench Press") > 0);
        assertTrue(ExerciseGifMatcher.score("Bent Over Row", "Dumbbell Bent Over Row") > 0);
    }

    @Test public void differentMovementVariantsAreRejected() {
        assertEquals(-1, ExerciseGifMatcher.score("Lateral Raise", "Lateral Raise with Towel"));
        assertEquals(-1, ExerciseGifMatcher.score("Lateral Raise", "Reverse Lateral Raise"));
        assertEquals(-1, ExerciseGifMatcher.score("Shoulder External Rotation", "Shoulder External Rotation Test"));
        assertEquals(-1, ExerciseGifMatcher.score("Reverse Lunge", "Reverse Lunge with Leg Kick"));

        assertEquals(-1, ExerciseGifMatcher.score("Plank", "Plank Jack"));
        assertEquals(-1, ExerciseGifMatcher.score("Plank", "Bodyweight Front Plank to Downward Dog"));
    }

    @Test public void bundledCoreGifsResolveFromCatalogueNames() {
        assertEquals("squat.gif", ExerciseGifView.bundledCoreAlias("Air Squat (VERSION 2)_[471413].gif"));
        assertEquals("pushup.gif", ExerciseGifView.bundledCoreAlias("Push-up_[288113].gif"));
        assertEquals("lunge.gif", ExerciseGifView.bundledCoreAlias("Bodyweight Reverse Lunge with Overhead..._[655113].gif"));
        assertEquals("plank.gif", ExerciseGifView.bundledCoreAlias("Bodyweight Front Plank to Downward Dog_[450013].gif"));
        assertEquals("row.gif", ExerciseGifView.bundledCoreAlias("Band Bent Over Wide Grip Row (male)_[445213].gif"));
        assertEquals("walk.gif", ExerciseGifView.bundledCoreAlias("Briskly Walking_[300413].gif"));
        assertEquals(null, ExerciseGifView.bundledCoreAlias("Unbundled Exercise.gif"));
    }

    @Test public void emptyNamesAreRejected() {
        assertEquals(-1, ExerciseGifMatcher.score("", "Push-up"));
        assertEquals(-1, ExerciseGifMatcher.score("Push-up", null));
    }
}
