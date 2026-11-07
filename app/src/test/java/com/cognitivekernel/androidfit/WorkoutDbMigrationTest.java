package com.cognitivekernel.androidfit;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.RobolectricTestRunner;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
public class WorkoutDbMigrationTest {
    @Test public void versionFiveUpgradeAddsTrainingDayWithoutDeletingSavedRows() {
        try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity=controller.get();
            activity.deleteDatabase(WorkoutDb.DATABASE_NAME);
            SQLiteDatabase legacy=activity.openOrCreateDatabase(WorkoutDb.DATABASE_NAME,0,null);
            legacy.execSQL("CREATE TABLE plans(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL)");
            legacy.execSQL("CREATE TABLE exercises(id INTEGER PRIMARY KEY AUTOINCREMENT,plan_id INTEGER NOT NULL,name TEXT NOT NULL,sets INTEGER NOT NULL,reps INTEGER NOT NULL,exercise_catalog_id TEXT,exercise_gender TEXT)");
            legacy.execSQL("INSERT INTO plans(id,name) VALUES(41,'Existing user plan')");
            legacy.execSQL("INSERT INTO exercises(id,plan_id,name,sets,reps,exercise_catalog_id,exercise_gender) VALUES(91,41,'Existing squat',3,10,'catalog-squat','male')");
            legacy.setVersion(5);
            legacy.close();

            WorkoutDb upgraded=new WorkoutDb(activity);
            try {
                List<String[]> rows=upgraded.exercises(41);
                assertEquals(1,rows.size());
                assertEquals("Existing squat",rows.get(0)[1]);
                assertEquals("catalog-squat",rows.get(0)[4]);
                assertEquals("male",rows.get(0)[5]);
                assertEquals("1",rows.get(0)[6]);
                assertEquals("Existing user plan",upgraded.plans().get(0)[1]);
            } finally {
                upgraded.close();
                activity.deleteDatabase(WorkoutDb.DATABASE_NAME);
            }
        }
    }
}
