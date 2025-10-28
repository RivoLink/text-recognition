package mg.rivolink.mnist.text;

import java.util.ArrayList;
import java.util.List;

import mg.rivolink.mnist.model.MNISTPredictor;

/**
 * TextRecognizer - Combines character detection, segmentation, and language model
 */
public class TextRecognizer {

    private MNISTPredictor charPredictor;
    private SimpleLanguageModel languageModel;

    public TextRecognizer(MNISTPredictor charPredictor, SimpleLanguageModel languageModel) {
        this.charPredictor = charPredictor;
        this.languageModel = languageModel;
    }

    public TextRecognizer(MNISTPredictor charPredictor) {
        this(charPredictor, new SimpleLanguageModel());
    }

    /**
     * RecognitionResult - Contains recognized text and confidence scores
     */
    public static class RecognitionResult {
        public String rawText;      // Raw character predictions
        public String correctedText; // After spell correction
        public List<CharDetail> charDetails;
        public double averageConfidence;

        public RecognitionResult(String rawText, String correctedText,
                               List<CharDetail> charDetails, double avgConfidence) {
            this.rawText = rawText;
            this.correctedText = correctedText;
            this.charDetails = charDetails;
            this.averageConfidence = avgConfidence;
        }

        @Override
        public String toString() {
            return "TextRecognition{\n" +
                   "  Raw: " + rawText + "\n" +
                   "  Corrected: " + correctedText + "\n" +
                   "  Confidence: " + String.format("%.2f%%", averageConfidence * 100) + "\n" +
                   "}";
        }
    }

    public static class CharDetail {
        public String character;
        public float confidence;
        public ImageSegmenter.CharacterBound bound;

        public CharDetail(String character, float confidence, ImageSegmenter.CharacterBound bound) {
            this.character = character;
            this.confidence = confidence;
            this.bound = bound;
        }
    }

    /**
     * Recognizes text from a flattened image (784 pixels for single char, or larger for text)
     */
    // public RecognitionResult recognizeText(float[] imagePixels, int imageWidth, int imageHeight) {
    //     // Reshape flat array to 2D
    //     float[][] image2D = new float[imageHeight][imageWidth];
    //     for (int i = 0; i < imagePixels.length; i++) {
    //         image2D[i / imageWidth][i % imageWidth] = imagePixels[i];
    //     }

    //     return recognizeTextFromImage(image2D, imageWidth, imageHeight);
    // }
    public RecognitionResult recognizeText(float[] imagePixels, int imageWidth, int imageHeight) {
        // Pas besoin de conversion, utilisez directement
        return recognizeTextFromImage(imagePixels, imageWidth, imageHeight);
    }

    /**
     * Main text recognition pipeline
     */
    public RecognitionResult recognizeTextFromImage(float[] image, int imageWidth, int imageHeight) {
        // Step 1: Segment image into individual characters
        List<ImageSegmenter.CharacterBound> charBounds = 
            ImageSegmenter.segmentCharacters(image, imageWidth, imageHeight);

        if (charBounds.isEmpty()) {
            return new RecognitionResult("", "", new ArrayList<>(), 0);
        }

        // Step 2: Recognize each character
        StringBuilder rawText = new StringBuilder();
        StringBuilder correctedText = new StringBuilder();
        List<CharDetail> charDetails = new ArrayList<>();
        double totalConfidence = 0;

        for (ImageSegmenter.CharacterBound bound : charBounds) {
            // Extract 28x28 pixels for EMNIST model
            float[] charPixels = ImageSegmenter.extractCharacterPixels(image, imageWidth, bound);

            // Predict character
            MNISTPredictor.PredictionResult prediction = 
                charPredictor.predictWithConfidence(charPixels);

            String character = prediction.prediction;
            rawText.append(character);
            totalConfidence += prediction.confidence;

            CharDetail detail = new CharDetail(character, prediction.confidence, bound);
            charDetails.add(detail);
        }

        // Step 3: Segment into words and apply spell correction
        String[] words = rawText.toString().split("\\s+");
        for (String word : words) {
            if (!word.isEmpty()) {
                String corrected = languageModel.suggestCorrection(word);
                if (correctedText.length() > 0) {
                    correctedText.append(" ");
                }
                correctedText.append(corrected);
            }
        }

        double averageConfidence = totalConfidence / charDetails.size();
        return new RecognitionResult(rawText.toString(), correctedText.toString(),
                                   charDetails, averageConfidence);
    }

    /**
     * For recognizing a single character (28x28)
     */
    public String recognizeSingleCharacter(float[] pixels28x28) {
        MNISTPredictor.PredictionResult result = charPredictor.predictWithConfidence(pixels28x28);
        return result.prediction;
    }
}
