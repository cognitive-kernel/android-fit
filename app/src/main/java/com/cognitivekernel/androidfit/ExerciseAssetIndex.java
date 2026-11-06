package com.cognitivekernel.androidfit;

import android.content.Context;
import android.content.res.AssetManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The single authoritative index for exercise identity, gender and GIF assets.
 * Gender is read only from exercise_catalog.json; it is never inferred from a filename.
 */
public final class ExerciseAssetIndex {
    private static final String CATALOG_PATH = "exercise_catalog.json";
    private static volatile IndexState cached;

    public static final class ExerciseAsset {
        public final String id;
        public final String gender;
        public final String name;
        public final String file;

        private ExerciseAsset(String id, String gender, String name, String file) {
            this.id = id;
            this.gender = gender;
            this.name = name;
            this.file = file;
        }
    }

    private static final class IndexState {
        final Map<String, ExerciseAsset> byId;
        final Map<String, List<ExerciseAsset>> byName;

        IndexState(Map<String, ExerciseAsset> byId, Map<String, List<ExerciseAsset>> byName) {
            this.byId = Collections.unmodifiableMap(byId);
            Map<String, List<ExerciseAsset>> immutable = new HashMap<>();
            for (Map.Entry<String, List<ExerciseAsset>> entry : byName.entrySet()) {
                immutable.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
            }
            this.byName = Collections.unmodifiableMap(immutable);
        }
    }

    private ExerciseAssetIndex() {}

    public static ExerciseAsset findBest(Context context, String exerciseName, String gender) {
        if (exerciseName == null || exerciseName.trim().isEmpty()) return null;
        String key = normalize(canonicalAlias(exerciseName));
        if (key.isEmpty()) return null;
        List<ExerciseAsset> matches = index(context).byName.get(key);
        if (matches == null || matches.isEmpty()) return null;

        ExerciseAsset best = null;
        int bestRank = -1;
        for (ExerciseAsset candidate : matches) {
            int rank = genderRank(candidate.gender, gender);
            if (rank > bestRank || (rank == bestRank && rank >= 0
                    && best != null && candidate.id.compareTo(best.id) < 0)) {
                best = candidate;
                bestRank = rank;
            }
        }
        return bestRank < 0 ? null : best;
    }

    public static String findFile(Context context, String exerciseName, String gender) {
        ExerciseAsset result = findBest(context, exerciseName, gender);
        return result == null ? null : "exercises/" + result.file;
    }

    public static String findFileById(Context context, String id, String gender) {
        if (id == null || id.trim().isEmpty()) return null;
        ExerciseAsset candidate = index(context).byId.get(id);
        if (candidate == null || genderRank(candidate.gender, gender) < 0) return null;
        return "exercises/" + candidate.file;
    }

    /** All catalog entries in stable ID order; used by the exercise library UI. */
    public static List<ExerciseAsset> all(Context context) {
        ArrayList<ExerciseAsset> result = new ArrayList<>(index(context).byId.values());
        Collections.sort(result, (left, right) -> left.id.compareTo(right.id));
        return Collections.unmodifiableList(result);
    }

    private static int genderRank(String candidate, String requested) {
        if (requested == null || requested.trim().isEmpty()
                || "all".equalsIgnoreCase(requested.trim())) return 1;
        String wanted = requested.trim().toLowerCase(Locale.ROOT);
        if (!"male".equals(wanted) && !"female".equals(wanted)) return -1;
        if (wanted.equals(candidate)) return 2;
        if ("unspecified".equals(candidate)) return 1;
        return -1;
    }

    private static IndexState index(Context context) {
        IndexState result = cached;
        if (result != null) return result;
        synchronized (ExerciseAssetIndex.class) {
            if (cached != null) return cached;
            cached = load(context.getApplicationContext().getAssets());
            return cached;
        }
    }

    private static IndexState load(AssetManager assets) {
        try {
            JSONObject root = new JSONObject(readAsset(assets, CATALOG_PATH));
            JSONArray rows = root.getJSONArray("exercises");
            int declaredCount = root.getInt("count");
            if (declaredCount != rows.length()) {
                throw new IllegalStateException("Exercise catalog count does not match its entries");
            }

            Set<String> bundledFiles = new HashSet<>();
            String[] listed = assets.list("exercises");
            if (listed != null) Collections.addAll(bundledFiles, listed);

            Map<String, ExerciseAsset> byId = new HashMap<>(rows.length() * 2);
            Map<String, List<ExerciseAsset>> byName = new HashMap<>(rows.length() * 2);
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                String id = requiredString(row, "id");
                String name = requiredString(row, "name");
                String gender = requiredString(row, "gender").toLowerCase(Locale.ROOT);
                String file = requiredString(row, "file");

                if (!"male".equals(gender) && !"female".equals(gender)
                        && !"unspecified".equals(gender)) {
                    throw new IllegalStateException("Invalid gender in exercise catalog: " + id);
                }
                if (!id.matches("\\d{3,10}") || !file.toLowerCase(Locale.ROOT).endsWith(".gif")
                        || file.contains("/") || file.contains("\\\\")) {
                    throw new IllegalStateException("Invalid exercise catalog identity: " + id);
                }
                if (!bundledFiles.contains(file)) {
                    throw new IllegalStateException("Missing exercise GIF referenced by catalog: " + file);
                }

                ExerciseAsset asset = new ExerciseAsset(id, gender, name, file);
                if (byId.put(id, asset) != null) {
                    throw new IllegalStateException("Duplicate exercise catalog ID: " + id);
                }
                String nameKey = normalize(name);
                if (nameKey.isEmpty()) {
                    throw new IllegalStateException("Empty normalized exercise name: " + id);
                }
                addName(byName, nameKey, asset);
                String versionlessKey = nameKey.replaceAll("\\s+version\\s+\\d+$", "").trim();
                if (!versionlessKey.isEmpty() && !versionlessKey.equals(nameKey)) {
                    addName(byName, versionlessKey, asset);
                }
                String localizedName = AdvancedFeatures.persianExerciseName(name);
                String localizedKey = normalize(localizedName);
                if (!localizedKey.isEmpty() && !localizedKey.equals(nameKey)) {
                    addName(byName, localizedKey, asset);
                    String localizedVersionlessKey = localizedKey.replaceAll("\\s+version\\s+\\d+$", "").trim();
                    if (!localizedVersionlessKey.isEmpty() && !localizedVersionlessKey.equals(localizedKey)) {
                        addName(byName, localizedVersionlessKey, asset);
                    }
                }
            }
            if (byId.size() != declaredCount) {
                throw new IllegalStateException("Exercise catalog contains duplicate identities");
            }
            return new IndexState(byId, byName);
        } catch (Exception error) {
            throw new IllegalStateException("Could not load authoritative exercise catalog", error);
        }
    }

    private static void addName(Map<String, List<ExerciseAsset>> byName,
                                String key, ExerciseAsset asset) {
        List<ExerciseAsset> named = byName.get(key);
        if (named == null) {
            named = new ArrayList<>();
            byName.put(key, named);
        }
        named.add(asset);
    }

    private static String readAsset(AssetManager assets, String path) throws IOException {
        try (InputStream input = assets.open(path);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toString("UTF-8");
        }
    }

    private static String requiredString(JSONObject object, String key) {
        String value = object.optString(key, "").trim();
        if (value.isEmpty() || "null".equalsIgnoreCase(value)) {
            throw new IllegalStateException("Missing exercise catalog field: " + key);
        }
        return value;
    }

    private static String canonicalAlias(String value) {
        switch (normalize(value)) {
            case "اسکوات": case "squat": return "Air Squat";
            case "شنا سوئدی": case "push up": return "Push-up";
            case "پارویی دمبل": case "dumbbell row": return "Dumbbell Bent Over Row";
            case "لانج": case "lunge": return "Lunge";
            case "پلانک": case "plank": return "Front Plank";
            default: return value;
        }
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("(?i)\\s*\\((male|female)\\)", " ")
                .replaceAll("(?i)\\s+(male|female)$", " ")
                .replaceAll("[_\\-]+", " ")
                .replaceAll("[^a-z0-9\\u0600-\\u06ff ]", " ")
                .replaceAll("\\s+", " ").trim();
    }
}
