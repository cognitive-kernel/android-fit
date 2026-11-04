package com.cognitivekernel.androidfit;

import org.junit.Test;
import static org.junit.Assert.*;

public class OfflineCoachEngineTest {
    @Test public void buildsWeeklyOfflineSessionsWithinRequestedDays() {
        OfflineCoachEngine.Input in=new OfflineCoachEngine.Input();
        in.daysPerWeek=4; in.sessionMinutes=40;
        OfflineCoachEngine.Plan plan=OfflineCoachEngine.build(in);
        assertFalse(plan.needsClinicalReview);
        assertEquals(4,plan.sessions.size());
        assertTrue(plan.sessions.get(0).sets>=1);
    }

    @Test public void reducesTrainingLoadAfterVeryHardRecentSession() {
        OfflineCoachEngine.Input in=new OfflineCoachEngine.Input();
        in.recentRpe=10; in.level=OfflineCoachEngine.Level.ADVANCED;
        OfflineCoachEngine.Plan plan=OfflineCoachEngine.build(in);
        assertEquals(3,plan.sessions.get(0).sets);
        assertTrue(plan.progression.contains("Recovery adjustment"));
    }

    @Test public void blocksPlanForRedFlagSymptomsOrAcuteInjury() {
        OfflineCoachEngine.Input in=new OfflineCoachEngine.Input();
        in.chestPain=true;
        OfflineCoachEngine.Plan plan=OfflineCoachEngine.build(in);
        assertTrue(plan.needsClinicalReview);
        assertTrue(plan.sessions.isEmpty());
    }

    @Test public void adaptsExerciseToNoEquipmentAndShortSessions() {
        OfflineCoachEngine.Input in=new OfflineCoachEngine.Input();
        in.equipment=OfflineCoachEngine.Equipment.NONE; in.sessionMinutes=15;
        OfflineCoachEngine.Plan plan=OfflineCoachEngine.build(in);
        assertEquals(3,plan.sessions.get(0).exercises.size());
        assertTrue(plan.sessions.get(0).exercises.contains("Prone W raise"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsInvalidTrainingDays() {
        OfflineCoachEngine.Input in=new OfflineCoachEngine.Input();
        in.daysPerWeek=0;
        OfflineCoachEngine.build(in);
    }
}
