package com.cognitivekernel.androidfit;

import android.view.ViewGroup;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class ExerciseGifViewTest {
    @Test public void bundledGifOpensFullscreenAndCloseButtonReturnsToPreviousScreen() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                ExerciseGifView preview = new ExerciseGifView(activity, "squat.gif");
                assertNotNull(preview);
                activity.addContentView(preview, new ViewGroup.LayoutParams(240, 180));
                preview.performClick();
            });
            Espresso.onView(withText("×")).inRoot(RootMatchers.isDialog())
                    .check(matches(isDisplayed())).perform(click());
            Espresso.onView(withText("×")).check(doesNotExist());
        }
    }

    @Test public void tappingFullscreenGifClosesIt() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                ExerciseGifView preview = new ExerciseGifView(activity, "squat.gif");
                activity.addContentView(preview, new ViewGroup.LayoutParams(240, 180));
                preview.performClick();
            });
            Espresso.onView(isAssignableFrom(ExerciseGifView.class))
                    .inRoot(RootMatchers.isDialog()).check(matches(isDisplayed())).perform(click());
            Espresso.onView(withText("×")).check(doesNotExist());
        }
    }
}
