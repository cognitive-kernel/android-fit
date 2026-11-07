package com.cognitivekernel.androidfit;

import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

public class FitnessPlanEngineTest {
    private FitnessPlanEngine.Profile profile() {
        FitnessPlanEngine.Profile p=new FitnessPlanEngine.Profile();p.english=true;
        p.age=30;p.heightCm=175;p.weightKg=75;p.sexIndex=0;p.goalIndex=1;
        p.activityIndex=2;p.levelIndex=1;p.equipmentIndex=2;p.trainingDays=4;
        p.sessionMinutes=60;p.sleepHours=7.5;p.mealsPerDay=4;
        p.conditions="none";p.injuries="none";p.medications="none";p.allergies="none";
        return p;
    }

    @Test public void calculatesMifflinStJeorAndGoalSpecificTargets() {
        FitnessPlanEngine.Profile p=profile();
        FitnessPlanEngine.Analysis a=FitnessPlanEngine.analyze(p);
        assertEquals(1698.75, a.bmr, 0.01);
        assertEquals(2633.0625, a.maintenanceCalories, 0.1);
        assertTrue(a.targetCalories>a.maintenanceCalories);
        assertEquals(135, a.proteinGrams, 0.01);
        assertTrue(a.fatGrams>0);
        assertTrue(a.carbohydrateGrams>0);
    }

    @Test public void unspecifiedSexUsesTheMidpointEnergyEstimate() {
        FitnessPlanEngine.Profile p=profile();p.sexIndex=2;
        FitnessPlanEngine.Analysis a=FitnessPlanEngine.analyze(p);
        assertEquals(1615.75,a.bmr,0.01);
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

    @Test public void medicalDisclosureRequiresClearanceBeforeDetailedWorkoutAndMenu() {
        FitnessPlanEngine.Profile p=profile();
        p.conditions="hypertension";
        FitnessPlanEngine.Analysis a=FitnessPlanEngine.analyze(p);
        String training=FitnessPlanEngine.trainingReport(p,a,Collections.emptyList());
        String nutrition=FitnessPlanEngine.nutritionReport(p,a);
        assertTrue(training.contains("No individualized exercise prescription"));
        assertTrue(nutrition.contains("No individualized food menu"));
        assertFalse(nutrition.contains("Breakfast"));
        p.medicalClearance=true;
        assertTrue(FitnessPlanEngine.trainingReport(p,FitnessPlanEngine.analyze(p),Collections.emptyList()).contains("Day 1"));
        assertTrue(FitnessPlanEngine.nutritionReport(p,FitnessPlanEngine.analyze(p)).contains("Breakfast"));
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

    @Test public void foodAllergiesMatchAdditionalEnglishAndPersianMenuIngredients() {
        FitnessPlanEngine.Profile p=profile();
        p.allergies="banana";
        String banana=FitnessPlanEngine.nutritionReport(p,FitnessPlanEngine.analyze(p));
        assertFalse(banana.contains("Banana (edible)"));
        p.allergies="برنج";
        String rice=FitnessPlanEngine.nutritionReport(p,FitnessPlanEngine.analyze(p));
        assertFalse(rice.contains("White rice (cooked)"));
        p.allergies="نخود";
        String chickpeas=FitnessPlanEngine.nutritionReport(p,FitnessPlanEngine.analyze(p));
        assertFalse(chickpeas.contains("Chickpeas (cooked)"));
        p.allergies="سیب‌زمینی";
        String potato=FitnessPlanEngine.nutritionReport(p,FitnessPlanEngine.analyze(p));
        assertFalse(potato.contains("Potato (boiled)"));
    }

    @Test public void dairyAllergyKeepsDairyFreeSoyYogurtButSoyAllergyRemovesIt() {
        FitnessPlanEngine.Profile p=profile();
        p.dietaryPreference="vegan";
        p.allergies="dairy";
        String dairySafe=FitnessPlanEngine.nutritionReport(p,FitnessPlanEngine.analyze(p));
        assertTrue(dairySafe.contains("Unsweetened soy yogurt"));
        assertFalse(dairySafe.contains("Plain Greek yogurt"));
        p.allergies="soy";
        String soySafe=FitnessPlanEngine.nutritionReport(p,FitnessPlanEngine.analyze(p));
        assertFalse(soySafe.contains("Unsweetened soy yogurt"));
    }

    @Test public void foodDislikesAreRemovedFromSampleMenu() {
        FitnessPlanEngine.Profile p=profile();
        p.dislikedFoods="chicken, beef";
        String report=FitnessPlanEngine.nutritionReport(p,FitnessPlanEngine.analyze(p));
        assertFalse(report.contains("Chicken breast"));
        assertFalse(report.contains("Lean beef"));
        assertTrue(report.contains("Foods to avoid"));
    }

    @Test public void foodDislikesAreMatchedInPersian() {
        FitnessPlanEngine.Profile p=profile();
        p.dislikedFoods="مرغ، گوشت قرمز، بادام";
        String report=FitnessPlanEngine.nutritionReport(p,FitnessPlanEngine.analyze(p));
        assertFalse(report.contains("Chicken breast"));
        assertFalse(report.contains("Lean beef"));
        assertFalse(report.contains("Almonds"));
    }

    @Test public void lowerCalorieProfilesReceiveReducedPortionsInsteadOfAnUnusableMenu() {
        FitnessPlanEngine.Profile p=profile();
        p.sexIndex=1;p.heightCm=160;p.weightKg=50;p.goalIndex=3;p.activityIndex=0;
        String report=FitnessPlanEngine.nutritionReport(p,FitnessPlanEngine.analyze(p));
        java.util.regex.Matcher matcher=java.util.regex.Pattern.compile("DAILY MENU TOTAL: ([0-9]+) kcal").matcher(report);
        assertTrue("daily menu total missing",matcher.find());
        double actual=Double.parseDouble(matcher.group(1));
        double target=FitnessPlanEngine.analyze(p).targetCalories;
        assertTrue("reduced portions did not match calorie target: "+actual+" vs "+target,
                Math.abs(actual-target)/target<0.15);
    }

    @Test public void calorieTotalsRemainCloseAfterVegetarianAndAllergenFiltering() {
        FitnessPlanEngine.Profile p=profile();
        p.dietaryPreference="vegetarian";
        p.allergies="egg, dairy, almonds";
        String report=FitnessPlanEngine.nutritionReport(p,FitnessPlanEngine.analyze(p));
        java.util.regex.Matcher matcher=java.util.regex.Pattern.compile("DAILY MENU TOTAL: ([0-9]+) kcal").matcher(report);
        assertTrue("daily menu total missing",matcher.find());
        double actual=Double.parseDouble(matcher.group(1));
        double target=FitnessPlanEngine.analyze(p).targetCalories;
        assertTrue("menu calories drifted after exclusions: "+actual+" vs "+target,
                Math.abs(actual-target)/target<0.15);
        assertFalse(report.contains("Egg whites"));
        assertFalse(report.contains("Greek yogurt"));
        assertFalse(report.contains("Almonds"));
        assertFalse(report.contains("Chicken breast"));
        assertFalse(report.contains("Lean beef"));
    }

    @Test public void broadPersianFoodExclusionsRemoveMatchingItems() {
        FitnessPlanEngine.Profile p=profile();
        p.dislikedFoods="گوشت، سبزیجات";
        String report=FitnessPlanEngine.nutritionReport(p,FitnessPlanEngine.analyze(p));
        assertFalse(report.contains("Chicken breast"));
        assertFalse(report.contains("Lean beef"));
        assertFalse(report.contains("Mixed non-starchy vegetables"));
    }

    @Test public void nutritionMenuHonorsThreeOrFiveMealsPerDay() {
        FitnessPlanEngine.Profile p=profile();
        p.mealsPerDay=3;
        String three=FitnessPlanEngine.nutritionReport(p,FitnessPlanEngine.analyze(p));
        assertTrue(three.contains("Breakfast + snack"));
        assertEquals(3,count(three,"Meal subtotal:"));
        p.mealsPerDay=5;
        String five=FitnessPlanEngine.nutritionReport(p,FitnessPlanEngine.analyze(p));
        assertTrue(five.contains("Snack 1"));
        assertTrue(five.contains("Snack 2"));
        assertEquals(5,count(five,"Meal subtotal:"));
    }

    private static int count(String source,String needle) {
        int total=0,at=0;
        while((at=source.indexOf(needle,at))>=0){total++;at+=needle.length();}
        return total;
    }

    @Test public void trainingReportIncludesWeeklyScheduleProgressionAndRecovery() {
        FitnessPlanEngine.Profile p=profile();
        String report=FitnessPlanEngine.trainingReport(p,FitnessPlanEngine.analyze(p),Collections.emptyList());
        assertTrue(report.contains("Day 4"));
        assertTrue(report.contains("repetitions in reserve"));
        assertTrue(report.contains("Add reps first"));
        assertTrue(report.contains("Sleep reported"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void requiresExplicitMedicalAndAllergyScreening() {
        FitnessPlanEngine.Profile p=profile();
        p.allergies="";
        FitnessPlanEngine.analyze(p);
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsInvalidDietPreference() {
        FitnessPlanEngine.Profile p=profile();p.dietaryPreference="anything";
        FitnessPlanEngine.analyze(p);
    }
}
