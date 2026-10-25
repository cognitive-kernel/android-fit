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
    }

    @Test public void differentMovementVariantsAreRejected() {
        assertEquals(-1, ExerciseGifMatcher.score("Lateral Raise", "Lateral Raise with Towel"));
        assertEquals(-1, ExerciseGifMatcher.score("Lateral Raise", "Reverse Lateral Raise"));
        assertEquals(-1, ExerciseGifMatcher.score("Shoulder External Rotation", "Shoulder External Rotation Test"));
        assertEquals(-1, ExerciseGifMatcher.score("Reverse Lunge", "Reverse Lunge with Leg Kick"));
        assertEquals(-1, ExerciseGifMatcher.score("Reverse Lunge", "Bodyweight Reverse Lunge with Overhead"));
        assertEquals(-1, ExerciseGifMatcher.score("Plank", "Plank Jack"));
    }

    @Test public void emptyNamesAreRejected() {
        assertEquals(-1, ExerciseGifMatcher.score("", "Push-up"));
        assertEquals(-1, ExerciseGifMatcher.score("Push-up", null));
    }
}
