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
        public final int day;

        PlannedExercise(ExerciseAssetIndex.ExerciseAsset asset, int sets, int reps) {
            this(asset,sets,reps,1);
        }
        PlannedExercise(ExerciseAssetIndex.ExerciseAsset asset, int sets, int reps, int day) {
            this.asset = asset;
            this.sets = sets;
            this.reps = reps;
            this.day = day;
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
        return generateInternal(catalog,goal,level,equipment,gender,sessionMinutes,limitations,3,false);
    }

    public static List<PlannedExercise> generate(
            List<ExerciseAssetIndex.ExerciseAsset> catalog,
            int goal, int level, int equipment, String gender, int sessionMinutes, String limitations, int trainingDays) {
        return generateInternal(catalog,goal,level,equipment,gender,sessionMinutes,limitations,trainingDays,true);
    }

    private static List<PlannedExercise> generateInternal(
            List<ExerciseAssetIndex.ExerciseAsset> catalog,
            int goal, int level, int equipment, String gender, int sessionMinutes, String limitations,
            int trainingDays, boolean weeklyPlan) {
        if (catalog == null || catalog.isEmpty()) throw new IllegalArgumentException("Exercise catalog is empty");
        if (goal < 0 || goal > 3 || level < 0 || level > 2 || equipment < 0 || equipment > 2
                || sessionMinutes < 10 || sessionMinutes > 180 || trainingDays < 1 || trainingDays > 6
                || (!"male".equals(gender) && !"female".equals(gender) && !"all".equals(gender))) {
            throw new IllegalArgumentException("Invalid personal plan preferences");
        }

        int count = sessionMinutes <= 20 ? 3 : sessionMinutes <= 35 ? 4 : sessionMinutes <= 50 ? 6 : 8;
        ArrayList<ExerciseAssetIndex.ExerciseAsset> eligible = new ArrayList<>();
        for (ExerciseAssetIndex.ExerciseAsset asset : catalog) {
            if (asset == null || asset.name == null || asset.name.trim().isEmpty()) continue;
            if (!"all".equals(gender) && !gender.equals(asset.gender) && !"unspecified".equals(asset.gender)) continue;
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
        if (hasUnmappedLimitation(limitations)) {
            throw new IllegalStateException("Specify the affected body region and clinician-approved movement restrictions; this offline coach cannot safely map the reported limitation");
        }
        if (eligible.isEmpty()) throw new IllegalStateException(limitations==null||limitations.trim().isEmpty()? "No exercises match the selected gender and equipment" : "No exercises match the reported injury restrictions; request a clinician-reviewed exercise selection");

        ArrayList<PlannedExercise> result = new ArrayList<>();
        Set<String> usedIds = new HashSet<>();
        Set<String> usedNames = new HashSet<>();
        int daysToBuild=weeklyPlan?trainingDays:1;
        for(int day=1;day<=daysToBuild;day++) {
            String split=weeklyPlan?splitKey(trainingDays,day):"full";
            String[] priorities=prioritiesFor(goal,split,day);
            ArrayList<ExerciseAssetIndex.ExerciseAsset> selected=new ArrayList<>();
            for(String category:priorities) {
                if(selected.size()>=count)break;
                ExerciseAssetIndex.ExerciseAsset candidate=choose(eligible,category,usedIds,usedNames);
                if(candidate!=null)add(candidate,selected,usedIds,usedNames);
            }
            for(ExerciseAssetIndex.ExerciseAsset candidate:eligible) {
                if(selected.size()>=count)break;
                String category=category(candidate.name);
                if(matchesSplit(split,category))add(candidate,selected,usedIds,usedNames);
            }
            if(selected.size()<Math.min(3,count)) {
                throw new IllegalStateException("Not enough distinct catalog movements for training day "+day+" ("+split+")");
            }
            int sets=(trainingDays>=5||level==0)?2:3;
            int reps=goal==0?(level==0?8:6):goal==1?10:goal==2?12:15;
            for(ExerciseAssetIndex.ExerciseAsset asset:selected) {
                result.add(new PlannedExercise(asset,sets,reps,day));
            }
        }
        if(result.isEmpty())throw new IllegalStateException("No suitable exercises could be generated");
        return Collections.unmodifiableList(result);
    }

    private static String splitKey(int days,int day) {
        if(days<=3)return "full";
        if(days==5)return day==1?"push":day==2?"pull":day==3?"legs":day==4?"upper":"lower";
        return day%2==1?"upper":"lower";
    }

    private static String[] prioritiesFor(int goal,String split,int day) {
        if("push".equals(split))return new String[]{"push","other"};
        if("pull".equals(split))return new String[]{"pull","other"};
        if("legs".equals(split))return new String[]{"legs","glutes","core","other"};
        if("upper".equals(split))return new String[]{"pull","push","other"};
        if("lower".equals(split))return new String[]{"legs","glutes","core","other"};
        String[] base=goal==0?new String[]{"legs","push","pull","glutes","core","other"}
                :goal==1?new String[]{"legs","push","pull","glutes","core","other"}
                :goal==3?new String[]{"cardio","legs","push","pull","core","glutes","other"}
                :new String[]{"legs","push","pull","core","cardio","glutes","other"};
        int rotate=(day-1)%base.length;
        String[] ordered=new String[base.length];
        for(int i=0;i<base.length;i++)ordered[i]=base[(i+rotate)%base.length];
        return ordered;
    }

    private static boolean matchesSplit(String split,String category) {
        if("full".equals(split))return true;
        if("upper".equals(split))return "push".equals(category)||"pull".equals(category)||"other".equals(category);
        if("lower".equals(split)||"legs".equals(split))return "legs".equals(category)||"glutes".equals(category)||"core".equals(category)||"other".equals(category);
        if("push".equals(split))return "push".equals(category)||"other".equals(category);
        if("pull".equals(split))return "pull".equals(category)||"other".equals(category);
        return false;
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
        String l=(limitations==null?"":limitations).toLowerCase(Locale.ROOT).trim();
        if(isNoLimitation(l)) return true;
        boolean knee=contains(l,"knee","زانو");
        boolean shoulder=contains(l,"shoulder","شانه","سرشانه","روتاتور کاف");
        boolean back=contains(l,"back","spine","lumbar","کمر","کمردرد","کمر درد","پشت","ستون فقرات","دیسک");
        boolean wrist=contains(l,"wrist","مچ دست");
        boolean ankle=contains(l,"ankle","مچ پا");
        boolean hip=contains(l,"hip","لگن","مفصل ران");
        boolean elbow=contains(l,"elbow","آرنج");
        boolean neck=contains(l,"neck","گردن");
        if(knee&&contains(n,"squat","lunge","leg press","leg extension","step up","step-up","jump","running","treadmill")) return false;
        if(shoulder&&contains(n,"overhead press","shoulder press","lateral raise","front raise","upright row","dip","chest fly")) return false;
        if(back&&contains(n,"deadlift","good morning","bent over","hyperextension","back extension","sit up","sit-up","barbell row")) return false;
        if(wrist&&contains(n,"push up","push-up","pushup","plank","burpee","handstand","front rack")) return false;
        if(ankle&&contains(n,"jump","running","treadmill","lunge","step up","step-up")) return false;
        if(hip&&contains(n,"squat","lunge","deadlift","hip thrust","leg press","adductor","abductor")) return false;
        if(elbow&&contains(n,"curl","triceps","dip","push up","push-up","pull up","pull-up","row")) return false;
        if(neck&&contains(n,"shrug","overhead press","shoulder press","handstand","loaded carry")) return false;
        return true;
    }

    private static boolean isNoLimitation(String value) {
        return value==null||value.trim().isEmpty()||value.trim().equals("none")
                ||value.trim().equals("no")||value.trim().equals("no injury")
                ||value.trim().equals("ندارد")||value.trim().equals("خیر")
                ||value.trim().equals("هیچ")||value.trim().equals("هیچکدام")
                ||value.trim().equals("هیچ‌کدام");
    }

    private static boolean hasUnmappedLimitation(String value) {
        if(isNoLimitation(value))return false;
        String l=value.toLowerCase(Locale.ROOT);
        return !contains(l,"knee","زانو","shoulder","شانه","سرشانه","روتاتور کاف",
                "back","spine","lumbar","کمر","کمردرد","کمر درد","پشت","ستون فقرات","دیسک",
                "wrist","مچ دست","ankle","مچ پا","hip","لگن","مفصل ران",
                "elbow","آرنج","neck","گردن");
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
