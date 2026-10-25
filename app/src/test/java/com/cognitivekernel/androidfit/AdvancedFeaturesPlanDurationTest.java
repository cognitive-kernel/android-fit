package com.cognitivekernel.androidfit;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class AdvancedFeaturesPlanDurationTest {
    @Test public void shortFullBodySessionKeepsCoreCoverage() {
        assertEquals(3, AdvancedFeatures.movementLimitForSession(10, 5, 1, true));
    }

    @Test public void shortMultiAreaSessionIncludesEachSelectedArea() {
        assertEquals(2, AdvancedFeatures.movementLimitForSession(10, 8, 2, false));
    }

    @Test public void longerSessionsCanIncludeMoreMovements() {
        assertEquals(9, AdvancedFeatures.movementLimitForSession(45, 12, 2, false));
        assertEquals(5, AdvancedFeatures.movementLimitForSession(180, 5, 1, true));
    }

    @Test public void sessionDurationScalesSetsWithinSafeUpperBound() {
        assertEquals(2, AdvancedFeatures.setsForSessionDuration(2, 45));
        assertEquals(4, AdvancedFeatures.setsForSessionDuration(3, 75));
        assertEquals(5, AdvancedFeatures.setsForSessionDuration(4, 120));
        assertEquals(5, AdvancedFeatures.setsForSessionDuration(4, 180));
    }
    @Test public void duplicateMovementNamesAreRemovedIgnoringCaseAndWhitespace() {
        java.util.ArrayList<String[]> movements = new java.util.ArrayList<>();
        movements.add(new String[]{"Push-Up", "chest", "10"});
        movements.add(new String[]{" push-up  ", "arms", "8"});
        movements.add(new String[]{"Squat", "legs", "12"});
        java.util.ArrayList<String[]> unique = AdvancedFeatures.deduplicateMovementRows(movements);
        assertEquals(2, unique.size());
        assertEquals("Push-Up", unique.get(0)[0]);
        assertEquals("Squat", unique.get(1)[0]);
        assertEquals(" push-up  ", movements.get(1)[0]);
    }

    @Test public void activityLevelChangesEstimatedMaintenanceMultiplier() {
        assertEquals(1.35, AdvancedFeatures.activityMultiplier(0), 0.0001);
        assertEquals(1.55, AdvancedFeatures.activityMultiplier(1), 0.0001);
        assertEquals(1.725, AdvancedFeatures.activityMultiplier(2), 0.0001);
    }
}
