package mg.rivolink.exemple;

import mg.rivolink.ai.Network;
import mg.rivolink.mnist.model.MNISTPredictor;
import mg.rivolink.mnist.model.MNISTTrainer;
import mg.rivolink.mnist.text.TextRecognizer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Example: Load a trained model and use it for predictions
 */
public class LoadModelExample {

    public static void main(String[] args) {
        System.out.println("=== Load Trained Model Example ===\n");

        try {
            // Method 1: Load EMNIST model directly
            loadAndUseEMNISTModel();

            System.out.println("\n" + "=".repeat(50) + "\n");

            // Method 2: Load with helper method
            useLoadedModelForTextRecognition();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Method 1: Load and use EMNIST model directly
     */
    private static void loadAndUseEMNISTModel() throws IOException, ClassNotFoundException {
        System.out.println("Method 1: Load EMNIST Model Directly\n");

        // Load the trained network from file
        String modelPath = "models/emnist_model.ser";
        System.out.println("Loading model from: " + modelPath);

        Network network = loadNetworkFromFile(modelPath);
        System.out.println("Model loaded successfully!\n");

        // Create predictor with the loaded network
        MNISTPredictor predictor = new MNISTPredictor(
            network, 
            MNISTTrainer.DatasetType.EMNIST
        );

        // Test with a sample image (28x28 = 784 pixels)
        System.out.println("Testing predictions:");
        float[] sampleImage = createSampleDigit();

        // Predict
        MNISTPredictor.PredictionResult result = predictor.predictWithConfidence(sampleImage);

        System.out.println("Predicted: " + result.prediction);
        System.out.println("Confidence: " + String.format("%.2f%%", result.confidence * 100));
        System.out.println("\nTop 5 predictions:");
        showTopPredictions(result.allProbabilities, 5);
    }

    /**
     * Method 2: Use loaded model with TextRecognizer
     */
    private static void useLoadedModelForTextRecognition() throws IOException, ClassNotFoundException {
        System.out.println("Method 2: Use Model with TextRecognizer\n");

        // Load model
        Network network = loadNetworkFromFile("models/emnist_model.ser");

        // Create predictor
        MNISTPredictor predictor = new MNISTPredictor(
            network, 
            MNISTTrainer.DatasetType.EMNIST
        );

        // Create text recognizer
        TextRecognizer textRecognizer = new TextRecognizer(predictor);

        // Create a simple word image
        float[] wordImage = createWordImageSimple("HELLO", 140, 28);

        // Recognize text
        TextRecognizer.RecognitionResult result = 
            textRecognizer.recognizeText(wordImage, 140, 28);

        System.out.println("Recognition results:");
        System.out.println("  Raw text: " + result.rawText);
        System.out.println("  Corrected: " + result.correctedText);
        System.out.println("  Confidence: " + String.format("%.2f%%", result.averageConfidence * 100));
    }

    /**
     * Load Network from serialized file
     */
    public static Network loadNetworkFromFile(String filepath) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filepath))) {
            return (Network) ois.readObject();
        }
    }

    /**
     * Helper: Create sample digit image
     */
    private static float[] createSampleDigit() {
        float[] image = new float[784];
        // Draw a simple "1" shape
        for (int y = 6; y < 22; y++) {
            image[y * 28 + 14] = 1.0f;  // Vertical line
        }
        for (int i = 0; i < 4; i++) {
            image[(6 + i) * 28 + (14 - i)] = 1.0f;  // Top diagonal
        }
        return image;
    }

    /**
     * Helper: Create simple word image
     */
    private static float[] createWordImageSimple(String word, int width, int height) {
        float[] image = new float[width * height];
        int x = 10;
        for (int i = 0; i < word.length(); i++) {
            drawSimpleChar(image, width, height, x, 4, 15, 20);
            x += 25;
        }
        return image;
    }

    /**
     * Helper: Draw simple character shape
     */
    private static void drawSimpleChar(float[] img, int w, int h, int x, int y, int cw, int ch) {
        for (int dy = 0; dy < ch; dy++) {
            for (int dx = 0; dx < cw; dx++) {
                int px = x + dx;
                int py = y + dy;
                if (px < w && py < h) {
                    img[py * w + px] = (dx == 0 || dy == 0 || dy == ch-1) ? 1.0f : 0.0f;
                }
            }
        }
    }

    /**
     * Helper: Show top N predictions
     */
    private static void showTopPredictions(float[] probabilities, int topN) {
        // Create array of indices
        Integer[] indices = new Integer[probabilities.length];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }

        // Sort by probability (descending)
        java.util.Arrays.sort(indices, (a, b) -> 
            Float.compare(probabilities[b], probabilities[a])
        );

        // Show top N
        for (int i = 0; i < Math.min(topN, indices.length); i++) {
            int idx = indices[i];
            String label = indexToLabel(idx);
            System.out.println("  " + (i+1) + ". " + label + " - " + 
                String.format("%.2f%%", probabilities[idx] * 100));
        }
    }

    /**
     * Helper: Convert index to EMNIST label
     */
    private static String indexToLabel(int index) {
        if (index >= 0 && index <= 9) {
            return String.valueOf(index);  // Digit
        } else if (index >= 10 && index <= 35) {
            return String.valueOf((char)('A' + (index - 10)));  // Uppercase
        } else if (index >= 36 && index <= 61) {
            return String.valueOf((char)('a' + (index - 36)));  // Lowercase
        }
        return "?";
    }
}
