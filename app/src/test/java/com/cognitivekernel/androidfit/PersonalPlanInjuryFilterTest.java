package com.cognitivekernel.androidfit;

import org.junit.Test;
import static org.junit.Assert.*;

public class PersonalPlanInjuryFilterTest {
    @Test public void kneeLimitationsExcludeHighLoadAndImpactKneeMovements() {
        assertFalse(PersonalPlanGenerator.isCompatibleWithLimitations("Barbell Squat","knee pain"));
        assertFalse(PersonalPlanGenerator.isCompatibleWithLimitations("Walking Lunge","درد زانو"));
        assertFalse(PersonalPlanGenerator.isCompatibleWithLimitations("Jump Squat","knee"));
        assertTrue(PersonalPlanGenerator.isCompatibleWithLimitations("Seated Biceps Curl","knee pain"));
    }

    @Test public void shoulderLimitationsExcludeOverheadAndShoulderIsolationMovements() {
        assertFalse(PersonalPlanGenerator.isCompatibleWithLimitations("Dumbbell Shoulder Press","shoulder injury"));
        assertFalse(PersonalPlanGenerator.isCompatibleWithLimitations("Lateral Raise","درد شانه"));
        assertTrue(PersonalPlanGenerator.isCompatibleWithLimitations("Leg Curl","shoulder injury"));
    }

    @Test public void backLimitationsExcludeLoadedHingeAndBentOverMovements() {
        assertFalse(PersonalPlanGenerator.isCompatibleWithLimitations("Barbell Deadlift","lower back pain"));
        assertFalse(PersonalPlanGenerator.isCompatibleWithLimitations("Bent Over Row","کمر درد"));
        assertTrue(PersonalPlanGenerator.isCompatibleWithLimitations("Seated Leg Extension","lower back pain"));
    }

    @Test public void wristAndAnkleLimitationsExcludeRelevantMovements() {
        assertFalse(PersonalPlanGenerator.isCompatibleWithLimitations("Push-Up","wrist pain"));
        assertFalse(PersonalPlanGenerator.isCompatibleWithLimitations("Running","ankle injury"));
        assertTrue(PersonalPlanGenerator.isCompatibleWithLimitations("Seated Row","ankle injury"));
    }

    @Test public void noReportedLimitationDoesNotFilterCatalog() {
        assertTrue(PersonalPlanGenerator.isCompatibleWithLimitations("Barbell Squat","none"));
        assertTrue(PersonalPlanGenerator.isCompatibleWithLimitations("Barbell Squat",""));
    }
}
