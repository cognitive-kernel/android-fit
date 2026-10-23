package com.cognitivekernel.androidfit;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.core.app.ActivityScenario;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Button;
import androidx.test.platform.app.InstrumentationRegistry;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {
 @Test public void mainScreenShowsAllNavigationActions() {
  try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
   scenario.onActivity(activity -> {
    assertNotNull(activity.findViewById(R.id.managePlansButton));
    assertNotNull(activity.findViewById(R.id.logWorkoutButton));
    assertNotNull(activity.findViewById(R.id.stepsButton));
    assertNotNull(activity.findViewById(R.id.exportButton));
    assertNotNull(activity.findViewById(R.id.sleepButton));
    assertNotNull(activity.findViewById(R.id.vitalsButton));
    assertNotNull(activity.findViewById(R.id.bodyMetricsButton));
    assertNotNull(activity.findViewById(R.id.reportButton));
    assertNotNull(activity.findViewById(R.id.stepGoalButton));
    assertNotNull(activity.findViewById(R.id.personalPlanButton));
    assertNotNull(activity.findViewById(R.id.historyButton));
    assertNotNull(activity.findViewById(R.id.libraryButton));
    assertNotNull(activity.findViewById(R.id.coachButton));
    assertNotNull(activity.findViewById(R.id.weeklyGoalButton));
    assertNotNull(activity.findViewById(R.id.reminderButton));
    assertNotNull(activity.findViewById(R.id.backupButton));
    assertNotNull(activity.findViewById(R.id.restoreButton));
    assertNotNull(activity.findViewById(R.id.heroIllustration));
    assertNotNull(activity.findViewById(R.id.goalNutritionButton));
   });
  }
 }
 @Test public void mainScreenUsesEnglishResourcesWhenEnglishIsSelected() {
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  SharedPreferences prefs=context.getSharedPreferences("fit_settings",Context.MODE_PRIVATE);
  String previous=prefs.getString("language","fa");
  prefs.edit().putString("language","en").commit();
  try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
   scenario.onActivity(activity -> assertEquals("Manage workout plans",((Button)activity.findViewById(R.id.managePlansButton)).getText().toString()));
  } finally { prefs.edit().putString("language",previous).commit(); }
 }

 @Test public void mainScreenUsesPersianResourcesWhenPersianIsSelected() {
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  SharedPreferences prefs=context.getSharedPreferences("fit_settings",Context.MODE_PRIVATE);
  String previous=prefs.getString("language","fa");
  prefs.edit().putString("language","fa").commit();
  try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
   scenario.onActivity(activity -> assertEquals("مدیریت برنامه‌های تمرینی",((Button)activity.findViewById(R.id.managePlansButton)).getText().toString()));
  } finally { prefs.edit().putString("language",previous).commit(); }
 }

}
