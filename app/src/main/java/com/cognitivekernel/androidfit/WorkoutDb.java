package com.cognitivekernel.androidfit;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class WorkoutDb extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "fit_yar.db";
    private static final int DATABASE_VERSION = 2;

    public WorkoutDb(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE plans(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL)");
        db.execSQL("CREATE TABLE exercises(id INTEGER PRIMARY KEY AUTOINCREMENT,plan_id INTEGER NOT NULL,name TEXT NOT NULL,sets INTEGER NOT NULL,reps INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE logs(id INTEGER PRIMARY KEY AUTOINCREMENT,date_jalali TEXT,plan_name TEXT,exercise_name TEXT,sets INTEGER,reps INTEGER,weight REAL,created_at INTEGER)");
        createIndexes(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createIndexes(db);
        }
    }

    private static void createIndexes(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_exercises_plan_id ON exercises(plan_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_logs_created_at ON logs(created_at DESC)");
    }

    public long addPlan(String name) {
        String validName = requireName(name, "Plan name is required");
        ContentValues values = new ContentValues();
        values.put("name", validName);
        return getWritableDatabase().insertOrThrow("plans", null, values);
    }

    public boolean updatePlan(long id, String name) {
        String validName = requireName(name, "Plan name is required");
        ContentValues values = new ContentValues();
        values.put("name", validName);
        return getWritableDatabase().update("plans", values, "id=?", new String[]{Long.toString(id)}) > 0;
    }

    public long addExercise(long planId, String name, int sets, int reps) {
        String validName = requireName(name, "Exercise name is required");
        requireSetsAndReps(sets, reps);
        ContentValues values = new ContentValues();
        values.put("plan_id", planId);
        values.put("name", validName);
        values.put("sets", sets);
        values.put("reps", reps);
        return getWritableDatabase().insertOrThrow("exercises", null, values);
    }

    public boolean updateExercise(long id, String name, int sets, int reps) {
        String validName = requireName(name, "Exercise name is required");
        requireSetsAndReps(sets, reps);
        ContentValues values = new ContentValues();
        values.put("name", validName);
        values.put("sets", sets);
        values.put("reps", reps);
        return getWritableDatabase().update("exercises", values, "id=?", new String[]{Long.toString(id)}) > 0;
    }

    public void deleteExercise(long id) {
        getWritableDatabase().delete("exercises", "id=?", new String[]{Long.toString(id)});
    }

    public void deletePlan(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("exercises", "plan_id=?", new String[]{Long.toString(id)});
            db.delete("plans", "id=?", new String[]{Long.toString(id)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<String[]> plans() {
        List<String[]> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT id,name FROM plans ORDER BY name COLLATE NOCASE", null)) {
            while (cursor.moveToNext()) {
                result.add(new String[]{cursor.getString(0), cursor.getString(1)});
            }
        }
        return result;
    }

    public List<String[]> exercises(long planId) {
        List<String[]> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id,name,sets,reps FROM exercises WHERE plan_id=? ORDER BY id",
                new String[]{Long.toString(planId)})) {
            while (cursor.moveToNext()) {
                result.add(new String[]{cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3)});
            }
        }
        return result;
    }

    public void log(String date, String plan, String exercise, int sets, int reps, double weightKg) {
        String validDate = requireName(date, "Workout date is required");
        String validPlan = requireName(plan, "Workout plan is required");
        String validExercise = requireName(exercise, "Workout exercise is required");
        requireSetsAndReps(sets, reps);
        if (Double.isNaN(weightKg) || Double.isInfinite(weightKg) || weightKg < 0) {
            throw new IllegalArgumentException("Weight must be a finite, non-negative number");
        }

        ContentValues values = new ContentValues();
        values.put("date_jalali", validDate);
        values.put("plan_name", validPlan);
        values.put("exercise_name", validExercise);
        values.put("sets", sets);
        values.put("reps", reps);
        values.put("weight", weightKg);
        values.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insertOrThrow("logs", null, values);
    }

    public Cursor allLogs() {
        return getReadableDatabase().rawQuery(
                "SELECT date_jalali,plan_name,exercise_name,sets,reps,weight FROM logs ORDER BY created_at DESC,id DESC",
                null);
    }

    private static String requireName(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static void requireSetsAndReps(int sets, int reps) {
        if (sets < 1 || reps < 1) {
            throw new IllegalArgumentException("Sets and reps must be positive integers");
        }
    }
}
