package com.cognitivekernel.androidfit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Deterministic, fully offline coaching rules. Educational guidance only; it is
 * not a diagnostic or rehabilitation tool.
 */
public final class OfflineCoachEngine {
    private OfflineCoachEngine() {}

    public enum Goal { STRENGTH, MUSCLE, FITNESS, WEIGHT }
    public enum Level { BEGINNER, INTERMEDIATE, ADVANCED }
    public enum Equipment { NONE, HOME, GYM }

    public static final class Input {
        public Goal goal = Goal.FITNESS;
        public Level level = Level.BEGINNER;
        public Equipment equipment = Equipment.NONE;
        public int daysPerWeek = 3;
        public int sessionMinutes = 35;
        public int age = 30;
        public int recentRpe = 7;
        public int sessionsCompletedThisWeek;
        public boolean acuteInjury;
        public boolean chestPain;
        public boolean faintnessOrSevereBreathlessness;
        public boolean newNumbnessOrWeakness;
    }

    public static final class Session {
        public final int day;
        public final String focus;
        public final List<String> exercises;
        public final int sets, repsMin, repsMax, restSeconds, targetRpe;

        Session(int day, String focus, List<String> exercises, int sets,
                int repsMin, int repsMax, int restSeconds, int targetRpe) {
            this.day=day; this.focus=focus;
            this.exercises=Collections.unmodifiableList(exercises);
            this.sets=sets; this.repsMin=repsMin; this.repsMax=repsMax;
            this.restSeconds=restSeconds; this.targetRpe=targetRpe;
        }
    }

    public static final class Plan {
        public final boolean needsClinicalReview;
        public final String safetyMessage;
        public final List<Session> sessions;
        public final String progression;
        Plan(boolean stop, String safety, List<Session> sessions, String progression) {
            this.needsClinicalReview=stop; this.safetyMessage=safety;
            this.sessions=Collections.unmodifiableList(sessions); this.progression=progression;
        }
    }

    public static Plan build(Input in) {
        if (in == null) throw new IllegalArgumentException("Input is required");
        if (in.daysPerWeek < 1 || in.daysPerWeek > 7 ||
            in.sessionMinutes < 10 || in.sessionMinutes > 180 ||
            in.age < 18 || in.age > 100 || in.recentRpe < 1 || in.recentRpe > 10 ||
            in.sessionsCompletedThisWeek < 0) throw new IllegalArgumentException("Invalid coaching input");

        if (in.chestPain || in.faintnessOrSevereBreathlessness || in.newNumbnessOrWeakness || in.acuteInjury) {
            String reason = in.chestPain || in.faintnessOrSevereBreathlessness
                ? "Stop exercise and seek urgent medical assessment for chest pain, faintness or severe breathlessness."
                : "Do not start or progress training with an acute injury or new numbness/weakness; seek qualified clinical assessment.";
            return new Plan(true, reason, new ArrayList<Session>(),
                "Resume only after appropriate clinical clearance.");
        }

        int days = Math.min(in.daysPerWeek, in.sessionMinutes < 20 ? 3 : 6);
        int sets = in.level == Level.BEGINNER ? 2 : in.level == Level.INTERMEDIATE ? 3 : 4;
        if (in.age >= 60) sets = Math.min(sets, 2);
        if (in.recentRpe >= 9) sets = Math.max(1, sets - 1);
        int repsMin = in.goal == Goal.STRENGTH ? 4 : in.goal == Goal.MUSCLE ? 6 : 8;
        int repsMax = in.goal == Goal.STRENGTH ? 6 : in.goal == Goal.MUSCLE ? 12 : 15;
        int rest = in.goal == Goal.STRENGTH ? 150 : in.goal == Goal.MUSCLE ? 90 : 60;
        int targetRpe = in.recentRpe >= 9 ? 6 : in.level == Level.BEGINNER ? 6 : 7;
        if (in.age >= 60) { targetRpe = Math.min(targetRpe, 6); rest = Math.max(rest, 90); }

        String[][] templates = {
            {"Squat or sit-to-stand", "Push-up or wall push-up", "Hip hinge", "Plank"},
            {"Row variation", "Glute bridge", "Overhead press variation", "Dead bug"},
            {"Split squat or step-up", "Incline push-up", "Band/dumbbell row", "Bird dog"}
        };
        List<Session> sessions = new ArrayList<>();
        for (int day=0; day<days; day++) {
            List<String> moves = new ArrayList<>();
            String[] template = templates[day % templates.length];
            int count = in.sessionMinutes < 25 ? 3 : 4;
            for (int i=0; i<count; i++) moves.add(adapt(template[i], in.equipment));
            String focus = day % 3 == 0 ? "Full body A" : day % 3 == 1 ? "Full body B" : "Full body C";
            sessions.add(new Session(day+1, focus, moves, sets, repsMin, repsMax, rest, targetRpe));
        }
        String progression = in.recentRpe >= 9
            ? "Recovery adjustment: reduce one set per exercise, keep 3–4 reps in reserve, and do not increase load this week."
            : "When every set reaches the top of the rep range with good technique and target effort, add the smallest available load or 1–2 reps next time.";
        if (in.sessionsCompletedThisWeek > days)
            progression += " Weekly target already met; prioritize recovery rather than adding extra hard sessions.";
        if (in.age >= 60)
            progression += " Use a gradual warm-up, stable supports and comfortable joint range.";
        return new Plan(false, "Stop if sharp/worsening pain, chest pain, faintness or unusual severe breathlessness occurs.", sessions, progression);
    }

    private static String adapt(String exercise, Equipment equipment) {
        if (equipment == Equipment.NONE) {
            if (exercise.startsWith("Band/dumbbell")) return "Prone or self-resisted row";
            if (exercise.startsWith("Overhead press")) return "Pike or wall press (comfortable range)";
            if (exercise.startsWith("Row")) return "Prone W raise";
        } else if (equipment == Equipment.HOME) {
            if (exercise.startsWith("Band/dumbbell")) return "Band or backpack row";
            if (exercise.startsWith("Overhead press")) return "Backpack or band press";
        }
        return exercise;
    }
}
