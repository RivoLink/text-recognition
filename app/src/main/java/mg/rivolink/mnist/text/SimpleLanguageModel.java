package mg.rivolink.mnist.text;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * SimpleLanguageModel - Validates words and suggests corrections
 */
public class SimpleLanguageModel {

    private Set<String> dictionary;
    private static final int MAX_EDIT_DISTANCE = 2;  // Maximum Levenshtein distance for suggestions

    public SimpleLanguageModel() {
        this.dictionary = new HashSet<>();
        initializeDictionary();
    }

    /**
     * Initialize with common English words
     */
    private void initializeDictionary() {
        // Common English words - in production, load from file
        String[] commonWords = {
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
            "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
            "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
            "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
            "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
            "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
            "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
            "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
            "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
            "even", "new", "want", "because", "any", "these", "give", "day", "most", "us"
        };
        dictionary.addAll(Arrays.asList(commonWords));
    }

    /**
     * Check if word is in dictionary
     */
    public boolean isValidWord(String word) {
        return dictionary.contains(word.toLowerCase());
    }

    /**
     * Suggest corrections using Levenshtein distance
     */
    public String suggestCorrection(String word) {
        if (isValidWord(word)) {
            return word;
        }

        String lowerWord = word.toLowerCase();
        String bestMatch = word;
        int minDistance = Integer.MAX_VALUE;

        for (String dictWord : dictionary) {
            int distance = levenshteinDistance(lowerWord, dictWord);
            if (distance < minDistance && distance <= MAX_EDIT_DISTANCE) {
                minDistance = distance;
                bestMatch = dictWord;
            }
        }

        return bestMatch;
    }

    /**
     * Calculate Levenshtein distance between two strings
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j],
                              Math.min(dp[i][j - 1], dp[i - 1][j - 1]));
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    public void addWord(String word) {
        dictionary.add(word.toLowerCase());
    }
}
