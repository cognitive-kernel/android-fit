package com.cognitivekernel.androidfit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Deterministic, catalog-backed workout generation for the personal plan builder. */
public final class PersonalPlanGenerator {
    public static final class PlannedExercise {
        public final ExerciseAssetIndex.ExerciseAsset asset;
        public final int sets;
        public final int reps;

        PlannedExercise(ExerciseAssetIndex.ExerciseAsset asset, int sets, int reps) {
            this.asset = asset;
            this.sets = sets;
            this.reps = reps;
        }
    }

    private PersonalPlanGenerator() {}

    public static List<PlannedExercise> generate(
            List<ExerciseAssetIndex.ExerciseAsset> catalog,
            int goal, int level, int equipment, String gender, int sessionMinutes) {
        return generate(catalog,goal,level,equipment,gender,sessionMinutes,"");
    }

    public static List<PlannedExercise> generate(
            List<ExerciseAssetIndex.ExerciseAsset> catalog,
            int goal, int level, int equipment, String gender, int sessionMinutes, String limitations) {
        return generate(catalog,goal,level,equipment,gender,sessionMinutes,limitations,3);
    }

    public static List<PlannedExercise> generate(
            List<ExerciseAssetIndex.ExerciseAsset> catalog,
            int goal, int level, int equipment, String gender, int sessionMinutes, String limitations, int trainingDays) {
        if (catalog == null || catalog.isEmpty()) throw new IllegalArgumentException("Exercise catalog is empty");
        if (goal < 0 || goal > 3 || level < 0 || level > 2 || equipment < 0 || equipment > 2
                || sessionMinutes < 10 || sessionMinutes > 180
                || (!"male".equals(gender) && !"female".equals(gender))) {
            throw new IllegalArgumentException("Invalid personal plan preferences");
        }

        int count = sessionMinutes <= 20 ? 3 : sessionMinutes <= 35 ? 4 : sessionMinutes <= 50 ? 6 : 8;
        ArrayList<ExerciseAssetIndex.ExerciseAsset> eligible = new ArrayList<>();
        for (ExerciseAssetIndex.ExerciseAsset asset : catalog) {
            if (asset == null || asset.name == null || asset.name.trim().isEmpty()) continue;
            if (!gender.equals(asset.gender) && !"unspecified".equals(asset.gender)) continue;
            if (!supportsEquipment(asset.name, equipment)) continue;
            if (!isCompatibleWithLimitations(asset.name, limitations)) continue;
            if (isStretchOnly(asset.name)) continue;
            eligible.add(asset);
        }
        Collections.sort(eligible, (left, right) -> {
            int leftGenderRank = gender.equals(left.gender) ? 0 : 1;
            int rightGenderRank = gender.equals(right.gender) ? 0 : 1;
            if (leftGenderRank != rightGenderRank) return Integer.compare(leftGenderRank, rightGenderRank);
            return left.id.compareTo(right.id);
        });
        if (eligible.isEmpty()) throw new IllegalStateException(limitations==null||limitations.trim().isEmpty()? "No exercises match the selected gender and equipment" : "No exercises match the reported injury restrictions; request a clinician-reviewed exercise selection");

        String[] priorities;
        if (goal == 0) priorities = new String[]{"legs", "push", "pull", "glutes", "core", "legs", "push", "pull"};
        else if (goal == 1) priorities = new String[]{"legs", "push", "pull", "glutes", "core", "push", "pull", "legs"};
        else if (goal == 3) priorities = new String[]{"cardio", "legs", "push", "pull", "core", "cardio", "glutes", "core"};
        else priorities = new String[]{"legs", "push", "pull", "core", "cardio", "glutes", "push", "legs"};

        ArrayList<ExerciseAssetIndex.ExerciseAsset> selected = new ArrayList<>();
        Set<String> usedIds = new HashSet<>();
        Set<String> usedNames = new HashSet<>();
        for (String category : priorities) {
            if (selected.size() >= count) break;
            ExerciseAssetIndex.ExerciseAsset candidate = choose(eligible, category, usedIds, usedNames);
            if (candidate != null) add(candidate, selected, usedIds, usedNames);
        }
        // Category classification is only a prioritization hint, never an eligibility rule.
        // Fill every remaining slot from the already gender- and equipment-filtered catalog.
        for (ExerciseAssetIndex.ExerciseAsset candidate : eligible) {
            if (selected.size() >= count) break;
            add(candidate, selected, usedIds, usedNames);
        }
        if (selected.isEmpty()) throw new IllegalStateException("No suitable exercises could be generated");

        int sets = (goal == 2 || goal == 3 ? 2 : 3) + level;\n        if(trainingDays>=4) sets=Math.min(sets,2);
        int reps = goal == 0 ? (level == 0 ? 8 : 6)
                : goal == 1 ? 10 : goal == 2 ? 12 : 15;
        ArrayList<PlannedExercise> result = new ArrayList<>();
        for (ExerciseAssetIndex.ExerciseAsset asset : selected) {
            result.add(new PlannedExercise(asset, sets, reps));
        }
        return Collections.unmodifiableList(result);
    }

    private static ExerciseAssetIndex.ExerciseAsset choose(
            List<ExerciseAssetIndex.ExerciseAsset> catalog, String category,
            Set<String> usedIds, Set<String> usedNames) {
        for (ExerciseAssetIndex.ExerciseAsset asset : catalog) {
            if (!usedIds.contains(asset.id) && !usedNames.contains(normalize(asset.name))
                    && category.equals(category(asset.name))) return asset;
        }
        return null;
    }

    private static void add(ExerciseAssetIndex.ExerciseAsset asset,
            List<ExerciseAssetIndex.ExerciseAsset> selected, Set<String> usedIds, Set<String> usedNames) {
        String name = normalize(asset.name);
        if (usedIds.add(asset.id) && usedNames.add(name)) selected.add(asset);
    }

    static String category(String name) {
        String n = normalize(name);
        if (contains(n, "running", "run ", "jog", "cycling", "treadmill", "jump rope", "jumping jack",
                "burpee", "mountain climber", "high knees", "skipping")) return "cardio";
        if (contains(n, "plank", "crunch", "sit up", "sit-up", "abdominal", "dead bug",
                "leg raise", "russian twist", "hollow body", "oblique")) return "core";
        if (contains(n, "hip thrust", "glute", "bridge", "kickback", "abductor", "adductor")) return "glutes";
        if (contains(n, "squat", "lunge", "leg press", "leg extension", "leg curl", "hamstring",
                "quadriceps", "calf raise", "step up", "step-up")) return "legs";
        if (contains(n, "row", "pull up", "pull-up", "pullup", "chin up", "chin-up",
                "pulldown", "pull down", "face pull", "rear delt", "biceps", "curl")) return "pull";
        if (contains(n, "push up", "push-up", "pushup", "bench press", "chest press",
                "chest fly", "chest fly", "overhead press", "shoulder press", "lateral raise",
                "triceps", "dip")) return "push";
        return "other";
    }

    static boolean isCompatibleWithLimitations(String exerciseName,String limitations) {
        String n=normalize(exerciseName==null?"":exerciseName);
        String l=(limitations==null?"":limitations).toLowerCase(Locale.ROOT);
        if(l.trim().isEmpty()||l.contains("none")||l.contains("ندارد")||l.contains("no injury")) return true;
        boolean knee=contains(l,"knee","زانو");
        boolean shoulder=contains(l,"shoulder","شانه","سرشانه","روتاتور کاف");
        boolean back=contains(l,"back pain","lower back","spine","کمر","ستون فقرات","دیسک");
        boolean wrist=contains(l,"wrist","مچ دست");
        boolean ankle=contains(l,"ankle","مچ پا");
        if(knee&&contains(n,"squat","lunge","leg press","leg extension","step up","step-up","jump","running","treadmill")) return false;
        if(shoulder&&contains(n,"overhead press","shoulder press","lateral raise","front raise","upright row","dip","chest fly")) return false;
        if(back&&contains(n,"deadlift","good morning","bent over","hyperextension","back extension","sit up","sit-up")) return false;
        if(wrist&&contains(n,"push up","push-up","pushup","plank","burpee","handstand")) return false;
        if(ankle&&contains(n,"jump","running","treadmill","lunge","step up","step-up")) return false;
        return true;
    }

    private static boolean supportsEquipment(String name, int equipment) {
        String n = normalize(name);
        boolean hasMachine = contains(n, "machine", "cable", "pulley", "smith", "leverage",
                "leg press machine", "lat pulldown machine", "selectorized", "sled", "trx", "suspension");
        boolean hasFreeWeight = contains(n, "barbell", "dumbbell", "kettlebell", "ez bar",
                "weight plate", "weighted", "medicine ball", "battle rope", "resistance band", "theraband");
        if (equipment == 0) return !hasMachine && !hasFreeWeight;
        if (equipment == 1) return !hasMachine && !contains(n, "barbell", "smith machine");
        return true;
    }

    private static boolean isStretchOnly(String name) {
        return contains(normalize(name), "stretch", "mobility", "yoga", "pose", "flexibility");
    }


    private static boolean contains(String value, String... terms) {
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ").trim();
    }
}
