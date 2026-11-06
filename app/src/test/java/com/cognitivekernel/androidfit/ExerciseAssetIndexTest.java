package com.cognitivekernel.androidfit;

import android.app.Activity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.RobolectricTestRunner;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
public class ExerciseAssetIndexTest {
    @Test public void firstLookupAfterColdStartBuildsIndexSynchronously() {
        try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()) {
            assertEquals("exercises/Air Squat (female)_[394813].gif",
                ExerciseImages.findGifById(controller.get(),"394813","female"));
        }
    }

    @Test public void idLookupRejectsOppositeGenderAndAllowsUnisexAssets() {
        try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()) {
            Activity context=controller.get();
            assertNull(ExerciseImages.findGifById(context,"394813","male"));
            assertEquals("exercises/Air Squat (female)_[394813].gif",
                ExerciseImages.findGifById(context,"394813","female"));
            assertEquals("exercises/Push-up_[288113].gif",
                ExerciseImages.findGifById(context,"288113","female"));
        }
    }

    @Test public void nameLookupPrefersRequestedGenderAndNeverReturnsOppositeVariant() {
        try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()) {
            Activity context=controller.get();
            assertEquals("exercises/Air Squat (female)_[394813].gif",
                ExerciseImages.findGif(context,"Air Squat","female"));
            assertEquals("exercises/Air Squat (VERSION 2)_[471413].gif",
                ExerciseImages.findGif(context,"Air Squat","male"));
            assertNull(ExerciseImages.findGif(context,"Wall sit (narrow stance)","female"));
            assertEquals("exercises/Air Squat (female)_[394813].gif",
                ExerciseImages.findGif(context,"اسکوات","female"));
            assertEquals("exercises/Single Leg Squat (pistol)male_[175913].gif",
                ExerciseImages.findGifById(context,"175913","male"));
            assertNull(ExerciseImages.findGifById(context,"175913","female"));
        }
    }

    @Test public void automaticPlanLookupReturnsStableCatalogMetadata() {
        try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()) {
            ExerciseAssetIndex.ExerciseAsset squat=ExerciseAssetIndex.findBest(controller.get(),"Air Squat","female");
            assertNotNull(squat);
            assertEquals("394813",squat.id);
            assertEquals("female",squat.gender);
            assertTrue(squat.file.endsWith(".gif"));
        }
    }
}
