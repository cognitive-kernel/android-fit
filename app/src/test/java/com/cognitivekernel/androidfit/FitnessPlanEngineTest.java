package com.cognitivekernel.androidfit;

import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

public class FitnessPlanEngineTest {
    private FitnessPlanEngine.Profile profile() {
        FitnessPlanEngine.Profile p=new FitnessPlanEngine.Profile();
        p.age=30;p.heightCm=175;p.weightKg=75;p.sexIndex=0;p.goalIndex=1;
        p.activityIndex=2;p.levelIndex=1;p.equipmentIndex=2;p.trainingDays=4;
        p.sessionMinutes=60;p.sleepHours=7.5;p.mealsPerDay=4;
        return p;
    }

    @Test public void calculatesMifflinStJeorAndGoalSpecificTargets() {
        FitnessPlanEngine.Profile p=profile();
        FitnessPlanEngine.Analysis a=FitnessPlanEngine.analyze(p);
        assertEquals(1730, a.bmr, 0.01);
        assertEquals(2681.5, a.maintenanceCalories, 0.1);
        assertTrue(a.targetCalories>a.maintenanceCalories);
        assertEquals(135, a.proteinGrams, 0.01);
        assertTrue(a.fatGrams>0);
        assertTrue(a.carbohydrateGrams>0);
    }

    @Test public void underweightProfileDoesNotReceiveFatLossDeficit() {
        FitnessPlanEngine.Profile p=profile();
        p.heightCm=190;p.weightKg=50;p.goalIndex=3;
        FitnessPlanEngine.Analysis a=FitnessPlanEngine.analyze(p);
        assertEquals(a.maintenanceCalories,a.targetCalories,0.01);
    }

    @Test public void reportsHealthConditionsAndInjuriesForClinicalReview() {
        FitnessPlanEngine.Profile p=profile();
        p.conditions="hypertension";
        p.injuries="right shoulder pain";
        FitnessPlanEngine.Analysis a=FitnessPlanEngine.analyze(p);
        assertTrue(a.needsMedicalReview);
        assertTrue(a.medicalReason.contains("hypertension"));
        assertTrue(a.medicalReason.contains("right shoulder pain"));
    }

    @Test public void noneValuesDoNotTriggerMedicalWarning() {
        FitnessPlanEngine.Profile p=profile();
        p.conditions="ندارد";p.injuries="none";p.medications="no";
        assertFalse(FitnessPlanEngine.analyze(p).needsMedicalReview);
    }

    @Test public void nutritionReportProvidesPortionQuantitiesAndCaloriesPerFood() {
        FitnessPlanEngine.Profile p=profile();
        String report=FitnessPlanEngine.nutritionReport(p,FitnessPlanEngine.analyze(p));
        assertTrue(report.contains("Egg whites"));
        assertTrue(report.contains("egg whites"));
        assertTrue(report.contains("kcal"));
        assertTrue(report.contains("Lean beef (cooked)"));
        assertTrue(report.contains("g"));
        assertTrue(report.contains("DAILY MENU TOTAL"));
    }

    @Test public void vegetarianAndVeganMenusDoNotContainMeat() {
        FitnessPlanEngine.Profile p=profile();
        p.dietaryPreference="vegetarian";
        String vegetarian=FitnessPlanEngine.nutritionReport(p,FitnessPlanEngine.analyze(p));
        assertFalse(vegetarian.contains("Chicken breast"));
        assertFalse(vegetarian.contains("Lean beef"));
        p.dietaryPreference="vegan";
        String vegan=FitnessPlanEngine.nutritionReport(p,FitnessPlanEngine.analyze(p));
        assertFalse(vegan.contains("Chicken breast"));
        assertFalse(vegan.contains("Lean beef"));
        assertFalse(vegan.contains("Egg whites"));
        assertFalse(vegan.contains("Greek yogurt"));
    }

    @Test public void recognizedAllergensAreRemovedFromSampleMenu() {
        FitnessPlanEngine.Profile p=profile();
        p.allergies="egg, dairy, almonds";
        String report=FitnessPlanEngine.nutritionReport(p,FitnessPlanEngine.analyze(p));
        assertFalse(report.contains("Egg whites"));
        assertFalse(report.contains("Greek yogurt"));
        assertFalse(report.contains("Almonds"));
        assertTrue(report.contains("ALLERGY NOTE"));
    }

    @Test public void foodDislikesAreRemovedFromSampleMenu() {
        FitnessPlanEngine.Profile p=profile();
        p.dislikedFoods="chicken, beef";
        String report=FitnessPlanEngine.nutritionReport(p,FitnessPlanEngine.analyze(p));
        assertFalse(report.contains("Chicken breast"));
        assertFalse(report.contains("Lean beef"));
        assertTrue(report.contains("Foods to avoid"));
    }

    @Test public void trainingReportIncludesWeeklyScheduleProgressionAndRecovery() {
        FitnessPlanEngine.Profile p=profile();
        String report=FitnessPlanEngine.trainingReport(p,FitnessPlanEngine.analyze(p),Collections.emptyList());
        assertTrue(report.contains("Day 4"));
        assertTrue(report.contains("repetitions in reserve"));
        assertTrue(report.contains("Progression")||report.contains("Add repetitions"));
        assertTrue(report.contains("Sleep reported"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsInvalidDietPreference() {
        FitnessPlanEngine.Profile p=profile();p.dietaryPreference="anything";
        FitnessPlanEngine.analyze(p);
    }
}
