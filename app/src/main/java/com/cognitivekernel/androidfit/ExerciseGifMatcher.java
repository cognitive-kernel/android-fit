package com.cognitivekernel.androidfit;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Conservative name matcher for the offline exercise GIF catalogue. */
final class ExerciseGifMatcher {
    private static final Set<String> ALLOWED = new HashSet<>(Arrays.asList("male", "female", "version", "2", "3", "4", "barbell", "dumbbell", "band", "cable", "machine", "smith", "bodyweight", "weighted", "kettlebell", "pvc", "plate", "rope", "standing", "seated", "lying", "prone", "supine", "kneeling", "bent", "over", "wide", "close", "grip", "low", "high", "incline", "decline", "front", "rear", "back", "reverse", "single", "one", "two", "alternate", "alternating", "arm", "arms", "leg", "legs", "hand", "hands", "elbow", "elbows", "foot", "feet", "toe", "heel", "side", "each", "parallel", "raised", "deep", "partial", "half", "straight", "with", "without", "using", "on", "at", "by", "from", "the", "of", "an", "a", "exercise", "stability", "medicine", "ball", "chair", "box", "bench", "wall", "floor", "mat", "support", "upright", "extension", "flexion", "rotation", "rotating", "assisted", "supported", "position"));
    private ExerciseGifMatcher() {}
    static int score(String targetName, String candidateName) {
        String target = normalize(targetName), candidate = normalize(candidateName);
        if (target.isEmpty() || candidate.isEmpty()) return -1;
        if (target.equals(candidate)) return 0;
        Set<String> targetWords = words(target), candidateWords = words(candidate);
        if (!candidateWords.containsAll(targetWords)) return -1;
        int extras = 0;
        for (String word : candidateWords) {
            if (targetWords.contains(word)) continue;
            if (!ALLOWED.contains(word)) return -1;
            extras++;
        }
        return extras == 0 ? 0 : extras;
    }
    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }
    private static Set<String> words(String value) { return new HashSet<>(Arrays.asList(value.split(" "))); }
}
