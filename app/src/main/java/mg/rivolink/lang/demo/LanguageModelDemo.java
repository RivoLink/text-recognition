package mg.rivolink.lang.demo;

import java.util.List;

import mg.rivolink.lang.helper.LanguageModel;
import mg.rivolink.lang.helper.WeightedLanguageModel;

public class LanguageModelDemo {

    public static void main(String[] args) {
        LanguageModel model = new WeightedLanguageModel();

        System.out.println("=== LanguageModel Demo ===");
        System.out.println();

        // 1. Word Validation
        demonstrateValidation(model);

        // 2. Single Correction
        demonstrateSingleCorrection(model);

        // 3. Multiple Suggestions
        demonstrateMultipleSuggestions(model);

        // 4. Adding Custom Words
        demonstrateCustomWords(model);

        // 5. Sentence Validation
        demonstrateSentenceValidation(model);

        // 6. Dictionary Management
        demonstrateDictionaryManagement(model);
    }

    private static void demonstrateValidation(LanguageModel model) {
        System.out.println("1. Word Validation");
        System.out.println("------------------");

        String[] testWords = {"hello", "world", "wrld", "python", "java"};
        for (String word : testWords) {
            boolean valid = model.isValidWord(word);
            System.out.printf("  '%s' is %s", word, valid ? "VALID" : "INVALID");
            System.out.println();

        }
        System.out.println();
    }

    private static void demonstrateSingleCorrection(LanguageModel model) {
        System.out.println("2. Single Word Correction");
        System.out.println("-------------------------");

        String[] misspelledWords = {"wrld", "teh", "recieve", "occured", "thier"};
        for (String word : misspelledWords) {
            String correction = model.suggestCorrection(word);
            System.out.printf("  '%s' -> '%s'", word, correction);
            System.out.println();
        }
        System.out.println();
    }

    private static void demonstrateMultipleSuggestions(LanguageModel model) {
        System.out.println("3. Multiple Suggestions");
        System.out.println("----------------------");

        String word = "wrld";
        List<String> suggestions = model.getSuggestions(word, 5);

        System.out.printf("  Suggestions for '%s':", word);
        System.out.println();
        for (int i = 0; i < suggestions.size(); i++) {
            System.out.printf("    %d. %s", i + 1, suggestions.get(i));
            System.out.println();
        }
        System.out.println();
    }

    private static void demonstrateCustomWords(LanguageModel model) {
        System.out.println("4. Adding Custom Words");
        System.out.println("---------------------");

        String[] customWords = {"tensorflow", "pytorch", "kubernetes", "blockchain"};
        System.out.println("  Before adding:");
        for (String word : customWords) {
            System.out.printf("    '%s' is %s", word, model.isValidWord(word) ? "VALID" : "INVALID");
            System.out.println();
        }

        System.out.println();
        System.out.println("  Adding custom words...");
        for (String word : customWords) {
            model.addWord(word);
        }

        System.out.println();
        System.out.println("  After adding:");
        for (String word : customWords) {
            System.out.printf("    '%s' is %s", word, model.isValidWord(word) ? "VALID" : "INVALID");
            System.out.println();
        }
        System.out.println();
    }

    private static void demonstrateSentenceValidation(LanguageModel model) {
        System.out.println("5. Sentence Validation");
        System.out.println("---------------------");

        String[] sentences = {
            "This is a correct sentence",
            "Ths sentense has misteaks",
            "I liek to wrte code"
        };

        for (String sentence : sentences) {
            System.out.printf("  Sentence: \"%s\"", sentence);
            System.out.println();

            LanguageModel.ValidationResult result = model.validateSentence(sentence);

            if (result.hasErrors()) {
                System.out.println("  Errors found:");
                for (LanguageModel.ValidationError error : result.getErrors()) {
                    System.out.printf("    - %s", error);
                    System.out.println();
                }
            } else {
                System.out.println("  No errors found");
            }
            System.out.println();
        }
    }

    private static void demonstrateDictionaryManagement(LanguageModel model) {
        System.out.println("6. Dictionary Management");
        System.out.println("-----------------------");

        System.out.printf("  Dictionary size: %d words", model.getDictionarySize());
        System.out.println();

        System.out.printf("  Is empty: %s", model.isEmpty());
        System.out.println();


        // Add a word
        String testWord = "testword";
        System.out.println();
        System.out.printf("  Adding '%s'...", testWord);
        model.addWord(testWord);

        System.out.println();
        System.out.printf("  '%s' is now %s", testWord, model.isValidWord(testWord) ? "VALID" : "INVALID");

        System.out.println();
        System.out.printf("  Dictionary size: %d words", model.getDictionarySize());
        System.out.println();

        // Remove the word
        System.out.println();
        System.out.printf("  Removing '%s'...", testWord);
        boolean removed = model.removeWord(testWord);

        System.out.println();
        System.out.printf("  Removal %s", removed ? "SUCCESSFUL" : "FAILED");

        System.out.println();
        System.out.printf("  '%s' is now %s", testWord, model.isValidWord(testWord) ? "VALID" : "INVALID");

        System.out.println();
        System.out.printf("  Dictionary size: %d words", model.getDictionarySize());

        System.out.println();
        System.out.println();
    }

}
