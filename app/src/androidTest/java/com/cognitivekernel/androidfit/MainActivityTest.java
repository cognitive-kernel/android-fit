package com.cognitivekernel.androidfit;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.core.app.ActivityScenario;
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
    assertNotNull(activity.findViewById(R.id.exportButton));\n    assertNotNull(activity.findViewById(R.id.languageButton));\n    assertNotNull(activity.findViewById(R.id.personalPlanButton));\n    assertNotNull(activity.findViewById(R.id.historyButton));\n    assertNotNull(activity.findViewById(R.id.libraryButton));\n    assertNotNull(activity.findViewById(R.id.coachButton));\n    assertNotNull(activity.findViewById(R.id.weeklyGoalButton));\n    assertNotNull(activity.findViewById(R.id.reminderButton));\n    assertNotNull(activity.findViewById(R.id.backupButton));\n    assertNotNull(activity.findViewById(R.id.restoreButton));
   });
  }
 }
}
