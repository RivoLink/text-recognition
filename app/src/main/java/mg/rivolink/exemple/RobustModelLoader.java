package mg.rivolink.exemple;

import java.io.IOException;

import mg.rivolink.mnist.model.MNISTPredictor;

/**
 * Example with error handling and model validation
 */
class RobustModelLoader {

    public static void main(String[] args) {
        System.out.println("=== Robust Model Loading Example ===\n");

        String modelPath = "models/emnist_model.ser";

        try {
            // Check if model exists
            if (!ModelLoader.modelExists(modelPath)) {
                System.err.println("Model file not found: " + modelPath);
                System.err.println("Please train the model first:");
                System.err.println("  ./gradlew downloadEmnist");
                System.err.println("  ./gradlew trainEmnist");
                return;
            }

            // Load model
            System.out.println("Loading model...");
            MNISTPredictor predictor = ModelLoader.loadEMNISTModel(modelPath);
            System.out.println("Model loaded successfully!\n");

            // Test with multiple samples
            System.out.println("Testing predictions:");
            float[][] testImages = {
                createTestImage(0),  // Digit pattern
                createTestImage(1),  // Letter pattern
                createTestImage(2)   // Another pattern
            };

            for (int i = 0; i < testImages.length; i++) {
                MNISTPredictor.PredictionResult result = 
                    predictor.predictWithConfidence(testImages[i]);
                System.out.println("Test " + (i+1) + ": " + result);
            }

        } catch (IOException e) {
            System.err.println("IO Error: " + e.getMessage());
            System.err.println("Make sure the model file exists and is readable");
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: " + e.getMessage());
            System.err.println("Model file may be corrupted");
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static float[] createTestImage(int pattern) {
        float[] image = new float[784];
        // Create different patterns for testing
        switch (pattern) {
            case 0: // Vertical line
                for (int y = 5; y < 23; y++) {
                    image[y * 28 + 14] = 1.0f;
                }
                break;
            case 1: // Horizontal line
                for (int x = 5; x < 23; x++) {
                    image[14 * 28 + x] = 1.0f;
                }
                break;
            case 2: // Circle
                int cx = 14, cy = 14, r = 8;
                for (int angle = 0; angle < 360; angle += 5) {
                    int x = cx + (int)(r * Math.cos(Math.toRadians(angle)));
                    int y = cy + (int)(r * Math.sin(Math.toRadians(angle)));
                    if (x >= 0 && x < 28 && y >= 0 && y < 28) {
                        image[y * 28 + x] = 1.0f;
                    }
                }
                break;
        }
        return image;
    }
}
