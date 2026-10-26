package com.cognitivekernel.androidfit;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.withSpinnerText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SmartGoalPlanSelectionTest {
    @Test public void selectedAreasCanBeCombinedAndFullBodyIsExclusive() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences("fit_settings", Context.MODE_PRIVATE);
        String previous = prefs.getString("language", "fa");
        prefs.edit().putString("language", "fa").commit();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> activity.findViewById(R.id.goalNutritionButton).performClick());

            Espresso.onView(withText("کل بدن")).check(matches(isChecked()));
            Espresso.onView(withText("سرشانه")).perform(click());
            Espresso.onView(withText("کل بدن")).check(matches(androidx.test.espresso.matcher.ViewMatchers.isNotChecked()));
            Espresso.onView(withText("سرشانه")).check(matches(isChecked()));

            Espresso.onView(withText("بازو")).perform(click());
            Espresso.onView(withText("سرشانه")).check(matches(isChecked()));
            Espresso.onView(withText("بازو")).check(matches(isChecked()));

            Espresso.onView(withText("کل بدن")).perform(click());
            Espresso.onView(withText("کل بدن")).check(matches(isChecked()));
            Espresso.onView(withText("سرشانه")).check(matches(androidx.test.espresso.matcher.ViewMatchers.isNotChecked()));
            Espresso.onView(withText("بازو")).check(matches(androidx.test.espresso.matcher.ViewMatchers.isNotChecked()));
        } finally {
            prefs.edit().putString("language", previous).commit();
        }
    }

    @Test public void dailyActivityLevelIsVisibleAndSelectable() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences("fit_settings", Context.MODE_PRIVATE);
        String previous = prefs.getString("language", "fa");
        prefs.edit().putString("language", "fa").commit();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> activity.findViewById(R.id.goalNutritionButton).performClick());

            Espresso.onView(withContentDescription("سطح فعالیت روزانه"))
                    .check(matches(isDisplayed()))
                    .perform(click());
            Espresso.onView(withText("فعالیت کم")).perform(click());
            Espresso.onView(withContentDescription("سطح فعالیت روزانه"))
                    .check(matches(withSpinnerText("فعالیت کم")));

            Espresso.onView(withContentDescription("سطح فعالیت روزانه")).perform(click());
            Espresso.onView(withText("فعالیت متوسط")).perform(click());
            Espresso.onView(withContentDescription("سطح فعالیت روزانه"))
                    .check(matches(withSpinnerText("فعالیت متوسط")));
        } finally {
            prefs.edit().putString("language", previous).commit();
        }
    }
}
