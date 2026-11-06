package com.cognitivekernel.androidfit;

import android.app.Activity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.util.ArrayList;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PlanListCacheTest {
    @Test public void plansSnapshotSurvivesDatabaseHelperRecreation() {
        try (ActivityController<Activity> controller =
                     Robolectric.buildActivity(Activity.class).setup()) {
            Activity context = controller.get();
            WorkoutDb firstDatabase = new WorkoutDb(context);
            long firstId = firstDatabase.addPlan("برنامه قدرتی");
            long secondId = firstDatabase.addPlan("برنامه هوازی");
            firstDatabase.plans();

            File[] cacheFiles = context.getFilesDir().listFiles(
                    (directory, name) -> name.startsWith("plans_exercises_v1_")
                            && name.endsWith(".json"));
            assertNotNull(cacheFiles);
            assertEquals(1, cacheFiles.length);
            assertTrue(cacheFiles[0].isFile() && cacheFiles[0].length() > 0);

            firstDatabase.close();
            WorkoutDb nextProcessDatabase = new WorkoutDb(context);
            ArrayList<String[]> restored = PlanListCache.read(context);
            assertNotNull("A new app process must restore the persisted plan list", restored);
            assertEquals(2, restored.size());
            assertEquals(String.valueOf(firstId), restored.get(0)[0]);
            assertEquals(String.valueOf(secondId), restored.get(1)[0]);
            nextProcessDatabase.close();
        }
    }

    @Test public void planMutationsInvalidateThePersistedSnapshot() {
        try (ActivityController<Activity> controller =
                     Robolectric.buildActivity(Activity.class).setup()) {
            Activity context = controller.get();
            WorkoutDb database = new WorkoutDb(context);
            long id = database.addPlan("برنامه اول");
            database.plans();
            assertNotNull(PlanListCache.read(context));

            database.addPlan("برنامه دوم");
            assertNull(PlanListCache.read(context));
            database.plans();
            assertNotNull(PlanListCache.read(context));

            assertTrue(database.updatePlan(id, "برنامه ویرایش‌شده"));
            assertNull(PlanListCache.read(context));
            database.plans();
            assertNotNull(PlanListCache.read(context));

            database.deletePlan(id);
            assertNull(PlanListCache.read(context));
            database.close();
        }
    }

    @Test public void emptyPlanListIsAlsoPersistedAsAValidSnapshot() {
        try (ActivityController<Activity> controller =
                     Robolectric.buildActivity(Activity.class).setup()) {
            WorkoutDb database = new WorkoutDb(controller.get());
            assertTrue(database.plans().isEmpty());
            ArrayList<String[]> restored = PlanListCache.read(controller.get());
            assertNotNull(restored);
            assertTrue(restored.isEmpty());
            database.close();
        }
    }
}
