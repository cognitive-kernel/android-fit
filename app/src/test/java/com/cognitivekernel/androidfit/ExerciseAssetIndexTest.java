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
    @Test public void authoritativeCatalogContainsEveryBundledGifAndExplicitGender() {
        try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()) {
            Activity context=controller.get();
            java.util.List<ExerciseAssetIndex.ExerciseAsset> catalog=ExerciseAssetIndex.all(context);
            assertEquals(6556,catalog.size());
            assertEquals("female",catalog.stream().filter(x->"394813".equals(x.id)).findFirst().get().gender);
            assertEquals("male",catalog.stream().filter(x->"175913".equals(x.id)).findFirst().get().gender);
            assertEquals("unspecified",catalog.stream().filter(x->"288113".equals(x.id)).findFirst().get().gender);
            assertNotNull(ExerciseImages.findGif(context,"Air Squat"));
            assertNull(ExerciseImages.findGifById(context,"175913","female"));
        }
    }


    @Test public void catalogIndexIsPersistedAndReadableAfterMemoryCacheReset() throws Exception {
        try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()) {
            Activity context=controller.get();
            assertEquals(6556,ExerciseAssetIndex.all(context).size());
            java.io.File[] cacheFiles=context.getFilesDir().listFiles(
                (directory,name)->name.startsWith("exercise_catalog_index_v1_")&&name.endsWith(".json"));
            assertNotNull(cacheFiles);
            assertEquals("Exactly one version-scoped catalog cache should be written",1,cacheFiles.length);
            assertTrue(cacheFiles[0].isFile()&&cacheFiles[0].length()>0);

            java.lang.reflect.Field memoryCache=ExerciseAssetIndex.class.getDeclaredField("cached");
            memoryCache.setAccessible(true);
            memoryCache.set(null,null);

            java.lang.reflect.Method readCache=ExerciseAssetIndex.class.getDeclaredMethod(
                "readDiskCache",android.content.Context.class);
            readCache.setAccessible(true);
            assertNotNull("The next app process must be able to restore the persisted index",
                readCache.invoke(null,context));
            assertEquals(6556,ExerciseAssetIndex.all(context).size());
        }
    }
}
