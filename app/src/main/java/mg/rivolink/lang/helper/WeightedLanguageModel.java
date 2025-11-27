package mg.rivolink.lang.helper;

import java.io.*;
import java.util.*;

/**
 * Enhanced implementation with word frequency support
 *
 * This implementation extends basic spell checking with word frequency analysis,
 * allowing it to suggest more common words when multiple corrections are equally valid.
 *
 * Example: "teh" could be corrected to "the" (common) or "ten" (less common)
 */
public class WeightedLanguageModel implements LanguageModel {

    private final Set<String> dictionary;
    private final Map<String, Integer> wordFrequency;

    private static final int MAX_EDIT_DISTANCE = 2;
    private static final int DEFAULT_FREQUENCY = 1;

    private static final String DICTIONARY_FILE = "/dictionary-frequency.txt";

    public WeightedLanguageModel() {
        this.dictionary = new HashSet<>();
        this.wordFrequency = new HashMap<>();
        initializeWithFrequencies();
    }

    /**
     * Initialize with common words and their frequencies
     */
    private void initializeWithFrequencies() {
        // Try to load from external file first
        if (loadDictionaryFromResource(DICTIONARY_FILE)) {
            System.out.println("Dictionary loaded successfully from " + DICTIONARY_FILE);
            return;
        }

        // Fallback to embedded dictionary with frequencies
        System.out.println("Using embedded dictionary (basic words with frequencies)");
        loadBasicDictionaryWithFrequencies();
    }

    /**
     * Loads dictionary with frequencies from external file
     * Expected format: word frequency (e.g., "the 1000000" or "the,1000000")
     */
    private boolean loadDictionaryFromResource(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                return false;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    // Skip empty lines and comments
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    // Parse line: "word frequency" or "word,frequency" or "word\tfrequency"
                    String[] parts = line.split("[\\s,\t]+");

                    if (parts.length >= 2) {
                        String word = parts[0].toLowerCase();

                        // Skip words that are too long
                        if (word.length() > 20) {
                            continue;
                        }

                        try {
                            int frequency = Integer.parseInt(parts[1]);
                            addWordWithFrequency(word, frequency);
                        } catch (NumberFormatException e) {
                            // Skip invalid frequency
                            continue;
                        }
                    } else if (parts.length == 1) {
                        // Word without frequency, use default
                        String word = parts[0].toLowerCase();
                        if (word.length() <= 20) {
                            addWordWithFrequency(word, DEFAULT_FREQUENCY);
                        }
                    }
                }
                return true;
            }
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Fallback: Load basic dictionary with estimated frequencies
     */
    private void loadBasicDictionaryWithFrequencies() {
        // Most common words with estimated frequencies
        addWordWithFrequency("the", 1000000);
        addWordWithFrequency("be", 500000);
        addWordWithFrequency("to", 450000);
        addWordWithFrequency("of", 400000);
        addWordWithFrequency("and", 350000);
        addWordWithFrequency("a", 300000);
        addWordWithFrequency("in", 250000);
        addWordWithFrequency("that", 200000);
        addWordWithFrequency("have", 180000);
        addWordWithFrequency("it", 170000);
        addWordWithFrequency("for", 160000);
        addWordWithFrequency("not", 150000);
        addWordWithFrequency("on", 140000);
        addWordWithFrequency("with", 130000);
        addWordWithFrequency("he", 120000);
        addWordWithFrequency("as", 110000);
        addWordWithFrequency("you", 100000);
        addWordWithFrequency("do", 95000);
        addWordWithFrequency("at", 90000);
        addWordWithFrequency("this", 85000);
        addWordWithFrequency("but", 80000);
        addWordWithFrequency("his", 75000);
        addWordWithFrequency("by", 70000);
        addWordWithFrequency("from", 65000);
        addWordWithFrequency("they", 60000);
        addWordWithFrequency("we", 58000);
        addWordWithFrequency("say", 56000);
        addWordWithFrequency("her", 54000);
        addWordWithFrequency("she", 52000);
        addWordWithFrequency("or", 50000);

        // Add more words with default frequency
        String[] additionalWords = {
            "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
            "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
            "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
            "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
            "even", "new", "want", "because", "any", "these", "give", "day", "most", "us"
        };

        for (String word : additionalWords) {
            addWordWithFrequency(word, DEFAULT_FREQUENCY);
        }
    }

    @Override
    public boolean isValidWord(String word) {
        if (word == null) {
            throw new IllegalArgumentException("Word cannot be null");
        }
        if (word.isEmpty()) {
            return false;
        }
        return dictionary.contains(word.toLowerCase());
    }

    @Override
    public String suggestCorrection(String word) {
        if (word == null) {
            throw new IllegalArgumentException("Word cannot be null");
        }
        if (word.isEmpty()) {
            return word;
        }

        String lowerWord = word.toLowerCase();

        // Return original if valid
        if (dictionary.contains(lowerWord)) {
            return word;
        }

        // Find candidates and rank by frequency
        List<CandidateMatch> candidates = findCandidates(lowerWord);

        if (candidates.isEmpty()) {
            return word;
        }

        // Sort by distance first, then by frequency (descending)
        candidates.sort((a, b) -> {
            int distComp = Integer.compare(a.distance, b.distance);
            if (distComp != 0) return distComp;
            return Integer.compare(b.frequency, a.frequency);  // Higher frequency first
        });

        return candidates.get(0).word;
    }

    @Override
    public List<String> getSuggestions(String word, int maxSuggestions) {
        if (word == null) {
            throw new IllegalArgumentException("Word cannot be null");
        }
        if (maxSuggestions < 1) {
            throw new IllegalArgumentException("maxSuggestions must be at least 1");
        }
        if (word.isEmpty()) {
            return Collections.emptyList();
        }

        String lowerWord = word.toLowerCase();
        List<CandidateMatch> candidates = findCandidates(lowerWord);

        // Sort by distance, then frequency
        candidates.sort((a, b) -> {
            int distComp = Integer.compare(a.distance, b.distance);
            if (distComp != 0) return distComp;
            return Integer.compare(b.frequency, a.frequency);
        });

        List<String> suggestions = new ArrayList<>();
        for (int i = 0; i < Math.min(maxSuggestions, candidates.size()); i++) {
            suggestions.add(candidates.get(i).word);
        }

        return suggestions;
    }

    @Override
    public void addWord(String word) {
        if (word == null) {
            throw new IllegalArgumentException("Word cannot be null");
        }
        if (word.isEmpty()) {
            throw new IllegalArgumentException("Word cannot be empty");
        }
        addWordWithFrequency(word.toLowerCase(), DEFAULT_FREQUENCY);
    }

    /**
     * Add word with specific frequency
     */
    public void addWordWithFrequency(String word, int frequency) {
        String lowerWord = word.toLowerCase();
        dictionary.add(lowerWord);
        wordFrequency.put(lowerWord, frequency);
    }

    /**
     * Increment word frequency (useful for learning from user input)
     */
    public void incrementFrequency(String word) {
        String lowerWord = word.toLowerCase();
        if (dictionary.contains(lowerWord)) {
            wordFrequency.merge(lowerWord, 1, Integer::sum);
        }
    }

    /**
     * Get word frequency
     */
    public int getFrequency(String word) {
        return wordFrequency.getOrDefault(word.toLowerCase(), 0);
    }

    @Override
    public int getDictionarySize() {
        return dictionary.size();
    }

    @Override
    public boolean removeWord(String word) {
        if (word == null) {
            throw new IllegalArgumentException("Word cannot be null");
        }
        if (word.isEmpty()) {
            return false;
        }

        String lowerWord = word.toLowerCase();
        boolean removed = dictionary.remove(lowerWord);
        if (removed) {
            wordFrequency.remove(lowerWord);
        }
        return removed;
    }

    /**
     * Find candidate corrections within edit distance
     */
    private List<CandidateMatch> findCandidates(String word) {
        List<CandidateMatch> candidates = new ArrayList<>();

        for (String dictWord : dictionary) {
            // Skip if length difference is too large
            if (Math.abs(dictWord.length() - word.length()) > MAX_EDIT_DISTANCE) {
                continue;
            }

            int distance = levenshteinDistance(word, dictWord);
            if (distance <= MAX_EDIT_DISTANCE) {
                int frequency = wordFrequency.getOrDefault(dictWord, DEFAULT_FREQUENCY);
                candidates.add(new CandidateMatch(dictWord, distance, frequency));
            }
        }

        return candidates;
    }

    /**
     * Optimized Levenshtein distance calculation
     */
    private int levenshteinDistance(String s1, String s2) {
        if (s1.equals(s2)) return 0;

        if (s1.length() > s2.length()) {
            String temp = s1;
            s1 = s2;
            s2 = temp;
        }

        int len1 = s1.length();
        int len2 = s2.length();

        int[] prevRow = new int[len1 + 1];
        int[] currRow = new int[len1 + 1];

        for (int i = 0; i <= len1; i++) {
            prevRow[i] = i;
        }

        for (int j = 1; j <= len2; j++) {
            currRow[0] = j;

            for (int i = 1; i <= len1; i++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;

                currRow[i] = Math.min(
                    Math.min(currRow[i - 1] + 1, prevRow[i] + 1),
                    prevRow[i - 1] + cost
                );
            }

            int[] temp = prevRow;
            prevRow = currRow;
            currRow = temp;
        }

        return prevRow[len1];
    }

    private static class CandidateMatch {

        final String word;
        final int distance;
        final int frequency;

        CandidateMatch(String word, int distance, int frequency) {
            this.word = word;
            this.distance = distance;
            this.frequency = frequency;
        }
    }

}