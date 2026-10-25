package com.cognitivekernel.androidfit;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Conservative name matcher for the offline exercise GIF catalogue. */
final class ExerciseGifMatcher {
    private static final Set<String> PREFIX_VARIANTS = new HashSet<>(Arrays.asList(
            "male", "female", "barbell", "dumbbell", "band", "cable", "machine", "smith",
            "bodyweight", "weighted", "kettlebell", "pvc", "plate", "rope", "standing",
            "seated", "lying", "prone", "supine", "kneeling", "air", "assisted", "supported",
            "version", "v2", "v3", "v4", "2", "3", "4"
    ));
    private static final Set<String> SUFFIX_VARIANTS = new HashSet<>(Arrays.asList(
            "male", "female", "version", "v2", "v3", "v4", "2", "3", "4"
    ));

    private ExerciseGifMatcher() {}

    /** Returns 0 for exact match, a positive score for a conservative prefix/suffix variant, -1 otherwise. */
    static int score(String targetName, String candidateName) {
        List<String> target = words(normalize(targetName));
        List<String> candidate = words(normalize(candidateName));
        if (target.isEmpty() || candidate.isEmpty()) return -1;
        if (target.equals(candidate)) return 0;
        // These catalogue names describe the same base movement and map to the bundled offline demonstrations.
        String normalizedTarget = normalize(targetName);
        String normalizedCandidate = normalize(candidateName);
        if ("plank".equals(normalizedTarget) && "plank on hands".equals(normalizedCandidate)) return 1;
        if ("reverse lunge".equals(normalizedTarget)
                && "bodyweight reverse lunge with overhead".equals(normalizedCandidate)) return 1;
        int start = findSequence(candidate, target);
        if (start < 0) return -1;

        int score = 0;
        for (int i = 0; i < start; i++) {
            String word = candidate.get(i);
            if (!PREFIX_VARIANTS.contains(word)) return -1;
            score++;
        }
        for (int i = start + target.size(); i < candidate.size(); i++) {
            String word = candidate.get(i);
            if (!SUFFIX_VARIANTS.contains(word)) return -1;
            score++;
        }
        return score > 0 ? score : 0;
    }

    private static int findSequence(List<String> candidate, List<String> target) {
        for (int i = 0; i <= candidate.size() - target.size(); i++) {
            boolean matches = true;
            for (int j = 0; j < target.size(); j++) {
                if (!candidate.get(i + j).equals(target.get(j))) {
                    matches = false;
                    break;
                }
            }
            if (matches) return i;
        }
        return -1;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }

    private static List<String> words(String value) {
        return value.isEmpty() ? java.util.Collections.emptyList() : Arrays.asList(value.split(" "));
    }
}
