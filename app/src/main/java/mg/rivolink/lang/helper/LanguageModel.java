package mg.rivolink.lang.helper;

import java.util.List;

/**
 * Interface for language processing and spell checking
 */
public interface LanguageModel {

    /**
     * Validates whether a word exists in the language model's dictionary
     *
     * @param word The word to validate (case-insensitive)
     * @return true if the word is recognized as valid, false otherwise
     * @throws IllegalArgumentException if word is null
     */
    boolean isValidWord(String word);

    /**
     * Suggests the best correction for a misspelled word
     *
     * If the word is already valid, returns it unchanged.
     * Otherwise, returns the most likely correction based on edit distance
     * and frequency (if available).
     *
     * @param word The word to correct
     * @return The suggested correction, or the original word if no better match found
     * @throws IllegalArgumentException if word is null
     */
    String suggestCorrection(String word);

    /**
     * Returns multiple spelling suggestions for a word
     *
     * Provides a ranked list of possible corrections, ordered by likelihood.
     * Useful for presenting multiple options to users.
     *
     * @param word The word to get suggestions for
     * @param maxSuggestions Maximum number of suggestions to return
     * @return List of suggested corrections (may be empty if no suggestions found)
     * @throws IllegalArgumentException if word is null or maxSuggestions < 1
     */
    List<String> getSuggestions(String word, int maxSuggestions);

    /**
     * Adds a new word to the language model's dictionary
     *
     * This allows the model to learn new vocabulary or domain-specific terms.
     * The word is typically stored in lowercase for case-insensitive matching.
     *
     * @param word The word to add to the dictionary
     * @throws IllegalArgumentException if word is null or empty
     */
    void addWord(String word);

    /**
     * Returns the total number of words in the dictionary
     *
     * @return The size of the dictionary
     */
    int getDictionarySize();

    /**
     * Removes a word from the language model's dictionary
     *
     * @param word The word to remove
     * @return true if the word was present and removed, false otherwise
     * @throws IllegalArgumentException if word is null
     */
    default boolean removeWord(String word) {
        throw new UnsupportedOperationException("removeWord is not supported by this implementation");
    }

    /**
     * Checks if the dictionary contains any words
     *
     * @return true if dictionary is empty, false otherwise
     */
    default boolean isEmpty() {
        return getDictionarySize() == 0;
    }

    /**
     * Validates a sentence and returns information about misspelled words
     *
     * @param sentence The sentence to validate
     * @return ValidationResult containing information about errors
     * @throws IllegalArgumentException if sentence is null
     */
    default ValidationResult validateSentence(String sentence) {
        if (sentence == null) {
            throw new IllegalArgumentException("Sentence cannot be null");
        }

        String[] words = sentence.split("\\s+");
        ValidationResult result = new ValidationResult();

        for (String word : words) {
            // Remove punctuation for validation
            String cleanWord = word.replaceAll("[^a-zA-Z]", "");
            if (!cleanWord.isEmpty() && !isValidWord(cleanWord)) {
                result.addError(word, suggestCorrection(cleanWord));
            }
        }

        return result;
    }

    class ValidationResult {

        private final List<ValidationError> errors;

        public ValidationResult() {
            this.errors = new java.util.ArrayList<>();
        }

        public void addError(String original, String suggestion) {
            errors.add(new ValidationError(original, suggestion));
        }

        public List<ValidationError> getErrors() {
            return java.util.Collections.unmodifiableList(errors);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public int getErrorCount() {
            return errors.size();
        }

        @Override
        public String toString() {
            if (errors.isEmpty()) {
                return "No errors found";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Found ").append(errors.size()).append(" error(s):\n");
            for (ValidationError error : errors) {
                sb.append("  - ").append(error).append("\n");
            }
            return sb.toString();
        }
    }

    class ValidationError {

        private final String originalWord;
        private final String suggestedCorrection;

        public ValidationError(String originalWord, String suggestedCorrection) {
            this.originalWord = originalWord;
            this.suggestedCorrection = suggestedCorrection;
        }

        public String getOriginalWord() {
            return originalWord;
        }

        public String getSuggestedCorrection() {
            return suggestedCorrection;
        }

        @Override
        public String toString() {
            return String.format("'%s' -> '%s'", originalWord, suggestedCorrection);
        }
    }

}
