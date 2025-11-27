package mg.rivolink.lang.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Implementation of LanguageModel interface using dictionary-based spell checking
 * with Levenshtein distance for corrections
 */
public class SimpleLanguageModel implements LanguageModel {

    private final Set<String> dictionary;
    private final Map<Integer, Set<String>> wordsByLength;

    private static final int MAX_EDIT_DISTANCE = 2;
    private static final int MAX_LENGTH_DIFF = MAX_EDIT_DISTANCE;

    private static final String DICTIONARY_FILE = "/dictionary.txt";

    public SimpleLanguageModel() {
        this.dictionary = new HashSet<>();
        this.wordsByLength = new HashMap<>();
        initializeDictionary();
    }

    /**
     * Load dictionary from embedded resource or fallback to common words
     */
    private void initializeDictionary() {
        // Try to load from external file first
        if (loadDictionaryFromResource(DICTIONARY_FILE)) {
            System.out.println("Dictionary loaded successfully from " + DICTIONARY_FILE);
            return;
        }

        // Fallback to embedded basic dictionary
        System.out.println("Using embedded dictionary (basic words)");
        loadBasicDictionary();
    }

    /**
     * Load dictionary from a resource file (one word per line)
     */
    private boolean loadDictionaryFromResource(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                return false;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim().toLowerCase();
                    if (!line.isEmpty() && line.length() <= 20) {
                        addWordInternal(line);
                    }
                }
                return true;
            }
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Fallback basic dictionary with common English words
     */
    private void loadBasicDictionary() {
        String[] commonWords = {
            // Articles & determiners
            "the", "a", "an", "this", "that", "these", "those", "my", "your", "his", "her",
            "its", "our", "their", "some", "any", "all", "both", "each", "every", "no",

            // Pronouns
            "i", "you", "he", "she", "it", "we", "they", "me", "him", "us", "them",
            "what", "which", "who", "whom", "whose", "whoever", "whatever", "whichever",

            // Verbs (common)
            "be", "am", "is", "are", "was", "were", "been", "being",
            "have", "has", "had", "having", "do", "does", "did", "doing",
            "will", "would", "shall", "should", "can", "could", "may", "might", "must",
            "go", "goes", "went", "gone", "going", "get", "gets", "got", "getting",
            "make", "makes", "made", "making", "know", "knows", "knew", "known", "knowing",
            "think", "thinks", "thought", "thinking", "take", "takes", "took", "taken", "taking",
            "see", "sees", "saw", "seen", "seeing", "come", "comes", "came", "coming",
            "want", "wants", "wanted", "wanting", "use", "uses", "used", "using",
            "find", "finds", "found", "finding", "give", "gives", "gave", "given", "giving",
            "tell", "tells", "told", "telling", "work", "works", "worked", "working",
            "call", "calls", "called", "calling", "try", "tries", "tried", "trying",
            "ask", "asks", "asked", "asking", "need", "needs", "needed", "needing",
            "feel", "feels", "felt", "feeling", "become", "becomes", "became", "becoming",
            "leave", "leaves", "left", "leaving", "put", "puts", "putting",

            // Prepositions
            "in", "on", "at", "to", "for", "of", "with", "from", "by", "about",
            "into", "through", "during", "before", "after", "above", "below", "between",
            "under", "over", "against", "among", "within", "without", "throughout",

            // Conjunctions
            "and", "or", "but", "if", "because", "as", "when", "while", "where",
            "although", "though", "unless", "until", "since", "so", "than", "whether",

            // Adverbs
            "not", "now", "just", "very", "too", "also", "here", "there", "then",
            "only", "well", "back", "even", "still", "again", "already", "always",
            "never", "often", "sometimes", "usually", "really", "almost", "quite",

            // Adjectives
            "good", "new", "first", "last", "long", "great", "little", "own", "other",
            "old", "right", "big", "high", "different", "small", "large", "next",
            "early", "young", "important", "few", "public", "bad", "same", "able",

            // Nouns (common)
            "time", "year", "people", "way", "day", "man", "thing", "woman", "life",
            "child", "world", "school", "state", "family", "student", "group", "country",
            "problem", "hand", "part", "place", "case", "week", "company", "system",
            "program", "question", "work", "government", "number", "night", "point",
            "home", "water", "room", "mother", "area", "money", "story", "fact",
            "month", "lot", "right", "study", "book", "eye", "job", "word", "business",
            "issue", "side", "kind", "head", "house", "service", "friend", "father",
            "power", "hour", "game", "line", "end", "member", "law", "car", "city",
            "community", "name", "president", "team", "minute", "idea", "kid", "body",
            "information", "back", "parent", "face", "others", "level", "office"
        };

        for (String word : commonWords) {
            addWordInternal(word);
        }
    }

    /**
     * Check if word is in dictionary (case-insensitive)
     *
     * @throws IllegalArgumentException if word is null
     */
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

    /**
     * Suggest correction using optimized Levenshtein distance
     * Returns the original word if valid, otherwise best match within MAX_EDIT_DISTANCE
     *
     * @throws IllegalArgumentException if word is null
     */
    @Override
    public String suggestCorrection(String word) {
        if (word == null) {
            throw new IllegalArgumentException("Word cannot be null");
        }
        if (word.isEmpty()) {
            return word;
        }

        String lowerWord = word.toLowerCase();

        if (dictionary.contains(lowerWord)) {
            // Return original casing if valid
            return word;
        }

        // Find candidates within reasonable length range
        List<CandidateMatch> candidates = new ArrayList<>();
        int wordLength = lowerWord.length();

        for (int len = Math.max(1, wordLength - MAX_LENGTH_DIFF);
                 len <= wordLength + MAX_LENGTH_DIFF;
                 len++
        ) {
            Set<String> wordsOfLength = wordsByLength.get(len);
            if (wordsOfLength == null) continue;

            for (String dictWord : wordsOfLength) {
                int distance = levenshteinDistance(lowerWord, dictWord);
                if (distance <= MAX_EDIT_DISTANCE) {
                    candidates.add(new CandidateMatch(dictWord, distance));
                }
            }
        }

        if (candidates.isEmpty()) {
            return word;
        }

        // Sort by distance (ascending), then by word length (prefer similar length)
        candidates.sort((a, b) -> {
            int distComp = Integer.compare(a.distance, b.distance);
            if (distComp != 0) return distComp;
            return Integer.compare(
                Math.abs(a.word.length() - wordLength),
                Math.abs(b.word.length() - wordLength)
            );
        });

        return candidates.get(0).word;
    }

    /**
     * Get multiple suggestions for a word
     *
     * @throws IllegalArgumentException if word is null or maxSuggestions < 1
     */
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
        List<CandidateMatch> candidates = new ArrayList<>();
        int wordLength = lowerWord.length();

        for (int len = Math.max(1, wordLength - MAX_LENGTH_DIFF);
                 len <= wordLength + MAX_LENGTH_DIFF;
                 len++
        ) {
            Set<String> wordsOfLength = wordsByLength.get(len);
            if (wordsOfLength == null) continue;

            for (String dictWord : wordsOfLength) {
                int distance = levenshteinDistance(lowerWord, dictWord);
                if (distance <= MAX_EDIT_DISTANCE) {
                    candidates.add(new CandidateMatch(dictWord, distance));
                }
            }
        }

        candidates.sort((a, b) -> {
            int distComp = Integer.compare(a.distance, b.distance);
            if (distComp != 0) return distComp;
            return Integer.compare(
                Math.abs(a.word.length() - wordLength),
                Math.abs(b.word.length() - wordLength)
            );
        });

        List<String> suggestions = new ArrayList<>();
        for (int i = 0; i < Math.min(maxSuggestions, candidates.size()); i++) {
            suggestions.add(candidates.get(i).word);
        }

        return suggestions;
    }

    /**
     * Space-optimized Levenshtein distance using only two rows
     */
    private int levenshteinDistance(String s1, String s2) {
        // Early exit for identical strings
        if (s1.equals(s2)) return 0;

        // Ensure s1 is the shorter string for memory efficiency
        if (s1.length() > s2.length()) {
            String temp = s1;
            s1 = s2;
            s2 = temp;
        }

        int len1 = s1.length();
        int len2 = s2.length();

        // Early exit if difference is too large
        if (Math.abs(len1 - len2) > MAX_EDIT_DISTANCE) {
            return MAX_EDIT_DISTANCE + 1;
        }

        // Use only two rows instead of full matrix
        int[] prevRow = new int[len1 + 1];
        int[] currRow = new int[len1 + 1];

        // Initialize first row
        for (int i = 0; i <= len1; i++) {
            prevRow[i] = i;
        }

        // Calculate distances
        for (int j = 1; j <= len2; j++) {
            currRow[0] = j;

            for (int i = 1; i <= len1; i++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;

                currRow[i] = Math.min(
                    Math.min(currRow[i - 1] + 1,       // insertion
                             prevRow[i] + 1),          // deletion
                    prevRow[i - 1] + cost              // substitution
                );
            }

            // Swap rows
            int[] temp = prevRow;
            prevRow = currRow;
            currRow = temp;
        }

        return prevRow[len1];
    }

    /**
     * Add word to dictionary
     *
     * @throws IllegalArgumentException if word is null or empty
     */
    @Override
    public void addWord(String word) {
        if (word == null) {
            throw new IllegalArgumentException("Word cannot be null");
        }
        if (word.isEmpty()) {
            throw new IllegalArgumentException("Word cannot be empty");
        }
        addWordInternal(word.toLowerCase());
    }

    /**
     * Internal method to add word to both dictionary and length index
     */
    private void addWordInternal(String word) {
        dictionary.add(word);
        wordsByLength.computeIfAbsent(word.length(), k -> new HashSet<>()).add(word);
    }

    /**
     * Get dictionary size
     */
    @Override
    public int getDictionarySize() {
        return dictionary.size();
    }

    /**
     * Remove word from dictionary
     *
     * @throws IllegalArgumentException if word is null
     */
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
            Set<String> wordsOfLength = wordsByLength.get(lowerWord.length());
            if (wordsOfLength != null) {
                wordsOfLength.remove(lowerWord);
                if (wordsOfLength.isEmpty()) {
                    wordsByLength.remove(lowerWord.length());
                }
            }
        }

        return removed;
    }

    private static class CandidateMatch {

        final String word;
        final int distance;

        CandidateMatch(String word, int distance) {
            this.word = word;
            this.distance = distance;
        }
    }

}
