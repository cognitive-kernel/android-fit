package com.cognitivekernel.androidfit;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class WorkoutDb extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "fit_yar.db";
    private static final int DATABASE_VERSION = 4;
    public WorkoutDb(Context context) { super(context, DATABASE_NAME, null, DATABASE_VERSION); }
    @Override public void onCreate(SQLiteDatabase db) { ensureSchema(db); }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { ensureSchema(db); }

    // Idempotent, additive migration supports old schema versions from every known branch.
    private void ensureSchema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS plans(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS exercises(id INTEGER PRIMARY KEY AUTOINCREMENT,plan_id INTEGER NOT NULL,name TEXT NOT NULL,sets INTEGER NOT NULL,reps INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS logs(id INTEGER PRIMARY KEY AUTOINCREMENT,date_jalali TEXT,plan_name TEXT,exercise_name TEXT,sets INTEGER,reps INTEGER,weight REAL,created_at INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS sleep_logs(id INTEGER PRIMARY KEY AUTOINCREMENT,start_at INTEGER NOT NULL,end_at INTEGER NOT NULL,duration_minutes INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS vital_logs(id INTEGER PRIMARY KEY AUTOINCREMENT,measured_at INTEGER NOT NULL,systolic INTEGER NOT NULL,diastolic INTEGER NOT NULL,pulse INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS body_metrics(id INTEGER PRIMARY KEY AUTOINCREMENT,measured_at INTEGER NOT NULL,weight_kg REAL,waist_cm REAL)");
        ensureColumn(db,"logs","rest_seconds","INTEGER NOT NULL DEFAULT 60");
        ensureColumn(db,"logs","effort","INTEGER NOT NULL DEFAULT 7");
        ensureColumn(db,"logs","notes","TEXT NOT NULL DEFAULT ''");
    }
    private void ensureColumn(SQLiteDatabase db,String table,String column,String definition) {
        boolean found=false;
        try(Cursor c=db.rawQuery("PRAGMA table_info("+table+")",null)) { while(c.moveToNext()) if(column.equalsIgnoreCase(c.getString(1))) { found=true; break; } }
        if(!found) db.execSQL("ALTER TABLE "+table+" ADD COLUMN "+column+" "+definition);
    }
    public long addPlan(String name) {
        if(name==null||name.trim().isEmpty()) throw new IllegalArgumentException("Plan name is required");
        ContentValues v=new ContentValues(); v.put("name",name.trim()); return getWritableDatabase().insertOrThrow("plans",null,v);
    }
    public boolean updatePlan(long id, String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Plan name is required");
        }
        ContentValues values = new ContentValues();
        values.put("name", name.trim());
        return getWritableDatabase().update("plans", values, "id=?", new String[]{String.valueOf(id)}) > 0;
    }

    public long addExercise(long planId,String name,int sets,int reps) {
        if(name==null||name.trim().isEmpty()||sets<1||reps<1) throw new IllegalArgumentException("Invalid exercise");
        ContentValues v=new ContentValues(); v.put("plan_id",planId); v.put("name",name.trim()); v.put("sets",sets); v.put("reps",reps); return getWritableDatabase().insertOrThrow("exercises",null,v);
    }
    public boolean updateExercise(long id,String name,int sets,int reps) {
        if(name==null||name.trim().isEmpty()||sets<1||reps<1) throw new IllegalArgumentException("Invalid exercise");
        ContentValues v=new ContentValues(); v.put("name",name.trim()); v.put("sets",sets); v.put("reps",reps); return getWritableDatabase().update("exercises",v,"id=?",new String[]{String.valueOf(id)})>0;
    }
    public void deleteExercise(long id) { getWritableDatabase().delete("exercises","id=?",new String[]{String.valueOf(id)}); }
    public void deletePlan(long id) {
        SQLiteDatabase db=getWritableDatabase(); db.beginTransaction();
        try { db.delete("exercises","plan_id=?",new String[]{String.valueOf(id)}); db.delete("plans","id=?",new String[]{String.valueOf(id)}); db.setTransactionSuccessful(); }
        finally { db.endTransaction(); }
    }
    public List<String[]> plans() {
        List<String[]> rows=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id,name FROM plans ORDER BY name",null)) { while(c.moveToNext()) rows.add(new String[]{c.getString(0),c.getString(1)}); }
        return rows;
    }
    public List<String[]> exercises(long planId) {
        List<String[]> rows=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id,name,sets,reps FROM exercises WHERE plan_id=? ORDER BY id",new String[]{String.valueOf(planId)})) { while(c.moveToNext()) rows.add(new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3)}); }
        return rows;
    }
    public void log(String date,String plan,String exercise,int sets,int reps,double kg) { logDetailed(date,plan,exercise,sets,reps,kg,60,7,""); }
    public void logDetailed(String date,String plan,String exercise,int sets,int reps,double kg,int restSeconds,int effort,String notes) {
        if(date==null||date.trim().isEmpty()||plan==null||plan.trim().isEmpty()||exercise==null||exercise.trim().isEmpty()||sets<1||reps<1||Double.isNaN(kg)||Double.isInfinite(kg)||kg<0||restSeconds<0||restSeconds>3600||effort<1||effort>10) throw new IllegalArgumentException("Invalid workout values");
        ContentValues v=new ContentValues(); v.put("date_jalali",date); v.put("plan_name",plan); v.put("exercise_name",exercise); v.put("sets",sets); v.put("reps",reps); v.put("weight",kg); v.put("created_at",System.currentTimeMillis()); v.put("rest_seconds",restSeconds); v.put("effort",effort); v.put("notes",notes==null?"":notes.trim()); getWritableDatabase().insertOrThrow("logs",null,v);
    }
    public Cursor allLogs() { return getReadableDatabase().rawQuery("SELECT date_jalali,plan_name,exercise_name,sets,reps,weight,rest_seconds,effort,notes FROM logs ORDER BY created_at DESC",null); }
    public Cursor reportLogs() { return getReadableDatabase().rawQuery("SELECT date_jalali,plan_name,exercise_name,sets,reps,weight,created_at FROM logs ORDER BY created_at ASC",null); }
    public Cursor summary() { return getReadableDatabase().rawQuery("SELECT COUNT(*),COUNT(DISTINCT date_jalali),COALESCE(SUM(sets*reps*weight),0),COALESCE(MAX(weight),0) FROM logs",null); }
    public Cursor recentLogs() { return getReadableDatabase().rawQuery("SELECT date_jalali,exercise_name,sets,reps,weight,effort,notes FROM logs ORDER BY created_at DESC LIMIT 30",null); }
    public int weeklyTrainingDays() {
        long since=System.currentTimeMillis()-7L*24*60*60*1000;
        try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(DISTINCT date_jalali) FROM logs WHERE created_at>=?",new String[]{String.valueOf(since)})) { return c.moveToFirst()?c.getInt(0):0; }
    }
    public Cursor volumeByDate() { return getReadableDatabase().rawQuery("SELECT date_jalali,COALESCE(SUM(sets*reps*weight),0) FROM logs GROUP BY date_jalali ORDER BY MIN(created_at) DESC LIMIT 7",null); }
    public long addSleep(long start,long end) {
        if(end<=start) throw new IllegalArgumentException("End must be after start");
        ContentValues v=new ContentValues(); v.put("start_at",start); v.put("end_at",end); v.put("duration_minutes",(end-start)/60000L); return getWritableDatabase().insertOrThrow("sleep_logs",null,v);
    }
    public Cursor sleepLogs() { return getReadableDatabase().rawQuery("SELECT start_at,end_at,duration_minutes FROM sleep_logs ORDER BY start_at ASC",null); }
    public long addVitals(long at,int systolic,int diastolic,int pulse) {
        if(systolic<50||systolic>300||diastolic<30||diastolic>200||diastolic>=systolic||pulse<20||pulse>250) throw new IllegalArgumentException("Invalid vital values");
        ContentValues v=new ContentValues(); v.put("measured_at",at); v.put("systolic",systolic); v.put("diastolic",diastolic); v.put("pulse",pulse); return getWritableDatabase().insertOrThrow("vital_logs",null,v);
    }
    public Cursor vitalLogs() { return getReadableDatabase().rawQuery("SELECT measured_at,systolic,diastolic,pulse FROM vital_logs ORDER BY measured_at ASC",null); }
    public long addBodyMetrics(long at,Double weightKg,Double waistCm) {
        if(weightKg==null&&waistCm==null) throw new IllegalArgumentException("Enter weight or waist");
        if(weightKg!=null&&(weightKg<=0||Double.isNaN(weightKg)||Double.isInfinite(weightKg))) throw new IllegalArgumentException("Invalid weight");
        if(waistCm!=null&&(waistCm<=0||Double.isNaN(waistCm)||Double.isInfinite(waistCm))) throw new IllegalArgumentException("Invalid waist");
        ContentValues v=new ContentValues(); v.put("measured_at",at); if(weightKg!=null)v.put("weight_kg",weightKg); if(waistCm!=null)v.put("waist_cm",waistCm); return getWritableDatabase().insertOrThrow("body_metrics",null,v);
    }
    public Cursor bodyMetrics() { return getReadableDatabase().rawQuery("SELECT measured_at,weight_kg,waist_cm FROM body_metrics ORDER BY measured_at ASC",null); }
}
