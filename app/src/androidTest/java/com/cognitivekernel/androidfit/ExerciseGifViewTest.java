package com.cognitivekernel.androidfit;

import android.content.Context;
import android.graphics.Movie;
import android.view.ViewGroup;
import java.io.InputStream;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
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
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ExerciseGifViewTest {
    // This file is present in app/src/main/assets/exercises and does not require network access.
    private static final String TEST_GIF = "squat.gif";
    private static final String CATALOG_GIF = "Air Squat (VERSION 2)_[471413].gif";

    @Test public void bundledGifOpensFullscreenAndCloseButtonReturnsToPreviousScreen() {
        ExerciseGifView.setAnimationPlaybackEnabledForTests(false);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                ExerciseGifView preview = new ExerciseGifView(activity, TEST_GIF);
                assertNotNull(preview);
                assertTrue("Bundled offline GIF must decode", preview.hasDecodedGifForTests());
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

    @Test public void catalogueExerciseAliasDecodesOfflineWithoutNetwork() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                ExerciseGifView preview = new ExerciseGifView(activity, CATALOG_GIF);
                assertTrue("A catalogue exercise must resolve to its bundled offline GIF", preview.hasDecodedGifForTests());
            });
        }
    }

    @Test public void realCatalogueGifDecodesFromAssets() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        try (InputStream stream = context.getAssets().open("exercises/1 2 Stick Drill (male)_[596513].gif")) {
            Movie movie = Movie.decodeStream(stream);
            assertNotNull("Real catalogue GIF must decode from its packaged path", movie);
            assertTrue("Catalogue GIF must have positive dimensions", movie.width() > 0 && movie.height() > 0);
        }
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                ExerciseGifView preview = new ExerciseGifView(activity, "1 2 Stick Drill (male)_[596513].gif");
                assertTrue("Non-core catalogue GIF should decode", preview.hasDecodedGifForTests());
            });
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
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            Espresso.onView(isAssignableFrom(ExerciseGifView.class))
                    .inRoot(RootMatchers.isDialog()).check(matches(isDisplayed())).perform(click());
            Espresso.onView(withContentDescription("Close enlarged GIF")).check(doesNotExist());
        } finally {
            ExerciseGifView.setAnimationPlaybackEnabledForTests(true);
        }
    }
}
