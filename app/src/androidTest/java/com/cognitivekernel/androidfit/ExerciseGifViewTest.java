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
    // This file is present in app/src/main/assets/exercises and does not require network access.
    private static final String TEST_GIF = "Air Squat (female)_[394813].gif";

    @Test public void bundledGifOpensFullscreenAndCloseButtonReturnsToPreviousScreen() {
        ExerciseGifView.setAnimationPlaybackEnabledForTests(false);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                ExerciseGifView preview = new ExerciseGifView(activity, TEST_GIF);
                assertNotNull(preview);
                activity.addContentView(preview, new ViewGroup.LayoutParams(240, 180));
                preview.performClick();
            });
            Espresso.onView(withText("×")).inRoot(RootMatchers.isDialog())
                    .check(matches(isDisplayed())).perform(click());
            Espresso.onView(withText("×")).check(doesNotExist());
        } finally {
            ExerciseGifView.setAnimationPlaybackEnabledForTests(true);
        }
    }

    @Test public void tappingFullscreenGifClosesIt() {
        ExerciseGifView.setAnimationPlaybackEnabledForTests(false);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                ExerciseGifView preview = new ExerciseGifView(activity, TEST_GIF);
                activity.addContentView(preview, new ViewGroup.LayoutParams(240, 180));
                preview.performClick();
            });
            Espresso.onView(isAssignableFrom(ExerciseGifView.class))
                    .inRoot(RootMatchers.isDialog()).check(matches(isDisplayed())).perform(click());
            Espresso.onView(withContentDescription("Close enlarged GIF")).check(doesNotExist());
        } finally {
            ExerciseGifView.setAnimationPlaybackEnabledForTests(true);
        }
    }
}
