package com.cognitivekernel.androidfit;

import android.content.Context;
import android.content.SharedPreferences;

/** Persists the user's planning profile separately from workout records. */
public final class FitnessProfileStore {
    private static final String PREFS = "fityar_preferences";
    private FitnessProfileStore() {}

    public static boolean hasProfile(Context context) {
        SharedPreferences p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        return p.contains("coach_age") && p.contains("coach_height_bits") && p.contains("coach_weight_bits");
    }

    public static FitnessPlanEngine.Profile load(Context context) {
        SharedPreferences p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        FitnessPlanEngine.Profile x=new FitnessPlanEngine.Profile();
        String language=context.getSharedPreferences("fit_settings",Context.MODE_PRIVATE).getString(
                "language",context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString("language","fa"));
        x.english="en".equalsIgnoreCase(language);
        x.medicalClearance=p.getBoolean("coach_medical_clearance",false);
        x.age=p.getInt("coach_age",30);
        x.heightCm=Double.longBitsToDouble(p.getLong("coach_height_bits",Double.doubleToLongBits(170)));
        x.weightKg=Double.longBitsToDouble(p.getLong("coach_weight_bits",Double.doubleToLongBits(70)));
        x.sexIndex=p.getInt("coach_sex",0);
        x.goalIndex=p.getInt("coach_goal",1);
        x.activityIndex=p.getInt("coach_activity",1);
        x.levelIndex=p.getInt("fitness_level",0);
        x.equipmentIndex=p.getInt("coach_equipment",1);
        x.trainingDays=p.getInt("weekly_goal",3);
        x.sessionMinutes=p.getInt("session_minutes",45);
        x.mealsPerDay=p.getInt("coach_meals",4);
        x.sleepHours=Double.longBitsToDouble(p.getLong("coach_sleep_bits",Double.doubleToLongBits(0)));
        x.conditions=p.getString("coach_conditions","");
        x.injuries=p.getString("coach_injuries","");
        x.medications=p.getString("coach_medications","");
        x.allergies=p.getString("coach_allergies","");
        x.dietaryPreference=p.getString("coach_diet","omnivore");
        x.dislikedFoods=p.getString("coach_dislikes","");
        x.notes=p.getString("coach_notes","");
        return x;
    }

    public static void save(Context context,FitnessPlanEngine.Profile x) {
        FitnessPlanEngine.validate(x);
        SharedPreferences.Editor e=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit();
        e.putBoolean("coach_medical_clearance",x.medicalClearance)
         .putInt("coach_age",x.age)
         .putLong("coach_height_bits",Double.doubleToLongBits(x.heightCm))
         .putLong("coach_weight_bits",Double.doubleToLongBits(x.weightKg))
         .putInt("coach_sex",x.sexIndex).putInt("coach_goal",x.goalIndex)
         .putInt("coach_activity",x.activityIndex).putInt("fitness_level",x.levelIndex)
         .putInt("coach_equipment",x.equipmentIndex).putInt("weekly_goal",x.trainingDays)
         .putInt("session_minutes",x.sessionMinutes).putInt("coach_meals",x.mealsPerDay)
         .putLong("coach_sleep_bits",Double.doubleToLongBits(x.sleepHours))
         .putString("coach_conditions",safe(x.conditions)).putString("coach_injuries",safe(x.injuries))
         .putString("coach_medications",safe(x.medications)).putString("coach_allergies",safe(x.allergies))
         .putString("coach_diet",safe(x.dietaryPreference)).putString("coach_dislikes",safe(x.dislikedFoods))
         .putString("coach_notes",safe(x.notes)).apply();
    }
    private static String safe(String value){return value==null?"":value.trim();}
}
